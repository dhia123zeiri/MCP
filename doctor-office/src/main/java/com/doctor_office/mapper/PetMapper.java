package com.doctor_office.mapper;

import com.doctor_office.dto.DoctorDto;
import com.doctor_office.dto.PetDto;
import com.doctor_office.entity.Doctor;
import com.doctor_office.entity.Pet;

import javax.print.Doc;

public class PetMapper {
    public static PetDto mapToPetDto(Pet pet, PetDto petDto) {
        petDto.setName(pet.getName());
        petDto.setSpecies(pet.getSpecies());
        petDto.setAge(pet.getAge());
        petDto.setWeight(pet.getWeight());
        petDto.setMedicalHistory(pet.getMedicalHistory());
        petDto.setBreed(pet.getBreed());
        return petDto;
    }

    public static Pet mapToPet(PetDto petDto, Pet pet) {
        pet.setName(petDto.getName());
        pet.setSpecies(petDto.getSpecies());
        pet.setAge(petDto.getAge());
        pet.setWeight(petDto.getWeight());
        pet.setMedicalHistory(petDto.getMedicalHistory());
        pet.setBreed(petDto.getBreed());
        return pet;
    }

}
