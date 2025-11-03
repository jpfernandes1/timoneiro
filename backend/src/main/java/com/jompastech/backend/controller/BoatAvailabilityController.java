package com.jompastech.backend.controller;

import com.jompastech.backend.model.entity.Boat;
import com.jompastech.backend.model.entity.BoatAvailability;
import com.jompastech.backend.service.BoatAvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/boats/{boatId}/availability")
@RequiredArgsConstructor
public class BoatAvailabilityController {

    private final BoatAvailabilityService availabilityService;

    @PostMapping
    public ResponseEntity<BoatAvailability> createAvailability(
            @PathVariable Long boatID,
            @RequestBody BoatAvailability availability){
        availability.getBoat().setId(boatID); // Associates boatId
        BoatAvailability saved = availabilityService.createAvailability(availability);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<List<BoatAvailability>> getBoatAvailabilities(@PathVariable Long boatId){
        return ResponseEntity.ok(availabilityService.findAvailabilityByBoatId(boatId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BoatAvailability> getAvailability(@PathVariable Long id){
        return ResponseEntity.ok(availabilityService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BoatAvailability> updateAvailability(
            @PathVariable Long id,
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate) {
        return ResponseEntity.ok(availabilityService.updateAvailability(id, startDate, endDate));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAvailability(@PathVariable Long id){
        availabilityService.deleteAvailability(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/check-availability")
    public ResponseEntity<Boolean> checkAvailability(
            @PathVariable Long boatId,
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate) {
        return ResponseEntity.ok(availabilityService.isBoatAvailable(boatId, startDate, endDate));
    }

    }

