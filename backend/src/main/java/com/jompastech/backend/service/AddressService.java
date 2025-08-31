package com.jompastech.backend.service;

import com.jompastech.backend.model.dto.AddressRequestDTO;
import com.jompastech.backend.model.entity.Address;
import com.jompastech.backend.repository.AddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressService {

    private final AddressRepository addressRepository;

    public AddressService(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    // CRUD
    public Address save(Address address){
        return addressRepository.save(address);
    }

    public Address findById(Long id){
        return addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Address not found!"));
    }

    public List<Address> findAll(){ return addressRepository.findAll();}


    // Aditional business methods
    public List<Address> findByCity(String city){
        return addressRepository.findByCity(city);
    }

    public List<Address> findByState(String state){
        return addressRepository.findByState(state);
    }

    // UPDATE
    @Transactional
    public Address update(Long id, AddressRequestDTO dto) {
        Address existingAddress = findById(id);

        // Atualizar campos
        existingAddress.setCep(dto.getCep());
        existingAddress.setNumber(dto.getNumber());
        existingAddress.setStreet(dto.getStreet());
        existingAddress.setNeighborhood(dto.getNeighborhood());
        existingAddress.setCity(dto.getCity());
        existingAddress.setState(dto.getState());

        return addressRepository.save(existingAddress);
    }

    @Transactional
    public Address update(Address address) {
        return addressRepository.save(address);
    }

    // DELETE
    @Transactional
    public void deleteById(Long id) {
        // check if exists
        if (!addressRepository.existsById(id)) {
            throw new RuntimeException("Address not found with ID: " + id);
        }
        addressRepository.deleteById(id);
    }

}
