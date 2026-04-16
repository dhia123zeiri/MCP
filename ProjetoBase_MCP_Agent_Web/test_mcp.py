"""
test_mcp.py
-----------
Run this BEFORE starting agent.py to verify that your Spring Boot MCP
server is reachable and exposes the expected tools / resources / prompts.

Usage:
    python test_mcp.py
"""

import asyncio
from mcp.client.sse import sse_client
from mcp.client.session import ClientSession

MCP_URL = "http://localhost:8080/sse"


async def main():
    print(f"🔗 Connecting to {MCP_URL} ...\n")

    async with sse_client(MCP_URL) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()
            print("✅ Session initialized.\n")

            # ── Tools ──────────────────────────────────────────────────────
            tools_result = await session.list_tools()
            print(f"🔧 Tools ({len(tools_result.tools)}):")
            for t in tools_result.tools:
                print(f"   • {t.name}: {t.description}")

            # ── Resources ─────────────────────────────────────────────────
            resources_result = await session.list_resources()
            print(f"\n📄 Resources ({len(resources_result.resources)}):")
            for r in resources_result.resources:
                print(f"   • {r.uri}: {r.description}")

            # ── Read each resource ─────────────────────────────────────────
            for r in resources_result.resources:
                try:
                    data = await session.read_resource(r.uri)
                    preview = data.contents[0].text[:200].replace("\n", " ")
                    print(f"   ↳ [{r.uri}] preview: {preview}...")
                except Exception as e:
                    print(f"   ↳ [{r.uri}] ERROR: {e}")

            # ── Prompts ────────────────────────────────────────────────────
            prompts_result = await session.list_prompts()
            print(f"\n💬 Prompts ({len(prompts_result.prompts)}):")
            for p in prompts_result.prompts:
                print(f"   • {p.name}: {p.description}")

            # ── Test the prompt ────────────────────────────────────────────
            try:
                pd = await session.get_prompt(
                    "health_advisor_prompt", arguments={"user_name": "Tester"}
                )
                print(f"\n   ↳ Rendered prompt preview: {pd.messages[0].content.text[:200]}...")
            except Exception as e:
                print(f"\n   ↳ Prompt render ERROR: {e}")

            print("\n✅ All checks done. Your MCP server looks good!")


if __name__ == "__main__":
    asyncio.run(main())