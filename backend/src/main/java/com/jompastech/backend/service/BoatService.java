package com.jompastech.backend.service;

import com.jompastech.backend.mapper.BoatMapper;
import com.jompastech.backend.model.dto.BoatResponseDTO;
import com.jompastech.backend.model.dto.UserResponseDTO;
import com.jompastech.backend.model.entity.Boat;
import com.jompastech.backend.repository.BoatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BoatService {

    private final BoatRepository boatRepository;
    private final BoatMapper boatMapper;

    public BoatService(BoatRepository boatRepository, BoatMapper boatMapper) {
        this.boatRepository = boatRepository;
        this.boatMapper = boatMapper;
    }

    // CRUD
    public Boat save(Boat boat) {
        return boatRepository.save(boat);
    }

    public Boat findById(Long id) {
        return boatRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Boat not found!"));
    }

    /**
     * Finds a boat by ID without throwing exceptions for query operations.
     * Used by other services that need to validate boat existence without exceptions.
     *
     * @param boatId the boat identifier
     * @return Optional containing boat if found, empty otherwise
     */
    @Transactional(readOnly = true)
    public Optional<Boat> findByIdOptional(Long boatId) {
        return boatRepository.findById(boatId);
    }

    public List<Boat> findAll() {
        return boatRepository.findAll();
    }

    public void deleteById(Long id) {
        boatRepository.deleteById(id);
    }

    // Aditional business methods
    public List<Boat> findByType(String type) {
        return boatRepository.findByType(type);
    }

    public List<Boat> findByOwnerId(Long ownerId) {
        return boatRepository.findByOwnerId(ownerId);
    }

    public List<BoatResponseDTO> findAllBoats() {
        return boatRepository.findAll()
                .stream()
                .map(boatMapper::toResponseDTO)
                .collect(Collectors.toList());
    }
}
