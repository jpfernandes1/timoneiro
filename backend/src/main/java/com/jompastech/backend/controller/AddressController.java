package com.jompastech.backend.controller;

import com.jompastech.backend.mapper.AddressMapper;
import com.jompastech.backend.model.dto.AddressFullResponseDTO;
import com.jompastech.backend.model.dto.AddressRequestDTO;
import com.jompastech.backend.model.dto.AddressResponseDTO;
import com.jompastech.backend.model.entity.Address;
import com.jompastech.backend.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/address")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;
    private final AddressMapper addressMapper;

    @PostMapping
    public ResponseEntity<AddressResponseDTO> createAddress(@RequestBody AddressRequestDTO dto){
        Address address = addressMapper.toEntity(dto);
        Address saved = addressService.save(address);
        return ResponseEntity.ok(addressMapper.toPublicResponseDTO(saved));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AddressResponseDTO> getAddress(@PathVariable Long id){
        Address address = addressService.findById(id);
        return ResponseEntity.ok(addressMapper.toPublicResponseDTO(address));
    }

    @GetMapping("/private/{id}")
    public ResponseEntity<AddressFullResponseDTO> getFullAddress(@PathVariable Long id){
        Address address = addressService.findById(id);
        return ResponseEntity.ok(addressMapper.toFullResponseDTO(address));
    }

    @GetMapping
    public ResponseEntity<List<AddressResponseDTO>> getAllAddresses() {
        List<Address> addresses = addressService.findAll();
        return ResponseEntity.ok(addresses.stream()
                .map(addressMapper::toPublicResponseDTO)
                .toList());
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressResponseDTO> updateAddress(@PathVariable Long id, @RequestBody AddressRequestDTO dto) {
        Address address = addressService.update(id, dto);
        return ResponseEntity.ok(addressMapper.toPublicResponseDTO(address));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long id) {
        addressService.deleteById(id);
        return ResponseEntity.noContent().build();
    }


}
