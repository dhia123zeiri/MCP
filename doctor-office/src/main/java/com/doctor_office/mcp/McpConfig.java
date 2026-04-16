package com.doctor_office.mcp;

import com.doctor_office.dto.DoctorDto;
import com.doctor_office.dto.PetDto;
import com.doctor_office.service.impl.DoctorServiceImpl;
import com.doctor_office.service.impl.PetServiceImpl;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
public class McpConfig {

    private final DoctorServiceImpl doctorService;
    private final PetServiceImpl petService;

    public McpConfig(DoctorServiceImpl doctorService, PetServiceImpl petService) {
        this.doctorService = doctorService;
        this.petService = petService;
    }

    // ─── Schema helpers ───────────────────────────────────────────────────────

    private Map<String, Object> stringProp(String description) {
        return Map.of("type", "string", "description", description);
    }

    private Map<String, Object> intProp(String description) {
        return Map.of("type", "integer", "description", description);
    }

    private Map<String, Object> floatProp(String description) {
        return Map.of("type", "number", "description", description);
    }

    private McpSchema.JsonSchema buildSchema(Map<String, Object> properties, List<String> required) {
        return new McpSchema.JsonSchema("object", properties, required, null);
    }

    // ─── Result helpers ───────────────────────────────────────────────────────

    /** Successful tool result — isError = false */
    private McpSchema.CallToolResult success(String message) {
        return new McpSchema.CallToolResult(
                List.of(new McpSchema.TextContent(message)), false
        );
    }

    /**
     * Graceful error result — isError = true but NO exception thrown.
     * The agent receives this as a normal tool response and can relay the
     * error message to the user. Satisfies requirement: "returns an error".
     */
    private McpSchema.CallToolResult error(String message) {
        return new McpSchema.CallToolResult(
                List.of(new McpSchema.TextContent(message)), true
        );
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  DOCTOR TOOLS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * create_doctor — yearsOfExperience as INTEGER.
     * (Pair #1 of the "two similar operations" requirement.)
     */
    @Bean
    public McpServerFeatures.SyncToolSpecification createDoctorTool() {
        var schema = buildSchema(
                Map.of(
                        "name",              stringProp("Doctor's full name"),
                        "phoneNumber",       stringProp("Phone number"),
                        "email",             stringProp("Email address"),
                        "yearsOfExperience", intProp("Years of experience (integer)")
                ),
                List.of("name", "email", "phoneNumber")
        );

        return new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool(
                        "create_doctor",
                        "Create a new doctor. yearsOfExperience is an integer.",
                        schema
                ),
                (exchange, args) -> {
                    try {
                        DoctorDto dto = new DoctorDto();
                        dto.setName((String) args.get("name"));
                        dto.setPhoneNumber((String) args.get("phoneNumber"));
                        dto.setEmail((String) args.get("email"));
                        dto.setYearsOfExperience(
                                args.get("yearsOfExperience") != null
                                        ? ((Number) args.get("yearsOfExperience")).intValue()
                                        : 0
                        );
                        doctorService.createAccount(dto);
                        return success("Doctor '" + dto.getName() + "' created with email: " + dto.getEmail());
                    } catch (Exception e) {
                        return error("Failed to create doctor: " + e.getMessage());
                    }
                }
        );
    }

    /**
     * create_doctor_with_rating — yearsOfExperience as FLOAT.
     * (Pair #2 of the "two similar operations" requirement.)
     * Input and output differ slightly: float value echoed back in result.
     */
    @Bean
    public McpServerFeatures.SyncToolSpecification createDoctorWithRatingTool() {
        var schema = buildSchema(
                Map.of(
                        "name",              stringProp("Doctor's full name"),
                        "phoneNumber",       stringProp("Phone number"),
                        "email",             stringProp("Email address"),
                        "yearsOfExperience", floatProp("Years of experience as a decimal (e.g. 4.5)")
                ),
                List.of("name", "email", "phoneNumber", "yearsOfExperience")
        );

        return new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool(
                        "create_doctor_with_rating",
                        "Create a new doctor. yearsOfExperience is a float (e.g. 4.5 years).",
                        schema
                ),
                (exchange, args) -> {
                    try {
                        DoctorDto dto = new DoctorDto();
                        dto.setName((String) args.get("name"));
                        dto.setPhoneNumber((String) args.get("phoneNumber"));
                        dto.setEmail((String) args.get("email"));
                        float exp = ((Number) args.get("yearsOfExperience")).floatValue();
                        dto.setYearsOfExperience((int) exp);
                        doctorService.createAccount(dto);
                        return success(
                                "Doctor '" + dto.getName() + "' created with email: " + dto.getEmail()
                                        + " (experience provided as float: " + exp + ")"
                        );
                    } catch (Exception e) {
                        return error("Failed to create doctor (float variant): " + e.getMessage());
                    }
                }
        );
    }

    @Bean
    public McpServerFeatures.SyncToolSpecification getAllDoctorsTool() {
        var schema = buildSchema(Map.of(), List.of());

        return new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool("get_all_doctors", "List all doctors in the system", schema),
                (exchange, args) -> {
                    try {
                        var doctors = doctorService.fetchAllDoctors();
                        if (doctors.isEmpty()) return success("No doctors found.");
                        StringBuilder sb = new StringBuilder("Doctors:\n");
                        for (DoctorDto d : doctors) {
                            sb.append("  • ID=").append(d.getDoctorId())
                                    .append(", Name=").append(d.getName())
                                    .append(", Email=").append(d.getEmail())
                                    .append(", Phone=").append(d.getPhoneNumber())
                                    .append(", Experience=").append(d.getYearsOfExperience()).append(" yrs\n");
                        }
                        return success(sb.toString());
                    } catch (Exception e) {
                        return error("Failed to fetch doctors: " + e.getMessage());
                    }
                }
        );
    }

    @Bean
    public McpServerFeatures.SyncToolSpecification getDoctorTool() {
        var schema = buildSchema(
                Map.of("phoneNumber", stringProp("Doctor's phone number")),
                List.of("phoneNumber")
        );

        return new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool("get_doctor", "Fetch a doctor by phone number", schema),
                (exchange, args) -> {
                    try {
                        String phone = (String) args.get("phoneNumber");
                        DoctorDto dto = doctorService.fetchAccount(phone);
                        return success(
                                "Doctor — ID: " + dto.getDoctorId()
                                        + ", Name: " + dto.getName()
                                        + ", Email: " + dto.getEmail()
                                        + ", Phone: " + dto.getPhoneNumber()
                                        + ", Experience: " + dto.getYearsOfExperience() + " years"
                        );
                    } catch (Exception e) {
                        return error("Doctor not found: " + e.getMessage());
                    }
                }
        );
    }

    @Bean
    public McpServerFeatures.SyncToolSpecification updateDoctorTool() {
        var schema = buildSchema(
                Map.of(
                        "doctorId",          intProp("Doctor ID (required to identify the record)"),
                        "name",              stringProp("New name (optional)"),
                        "phoneNumber",       stringProp("New phone number (optional)"),
                        "email",             stringProp("New email (optional)"),
                        "yearsOfExperience", intProp("New years of experience (optional)")
                ),
                List.of("doctorId")
        );

        return new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool("update_doctor", "Update an existing doctor by ID", schema),
                (exchange, args) -> {
                    try {
                        DoctorDto dto = new DoctorDto();
                        dto.setDoctorId(((Number) args.get("doctorId")).longValue());
                        if (args.get("name") != null)              dto.setName((String) args.get("name"));
                        if (args.get("email") != null)             dto.setEmail((String) args.get("email"));
                        if (args.get("phoneNumber") != null)       dto.setPhoneNumber((String) args.get("phoneNumber"));
                        if (args.get("yearsOfExperience") != null)
                            dto.setYearsOfExperience(((Number) args.get("yearsOfExperience")).intValue());

                        boolean updated = doctorService.updateAccount(dto);
                        return updated
                                ? success("Doctor ID=" + dto.getDoctorId() + " updated successfully.")
                                : error("Update failed: doctor ID=" + dto.getDoctorId() + " not found.");
                    } catch (Exception e) {
                        return error("Failed to update doctor: " + e.getMessage());
                    }
                }
        );
    }

    /**
     * delete_doctor — THROWS a RuntimeException if the doctor still has pets.
     * This satisfies the requirement: "raises an exception that cannot be concluded."
     * The MCP framework catches the exception and sends an error response to the client.
     */
    @Bean
    public McpServerFeatures.SyncToolSpecification deleteDoctorTool() {
        var schema = buildSchema(
                Map.of("phoneNumber", stringProp("Doctor's phone number")),
                List.of("phoneNumber")
        );

        return new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool(
                        "delete_doctor",
                        "Delete a doctor by phone number. THROWS an exception if the doctor still has active pets assigned — reassign or remove pets first.",
                        schema
                ),
                (exchange, args) -> {
                    String phone = (String) args.get("phoneNumber");
                    DoctorDto dto = doctorService.fetchAccount(phone);

                    // ✅ Requirement: "raises an exception that cannot be concluded"
                    if (dto.getPets() != null && !dto.getPets().isEmpty()) {
                        throw new RuntimeException(
                                "Cannot delete doctor '" + dto.getName() + "' (phone: " + phone + "): "
                                        + dto.getPets().size() + " pet(s) are still assigned to them. "
                                        + "Reassign or delete those pets first."
                        );
                    }

                    boolean deleted = doctorService.deleteAccount(phone);
                    return deleted
                            ? success("Doctor with phone " + phone + " deleted successfully.")
                            : error("Delete failed: no doctor found with phone " + phone + ".");
                }
        );
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PET TOOLS
    // ═══════════════════════════════════════════════════════════════════════

    @Bean
    public McpServerFeatures.SyncToolSpecification createPetTool() {
        var schema = buildSchema(
                Map.of(
                        "name",           stringProp("Pet name"),
                        "species",        stringProp("Species (e.g. Dog, Cat)"),
                        "breed",          stringProp("Breed (optional)"),
                        "age",            intProp("Age in years (optional)"),
                        "weight",         floatProp("Weight in kg (optional)"),
                        "medicalHistory", stringProp("Medical history (optional)"),
                        "doctorId",       intProp("ID of the assigned doctor")
                ),
                List.of("name", "species", "doctorId")
        );

        return new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool("create_pet", "Create a new pet and assign it to a doctor", schema),
                (exchange, args) -> {
                    try {
                        PetDto dto = new PetDto();
                        dto.setName((String) args.get("name"));
                        dto.setSpecies((String) args.get("species"));
                        dto.setBreed((String) args.get("breed"));
                        dto.setAge(args.get("age") != null ? ((Number) args.get("age")).intValue() : null);
                        dto.setWeight(args.get("weight") != null ? ((Number) args.get("weight")).floatValue() : 0f);
                        dto.setMedicalHistory((String) args.get("medicalHistory"));

                        DoctorDto doctorDto = new DoctorDto();
                        doctorDto.setDoctorId(((Number) args.get("doctorId")).longValue());
                        dto.setDoctorDto(doctorDto);

                        petService.createAccount(dto);
                        return success("Pet '" + dto.getName() + "' (" + dto.getSpecies() + ") created successfully.");
                    } catch (Exception e) {
                        return error("Failed to create pet: " + e.getMessage());
                    }
                }
        );
    }

    @Bean
    public McpServerFeatures.SyncToolSpecification getAllPetsTool() {
        var schema = buildSchema(Map.of(), List.of());

        return new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool("get_all_pets", "List all pets in the system", schema),
                (exchange, args) -> {
                    try {
                        var pets = petService.fetchAllAccounts();
                        if (pets.isEmpty()) return success("No pets found.");
                        StringBuilder sb = new StringBuilder("Pets:\n");
                        for (PetDto p : pets) {
                            sb.append("  • ID=").append(p.getPetId())
                                    .append(", Name=").append(p.getName())
                                    .append(", Species=").append(p.getSpecies())
                                    .append(", Breed=").append(p.getBreed())
                                    .append(", Age=").append(p.getAge())
                                    .append(", Weight=").append(p.getWeight()).append("kg\n");
                        }
                        return success(sb.toString());
                    } catch (Exception e) {
                        return error("Failed to fetch pets: " + e.getMessage());
                    }
                }
        );
    }

    @Bean
    public McpServerFeatures.SyncToolSpecification getPetTool() {
        var schema = buildSchema(
                Map.of("petId", intProp("Pet ID")),
                List.of("petId")
        );

        return new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool("get_pet", "Fetch a pet by its ID", schema),
                (exchange, args) -> {
                    try {
                        Long id = ((Number) args.get("petId")).longValue();
                        PetDto dto = petService.fetchAccount(id);
                        return success(
                                "Pet — ID: " + dto.getPetId()
                                        + ", Name: " + dto.getName()
                                        + ", Species: " + dto.getSpecies()
                                        + ", Breed: " + dto.getBreed()
                                        + ", Age: " + dto.getAge()
                                        + ", Weight: " + dto.getWeight() + "kg"
                                        + ", History: " + dto.getMedicalHistory()
                        );
                    } catch (Exception e) {
                        return error("Pet not found: " + e.getMessage());
                    }
                }
        );
    }

    @Bean
    public McpServerFeatures.SyncToolSpecification updatePetTool() {
        var schema = buildSchema(
                Map.of(
                        "petId",          intProp("Pet ID (required)"),
                        "name",           stringProp("New name (optional)"),
                        "species",        stringProp("New species (optional)"),
                        "breed",          stringProp("New breed (optional)"),
                        "age",            intProp("New age (optional)"),
                        "weight",         floatProp("New weight in kg (optional)"),
                        "medicalHistory", stringProp("Updated medical history (optional)")
                ),
                List.of("petId")
        );

        return new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool("update_pet", "Update an existing pet by ID", schema),
                (exchange, args) -> {
                    try {
                        PetDto dto = new PetDto();
                        dto.setPetId(((Number) args.get("petId")).longValue());
                        if (args.get("name") != null)           dto.setName((String) args.get("name"));
                        if (args.get("species") != null)        dto.setSpecies((String) args.get("species"));
                        if (args.get("breed") != null)          dto.setBreed((String) args.get("breed"));
                        if (args.get("age") != null)            dto.setAge(((Number) args.get("age")).intValue());
                        if (args.get("weight") != null)         dto.setWeight(((Number) args.get("weight")).floatValue());
                        if (args.get("medicalHistory") != null) dto.setMedicalHistory((String) args.get("medicalHistory"));

                        boolean updated = petService.updateAccount(dto);
                        return updated
                                ? success("Pet ID=" + dto.getPetId() + " updated successfully.")
                                : error("Update failed: pet ID=" + dto.getPetId() + " not found.");
                    } catch (Exception e) {
                        return error("Failed to update pet: " + e.getMessage());
                    }
                }
        );
    }

    /**
     * delete_pet — returns a graceful ERROR result (isError=true) instead of throwing.
     * This satisfies the requirement: "returns an error" (as opposed to raising an exception).
     */
    @Bean
    public McpServerFeatures.SyncToolSpecification deletePetTool() {
        var schema = buildSchema(
                Map.of("petId", intProp("Pet ID")),
                List.of("petId")
        );

        return new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool(
                        "delete_pet",
                        "Delete a pet by ID. Returns a graceful error message if the pet is not found.",
                        schema
                ),
                (exchange, args) -> {
                    try {
                        Long id = ((Number) args.get("petId")).longValue();
                        boolean deleted = petService.deleteAccount(id);
                        return deleted
                                ? success("Pet ID=" + id + " deleted successfully.")
                                : error("Delete failed: pet ID=" + id + " was not found.");   // ✅ graceful error
                    } catch (Exception e) {
                        // ✅ Requirement: "returns an error" — isError=true, no exception propagated
                        return error("Error deleting pet: " + e.getMessage());
                    }
                }
        );
    }
}