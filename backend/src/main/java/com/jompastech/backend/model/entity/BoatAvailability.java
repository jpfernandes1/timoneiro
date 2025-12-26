package com.jompastech.backend.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;

/**
 * Represents a time window during which a boat is available for booking
 * with specific pricing for that period.
 *
 * <p>This entity enables dynamic pricing strategies where boat owners can
 * set different hourly rates for different time periods (e.g., high season,
 * weekends, holidays). Each availability window has its own price per hour
 * which overrides the boat's base price during that window.</p>
 *
 * <p><b>Design Rationale:</b> Separating availability from the boat entity
 * allows for flexible pricing models and better inventory management.
 * The pricePerHour field enables seasonal and demand-based pricing.</p>
 */
@Entity
@Data
@NoArgsConstructor
@Table(name="boats_availability")
public class BoatAvailability {

    /**
     * Unique identifier for the availability window.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "availability_id")

    private Long id;

    /**
     * Boat associated with this availability window.
     *
     * <p>Many-to-one relationship as a boat can have multiple
     * availability windows with different pricing.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boat_id", nullable = false)
    private Boat boat;

    /**
     * Start date and time of the availability window.
     *
     * <p>Defines when the boat becomes available for booking
     * at the specific price per hour for this window.</p>
     */
    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    /**
     * End date and time of the availability window.
     *
     * <p>Defines when this specific pricing window ends.
     * Bookings must be completely within this window to use
     * this price per hour.</p>
     */
    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    /**
     * Price per hour for bookings within this availability window.
     *
     * <p>Overrides the boat's base pricePerHour during this specific
     * time period. Allows for dynamic pricing strategies based on
     * season, demand, or special events.</p>
     */
    @Column(name = "price_per_hour", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerHour;

    /**
     * Constructs a new availability window with validation.
     *
     * <p>Validates that start date is before end date and that
     * price per hour is a positive value. These validations ensure
     * data integrity at the entity level.</p>
     *
     * @param boat the boat associated with this availability
     * @param startDate start of the availability window
     * @param endDate end of the availability window
     * @param pricePerHour specific price per hour for this window
     * @throws IllegalArgumentException if dates are invalid or price is not positive
     */
    public BoatAvailability(Boat boat, LocalDateTime startDate,
                            LocalDateTime endDate, BigDecimal pricePerHour) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }
        if (pricePerHour == null || pricePerHour.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price per hour must be positive");
        }
        this.boat = boat;
        this.startDate = startDate;
        this.endDate = endDate;
        this.pricePerHour = pricePerHour;
    }

    /**
     * Gets the day of week for the start of this availability window.
     *
     * <p>Useful for business rules based on weekday vs weekend pricing
     * or for UI display purposes.</p>
     *
     * @return DayOfWeek for the window start date
     */
    public DayOfWeek getDayOfWeek() {
        return startDate.getDayOfWeek();
    }

    /**
     * Checks if this availability window covers a specific period.
     *
     * <p>Determines whether a requested booking period falls completely
     * within this availability window. Used for validating booking
     * requests against availability constraints.</p>
     *
     * @param checkStart start of the period to check
     * @param checkEnd end of the period to check
     * @return true if the period is completely within this window, false otherwise
     */
    public boolean coversPeriod(LocalDateTime checkStart, LocalDateTime checkEnd) {
        return !checkStart.isBefore(startDate) && !checkEnd.isAfter(endDate);
    }

    /**
     * Calculates total price for a booking period within this window.
     *
     * <p>Computes price based on this window's specific price per hour
     * and the duration of the booking. Ensures the requested period
     * falls within this availability window before calculating.</p>
     *
     * @param periodStart start of the booking period
     * @param periodEnd end of the booking period
     * @return total price for the period based on this window's hourly rate
     * @throws IllegalArgumentException if period is not within this window
     */
    public BigDecimal calculatePriceForPeriod(LocalDateTime periodStart, LocalDateTime periodEnd) {
        if (!coversPeriod(periodStart, periodEnd)) {
            throw new IllegalArgumentException(
                    "Requested period is not within this availability window");
        }

        long hours = java.time.Duration.between(periodStart, periodEnd).toHours();
        if (hours < 1) {
            hours = 1; // Minimum charge of 1 hour for any booking
        }

        return pricePerHour.multiply(BigDecimal.valueOf(hours));
    }
}