package com.jompastech.backend.controller;

import com.jompastech.backend.mapper.BoatMapper;
import com.jompastech.backend.model.dto.BoatRequestDTO;
import com.jompastech.backend.model.dto.BoatResponseDTO;
import com.jompastech.backend.model.entity.Boat;
import com.jompastech.backend.service.BoatService;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/boats")
@RequiredArgsConstructor
public class BoatController {

    private final BoatService boatService;
    private final BoatMapper boatMapper;

    @PostMapping
    public ResponseEntity<BoatResponseDTO> createBoat(@RequestBody BoatRequestDTO dto) {
        Boat boat = boatMapper.toEntity(dto);
        Boat saved = boatService.save(boat);
        return ResponseEntity.ok(boatMapper.toResponseDTO(saved));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BoatResponseDTO> getBoat(@PathVariable Long id) {
        Boat boat = boatService.findById(id);
        return ResponseEntity.ok(boatMapper.toResponseDTO(boat));
    }

}
