package com.jompastech.backend.model.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name="addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private long id;

    private String cep;
    private String number;
    private String street;
    private String neighborhood;
    private String city;
    private String state;

}
