package com.jompastech.backend.model.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO for booking creation requests.
 *
 * <p>Contains the essential data required to create a new booking,
 * with validation annotations to ensure data integrity at the API level.</p>
 *
 * <p><b>Validation Strategy:</b> Uses JSR-303 annotations for basic validation
 * and includes domain-specific validation methods for business rules.</p>
 */
@Data
public class BookingRequestDTO {

    /**
     * ID of the user making the booking.
     * Required to associate the booking with an existing user.
     */
    @NotNull(message = "User ID is required")
    private Long userId;

    /**
     * ID of the boat being booked.
     * Required to identify the specific boat for the reservation.
     */
    @NotNull(message = "Boat ID is required")
    private Long boatId;

    /**
     * Start date and time of the booking period.
     * Must be in the future to prevent booking past dates.
     */
    @NotNull(message = "Start date is required")
    @Future(message = "Start date must be in the future")
    private LocalDateTime startDate;

    /**
     * End date and time of the booking period.
     * Must be in the future and after the start date.
     */
    @NotNull(message = "End date is required")
    @Future(message = "End date must be in the future")
    private LocalDateTime endDate;

    /**
     * Validates that the date range is logically consistent.
     *
     * <p>This method provides domain-specific validation beyond basic JSR-303 constraints,
     * ensuring the start date precedes the end date as required by business rules.</p>
     *
     * @return true if the date range is valid (start before end), false otherwise
     */
    public boolean hasValidDateRange() {
        return startDate != null && endDate != null &&
                startDate.isBefore(endDate);
    }
}