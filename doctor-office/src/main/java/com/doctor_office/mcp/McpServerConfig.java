package com.doctor_office.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.WebMvcSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;

@Configuration
public class McpServerConfig {

    /**
     * Transport provider.
     *
     * IMPORTANT: The second argument is the "message endpoint" path, i.e. where
     * the client POSTs tool-call messages. The SSE *stream* is always served at
     * GET /sse regardless of this value (it is hard-coded in the library).
     *
     * So the Python agent must connect to:  http://localhost:8080/sse
     */
    @Bean
    public WebMvcSseServerTransportProvider transportProvider(ObjectMapper objectMapper) {
        return new WebMvcSseServerTransportProvider(objectMapper, "/mcp/messages");
    }

    /**
     * Registers the SSE + message routes into Spring's functional router.
     */
    @Bean
    public RouterFunction<ServerResponse> mcpRoutes(WebMvcSseServerTransportProvider provider) {
        return provider.getRouterFunction();
    }

    /**
     * Builds and starts the synchronous MCP server, wiring in all tools,
     * resources, and prompts collected from the application context.
     */
    @Bean
    public McpSyncServer mcpServer(
            WebMvcSseServerTransportProvider transportProvider,
            List<McpServerFeatures.SyncToolSpecification> tools,
            List<McpServerFeatures.SyncResourceSpecification> resources,
            List<McpServerFeatures.SyncPromptSpecification> prompts
    ) {
        return McpServer.sync(transportProvider)
                .serverInfo("DoctorOfficeMCP", "1.0.0")
                .capabilities(
                        McpSchema.ServerCapabilities.builder()
                                .tools(true)
                                .resources(true, true)   // (listChanged, subscribeChanged)
                                .prompts(true)
                                .build()
                )
                .tools(tools)
                .resources(resources)
                .prompts(prompts)
                .build();
    }
}