import os
import uuid
import traceback
from contextlib import asynccontextmanager
from dotenv import load_dotenv

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

# LangChain & LangGraph
from langchain_google_genai import ChatGoogleGenerativeAI
from langchain_core.messages import HumanMessage
from langgraph.prebuilt import create_react_agent, ToolNode   # ← added ToolNode
from langgraph.checkpoint.memory import InMemorySaver

# MCP Adapters
from mcp.client.sse import sse_client
from mcp.client.session import ClientSession
from langchain_mcp_adapters.tools import load_mcp_tools

# ── ENV ───────────────────────────────────────────────────────────────────────
load_dotenv()

GOOGLE_API_KEY = os.getenv("GOOGLE_API_KEY")
if not GOOGLE_API_KEY:
    raise ValueError("GOOGLE_API_KEY is not set in .env")

MCP_SERVER_URL = os.getenv("MCP_SERVER_URL", "http://localhost:8080/sse")

# ── LLM ───────────────────────────────────────────────────────────────────────
llm = ChatGoogleGenerativeAI(
    model="gemini-2.5-flash",
    google_api_key=GOOGLE_API_KEY,
    temperature=0.2,
)

# ── MEMORY ────────────────────────────────────────────────────────────────────
memory = InMemorySaver()


def new_thread_config() -> dict:
    return {"configurable": {"thread_id": str(uuid.uuid4())}}


# Global state
agent = None
thread_config = new_thread_config()


# ── REQUEST/RESPONSE MODELS ───────────────────────────────────────────────────
class ChatRequest(BaseModel):
    message: str


class ChatResponse(BaseModel):
    reply: str


# ── HELPERS ───────────────────────────────────────────────────────────────────
def extract_text(content) -> str:
    if isinstance(content, str):
        return content
    if isinstance(content, list):
        parts = [
            block["text"]
            for block in content
            if isinstance(block, dict) and block.get("type") == "text" and block.get("text")
        ]
        return " ".join(parts)
    return str(content)


def is_invalid_chat_history_error(e: Exception) -> bool:
    msg = str(e)
    return (
        "INVALID_CHAT_HISTORY" in msg
        or "do not have a corresponding ToolMessage" in msg
    )


async def run_agent(message: str) -> str:
    global thread_config

    inputs = {"messages": [HumanMessage(content=message)]}

    try:
        return await _stream_agent(inputs)

    except Exception as e:
        if is_invalid_chat_history_error(e):
            print(
                f"\n⚠️  INVALID_CHAT_HISTORY detected — resetting thread and retrying.\n"
                f"   Old thread: {thread_config['configurable']['thread_id']}"
            )
            thread_config = new_thread_config()
            print(f"   New thread: {thread_config['configurable']['thread_id']}")
            try:
                return await _stream_agent(inputs)
            except Exception as retry_exc:
                traceback.print_exc()
                raise HTTPException(
                    status_code=500,
                    detail=f"Agent error after thread reset: {str(retry_exc)}"
                )
        else:
            traceback.print_exc()
            raise HTTPException(status_code=500, detail=f"Agent error: {str(e)}")


async def _stream_agent(inputs: dict) -> str:
    final_response = ""

    async for event in agent.astream(
        inputs,
        config=thread_config,
        stream_mode="values",
    ):
        msg = event["messages"][-1]

        if msg.type == "ai" and msg.tool_calls:
            for call in msg.tool_calls:
                print(f"\n🔧 TOOL CALL : {call['name']}")
                print(f"   Args      : {call['args']}")

        elif msg.type == "tool":
            is_error = getattr(msg, "status", None) == "error"
            icon = "❌" if is_error else "✅"
            print(f"\n{icon} TOOL RESULT [{msg.name}]: {msg.content}")

        elif msg.type == "ai" and msg.content:
            final_response = extract_text(msg.content)
            print(f"\n💬 AI: {final_response}")

    return final_response


async def load_system_prompt(session: ClientSession) -> str:
    system = ""

    try:
        prompt_data = await session.get_prompt(
            "health_advisor_prompt",
            arguments={"user_name": "Visitor"},
        )
        system = prompt_data.messages[0].content.text
        print("✅ Prompt loaded from MCP server.")
    except Exception as e:
        print(f"⚠️  Could not load MCP prompt: {e}")
        system = (
            "You are a helpful assistant for a doctor's office. "
            "You can manage doctors and pets in the system. "
            "Always confirm before performing any create, update, or delete operations."
        )

    try:
        resources_list = await session.list_resources()
        for res in resources_list.resources:
            uri = res.uri
            try:
                data = await session.read_resource(uri)
                text = data.contents[0].text
                label = res.name or uri
                system += f"\n\n--- {label} ---\n{text}"
                print(f"✅ Resource [{uri}] loaded.")
            except Exception as e:
                print(f"⚠️  Could not read resource [{uri}]: {e}")
    except Exception as e:
        print(f"⚠️  Could not list resources: {e}")

    return system


# ── LIFESPAN ──────────────────────────────────────────────────────────────────
@asynccontextmanager
async def lifespan(app: FastAPI):
    global agent

    print(f"\n🔗 Connecting to MCP server at {MCP_SERVER_URL} ...")

    async with sse_client(MCP_SERVER_URL) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()
            print("✅ MCP session initialized.")

            system_instruction = await load_system_prompt(session)

            tools = await load_mcp_tools(session)
            tool_names = [t.name for t in tools]
            print(f"✅ Tools loaded ({len(tools)}): {tool_names}")

            # ── KEY FIX ───────────────────────────────────────────────────────
            # langchain_mcp_adapters raises ToolException for every isError=true
            # result from the Java MCP server. In newer LangGraph versions,
            # handle_tool_errors was removed from create_react_agent and moved
            # to ToolNode. Passing ToolNode(handle_tool_errors=True) means
            # LangGraph catches ToolException and wraps it in a proper
            # ToolMessage, keeping the chat history valid and letting the agent
            # relay the error message conversationally instead of crashing.
            tool_node = ToolNode(tools, handle_tool_errors=True)

            agent = create_react_agent(
                model=llm,
                tools=tool_node,        # ← pass ToolNode, not raw list
                prompt=system_instruction,
                checkpointer=memory,
            )
            print("✅ Agent ready.\n")

            yield

    print("🛑 MCP session closed. Shutting down.")


# ── APP ───────────────────────────────────────────────────────────────────────
app = FastAPI(
    title="Doctor Office AI Agent",
    description="LangChain agent backed by a Spring Boot MCP server",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ── ROUTES ────────────────────────────────────────────────────────────────────
@app.post("/chat", response_model=ChatResponse)
async def chat(req: ChatRequest):
    if agent is None:
        raise HTTPException(status_code=503, detail="Agent not yet initialized.")
    reply = await run_agent(req.message)
    return ChatResponse(reply=reply)


@app.post("/reset")
async def reset():
    global thread_config
    thread_config = new_thread_config()
    return {
        "status": "Conversation history cleared.",
        "thread_id": thread_config["configurable"]["thread_id"]
    }


@app.get("/health")
async def health():
    return {
        "status": "ok",
        "agent_ready": agent is not None,
        "mcp_url": MCP_SERVER_URL,
    }


@app.get("/tools")
async def list_tools():
    if agent is None:
        raise HTTPException(status_code=503, detail="Agent not ready.")
    tool_names = [t.name for t in agent.tools] if hasattr(agent, "tools") else []
    return {"tools": tool_names}


# ── ENTRY POINT ───────────────────────────────────────────────────────────────
if __name__ == "__main__":
    import uvicorn
    uvicorn.run("langchain_agent:app", host="0.0.0.0", port=8000, reload=False)