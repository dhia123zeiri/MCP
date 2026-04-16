package com.doctor_office.mapper;

import com.doctor_office.dto.DoctorDto;
import com.doctor_office.entity.Doctor;

public class DoctorMapper {

    public static DoctorDto mapToDoctorDto(Doctor doctor, DoctorDto doctorDto) {
        doctorDto.setDoctorId(doctor.getDoctorId());
        doctorDto.setName(doctor.getName());
        doctorDto.setEmail(doctor.getEmail());
        doctorDto.setPhoneNumber(doctor.getPhoneNumber());
        doctorDto.setYearsOfExperience(doctor.getYearsOfExperience());
        return doctorDto;
    }

    public static Doctor mapToDoctor(DoctorDto doctorDto, Doctor doctor) {
        doctor.setName(doctorDto.getName());
        doctor.setEmail(doctorDto.getEmail());
        doctor.setPhoneNumber(doctorDto.getPhoneNumber());
        doctor.setYearsOfExperience(doctorDto.getYearsOfExperience()); // was incorrectly reading from doctor instead of doctorDto
        return doctor;
    }
}