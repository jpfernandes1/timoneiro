package com.jompastech.backend.model.dto;

import com.jompastech.backend.model.entity.Address;
import com.jompastech.backend.model.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class BoatRequestDTO {

    private String name;
    private String description;
    private String type;
    private Integer capacity;
    private Double length;
    private Double speed;
    private Integer fabrication; // year
    private List<String> amenities;
    private BigDecimal pricePerHour;

    // Address full data
    private String cep;
    private String number;
    private String street;
    private String neighborhood;
    private String city;
    private String state;
    private String marina;
}