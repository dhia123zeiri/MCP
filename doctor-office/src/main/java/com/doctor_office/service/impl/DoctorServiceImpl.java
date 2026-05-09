package com.doctor_office.service.impl;

import com.doctor_office.dto.DoctorDto;
import com.doctor_office.entity.Doctor;
import com.doctor_office.exception.DoctorAlreadyExistsException;
import com.doctor_office.exception.ResourceNotFoundException;
import com.doctor_office.mapper.DoctorMapper;
import com.doctor_office.repository.DoctorRepository;
import com.doctor_office.service.IDoctorService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class DoctorServiceImpl implements IDoctorService {

    private DoctorRepository doctorRepository;

    @Override
    public void createAccount(DoctorDto doctorDto) {
        Optional<Doctor> optionalDoctor = doctorRepository.findByEmail(doctorDto.getEmail());
        if (optionalDoctor.isPresent()) {
            throw new DoctorAlreadyExistsException("Doctor already registered with email: " + doctorDto.getEmail());
        }
        Doctor doctor = DoctorMapper.mapToDoctor(doctorDto, new Doctor());
        doctorRepository.save(doctor);
    }

    @Override
    public DoctorDto fetchAccount(String phoneNumber) {
        Doctor doctor = doctorRepository.findByPhoneNumber(phoneNumber).orElseThrow(
                () -> new ResourceNotFoundException("Doctor", "phoneNumber", phoneNumber)
        );
        return DoctorMapper.mapToDoctorDto(doctor, new DoctorDto());
    }

    @Override
    public boolean updateAccount(DoctorDto doctorDto) {
        Doctor doctor = doctorRepository.findById(doctorDto.getDoctorId()).orElseThrow(
                () -> new ResourceNotFoundException("Doctor", "doctorId", doctorDto.getDoctorId().toString())
        );
        DoctorMapper.mapToDoctor(doctorDto, doctor);
        doctorRepository.save(doctor);
        return true;
    }

    @Override
    public boolean deleteAccount(String phoneNumber) {
        Doctor doctor = doctorRepository.findByPhoneNumber(phoneNumber).orElseThrow(
                () -> new ResourceNotFoundException("Doctor", "phoneNumber", phoneNumber)
        );
        doctorRepository.deleteById(doctor.getDoctorId());
        return true;
    }

    @Override
    public List<DoctorDto> fetchAllDoctors() {
        return doctorRepository.findAll()
                .stream()
                .map(doctor -> DoctorMapper.mapToDoctorDto(doctor, new DoctorDto()))
                .collect(Collectors.toList());
    }
}