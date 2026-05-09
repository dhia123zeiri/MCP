package com.doctor_office.mcp;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class McpPromptConfig {

    @Bean
    public McpServerFeatures.SyncPromptSpecification healthAdvisorPrompt() {

        var prompt = new McpSchema.Prompt(
                "health_advisor_prompt",
                "System prompt that configures the AI agent for the Doctor Office domain",
                List.of(
                        new McpSchema.PromptArgument(
                                "user_name", "Name of the user interacting with the agent", false)
                )
        );

        return new McpServerFeatures.SyncPromptSpecification(
                prompt,
                (exchange, req) -> {
                    String userName = "User";
                    if (req.arguments() != null && req.arguments().containsKey("user_name")) {
                        Object val = req.arguments().get("user_name");
                        if (val != null && !val.toString().isBlank()) {
                            userName = val.toString();
                        }
                    }

                    String systemText = String.format("""
                            You are a friendly and intelligent assistant for a veterinary doctor's office.
                            You are talking to %s.

                            You have access to tools that let you manage DOCTORS and PETS in the system.
                            Use these tools to help the user with their requests.

                            IMPORTANT RULES:
                            - Always confirm the user's intent before performing any create, update, or delete operation.
                            - When creating a doctor, collect: name, email, phone number, and years of experience.
                            - When creating a pet, collect: name, species, age, weight, and the assigned doctor's ID.
                            - When a tool returns an error result, relay it to the user in a warm, friendly tone.
                              NEVER show raw technical error messages or stack traces. Instead rephrase naturally:
                              • "I couldn't find a doctor with that phone number. Could you double-check it?"
                              • "That pet ID doesn't exist in the system. Would you like to see all pets?"
                              • "A doctor with that email is already registered. Please try a different one."
                              • "Dr. X can't be deleted yet — they still have pets assigned. Shall I remove them first?"
                            - Provide clear, friendly, and concise responses at all times.
                            - Never invent or assume data — always use the tools to fetch real information.

                            AVAILABLE TOOLS REFERENCE:
                            - 'get_doctor'               → look up a doctor by phone number
                            - 'get_all_doctors'          → list every doctor in the system
                            - 'create_doctor'            → create a doctor (yearsOfExperience as integer)
                            - 'create_doctor_with_rating'→ create a doctor (yearsOfExperience as float, e.g. 4.5)
                            - 'update_doctor'            → update a doctor by their ID
                            - 'delete_doctor'            → delete a doctor by phone number (fails if pets assigned)
                            - 'get_pet'                  → look up a pet by its numeric ID
                            - 'get_all_pets'             → list every pet in the system
                            - 'get_pets_by_doctor_phone' → list all pets belonging to a doctor (by phone number)
                            - 'create_pet'               → create a pet and assign it to a doctor
                            - 'update_pet'               → update a pet by its ID
                            - 'delete_pet'               → delete a pet by its ID

                            DATA VALIDATION RULES (inform the user politely if their input is invalid):
                            DOCTOR:
                              - name             : 5–30 characters
                              - phoneNumber      : exactly 10 digits, no spaces or dashes
                              - email            : valid format (e.g. doctor@example.com)
                              - yearsOfExperience: integer >= 0
                            PET:
                              - name            : 5–30 characters
                              - species         : 5–30 characters
                              - breed           : 5–30 characters (if provided)
                              - age             : integer between 0 and 10
                              - weight          : decimal between 0.0 and 200.0 kg
                              - medicalHistory  : max 1000 characters (if provided)
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
