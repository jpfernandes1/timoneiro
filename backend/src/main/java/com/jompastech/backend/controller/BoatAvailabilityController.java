package com.jompastech.backend.controller;

import com.jompastech.backend.exception.AvailabilityNotFoundException;
import com.jompastech.backend.model.dto.BoatAvailabilityRequestDTO;
import com.jompastech.backend.model.dto.BoatAvailabilityResponseDTO;
import com.jompastech.backend.service.BoatAvailabilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/boats/{boatId}/availability")
@RequiredArgsConstructor
public class BoatAvailabilityController {

    private final BoatAvailabilityService availabilityService;

    @PostMapping
    public ResponseEntity<BoatAvailabilityResponseDTO> createAvailability(
            @PathVariable Long boatId,
            @RequestBody BoatAvailabilityRequestDTO requestDTO,
            @AuthenticationPrincipal String email) {

        log.info("POST /api/boats/{}/availability called", boatId);

        try {
            var responseDTO = availabilityService.createAvailability(boatId, requestDTO, email);
            return ResponseEntity.ok(responseDTO);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not authorized") || e.getMessage().contains("not the owner")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            } else if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @GetMapping
    public ResponseEntity<List<BoatAvailabilityResponseDTO>> getBoatAvailabilities(@PathVariable Long boatId) {
        log.info("GET /api/boats/{}/availability called", boatId);
        try {
            var availabilities = availabilityService.findAvailabilityByBoatId(boatId);
            return ResponseEntity.ok(availabilities);
        } catch (Exception e) {
            log.error("Failed to get availabilities for boat ID {}: {}", boatId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<BoatAvailabilityResponseDTO> getAvailability(@PathVariable Long id) {
        log.info("GET /api/boats/{}/availability/{} called", "{boatId}", id);

        try {
            var availability = availabilityService.findById(id);
            return ResponseEntity.ok(availability);
        } catch (AvailabilityNotFoundException e) {
            log.error("Availability not found with ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (RuntimeException e) {
            log.error("Failed to get availability with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<BoatAvailabilityResponseDTO> updateAvailability(
            @PathVariable Long id,
            @RequestBody BoatAvailabilityRequestDTO requestDTO) {

        log.info("PUT /api/boats/{}/availability/{} called", "{boatId}", id);

        try {
            var updatedAvailability = availabilityService.updateAvailability(id, requestDTO);
            return ResponseEntity.ok(updatedAvailability);
        } catch (AvailabilityNotFoundException e) {
            log.error("Availability not found with ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (RuntimeException e) {
            log.error("Failed to update availability with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAvailability(@PathVariable Long id) {
        log.info("DELETE /api/boats/{}/availability/{} called", "{boatId}", id);
        try {
            availabilityService.deleteAvailability(id);
            return ResponseEntity.noContent().build();
        } catch (AvailabilityNotFoundException e) {
            log.error("Availability not found with ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/check-availability")
    public ResponseEntity<Boolean> checkAvailability(
            @PathVariable Long boatId,
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate) {

        log.info("GET /api/boats/{}/availability/check-availability?startDate={}&endDate={} called",
                boatId, startDate, endDate);

        if (startDate.isAfter(endDate)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(false);
        }

        var isAvailable = availabilityService.isBoatAvailable(boatId, startDate, endDate);
        return ResponseEntity.ok(isAvailable);
    }
}