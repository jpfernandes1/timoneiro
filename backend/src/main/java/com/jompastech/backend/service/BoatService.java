package com.jompastech.backend.service;

import com.jompastech.backend.mapper.BoatMapper;
import com.jompastech.backend.model.dto.BoatRequestDTO;
import com.jompastech.backend.model.dto.BoatResponseDTO;
import com.jompastech.backend.model.entity.Address;
import com.jompastech.backend.model.entity.Boat;
import com.jompastech.backend.model.entity.User;
import com.jompastech.backend.repository.AddressRepository;
import com.jompastech.backend.repository.BoatRepository;
import com.jompastech.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoatService {

    private final BoatRepository boatRepository;
    private final AddressRepository addressRepository;
    private final BoatMapper boatMapper;

    @Autowired
    private UserRepository userRepository;

    // CRUD - Retorna DTOs para a API
    @Transactional
    public BoatResponseDTO save(BoatRequestDTO boatRequestDTO,  UserDetails userDetails) {

        // 1. Find user by Email
        String username = userDetails.getUsername();
        User owner = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuário não encontrado: " + username
                ));

        // 2. Criar Address a partir dos dados do formulário
        Address address = new Address();

        address.setCep(boatRequestDTO.getCep());
        address.setNumber(boatRequestDTO.getNumber());
        address.setStreet(boatRequestDTO.getStreet());
        address.setNeighborhood(boatRequestDTO.getNeighborhood());
        address.setCity(boatRequestDTO.getCity());
        address.setState(boatRequestDTO.getState());
        address.setMarina(boatRequestDTO.getMarina());

        Address savedAddress = addressRepository.save(address);

        // 2. Convert DTO to Entity
        Boat boat = boatMapper.toEntity(boatRequestDTO);
        boat.setOwner(owner);
        boat.setAddress(savedAddress);
        // TODO: Associar owner do usuário logado
        // boat.setOwner(currentUser);

        // 3. Salvar e retornar DTO
        Boat savedBoat = boatRepository.save(boat);
        return boatMapper.toResponseDTO(savedBoat);
    }

    @Transactional(readOnly = true)
    public BoatResponseDTO findById(Long id) {
        Boat boat = boatRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Barco não encontrado"));
        return boatMapper.toResponseDTO(boat);
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

    @Transactional(readOnly = true)
    public List<Boat> findAll() {
        return boatRepository.findAll();
    }

    @Transactional
    public void deleteById(Long id) {
        boatRepository.deleteById(id);
    }

    // Additional business methods - Retornam entidades para uso interno
    @Transactional(readOnly = true)
    public List<Boat> findByType(String type) {
        return boatRepository.findByType(type);
    }

    @Transactional(readOnly = true)
    public List<Boat> findByOwnerId(Long ownerId) {
        return boatRepository.findByOwnerId(ownerId);
    }

    @Transactional(readOnly = true)
    public List<BoatResponseDTO> findAllBoats() {
        return boatRepository.findAll()
                .stream()
                .map(boatMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    // Método auxiliar para uso interno (se outros serviços precisarem da entidade)
    @Transactional(readOnly = true)
    public Boat getBoatEntity(Long id) {
        return boatRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Barco não encontrado"));
    }
}