package com.doctor_office.mapper;

import com.doctor_office.dto.DoctorDto;
import com.doctor_office.dto.PetDto;
import com.doctor_office.entity.Doctor;
import com.doctor_office.entity.Pet;

import javax.print.Doc;

public class PetMapper {
    public static PetDto mapToPetDto(Pet pet, PetDto petDto) {
        petDto.setName(pet.getName());
        if (pet.getPetType() != null) {
            petDto.setSpecies(pet.getPetType().getSpecies());
            petDto.setBreed(pet.getPetType().getBreed());
            petDto.setPrice(pet.getPetType().getPrice());
        }
        petDto.setAge(pet.getAge());
        petDto.setWeight(pet.getWeight());
        petDto.setMedicalHistory(pet.getMedicalHistory());
        return petDto;
    }

    public static Pet mapToPet(PetDto petDto, Pet pet) {
        pet.setName(petDto.getName());
        if (pet.getPetType() == null) {
            pet.setPetType(new com.doctor_office.entity.PetType());
        }
        pet.getPetType().setSpecies(petDto.getSpecies());
        pet.getPetType().setBreed(petDto.getBreed());
        pet.getPetType().setPrice(petDto.getPrice());
        pet.setAge(petDto.getAge());
        pet.setWeight(petDto.getWeight());
        pet.setMedicalHistory(petDto.getMedicalHistory());
        return pet;
    }

}
