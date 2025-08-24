package com.jompastech.backend.model.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

public class BoatAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "availability_id")
    private long id;

    @ManyToOne
    private Boat boat_id;

    private LocalDateTime start_date;
    private LocalDateTime end_date;

}
