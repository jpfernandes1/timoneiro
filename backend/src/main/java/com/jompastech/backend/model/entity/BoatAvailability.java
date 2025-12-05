package com.jompastech.backend.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name="boats_availability")
public class BoatAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "availability_id")
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boat_id", nullable = false)
    private Boat boat;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    // Validations
    public BoatAvailability(Boat boat, LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }
        this.boat = boat;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // Util method to extract day os week
    public DayOfWeek getDayOfWeek() {
        return startDate.getDayOfWeek();
    }

    // Method to check if a period is on this availability
    public boolean coversPeriod(LocalDateTime checkStart, LocalDateTime checkEnd) {
        return !checkStart.isBefore(startDate) && !checkEnd.isAfter(endDate);
    }

}
