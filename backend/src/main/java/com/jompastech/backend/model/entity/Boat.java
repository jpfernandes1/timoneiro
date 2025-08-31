package com.jompastech.backend.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Data
@Table(name="boats")
public class Boat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="boat_id")
    private Long id;

    private String name;
    private String description;
    private String type;
    private int capacity;

    @Column(name="price_per_hour")
    private BigDecimal pricePerHour;

    @ManyToOne
    @JoinColumn(name = "address_id")
    private Address address;

    @Column(name="photo_url")
    private String photoUrl;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

}
