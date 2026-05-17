package com.doctor_office.controller;

import com.doctor_office.entity.Country;
import com.doctor_office.entity.PetType;
import com.doctor_office.repository.CountryRepository;
import com.doctor_office.repository.PetTypeRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Covers assignment requirements #1, #2, #3, #4:
 *   #1 add countries (no delete, no update)
 *   #2 list countries
 *   #3 add pet types (items for sale)
 *   #4 list pet types
 */
@Tag(name = "Reference data (Countries & Items)",
     description = "Reference data feeding the DBInfo Kafka topics")
@RestController
@RequestMapping(path = "/api/refdata", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
public class ReferenceDataController {

    private final CountryRepository  countryRepository;
    private final PetTypeRepository  petTypeRepository;

    public ReferenceDataController(CountryRepository countryRepository,
                                   PetTypeRepository petTypeRepository) {
        this.countryRepository = countryRepository;
        this.petTypeRepository = petTypeRepository;
    }

    // ── Countries ────────────────────────────────────────────────────────

    @GetMapping("/countries")
    public List<Country> listCountries() {
        return countryRepository.findAll();
    }

    @PostMapping("/countries")
    public ResponseEntity<Country> addCountry(
            @RequestParam @NotBlank @Size(min = 2, max = 3) String code,
            @RequestParam @NotBlank @Size(min = 2, max = 80) String name) {

        if (countryRepository.existsById(code.toUpperCase())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        Country saved = countryRepository.save(new Country(code.toUpperCase(), name));
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // Per requirement #1: NO DELETE, NO UPDATE for countries.

    // ── Pet Types (Items for sale) ───────────────────────────────────────

    @GetMapping("/pettypes")
    public List<PetType> listPetTypes() {
        return petTypeRepository.findAll();
    }

    @PostMapping("/pettypes")
    public ResponseEntity<PetType> addPetType(
            @RequestParam @NotBlank String species,
            @RequestParam @NotBlank String breed,
            @RequestParam Double price) {

        PetType pt = new PetType();
        pt.setSpecies(species);
        pt.setBreed(breed);
        pt.setPrice(price);
        return ResponseEntity.status(HttpStatus.CREATED).body(petTypeRepository.save(pt));
    }

    @PutMapping("/pettypes/{id}")
    public ResponseEntity<PetType> updatePetType(
            @PathVariable Long id,
            @RequestParam(required = false) String species,
            @RequestParam(required = false) String breed,
            @RequestParam(required = false) Double price) {

        return petTypeRepository.findById(id)
            .map(pt -> {
                if (species != null) pt.setSpecies(species);
                if (breed != null)   pt.setBreed(breed);
                if (price != null)   pt.setPrice(price);
                return ResponseEntity.ok(petTypeRepository.save(pt));
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
