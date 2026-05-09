package com.doctor_office.repository;

import com.doctor_office.entity.PetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PetTypeRepository extends JpaRepository<PetType, Long> {
}
