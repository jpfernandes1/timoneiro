package com.jompastech.backend.model.dto.booking;

import com.jompastech.backend.model.dto.payment.MockCardData;
import com.jompastech.backend.model.enums.PaymentMethod;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO for booking creation requests from clients.
 *
 * <p>Contains all data required to initiate a booking creation process,
 * including user selection, boat identification, dates, and payment method.
 * This DTO is validated both at the API level and in the service layer
 * to ensure data integrity.</p>
 *
 * <p><b>Design Note:</b> The userId is typically set by the controller
 * from the authentication context to prevent users from booking on behalf
 * of others. The total price is calculated by the service layer based on
 * dynamic pricing rules.</p>
 */
@Data
public class BookingRequestDTO {

    /**
     * ID of the user making the booking.
     *
     * <p>Usually populated by the controller from the JWT token
     * to ensure users can only book for themselves.</p>
     */
    private Long userId;

    /**
     * ID of the boat to be booked.
     *
     * <p>Must reference an existing, active boat that has availability
     * windows defined for the requested period.</p>
     */
    private Long boatId;

    /**
     * Start date and time of the requested booking period.
     *
     * <p>Must be in the future and within an existing availability
     * window for the boat. Validated against business rules in
     * the service layer.</p>
     */
    private LocalDateTime startDate;

    /**
     * End date and time of the requested booking period.
     *
     * <p>Must be after startDate and within the same availability
     * window. Duration must meet minimum booking requirements.</p>
     */
    private LocalDateTime endDate;

    /**
     * Payment method selected for this booking.
     *
     * <p>Determines how the payment will be processed and may
     * affect available features (e.g., instant confirmation for
     * credit cards vs delayed for PIX/boleto).</p>
     */
    private PaymentMethod paymentMethod;

    /**
     * Mock payment card data for sandbox environment testing.
     *
     * Security Note: In production, this would be handled by a secure
     * payment processor with proper PCI compliance. This mock implementation
     * is suitable for portfolio demonstration purposes only.
     */
    private MockCardData mockCardData;
}