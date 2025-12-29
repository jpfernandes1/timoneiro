package com.jompastech.backend.repository;

import com.jompastech.backend.model.dto.BoatResponseDTO;
import com.jompastech.backend.model.entity.Boat;
import com.jompastech.backend.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoatRepository extends JpaRepository<Boat, Long> {
    List<Boat> findByType(String type);
    List<Boat> findByOwnerId(long owner);
    Page<Boat> findByOwner(User owner, Pageable pageable);

    // to verify if the boat belongs to the user.
    boolean existsByIdAndOwner(Long boatId, User owner);
}
