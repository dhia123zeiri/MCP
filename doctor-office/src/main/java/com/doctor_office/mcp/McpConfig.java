package com.doctor_office.mcp;

import com.doctor_office.dto.DoctorDto;
import com.doctor_office.dto.PetDto;
import com.doctor_office.exception.DoctorAlreadyExistsException;
import com.doctor_office.exception.PetAlreadyExistsException;
import com.doctor_office.exception.ResourceNotFoundException;
import com.doctor_office.service.impl.DoctorServiceImpl;
import com.doctor_office.service.impl.PetServiceImpl;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class McpConfig {

    private final DoctorServiceImpl doctorService;
    private final PetServiceImpl petService;
    private final Validator validator;

    public McpConfig(DoctorServiceImpl doctorService, PetServiceImpl petService, Validator validator) {
        this.doctorService = doctorService;
        this.petService = petService;
        this.validator = validator;
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

    private McpSchema.CallToolResult success(String message) {
        return new McpSchema.CallToolResult(
                List.of(new McpSchema.TextContent(message)), false
        );
    }

    /**
     * GOLDEN RULE: NEVER throw from a tool — always return error(...).
     * Throwing skips producing a ToolMessage, which corrupts the agent's
     * chat history and causes INVALID_CHAT_HISTORY on every subsequent message.
     */
    private McpSchema.CallToolResult error(String message) {
        return new McpSchema.CallToolResult(
                List.of(new McpSchema.TextContent(message)), true
        );
    }

    // ─── Validation helper ────────────────────────────────────────────────────

    private <T> McpSchema.CallToolResult validate(T dto) {
        Set<ConstraintViolation<T>> violations = validator.validate(dto);
        if (violations.isEmpty()) return null;

        String messages = violations.stream()
                .map(v -> "'" + v.getPropertyPath() + "': " + v.getMessage())
                .collect(Collectors.joining(", "));

        return error("Validation failed — " + messages);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  DOCTOR TOOLS
    // ═══════════════════════════════════════════════════════════════════════

    @Bean
    public McpServerFeatures.SyncToolSpecification createDoctorTool() {
        var schema = buildSchema(
                Map.of(
                        "name",              stringProp("Doctor's full name (5–30 characters)"),
                        "phoneNumber",       stringProp("Phone number (exactly 10 digits)"),
                        "email",             stringProp("Valid email address"),
                        "yearsOfExperience", intProp("Years of experience (integer >= 0)")
                ),
                List.of("name", "email", "phoneNumber", "yearsOfExperience")
        );

        return new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool("create_doctor",
                        "Create a new doctor. yearsOfExperience is an integer.", schema),
                (exchange, args) -> {
                    try {
                        DoctorDto dto = new DoctorDto();
                        dto.setName((String) args.get("name"));
                        dto.setPhoneNumber((String) args.get("phoneNumber"));
                        dto.setEmail((String) args.get("email"));
                        dto.setYearsOfExperience(
                                args.get("yearsOfExperience") != null
                                        ? ((Number) args.get("yearsOfExperience")).intValue() : 0
                        );

                        McpSchema.CallToolResult validationError = validate(dto);
                        if (validationError != null) return validationError;

                        doctorService.createAccount(dto);
                        return success("Doctor '" + dto.getName() + "' was created successfully.");

                    } catch (DoctorAlreadyExistsException e) {
                        // 400 — mirrors GlobalExceptionHandler
                        return error("A doctor with the email '" + args.get("email")
                                + "' is already registered. Please use a different email address.");
                    } catch (Exception e) {
                        // 500 — unexpected
                        return error("Something went wrong while creating the doctor. Please try again.");
                    }
                }
        );
    }

    @Bean
    public McpServerFeatures.SyncToolSpecification createDoctorWithRatingTool() {
        var schema = buildSchema(
                Map.of(
                        "name",              stringProp("Doctor's full name (5–30 characters)"),
                        "phoneNumber",       stringProp("Phone number (exactly 10 digits)"),
                        "email",             stringProp("Valid email address"),
                        "yearsOfExperience", floatProp("Years of experience as a decimal (e.g. 4.5, >= 0)")
                ),
                List.of("name", "email", "phoneNumber", "yearsOfExperience")
        );

        return new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool("create_doctor_with_rating",
                        "Create a new doctor. yearsOfExperience is a float (e.g. 4.5 years).", schema),
                (exchange, args) -> {
                    try {
                        DoctorDto dto = new DoctorDto();
                        dto.setName((String) args.get("name"));
                        dto.setPhoneNumber((String) args.get("phoneNumber"));
                        dto.setEmail((String) args.get("email"));
                        float exp = ((Number) args.get("yearsOfExperience")).floatValue();
                        dto.setYearsOfExperience((int) exp);

                        McpSchema.CallToolResult validationError = validate(dto);
                        if (validationError != null) return validationError;

                        doctorService.createAccount(dto);
                        return success("Doctor '" + dto.getName() + "' was created successfully"
                                + " (experience: " + exp + " years).");

                    } catch (DoctorAlreadyExistsException e) {
                        // 400 — mirrors GlobalExceptionHandler
                        return error("A doctor with the email '" + args.get("email")
                                + "' is already registered. Please use a different email address.");
                    } catch (Exception e) {
                        // 500 — unexpected
                        return error("Something went wrong while creating the doctor. Please try again.");
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
                        if (doctors.isEmpty())
                            return success("There are no doctors registered in the system yet.");

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
                        // 500 — unexpected
                        return error("Something went wrong while fetching the doctor list. Please try again.");
                    }
                }
        );
    }

    @Bean
    public McpServerFeatures.SyncToolSpecification getDoctorTool() {
        var schema = buildSchema(
                Map.of("phoneNumber", stringProp("Doctor's phone number (exactly 10 digits)")),
                List.of("phoneNumber")
        );

        return new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool("get_doctor", "Fetch a doctor by phone number", schema),
                (exchange, args) -> {
                    try {
                        String phone = (String) args.get("phoneNumber");
                        if (phone == null || !phone.matches("[0-9]{10}")) {
                            return error("The phone number '" + phone
                                    + "' is not valid. Please enter exactly 10 digits with no spaces or dashes.");
                        }

                        DoctorDto dto = doctorService.fetchAccount(phone);
                        return success(
                                "Doctor found — ID: " + dto.getDoctorId()
                                        + ", Name: " + dto.getName()
                                        + ", Email: " + dto.getEmail()
                                        + ", Phone: " + dto.getPhoneNumber()
                                        + ", Experience: " + dto.getYearsOfExperience() + " years"
                        );

                    } catch (ResourceNotFoundException e) {
                        // 404 — mirrors GlobalExceptionHandler
                        return error("No doctor was found with phone number '" + args.get("phoneNumber")
                                + "'. Please double-check the number and try again.");
                    } catch (Exception e) {
                        // 500 — unexpected
                        return error("Something went wrong while looking up the doctor. Please try again.");
                    }
                }
        );
    }

    @Bean
    public McpServerFeatures.SyncToolSpecification getPetsByDoctorPhoneTool() {
        var schema = buildSchema(
                Map.of("phoneNumber", stringProp("Doctor's phone number (exactly 10 digits)")),
                List.of("phoneNumber")
        );

        return new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool("get_pets_by_doctor_phone",
                        "Fetch all pets assigned to a doctor identified by their phone number", schema),
                (exchange, args) -> {
                    try {
                        String phone = (String) args.get("phoneNumber");
                        if (phone == null || !phone.matches("[0-9]{10}")) {
                            return error("The phone number '" + phone
                                    + "' is not valid. Please enter exactly 10 digits with no spaces or dashes.");
                        }

                        DoctorDto doctor = doctorService.fetchAccount(phone);

                        if (doctor.getPets() == null || doctor.getPets().isEmpty()) {
                            return success("Dr. " + doctor.getName()
                                    + " currently has no pets assigned.");
                        }

                        StringBuilder sb = new StringBuilder();
                        sb.append("Pets assigned to Dr. ").append(doctor.getName()).append(":\n");
                        for (PetDto pet : doctor.getPets()) {
                            sb.append("  • ID=").append(pet.getPetId())
                                    .append(", Name=").append(pet.getName())
                                    .append(", Species=").append(pet.getSpecies())
                                    .append(", Breed=").append(pet.getBreed())
                                    .append(", Age=").append(pet.getAge())
                                    .append(", Weight=").append(pet.getWeight()).append("kg\n");
                        }
                        return success(sb.toString());

                    } catch (ResourceNotFoundException e) {
                        // 404 — mirrors GlobalExceptionHandler
                        return error("No doctor was found with phone number '" + args.get("phoneNumber")
                                + "'. Please verify the number and try again.");
                    } catch (Exception e) {
                        // 500 — unexpected
                        return error("Something went wrong while fetching the pets. Please try again.");
                    }
                }
        );
    }

    @Bean
    public McpServerFeatures.SyncToolSpecification updateDoctorTool() {
        var schema = buildSchema(
                Map.of(
                        "doctorId",          intProp("Doctor ID (required to identify the record)"),
                        "name",              stringProp("New name (5–30 characters, optional)"),
                        "phoneNumber",       stringProp("New phone number (exactly 10 digits, optional)"),
                        "email",             stringProp("New valid email (optional)"),
                        "yearsOfExperience", intProp("New years of experience (>= 0, optional)")
                ),
                List.of("doctorId")
        );

        return new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool("update_doctor", "Update an existing doctor by ID", schema),
                (exchange, args) -> {
                    try {
                        DoctorDto dto = new DoctorDto();
                        dto.setDoctorId(((Number) args.get("doctorId")).longValue());
                        if (args.get("name") != null)        dto.setName((String) args.get("name"));
                        if (args.get("email") != null)       dto.setEmail((String) args.get("email"));
                        if (args.get("phoneNumber") != null) dto.setPhoneNumber((String) args.get("phoneNumber"));
                        if (args.get("yearsOfExperience") != null)
                            dto.setYearsOfExperience(((Number) args.get("yearsOfExperience")).intValue());

                        // Partial field validation — only check what was provided
                        if (args.get("phoneNumber") != null
                                && !dto.getPhoneNumber().matches("[0-9]{10}")) {
                            return error("The phone number '" + dto.getPhoneNumber()
                                    + "' is not valid. Please enter exactly 10 digits.");
                        }
                        if (args.get("email") != null
                                && !dto.getEmail().matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
                            return error("The email address '" + dto.getEmail()
                                    + "' is not valid. Please provide a proper email (e.g. doctor@example.com).");
                        }
                        if (args.get("yearsOfExperience") != null && dto.getYearsOfExperience() < 0) {
                            return error("Years of experience cannot be a negative number.");
                        }

                        boolean updated = doctorService.updateAccount(dto);
                        return updated
                                ? success("Doctor ID=" + dto.getDoctorId() + " was updated successfully.")
                                : error("Update failed: no doctor was found with ID " + dto.getDoctorId() + ".");

                    } catch (ResourceNotFoundException e) {
                        // 404 — mirrors GlobalExceptionHandler
                        return error("Cannot update: no doctor was found with ID '"
                                + args.get("doctorId") + "'.");
                    } catch (Exception e) {
                        // 500 — unexpected
                        return error("Something went wrong while updating the doctor. Please try again.");
                    }
                }
        );
    }

    @Bean
    public McpServerFeatures.SyncToolSpecification deleteDoctorTool() {
        var schema = buildSchema(
                Map.of("phoneNumber", stringProp("Doctor's phone number (exactly 10 digits)")),
                List.of("phoneNumber")
        );

        return new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool("delete_doctor",
                        "Delete a doctor by phone number. Returns an error if the doctor still has pets assigned.",
                        schema),
                (exchange, args) -> {
                    try {
                        String phone = (String) args.get("phoneNumber");
                        if (phone == null || !phone.matches("[0-9]{10}")) {
                            return error("The phone number '" + phone
                                    + "' is not valid. Please enter exactly 10 digits.");
                        }

                        DoctorDto dto = doctorService.fetchAccount(phone);

                        // ✅ Graceful error — NOT a throw, agent always gets a ToolMessage back
                        if (dto.getPets() != null && !dto.getPets().isEmpty()) {
                            return error(
                                    "Dr. " + dto.getName() + " cannot be deleted because they still have "
                                            + dto.getPets().size() + " pet(s) assigned. "
                                            + "Please delete or reassign those pets first, then try again."
                            );
                        }

                        boolean deleted = doctorService.deleteAccount(phone);
                        return deleted
                                ? success("Dr. " + dto.getName() + " was deleted successfully.")
                                : error("Delete failed: no doctor was found with phone number '" + phone + "'.");

                    } catch (ResourceNotFoundException e) {
                        // 404 — mirrors GlobalExceptionHandler
                        return error("Cannot delete: no doctor was found with phone number '"
                                + args.get("phoneNumber") + "'.");
                    } catch (Exception e) {
                        // 500 — unexpected
                        return error("Something went wrong while deleting the doctor. Please try again.");
                    }
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
                        "name",           stringProp("Pet name (5–30 characters)"),
                        "species",        stringProp("Species (5–30 characters, e.g. Dog, Cat)"),
                        "breed",          stringProp("Breed (5–30 characters, optional)"),
                        "age",            intProp("Age in years (0–10)"),
                        "weight",         floatProp("Weight in kg (0.0–200.0)"),
                        "medicalHistory", stringProp("Medical history (max 1000 characters, optional)"),
                        "doctorId",       intProp("ID of the assigned doctor")
                ),
                List.of("name", "species", "doctorId", "age", "weight")
        );

        return new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool("create_pet",
                        "Create a new pet and assign it to a doctor", schema),
                (exchange, args) -> {
                    try {
                        PetDto dto = new PetDto();
                        dto.setName((String) args.get("name"));
                        dto.setSpecies((String) args.get("species"));
                        dto.setBreed((String) args.get("breed"));
                        dto.setAge(args.get("age") != null
                                ? ((Number) args.get("age")).intValue() : null);
                        dto.setWeight(args.get("weight") != null
                                ? ((Number) args.get("weight")).floatValue() : 0f);
                        dto.setMedicalHistory((String) args.get("medicalHistory"));

                        DoctorDto doctorDto = new DoctorDto();
                        doctorDto.setDoctorId(((Number) args.get("doctorId")).longValue());
                        dto.setDoctorDto(doctorDto);

                        McpSchema.CallToolResult validationError = validate(dto);
                        if (validationError != null) return validationError;

                        petService.createAccount(dto);
                        return success("Pet '" + dto.getName() + "' (" + dto.getSpecies()
                                + ") was created and assigned to doctor ID=" + args.get("doctorId") + ".");

                    } catch (PetAlreadyExistsException e) {
                        // 400 — mirrors GlobalExceptionHandler
                        return error("A pet named '" + args.get("name")
                                + "' is already registered. Please use a different name.");
                    } catch (ResourceNotFoundException e) {
                        // 404 — doctor ID not found
                        return error("Cannot create pet: no doctor was found with ID '"
                                + args.get("doctorId") + "'. Please provide a valid doctor ID.");
                    } catch (Exception e) {
                        // 500 — unexpected
                        return error("Something went wrong while creating the pet. Please try again.");
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
                        if (pets.isEmpty())
                            return success("There are no pets registered in the system yet.");

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
                        // 500 — unexpected
                        return error("Something went wrong while fetching the pet list. Please try again.");
                    }
                }
        );
    }

    @Bean
    public McpServerFeatures.SyncToolSpecification getPetTool() {
        var schema = buildSchema(
                Map.of("petId", intProp("Pet ID (positive integer)")),
                List.of("petId")
        );

        return new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool("get_pet", "Fetch a pet by its ID", schema),
                (exchange, args) -> {
                    try {
                        Long id = ((Number) args.get("petId")).longValue();
                        if (id <= 0)
                            return error("Pet ID must be a positive number. Please provide a valid ID.");

                        PetDto dto = petService.fetchAccount(id);
                        return success(
                                "Pet found — ID: " + dto.getPetId()
                                        + ", Name: " + dto.getName()
                                        + ", Species: " + dto.getSpecies()
                                        + ", Breed: " + dto.getBreed()
                                        + ", Age: " + dto.getAge()
                                        + ", Weight: " + dto.getWeight() + "kg"
                                        + ", Medical History: " + dto.getMedicalHistory()
                        );

                    } catch (ResourceNotFoundException e) {
                        // 404 — mirrors GlobalExceptionHandler
                        return error("No pet was found with ID '" + args.get("petId")
                                + "'. Please check the ID and try again.");
                    } catch (Exception e) {
                        // 500 — unexpected
                        return error("Something went wrong while looking up the pet. Please try again.");
                    }
                }
        );
    }

    @Bean
    public McpServerFeatures.SyncToolSpecification updatePetTool() {
        var schema = buildSchema(
                Map.of(
                        "petId",          intProp("Pet ID (required, positive integer)"),
                        "name",           stringProp("New name (5–30 characters, optional)"),
                        "species",        stringProp("New species (5–30 characters, optional)"),
                        "breed",          stringProp("New breed (5–30 characters, optional)"),
                        "age",            intProp("New age (0–10, optional)"),
                        "weight",         floatProp("New weight in kg (0.0–200.0, optional)"),
                        "medicalHistory", stringProp("Updated medical history (max 1000 characters, optional)")
                ),
                List.of("petId")
        );

        return new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool("update_pet", "Update an existing pet by ID", schema),
                (exchange, args) -> {
                    try {
                        Long id = ((Number) args.get("petId")).longValue();
                        if (id <= 0)
                            return error("Pet ID must be a positive number. Please provide a valid ID.");

                        // Partial field validation — only check what was provided
                        if (args.get("age") != null) {
                            int age = ((Number) args.get("age")).intValue();
                            if (age < 0 || age > 10)
                                return error("Age must be between 0 and 10 years.");
                        }
                        if (args.get("weight") != null) {
                            float weight = ((Number) args.get("weight")).floatValue();
                            if (weight < 0 || weight > 200)
                                return error("Weight must be between 0.0 and 200.0 kg.");
                        }
                        if (args.get("medicalHistory") != null
                                && ((String) args.get("medicalHistory")).length() > 1000) {
                            return error("Medical history must not exceed 1000 characters.");
                        }

                        PetDto dto = new PetDto();
                        dto.setPetId(id);
                        if (args.get("name") != null)
                            dto.setName((String) args.get("name"));
                        if (args.get("species") != null)
                            dto.setSpecies((String) args.get("species"));
                        if (args.get("breed") != null)
                            dto.setBreed((String) args.get("breed"));
                        if (args.get("age") != null)
                            dto.setAge(((Number) args.get("age")).intValue());
                        if (args.get("weight") != null)
                            dto.setWeight(((Number) args.get("weight")).floatValue());
                        if (args.get("medicalHistory") != null)
                            dto.setMedicalHistory((String) args.get("medicalHistory"));

                        boolean updated = petService.updateAccount(dto);
                        return updated
                                ? success("Pet ID=" + dto.getPetId() + " was updated successfully.")
                                : error("Update failed: no pet was found with ID " + dto.getPetId() + ".");

                    } catch (ResourceNotFoundException e) {
                        // 404 — mirrors GlobalExceptionHandler
                        return error("Cannot update: no pet was found with ID '"
                                + args.get("petId") + "'.");
                    } catch (Exception e) {
                        // 500 — unexpected
                        return error("Something went wrong while updating the pet. Please try again.");
                    }
                }
        );
    }

    @Bean
    public McpServerFeatures.SyncToolSpecification deletePetTool() {
        var schema = buildSchema(
                Map.of("petId", intProp("Pet ID (positive integer)")),
                List.of("petId")
        );

        return new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool("delete_pet",
                        "Delete a pet by ID. Returns a clear error if the pet is not found.", schema),
                (exchange, args) -> {
                    try {
                        Long id = ((Number) args.get("petId")).longValue();
                        if (id <= 0)
                            return error("Pet ID must be a positive number. Please provide a valid ID.");

                        boolean deleted = petService.deleteAccount(id);
                        return deleted
                                ? success("Pet ID=" + id + " was deleted successfully.")
                                : error("Delete failed: no pet was found with ID " + id + ".");

                    } catch (ResourceNotFoundException e) {
                        // 404 — mirrors GlobalExceptionHandler
                        return error("Cannot delete: no pet was found with ID '"
                                + args.get("petId") + "'.");
                    } catch (Exception e) {
                        // 500 — unexpected
                        return error("Something went wrong while deleting the pet. Please try again.");
                    }
                }
        );
    }
}