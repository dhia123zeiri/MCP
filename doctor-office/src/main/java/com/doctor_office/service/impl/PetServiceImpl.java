package com.doctor_office.service.impl;

import com.doctor_office.dto.PetDto;
import com.doctor_office.entity.Pet;
import com.doctor_office.exception.PetAlreadyExistsException;
import com.doctor_office.exception.ResourceNotFoundException;
import com.doctor_office.mapper.PetMapper;
import com.doctor_office.repository.PetRepository;
import com.doctor_office.service.IPetService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PetServiceImpl implements IPetService {

    private PetRepository petRepository;

    public PetServiceImpl(PetRepository petRepository) {
        this.petRepository = petRepository;
    }

    @Override
    public void createAccount(PetDto petDto) {
        Optional<Pet> optionalPet = petRepository.findByName(petDto.getName());
        if (optionalPet.isPresent()) {
            throw new PetAlreadyExistsException("Pet already registered with name: " + petDto.getName());
        }
        Pet pet = PetMapper.mapToPet(petDto, new Pet());
        if (pet.getPetType() != null) {
            pet.getPetType().setPrice(calculatePrice(pet.getPetType().getSpecies(), pet.getPetType().getBreed()));
        }
        petRepository.save(pet);
    }

    @Override
    public PetDto fetchAccount(Long petId) {
        Pet pet = petRepository.findById(petId).orElseThrow(
                () -> new ResourceNotFoundException("Pet", "petId", petId.toString()));
        return PetMapper.mapToPetDto(pet, new PetDto());
    }

    @Override
    public List<PetDto> fetchAllAccounts() {
        return petRepository.findAll()
                .stream()
                .map(pet -> PetMapper.mapToPetDto(pet, new PetDto()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean updateAccount(PetDto petDto) {
        Pet pet = petRepository.findById(petDto.getPetId()).orElseThrow(
                () -> new ResourceNotFoundException("Pet", "petId", petDto.getPetId().toString()));
        PetMapper.mapToPet(petDto, pet);
        if (pet.getPetType() != null) {
            pet.getPetType().setPrice(calculatePrice(pet.getPetType().getSpecies(), pet.getPetType().getBreed()));
        }
        petRepository.save(pet);
        return true;
    }

    @Override
    public boolean deleteAccount(Long petId) {
        petRepository.findById(petId).orElseThrow(
                () -> new ResourceNotFoundException("Pet", "petId", petId.toString()));
        petRepository.deleteById(petId);
        return true;
    }

    private Double calculatePrice(String species, String breed) {
        double price = 100.0;
        if (species != null) {
            price += species.length() * 5.0;
        }
        if (breed != null) {
            price += breed.length() * 10.0;
        }
        return price;
    }
}