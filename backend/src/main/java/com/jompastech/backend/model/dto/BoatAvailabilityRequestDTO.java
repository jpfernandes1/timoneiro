package com.jompastech.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for creating or updating boat availability windows.
 *
 * <p>Contains all necessary data for defining when a boat is available
 * for booking with specific pricing during that period.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoatAvailabilityRequestDTO {

    /**
     * Start date and time of the availability window.
     * Must be in ISO format (e.g., "2024-12-30T14:30:00").
     */
    private LocalDateTime startDate;

    /**
     * End date and time of the availability window.
     * Must be in ISO format (e.g., "2024-12-30T18:30:00").
     */
    private LocalDateTime endDate;

    /**
     * Price per hour for bookings within this availability window.
     * Must be a positive value greater than zero.
     */
    private BigDecimal pricePerHour;
}