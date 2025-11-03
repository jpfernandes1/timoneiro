package com.jompastech.backend.repository;

import com.jompastech.backend.model.entity.Boat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoatRepository extends JpaRepository<Boat, Long> {
    List<Boat> findByType(String type);
    List<Boat> findByOwnerId(Long ownerId);
}
