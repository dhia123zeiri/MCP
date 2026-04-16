package com.doctor_office.repository;

import com.doctor_office.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor,Long> {
    Optional<Doctor> findByEmail(String email);

    Optional<Doctor> findByPhoneNumber(String phoneNumber);
}
