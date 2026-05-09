package com.doctor_office.service;

import com.doctor_office.dto.PetDto;

import java.util.List;

public interface IPetService {

    void createAccount(PetDto petDto);

    PetDto fetchAccount(Long id);

    List<PetDto> fetchAllAccounts();

    boolean updateAccount(PetDto petDto);

    boolean deleteAccount(Long id);
}