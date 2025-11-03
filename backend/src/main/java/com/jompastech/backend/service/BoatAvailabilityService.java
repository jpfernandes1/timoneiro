package com.jompastech.backend.service;

import com.jompastech.backend.model.entity.BoatAvailability;
import com.jompastech.backend.repository.BoatAvailabilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BoatAvailabilityService {

    private final BoatAvailabilityRepository boatAvailabilityRepository;

    //CREATE
    public BoatAvailability createAvailability(BoatAvailability availability){
        return boatAvailabilityRepository.save(availability);
    }

    //READ
    public BoatAvailability findById(Long id) {
        return boatAvailabilityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Availability not found with id: " + id));
    }

    public List<BoatAvailability> findAvailabilityByBoatId(Long boatId) {
        return boatAvailabilityRepository.findByBoatId(boatId);
    }

    public List<BoatAvailability> findAvailableSlots(Long boatId, LocalDateTime startDate, LocalDateTime endDate) {
        return boatAvailabilityRepository.findByBoatIdAndStartDateAfterAndEndDateBefore(boatId, startDate, endDate);
    }

    public boolean isBoatAvailable(Long boatId, LocalDateTime startDate, LocalDateTime endDate) {
        return !boatAvailabilityRepository.existsByBoatIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                boatId, endDate, startDate);
    }

    // UPDATE
    public BoatAvailability updateAvailability(Long id, LocalDateTime newStartDate, LocalDateTime newEndDate ){
        BoatAvailability availability = findById(id);
        availability.setStartDate(newStartDate);
        availability.setEndDate(newEndDate);
        return boatAvailabilityRepository.save(availability);
    }

    // DELETE
    public void deleteAvailability(Long id){
        boatAvailabilityRepository.deleteById(id);
    }

    public void deleteByBoatId(Long boatId){
        boatAvailabilityRepository.deleteAll(findAvailabilityByBoatId(boatId));
    }

}
