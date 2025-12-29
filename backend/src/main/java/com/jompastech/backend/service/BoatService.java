package com.jompastech.backend.service;

import com.jompastech.backend.mapper.BoatMapper;
import com.jompastech.backend.model.dto.BoatRequestDTO;
import com.jompastech.backend.model.dto.BoatResponseDTO;
import com.jompastech.backend.model.entity.Address;
import com.jompastech.backend.model.entity.Boat;
import com.jompastech.backend.model.entity.BoatPhoto;
import com.jompastech.backend.model.entity.User;
import com.jompastech.backend.repository.AddressRepository;
import com.jompastech.backend.repository.BoatRepository;
import com.jompastech.backend.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
@Slf4j
public class BoatService {

    private final BoatRepository boatRepository;
    private final AddressRepository addressRepository;
    private final BoatMapper boatMapper;

    @Autowired
    private UserRepository userRepository;

    /**
     * Saves a new boat with associated photos.
     */
    @Transactional
    public BoatResponseDTO saveWithPhotos(BoatRequestDTO boatRequestDTO,
                                          List<BoatPhoto> photos,
                                          UserDetails userDetails) {

        // 1. Find user by Email
        String username = userDetails.getUsername();
        User owner = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found: " + username
                ));

        // 2. Create Address from DTO data
        Address address = new Address();
        address.setCep(boatRequestDTO.getCep());
        address.setNumber(boatRequestDTO.getNumber());
        address.setStreet(boatRequestDTO.getStreet());
        address.setNeighborhood(boatRequestDTO.getNeighborhood());
        address.setCity(boatRequestDTO.getCity());
        address.setState(boatRequestDTO.getState());
        address.setMarina(boatRequestDTO.getMarina());

        Address savedAddress = addressRepository.save(address);

        // 3. Convert DTO to Entity (ignoring photos in mapper)
        Boat boat = boatMapper.toEntity(boatRequestDTO);
        boat.setOwner(owner);
        boat.setAddress(savedAddress);

        // 4. Associate photos with the boat (bidirectional relationship)
        if (photos != null && !photos.isEmpty()) {
            for (BoatPhoto photo : photos) {
                boat.addPhoto(photo); // This sets the boat in the photo
            }
        }

        // 5. Save boat (cascade will save photos)
        Boat savedBoat = boatRepository.save(boat);
        return boatMapper.toResponseDTO(savedBoat);
    }

    /**
     * Saves a boat without photos (for compatibility with existing code).
     */
    @Transactional
    public BoatResponseDTO save(BoatRequestDTO boatRequestDTO, UserDetails userDetails) {
        return saveWithPhotos(boatRequestDTO, List.of(), userDetails);
    }

    @Transactional(readOnly = true)
    public BoatResponseDTO findById(Long id) {
        Boat boat = boatRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Boat not found"));
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

    // Additional business methods - Return entities for internal use
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

    // Auxiliary method for internal use (if other services need the entity)
    @Transactional(readOnly = true)
    public Boat getBoatEntity(Long id) {
        return boatRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Boat not found"));
    }

    @Operation(
            summary = "Get boats by current user",
            description = "Retrieves a list of boats owned by the currently authenticated user"
    )
    @Transactional(readOnly = true)
    public Page<BoatResponseDTO> findBoatsByUserPaginated(String email, Pageable pageable) {
        log.info("Buscando barcos do usuário com paginação: {}", email);

        // Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado com email: " + email));

        log.info("Usuário encontrado: {} (ID: {})", user.getName(), user.getId());

        // Search for user's boats with pagination.
        Page<Boat> boatsPage = boatRepository.findByOwner(user, pageable);
        log.info("Encontrados {} barcos para o usuário {}", boatsPage.getTotalElements(), email);

        // Convert to DTO
        return boatsPage.map(boatMapper::toResponseDTO);
    }

    /**
     * Updates an existing boat.
     *
     * @param id the ID of the boat to update
     * @param dto the updated boat data
     * @param userDetails the authenticated user details
     * @return the updated boat as a response DTO
     * @throws RuntimeException if the boat is not found or user is not the owner
     */
    @Transactional
    public BoatResponseDTO updateBoat(Long id, BoatRequestDTO dto, UserDetails userDetails) {
        log.info("Updating boat with ID: {}", id);

        // Find the boat
        var boat = boatRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Boat not found with id: " + id));

        // Verify ownership
        var currentUserEmail = userDetails.getUsername();
        if (!boat.getOwner().getEmail().equals(currentUserEmail)) {
            throw new RuntimeException("User is not the owner of this boat");
        }

        // Update boat fields
        boat.setName(dto.getName());
        boat.setDescription(dto.getDescription());
        boat.setType(dto.getType());
        boat.setCapacity(dto.getCapacity());
        boat.setLength(dto.getLength());
        boat.setSpeed(dto.getSpeed());
        boat.setFabrication(dto.getFabrication());
        boat.setAmenities(dto.getAmenities());
        boat.setPricePerHour(dto.getPricePerHour());

        // Update address
        var address = boat.getAddress();
        if (address == null) {
            address = new Address();
            boat.setAddress(address);
        }
        address.setCep(dto.getCep());
        address.setNumber(dto.getNumber());
        address.setStreet(dto.getStreet());
        address.setNeighborhood(dto.getNeighborhood());
        address.setCity(dto.getCity());
        address.setState(dto.getState());
        address.setMarina(dto.getMarina());

        // Save the updated boat
        boat = boatRepository.save(boat);
        log.info("Boat with ID: {} updated successfully", id);

        return boatMapper.toResponseDTO(boat);
    }
}
