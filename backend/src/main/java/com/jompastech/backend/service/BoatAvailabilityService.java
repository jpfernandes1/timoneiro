// BoatAvailabilityService.java - Versão atualizada
package com.jompastech.backend.service;

import com.jompastech.backend.model.dto.BoatAvailabilityRequestDTO;
import com.jompastech.backend.model.dto.BoatAvailabilityResponseDTO;
import com.jompastech.backend.model.entity.BoatAvailability;
import com.jompastech.backend.repository.BoatAvailabilityRepository;
import com.jompastech.backend.repository.BoatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for managing boat availability operations.
 *
 * <p>Provides business logic for:
 * <ul>
 *     <li>Creating, reading, updating, and deleting boat availability slots</li>
 *     <li>Checking boat availability for specific time periods</li>
 *     <li>Finding available slots within date ranges</li>
 *     <li>Managing all availability slots for specific boats</li>
 * </ul>
 * </p>
 *
 * <p>This service coordinates between the controller layer and repository layer,
 * enforcing business rules and handling exceptions for availability management.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BoatAvailabilityService {

    private final BoatAvailabilityRepository boatAvailabilityRepository;
    private final BoatRepository boatRepository;

    /**
     * Creates and persists a new boat availability slot.
     *
     * @param boatId the ID of the boat to create availability for
     * @param requestDTO the availability data to be created
     * @return the created availability as a response DTO
     */
    @Transactional
    public BoatAvailabilityResponseDTO createAvailability(Long boatId, BoatAvailabilityRequestDTO requestDTO) {
        log.info("Creating availability for boat ID: {}", boatId);

        var boat = boatRepository.findById(boatId)
                .orElseThrow(() -> new RuntimeException("Boat not found with id: " + boatId));

        var availability = new BoatAvailability(
                boat,
                requestDTO.getStartDate(),
                requestDTO.getEndDate(),
                requestDTO.getPricePerHour()
        );

        availability = boatAvailabilityRepository.save(availability);
        log.info("Availability created with ID: {}", availability.getId());

        return convertToResponseDTO(availability);
    }

    /**
     * Retrieves an availability slot by its unique identifier.
     *
     * @param id the ID of the availability slot to retrieve
     * @return the found availability as a response DTO
     * @throws RuntimeException if no availability is found with the given ID
     */
    public BoatAvailabilityResponseDTO findById(Long id) {
        log.info("Finding availability by ID: {}", id);

        var availability = boatAvailabilityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Availability not found with id: " + id));

        return convertToResponseDTO(availability);
    }

    /**
     * Retrieves all availability slots for a specific boat.
     *
     * @param boatId the ID of the boat to find availabilities for
     * @return list of availability response DTOs for the specified boat
     */
    public List<BoatAvailabilityResponseDTO> findAvailabilityByBoatId(Long boatId) {
        log.info("Finding all availabilities for boat ID: {}", boatId);

        var availabilities = boatAvailabilityRepository.findByBoatId(boatId);
        return availabilities.stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Finds available slots for a boat within a specific date range.
     *
     * @param boatId the ID of the boat to search availability for
     * @param startDate the start date of the search range (inclusive)
     * @param endDate the end date of the search range (inclusive)
     * @return list of availability slots that fall within the specified range
     */
    public List<BoatAvailability> findAvailableSlots(Long boatId, LocalDateTime startDate, LocalDateTime endDate) {
        return boatAvailabilityRepository.findByBoatIdAndStartDateAfterAndEndDateBefore(boatId, startDate, endDate);
    }

    /**
     * Checks if a boat is available during a specific time period.
     *
     * @param boatId the ID of the boat to check availability for
     * @param startDate the start date of the requested period
     * @param endDate the end date of the requested period
     * @return true if the boat is available (no overlapping bookings), false otherwise
     */
    public boolean isBoatAvailable(Long boatId, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Checking availability for boat ID: {} from {} to {}", boatId, startDate, endDate);

        boolean hasOverlap = boatAvailabilityRepository.existsByBoatIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                boatId, endDate, startDate);
        // If there is no overlap, the boat is available
        return !hasOverlap;
    }

    /**
     * Updates the date range and price of an existing availability slot.
     *
     * @param id the ID of the availability slot to update
     * @param requestDTO the updated availability data
     * @return the updated availability as a response DTO
     * @throws RuntimeException if no availability is found with the given ID
     */
    @Transactional
    public BoatAvailabilityResponseDTO updateAvailability(Long id, BoatAvailabilityRequestDTO requestDTO) {
        log.info("Updating availability with ID: {}", id);

        var availability = boatAvailabilityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Availability not found with id: " + id));

        availability.setStartDate(requestDTO.getStartDate());
        availability.setEndDate(requestDTO.getEndDate());
        availability.setPricePerHour(requestDTO.getPricePerHour());

        availability = boatAvailabilityRepository.save(availability);
        log.info("Availability updated with ID: {}", availability.getId());

        return convertToResponseDTO(availability);
    }

    /**
     * Deletes a specific availability slot by its ID.
     *
     * @param id the ID of the availability slot to delete
     */
    @Transactional
    public void deleteAvailability(Long id) {
        log.info("Deleting availability with ID: {}", id);

        if (!boatAvailabilityRepository.existsById(id)) {
            throw new RuntimeException("Availability not found with id: " + id);
        }
        boatAvailabilityRepository.deleteById(id);
    }

    /**
     * Deletes all availability slots for a specific boat.
     *
     * @param boatId the ID of the boat whose availability slots should be deleted
     */
    @Transactional
    public void deleteByBoatId(Long boatId) {
        log.info("Deleting all availabilities for boat ID: {}", boatId);

        var availabilities = boatAvailabilityRepository.findByBoatId(boatId);
        boatAvailabilityRepository.deleteAll(availabilities);
        log.info("Deleted {} availabilities for boat ID: {}", availabilities.size(), boatId);
    }

    /**
     * Converts a BoatAvailability entity to a response DTO.
     *
     * @param availability the availability entity to convert
     * @return the response DTO representation
     */
    private BoatAvailabilityResponseDTO convertToResponseDTO(BoatAvailability availability) {
        return new BoatAvailabilityResponseDTO(
                availability.getId(),
                availability.getBoat().getId(),
                availability.getStartDate(),
                availability.getEndDate(),
                availability.getPricePerHour()
        );
    }
}