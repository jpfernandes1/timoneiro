package com.jompastech.backend.service;

import com.jompastech.backend.model.entity.Boat;
import com.jompastech.backend.repository.BoatRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BoatService {

    private final BoatRepository boatRepository;

    public BoatService(BoatRepository boatRepository) {
        this.boatRepository = boatRepository;
    }

    // CRUD básico
    public Boat save(Boat boat) {
        return boatRepository.save(boat);
    }

    public Boat findById(Long id) {
        return boatRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Boat não encontrado"));
    }

    public List<Boat> findAll() {
        return boatRepository.findAll();
    }

    public void deleteById(Long id) {
        boatRepository.deleteById(id);
    }

    // Exemplos de métodos adicionais de negócio
    public List<Boat> findByType(String type) {
        return boatRepository.findByType(type);
    }

    public List<Boat> findByOwnerId(Long ownerId) {
        return boatRepository.findByOwnerId(ownerId);
    }
}
