package com.doctor_office.controller;

import com.doctor_office.dto.PetDto;
import com.doctor_office.dto.ResponseDto;
import com.doctor_office.dto.ErrorResponseDto;
import com.doctor_office.service.IPetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "REST API for Pets in Doctor Office",
        description = "REST APIs to CREATE, FETCH, UPDATE and DELETE pet details"
)
@RestController
@RequestMapping(path = "/api/pets", produces = {MediaType.APPLICATION_JSON_VALUE})
@Validated
public class PetController {

    private static final Logger logger = LoggerFactory.getLogger(PetController.class);

    private final IPetService iPetService;

    public PetController(IPetService iPetService) {
        this.iPetService = iPetService;
    }

    @Operation(
            summary = "Create Pet REST API",
            description = "REST API to create a new Pet in Doctor Office"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "HTTP Status CREATED"),
            @ApiResponse(
                    responseCode = "500",
                    description = "HTTP Status Internal Server Error",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))
            )
    })
    @PostMapping("/create")
    public ResponseEntity<ResponseDto> createPet(@Valid @RequestBody PetDto petDto) {
        logger.debug("createPet method start");
        iPetService.createAccount(petDto);
        logger.debug("createPet method end");
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ResponseDto("201", "Pet created successfully"));
    }

    @Operation(
            summary = "Fetch Pet REST API",
            description = "REST API to fetch Pet details based on pet ID"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
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
    public ResponseEntity<PetDto> fetchPet(@RequestParam
                                           @Positive(message = "Pet ID must be a positive number")
                                           Long petId) {
        logger.debug("fetchPet method start - petId: {}", petId);
        PetDto petDto = iPetService.fetchAccount(petId);
        logger.debug("fetchPet method end");
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(petDto);
    }

    @Operation(
            summary = "Fetch All Pets REST API",
            description = "REST API to fetch all pets registered in Doctor Office"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
            @ApiResponse(
                    responseCode = "500",
                    description = "HTTP Status Internal Server Error",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))
            )
    })
    @GetMapping("/fetchAll")
    public ResponseEntity<List<PetDto>> fetchAllPets() {
        logger.debug("fetchAllPets method start");
        List<PetDto> pets = iPetService.fetchAllAccounts();
        logger.debug("fetchAllPets method end - count: {}", pets.size());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(pets);
    }

    @Operation(
            summary = "Update Pet REST API",
            description = "REST API to update Pet details"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
            @ApiResponse(responseCode = "417", description = "Expectation Failed"),
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
    @PutMapping("/update")
    public ResponseEntity<ResponseDto> updatePet(@Valid @RequestBody PetDto petDto) {
        logger.debug("updatePet method start");
        boolean isUpdated = iPetService.updateAccount(petDto);
        logger.debug("updatePet method end");
        if (isUpdated) {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(new ResponseDto("200", "Pet updated successfully"));
        } else {
            return ResponseEntity
                    .status(HttpStatus.EXPECTATION_FAILED)
                    .body(new ResponseDto("417", "Pet update failed"));
        }
    }

    @Operation(
            summary = "Delete Pet REST API",
            description = "REST API to delete Pet details based on pet ID"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
            @ApiResponse(responseCode = "417", description = "Expectation Failed"),
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
    @DeleteMapping("/delete")
    public ResponseEntity<ResponseDto> deletePet(@RequestParam
                                                 @Positive(message = "Pet ID must be a positive number")
                                                 Long petId) {
        logger.debug("deletePet method start - petId: {}", petId);
        boolean isDeleted = iPetService.deleteAccount(petId);
        logger.debug("deletePet method end");
        if (isDeleted) {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(new ResponseDto("200", "Pet deleted successfully"));
        } else {
            return ResponseEntity
                    .status(HttpStatus.EXPECTATION_FAILED)
                    .body(new ResponseDto("417", "Pet deletion failed"));
        }
    }
}