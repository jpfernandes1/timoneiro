package com.jompastech.backend.model.dto.basicDTO;

import com.jompastech.backend.model.entity.Address;
import com.jompastech.backend.model.entity.Boat;
import lombok.Data;

@Data
public class BoatBasicDTO {
    private Long id;
    private String name;
    private String type;
    private Address address;

    // For Boat Entity
    public BoatBasicDTO(Boat boat) {
        this.id = boat.getId();
        this.name = boat.getName();
        this.type = boat.getType();
        this.address = boat.getAddress();
    }

    // For Mapper
    public BoatBasicDTO(Long id, String name, String type, Address address) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.address = address;
    }

    // For serialization
    public BoatBasicDTO() {}

}