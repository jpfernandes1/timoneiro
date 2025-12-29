package com.jompastech.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO representing a boat availability window in response data.
 *
 * <p>Includes the unique identifier and associated boat ID
 * along with the availability details.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoatAvailabilityResponseDTO {

    /**
     * Unique identifier of the availability window.
     */
    private Long id;

    /**
     * ID of the boat this availability window belongs to.
     */
    private Long boatId;

    /**
     * Start date and time of the availability window.
     */
    private LocalDateTime startDate;

    /**
     * End date and time of the availability window.
     */
    private LocalDateTime endDate;

    /**
     * Price per hour for bookings within this window.
     */
    private BigDecimal pricePerHour;
}