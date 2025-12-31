package com.jompastech.backend.model.dto;

import com.jompastech.backend.model.entity.Address;
import com.jompastech.backend.model.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class BoatResponseDTO {
    private Long id;
    private String name;
    private String description;
    private String type;
    private int capacity;
    private Double length;
    private Double speed;
    private Integer fabrication; // year
    private List<String> amenities;
    private List<String> photos;
    private BigDecimal pricePerHour;
    private String city; // from address
    private String state; // from address
    private String marina;
    private String ownerName; // from User
    private Long ownerId;
}
