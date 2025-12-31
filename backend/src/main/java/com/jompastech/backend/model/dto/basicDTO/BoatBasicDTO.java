package com.jompastech.backend.model.dto.basicDTO;

import com.jompastech.backend.model.entity.Address;
import com.jompastech.backend.model.entity.Boat;
import com.jompastech.backend.model.entity.BoatPhoto;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BoatBasicDTO {
    private Long id;
    private String name;
    private String type;
    private Address address;
    private List<String> photos;

    // For Mapper
    public BoatBasicDTO(Long id, String name, String type, Address address, List<String> photos) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.address = address;
        this.photos = photos;
    }

    // For serialization
    public BoatBasicDTO() {}

}