package com.doctor_office.mcp;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
public class McpPromptConfig {

    /**
     * Startup system prompt injected into the LangChain agent.
     * The agent requests this prompt by name ("health_advisor_prompt")
     * during its lifespan initialization.
     */
    @Bean
    public McpServerFeatures.SyncPromptSpecification healthAdvisorPrompt() {

        var prompt = new McpSchema.Prompt(
                "health_advisor_prompt",
                "System prompt that configures the AI agent for the Doctor Office domain",
                List.of(
                        new McpSchema.PromptArgument("user_name", "Name of the user interacting with the agent", false)
                )
        );

        return new McpServerFeatures.SyncPromptSpecification(
                prompt,
                (exchange, req) -> {
                    // Resolve the optional user_name argument
                    String userName = "User";
                    if (req.arguments() != null && req.arguments().containsKey("user_name")) {
                        Object val = req.arguments().get("user_name");
                        if (val != null && !val.toString().isBlank()) {
                            userName = val.toString();
                        }
                    }

                    String systemText = String.format("""
                            You are an intelligent assistant for a veterinary doctor's office.
                            You are talking to %s.

                            You have access to tools that let you manage DOCTORS and PETS in the system.
                            Use these tools to help the user with their requests.

                            IMPORTANT RULES:
                            - Always confirm the user's intent before creating, updating, or deleting any record.
                            - When creating a doctor, always ask for: name, email, phone number, and years of experience.
                            - When creating a pet, always ask for: name, species, and the ID of the assigned doctor.
                            - If a tool returns an error, explain it clearly to the user and suggest a fix.
                            - If a tool raises an exception (e.g., deleting a doctor who still has pets), explain why
                              the operation cannot be completed and what the user should do first.
                            - Provide clear, friendly, and concise responses.
                            - Never invent data; always use the tools to fetch real information.
                            - You can use 'get_doctor' to look up a doctor by phone number.
                            - You can use 'get_pet' to look up a pet by its numeric ID.
                            - Two similar create-doctor tools exist:
                              • 'create_doctor' — years of experience as an integer
                              • 'create_doctor_with_rating' — years of experience as a float
                              Use whichever best matches what the user provides.
                            """, userName);

                    return new McpSchema.GetPromptResult(
                            "Doctor Office system prompt",
                            List.of(
                                    new McpSchema.PromptMessage(
                                            McpSchema.Role.USER,
                                            new McpSchema.TextContent(systemText)
                                    )
                            )
                    );
                }
        );
    }
}