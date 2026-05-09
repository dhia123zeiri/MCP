package com.doctor_office.service;

import com.doctor_office.dto.DoctorDto;
import com.doctor_office.entity.Doctor;

import java.util.List;

public interface IDoctorService {

    void createAccount(DoctorDto doctorDto);

    List<DoctorDto> fetchAllDoctors();


    DoctorDto fetchAccount(String mobileNumber);

    boolean updateAccount(DoctorDto doctorDto);


    boolean deleteAccount(String mobileNumber);

}
