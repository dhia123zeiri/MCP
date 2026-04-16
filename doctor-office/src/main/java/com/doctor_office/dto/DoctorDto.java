package com.doctor_office.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Data
@Schema(
        name = "Doctor",
        description = "Schema to hold doctor information"
)
public class DoctorDto {

    @Schema(description = "ID of the doctor", example = "1")
    private Long doctorId;

    @Schema(description = "Name of the doctor", example = "Dr. John")
    @NotEmpty(message = "Name can not be a null or empty")
    @Size(min = 5, max = 30, message = "The length of the doctor name should be between 5 and 30")
    private String name;

    @Schema(description = "Phone number of the doctor", example = "1234567890")
    @NotEmpty(message = "Phone number can not be null or empty")
    @Pattern(regexp = "(^$|[0-9]{10})", message = "Phone number must be 10 digits")
    private String phoneNumber;

    @Schema(description = "Email of the doctor", example = "doctor@example.com")
    @NotEmpty(message = "Email can not be a null or empty")
    @Email(message = "Email address should be a valid value")
    private String email;

    @Schema(description = "Years of experience", example = "5")
    @NotNull(message = "Years of experience can not be null")
    @Min(value = 0, message = "Years of experience cannot be negative")
    private Integer yearsOfExperience;

    // ✅ Added — allows getPets() to work in McpConfig deleteDoctorTool
    @Schema(description = "List of pets assigned to this doctor")
    private List<PetDto> pets;
}