package com.doctor_office.mcp;

import com.doctor_office.dto.DoctorDto;
import com.doctor_office.dto.PetDto;
import com.doctor_office.service.impl.DoctorServiceImpl;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class McpResourceConfig {

    private final DoctorServiceImpl doctorService;

    public McpResourceConfig(DoctorServiceImpl doctorService) {
        this.doctorService = doctorService;
    }

    // ─── Resource 1: Database schema description ──────────────────────────────

    @Bean
    public McpServerFeatures.SyncResourceSpecification schemaResource() {
        var resource = new McpSchema.Resource(
                "file://schema.txt/",
                "Database Schema",
                "Describes the Doctor and Pet database schema",
                "text/plain",
                null
        );

        return new McpServerFeatures.SyncResourceSpecification(
                resource,
                (exchange, req) -> {
                    String schema = """
                            === Doctor Office Database Schema ===

                            Table: DOCTOR
                            - doctor_id           BIGINT (PK, auto-increment)
                            - name                VARCHAR
                            - phone_number        VARCHAR
                            - email               VARCHAR
                            - years_of_experience INTEGER

                            Table: PET
                            - pet_id         BIGINT (PK, auto-increment)
                            - name           VARCHAR
                            - species        VARCHAR
                            - breed          VARCHAR
                            - age            INTEGER
                            - weight         FLOAT
                            - medical_history VARCHAR
                            - doctor_id      BIGINT (FK -> DOCTOR.doctor_id)

                            Relationship: One Doctor has many Pets (OneToMany)
                            """;

                    return new McpSchema.ReadResourceResult(
                            List.of(new McpSchema.TextResourceContents(
                                    req.uri(), "text/plain", schema
                            ))
                    );
                }
        );
    }

    // ─── Resource 2: Live report of all doctors and their pets ────────────────

    @Bean
    public McpServerFeatures.SyncResourceSpecification reportResource() {
        var resource = new McpSchema.Resource(
                "file://report.txt/",
                "Doctors and Pets Report",
                "Live report listing all doctors and their assigned pets",
                "text/plain",
                null
        );

        return new McpServerFeatures.SyncResourceSpecification(
                resource,
                (exchange, req) -> {
                    List<DoctorDto> doctors = doctorService.fetchAllDoctors();
                    StringBuilder report = new StringBuilder("=== Doctor Office Report ===\n\n");

                    for (DoctorDto doc : doctors) {
                        report.append("Dr. ").append(doc.getName())
                                .append(" | Email: ").append(doc.getEmail())
                                .append(" | Phone: ").append(doc.getPhoneNumber())
                                .append(" | Experience: ").append(doc.getYearsOfExperience()).append(" yrs\n");

                        if (doc.getPets() == null || doc.getPets().isEmpty()) {
                            report.append("  → No pets assigned\n");
                        } else {
                            for (PetDto pet : doc.getPets()) {
                                report.append("  → Pet: ").append(pet.getName())
                                        .append(" (").append(pet.getSpecies()).append(")")
                                        .append(", Age: ").append(pet.getAge())
                                        .append(", Weight: ").append(pet.getWeight()).append("kg\n");
                            }
                        }
                        report.append("\n");
                    }

                    return new McpSchema.ReadResourceResult(
                            List.of(new McpSchema.TextResourceContents(
                                    req.uri(), "text/plain", report.toString()
                            ))
                    );
                }
        );
    }
}