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
@AllArgsConstructor
public class PetServiceImpl implements IPetService {

    private PetRepository petRepository;

    @Override
    public void createAccount(PetDto petDto) {
        Optional<Pet> optionalPet = petRepository.findByName(petDto.getName());
        if (optionalPet.isPresent()) {
            throw new PetAlreadyExistsException("Pet already registered with name: " + petDto.getName());
        }
        Pet pet = PetMapper.mapToPet(petDto, new Pet());
        petRepository.save(pet);
    }

    @Override
    public PetDto fetchAccount(Long petId) {
        Pet pet = petRepository.findById(petId).orElseThrow(
                () -> new ResourceNotFoundException("Pet", "petId", petId.toString())
        );
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
                () -> new ResourceNotFoundException("Pet", "petId", petDto.getPetId().toString())
        );
        PetMapper.mapToPet(petDto, pet);
        petRepository.save(pet);
        return true;
    }

    @Override
    public boolean deleteAccount(Long petId) {
        petRepository.findById(petId).orElseThrow(
                () -> new ResourceNotFoundException("Pet", "petId", petId.toString())
        );
        petRepository.deleteById(petId);
        return true;
    }
}