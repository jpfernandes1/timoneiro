package com.jompastech.backend.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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

    @ElementCollection
    @CollectionTable(name = "boat_photos", joinColumns = @JoinColumn(name = "boat_id"))
    @Column(name = "photo_url")
    private List<String> photos = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    private Double length;
    private Double speed;
    private Integer fabrication;

    @ElementCollection
    @CollectionTable(name = "boat_amenities", joinColumns = @JoinColumn(name = "boat_id"))
    @Column(name = "amenity")
    private List<String> amenities = new ArrayList<>();

}
