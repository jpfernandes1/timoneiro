package com.jompastech.backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name="address")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private long id;

    private String cep;
    private int number;
    private String street;
    private String neighborhood;
    private String city;
    private String state;

}
