package com.jompastech.backend.model.dto;

import lombok.Data;

@Data
public class AddressRequestDTO {

    private String cep;
    private String number;
    private String street;
    private String neighborhood;
    private String city;
    private String state;
}
