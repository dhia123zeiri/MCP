package com.doctor_office.controller;

import com.doctor_office.dto.DoctorDto;
import com.doctor_office.dto.ErrorResponseDto;
import com.doctor_office.dto.ResponseDto;
import com.doctor_office.service.IDoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "REST API for Doctors in Doctor Office",
        description = "REST APIs to CREATE, FETCH, UPDATE and DELETE doctor details"
)
@RestController
@RequestMapping(path = "/api/doctors", produces = {MediaType.APPLICATION_JSON_VALUE})
@Validated
public class DoctorController {

    private static final Logger logger = LoggerFactory.getLogger(DoctorController.class);

    private final IDoctorService iDoctorService;

    public DoctorController(IDoctorService iDoctorService) {
        this.iDoctorService = iDoctorService;
    }

    @Operation(
            summary = "Create Doctor REST API",
            description = "REST API to create a new Doctor in Doctor Office"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "HTTP Status CREATED"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "HTTP Status Bad Request",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "HTTP Status Internal Server Error",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))
            )
    })
    @PostMapping("/create")
    public ResponseEntity<ResponseDto> createDoctor(@Valid @RequestBody DoctorDto doctorDto) {
        logger.debug("createDoctor method start");
        iDoctorService.createAccount(doctorDto);
        logger.debug("createDoctor method end");
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ResponseDto("201", "Doctor created successfully"));
    }

    @Operation(
            summary = "Fetch Doctor REST API",
            description = "REST API to fetch Doctor details based on phone number"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "HTTP Status OK"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "HTTP Status Not Found",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "HTTP Status Internal Server Error",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))
            )
    })
    @GetMapping("/fetch")
    public ResponseEntity<DoctorDto> fetchDoctor(@RequestParam
                                                 @Pattern(regexp = "(^$|[0-9]{10})",
                                                         message = "Phone number must be 10 digits")
                                                 String phoneNumber) {
        logger.debug("fetchDoctor method start - phoneNumber: {}", phoneNumber);
        DoctorDto doctorDto = iDoctorService.fetchAccount(phoneNumber);
        logger.debug("fetchDoctor method end");
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(doctorDto);
    }

    @Operation(
            summary = "Update Doctor REST API",
            description = "REST API to update Doctor details"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "HTTP Status OK"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "HTTP Status Not Found",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "417",
                    description = "Expectation Failed"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "HTTP Status Internal Server Error",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))
            )
    })
    @PutMapping("/update")
    public ResponseEntity<ResponseDto> updateDoctor(@Valid @RequestBody DoctorDto doctorDto) {

        boolean isUpdated = iDoctorService.updateAccount(doctorDto);
        logger.debug("updateDoctor method end");
        if (isUpdated) {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(new ResponseDto("200", "Doctor updated successfully"));
        } else {
            return ResponseEntity
                    .status(HttpStatus.EXPECTATION_FAILED)
                    .body(new ResponseDto("417", "Doctor update failed"));
        }
    }

    @Operation(
            summary = "Delete Doctor REST API",
            description = "REST API to delete Doctor details based on phone number"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "HTTP Status OK"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "HTTP Status Not Found",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "417",
                    description = "Expectation Failed"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "HTTP Status Internal Server Error",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))
            )
    })
    @DeleteMapping("/delete")
    public ResponseEntity<ResponseDto> deleteDoctor(@RequestParam
                                                    @Pattern(regexp = "(^$|[0-9]{10})",
                                                            message = "Phone number must be 10 digits")
                                                    String phoneNumber) {
        logger.debug("deleteDoctor method start - phoneNumber: {}", phoneNumber);
        boolean isDeleted = iDoctorService.deleteAccount(phoneNumber);
        logger.debug("deleteDoctor method end");
        if (isDeleted) {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(new ResponseDto("200", "Doctor deleted successfully"));
        } else {
            return ResponseEntity
                    .status(HttpStatus.EXPECTATION_FAILED)
                    .body(new ResponseDto("417", "Doctor deletion failed"));
        }
    }
}