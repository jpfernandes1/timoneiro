package com.jompastech.backend.service;

import com.jompastech.backend.model.entity.BoatAvailability;
import com.jompastech.backend.repository.BoatAvailabilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

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
@Service
@RequiredArgsConstructor
public class BoatAvailabilityService {

    private final BoatAvailabilityRepository boatAvailabilityRepository;

    /**
     * Creates and persists a new boat availability slot.
     *
     * @param availability the availability entity to be created
     * @return the persisted availability entity with generated ID
     */
    public BoatAvailability createAvailability(BoatAvailability availability){
        return boatAvailabilityRepository.save(availability);
    }

    /**
     * Retrieves an availability slot by its unique identifier.
     *
     * @param id the ID of the availability slot to retrieve
     * @return the found availability entity
     * @throws RuntimeException if no availability is found with the given ID
     */
    public BoatAvailability findById(Long id) {
        return boatAvailabilityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Availability not found with id: " + id));
    }

    /**
     * Retrieves all availability slots for a specific boat.
     *
     * @param boatId the ID of the boat to find availabilities for
     * @return list of availability slots for the specified boat
     */
    public List<BoatAvailability> findAvailabilityByBoatId(Long boatId) {
        return boatAvailabilityRepository.findByBoatId(boatId);
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
        return boatAvailabilityRepository.existsByBoatIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                boatId, startDate, endDate);
    }

    /**
     * Updates the date range of an existing availability slot.
     *
     * @param id the ID of the availability slot to update
     * @param newStartDate the new start date for the availability slot
     * @param newEndDate the new end date for the availability slot
     * @return the updated availability entity
     * @throws RuntimeException if no availability is found with the given ID
     */
    public BoatAvailability updateAvailability(Long id, LocalDateTime newStartDate, LocalDateTime newEndDate ){
        BoatAvailability availability = findById(id);
        availability.setStartDate(newStartDate);
        availability.setEndDate(newEndDate);
        return boatAvailabilityRepository.save(availability);
    }

    /**
     * Deletes a specific availability slot by its ID.
     *
     * @param id the ID of the availability slot to delete
     */
    public void deleteAvailability(Long id){
        boatAvailabilityRepository.deleteById(id);
    }

    /**
     * Deletes all availability slots for a specific boat.
     *
     * @param boatId the ID of the boat whose availability slots should be deleted
     */
    public void deleteByBoatId(Long boatId){
        boatAvailabilityRepository.deleteAll(findAvailabilityByBoatId(boatId));
    }
}