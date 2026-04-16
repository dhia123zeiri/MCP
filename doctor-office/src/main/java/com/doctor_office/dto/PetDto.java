package com.doctor_office.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(
        name = "Pet",
        description = "Schema to hold pet information"
)
public class PetDto {

    @Schema(
            description = "ID of the pet", example = "1"
    )
    private Long petId;

    @Schema(
            description = "Name of the pet", example = "Rex"
    )
    @NotEmpty(message = "Name can not be a null or empty")
    @Size(min = 5, max = 30, message = "The length of the pet name should be between 5 and 30")
    private String name;

    @Schema(
            description = "species of the pet", example = "cat"
    )
    @NotEmpty(message = "Species can not be a null or empty")
    @Size(min = 5, max = 30, message = "The length of the pet species should be between 5 and 30")
    private String species;

    @Size(min = 5, max = 30, message = "The length of the pet breed should be between 5 and 30")
    private String breed;

    @NotNull(message = "Age can not be null")
    @Min(value = 0, message = "Age cannot be negative")
    @Max(value = 10, message = "Age seems too high")
    private Integer age;

    @NotNull(message = "Weight cannot be null")
    @DecimalMin(value = "0.0", inclusive = true, message = "Weight cannot be negative")
    @DecimalMax(value = "200.0", inclusive = true, message = "Weight seems too high")
    private Float weight;

    @Size(max = 1000, message = "Medical history should not exceed 1000 characters")
    private String medicalHistory;

    @Schema(
            description = "Doctor Details of the pet"
    )
    private DoctorDto doctorDto;
}