package com.jompastech.backend.model.entity;

import com.jompastech.backend.model.enums.BookingStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boat_id", nullable = false)
    private Boat boat;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "total_price", precision = 10, scale = 2)
    private BigDecimal totalPrice;

    // Protected constructor for JPA
    protected Booking() {}

    // Main constructor for creation via Application Service
    public Booking(User user, Boat boat, LocalDateTime startDate,
                   LocalDateTime endDate, BigDecimal totalPrice) {
        validateBasicInvariants(user, boat, startDate, endDate, totalPrice);
        this.user = user;
        this.boat = boat;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalPrice = totalPrice;
        this.status = BookingStatus.PENDING;
    }

    private void validateBasicInvariants(User user, Boat boat, LocalDateTime startDate,
                                         LocalDateTime endDate, BigDecimal totalPrice) {
        if (user == null || boat == null) {
            throw new IllegalArgumentException("User and Boat are required");
        }
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Dates are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
        if (startDate.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("It is not possible to book in the past");
        }
        if (totalPrice == null || totalPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price must be non-negative");
        }
    }

    // Domain Methods
    public void confirm() {
        if (this.status != BookingStatus.PENDING) {
            throw new IllegalStateException("Only pending reservations can be confirmed.");
        }
        this.status = BookingStatus.CONFIRMED;
    }

    public void cancel() {
        if (this.status == BookingStatus.CANCELLED || this.status == BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Reservation is already canceled or completed");
        }
        this.status = BookingStatus.CANCELLED;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Boat getBoat() { return boat; }
    public LocalDateTime getStartDate() { return startDate; }
    public LocalDateTime getEndDate() { return endDate; }
    public BookingStatus getStatus() { return status; }
    public BigDecimal getTotalPrice() { return totalPrice; }

    @PrePersist
    public void prePersist() {
        if (status == null) {
            status = BookingStatus.PENDING;
        }
    }

    /**
     * Checks if this booking overlaps with another booking timeframe.
     * Useful for conflict detection within the same boat.
     *
     * @param otherStart the start date of the other booking
     * @param otherEnd the end date of the other booking
     * @return true if the time periods overlap, false otherwise
     */
    public boolean overlapsWith(LocalDateTime otherStart, LocalDateTime otherEnd) {
        return this.startDate.isBefore(otherEnd) &&
                this.endDate.isAfter(otherStart);
    }

    /**
     * Validates if the booking duration meets minimum business requirements.
     * This is a basic domain rule that doesn't require external dependencies.
     *
     * @return true if duration is valid according to business rules
     */
    public boolean hasValidDuration() {
        long hours = java.time.Duration.between(startDate, endDate).toHours();
        return hours >= 4; // Minimum 4-hour booking
    }
}