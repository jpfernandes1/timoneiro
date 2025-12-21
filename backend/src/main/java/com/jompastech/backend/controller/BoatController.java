package com.jompastech.backend.controller;

import com.jompastech.backend.model.dto.BoatRequestDTO;
import com.jompastech.backend.model.dto.BoatResponseDTO;
import com.jompastech.backend.service.BoatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boats")
@RequiredArgsConstructor
public class BoatController {

    private final BoatService boatService;

    @PostMapping
    public ResponseEntity<BoatResponseDTO> createBoat(@Valid @RequestBody BoatRequestDTO dto) {
        BoatResponseDTO saved = boatService.save(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BoatResponseDTO> getBoat(@PathVariable Long id) {
        BoatResponseDTO boat = boatService.findById(id);
        return ResponseEntity.ok(boat);
    }

    @GetMapping
    public ResponseEntity<List<BoatResponseDTO>> getAllBoats() {
        return ResponseEntity.ok(boatService.findAllBoats());
    }
}