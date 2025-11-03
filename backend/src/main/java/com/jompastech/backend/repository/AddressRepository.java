package com.jompastech.backend.repository;

import com.jompastech.backend.model.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByCity(String city);
    List<Address> findByState(String state);
}
