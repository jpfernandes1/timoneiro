package com.jompastech.backend.model.dto;

import com.jompastech.backend.model.entity.Address;
import com.jompastech.backend.model.entity.User;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class BoatResponseDTO {
    private Long id;
    private String name;
    private String description;
    private String type;
    private int capacity;
    private BigDecimal pricePerHour;
    private String photoUrl;
    private String city; // from address
    private String state; // from address
    private String ownerName; // from User
}
