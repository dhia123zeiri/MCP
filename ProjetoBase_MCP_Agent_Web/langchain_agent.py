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
from langgraph.prebuilt import create_react_agent
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
    """Safely extract plain text from various LangChain message content formats."""
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


async def load_system_prompt(session: ClientSession) -> str:
    """Load the MCP prompt and inject resource context into the system instruction."""
    system = ""

    # 1. Load the MCP prompt template
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

    # 2. Discover and inject all resources dynamically.
    # Uses URIs returned by list_resources() to avoid hardcoding issues
    # (Spring Boot MCP library appends a trailing slash to file:// URIs).
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

            # Load system prompt (with resources injected)
            system_instruction = await load_system_prompt(session)

            # Load tools from MCP server
            tools = await load_mcp_tools(session)
            tool_names = [t.name for t in tools]
            print(f"✅ Tools loaded ({len(tools)}): {tool_names}")

            # Build the LangGraph ReAct agent
            agent = create_react_agent(
                model=llm,
                tools=tools,
                prompt=system_instruction,
                checkpointer=memory,
            )
            print("✅ Agent ready.\n")

            yield  # Server is running — SSE session stays alive

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
    """Send a message to the agent and receive a reply."""
    if agent is None:
        raise HTTPException(status_code=503, detail="Agent not yet initialized.")

    try:
        inputs = {"messages": [HumanMessage(content=req.message)]}
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
                print(f"\n✅ TOOL RESULT [{msg.name}]: {msg.content}")

            elif msg.type == "ai" and msg.content:
                final_response = extract_text(msg.content)
                print(f"\n💬 AI: {final_response}")

        return ChatResponse(reply=final_response)

    except Exception as e:
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"Agent error: {str(e)}")


@app.post("/reset")
async def reset():
    """Clear conversation memory by switching to a new thread."""
    global thread_config
    thread_config = new_thread_config()
    return {"status": "Conversation history cleared.", "thread_id": thread_config["configurable"]["thread_id"]}


@app.get("/health")
async def health():
    return {
        "status": "ok",
        "agent_ready": agent is not None,
        "mcp_url": MCP_SERVER_URL,
    }


@app.get("/tools")
async def list_tools():
    """List the tool names loaded from the MCP server (for debugging)."""
    if agent is None:
        raise HTTPException(status_code=503, detail="Agent not ready.")
    tool_names = [t.name for t in agent.tools] if hasattr(agent, "tools") else []
    return {"tools": tool_names}


# ── ENTRY POINT ───────────────────────────────────────────────────────────────
if __name__ == "__main__":
    import uvicorn
    uvicorn.run("langchain_agent:app", host="0.0.0.0", port=8000, reload=False)