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

    @OneToMany(mappedBy = "boat", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("ordem ASC")
    private List<BoatPhoto> photos = new ArrayList<>();

    // helper to add photos maintaining bidirectional relationship
    public void addPhoto(BoatPhoto photo) {
        photos.add(photo);
        photo.setBoat(this);
    }

    // helper to remove photos
    public void removePhoto(BoatPhoto photo) {
        photos.remove(photo);
        photo.setBoat(null);
    }

    // Method to remove all photos
    public void clearPhotos() {
        photos.forEach(photo -> photo.setBoat(null));
        photos.clear();
    }

}
