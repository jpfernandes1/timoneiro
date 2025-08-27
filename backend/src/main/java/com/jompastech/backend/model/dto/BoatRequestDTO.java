package com.jompastech.backend.model.dto;

import com.jompastech.backend.model.entity.Address;
import com.jompastech.backend.model.entity.User;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BoatRequestDTO {
    private String name;
    private String description;
    private String type;
    private int capacity;
    private BigDecimal pricePerHour;
    private Long addressId;
    private String photoUrl;
    private Long ownerId;
}
