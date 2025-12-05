package com.jompastech.backend.model.dto.booking;

import com.jompastech.backend.model.dto.payment.MockCardData;
import com.jompastech.backend.model.entity.Booking;
import com.jompastech.backend.model.entity.User;
import com.jompastech.backend.model.entity.Boat;
import com.jompastech.backend.model.enums.PaymentMethod;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Command object for booking creation that encapsulates all required data
 * in a single atomic operation.
 *
 * Implements the Command Pattern to represent a complete booking request
 * with all necessary information, including payment details and temporal data.
 * This approach ensures that all booking creation parameters are validated
 * and processed together, maintaining transaction integrity.
 */
@Data
public class CreateBookingCommand {

    /**
     * Identifier of the user making the booking request.
     * Used to fetch the complete User entity from the repository.
     */
    private Long userId;

    /**
     * Identifier of the boat being booked.
     * Used to fetch the complete Boat entity from the repository.
     */
    private Long boatId;

    /**
     * Start date and time of the requested booking period.
     */
    private LocalDateTime startDate;

    /**
     * End date and time of the requested booking period.
     */
    private LocalDateTime endDate;

    /**
     * Pre-calculated total price for the booking period.
     *
     * Design Decision: The price is calculated separately by a PricingService
     * before creating this command to ensure business logic separation and
     * allow for potential pricing strategies and discounts.
     */
    private BigDecimal totalPrice;

    /**
     * Payment method selected by the user for this booking.
     * Determines how the payment will be processed.
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

    /**
     * Converts this command to a Booking entity using provided User and Boat entities.
     *
     * This method requires pre-fetched entities to ensure data consistency
     * and avoid lazy loading issues. The service layer is responsible for
     * fetching these entities before calling this conversion method.
     *
     * @param user the User entity associated with the booking
     * @param boat the Boat entity being booked
     * @return a new Booking entity with the command data
     * @throws IllegalArgumentException if user or boat entities are null
     */
    public Booking toBooking(User user, Boat boat) {
        // Validation ensures that the conversion only occurs with complete entity data
        if (user == null || boat == null) {
            throw new IllegalArgumentException("User and Boat must be provided to create a Booking");
        }

        // Delegates entity creation to Booking's constructor which contains domain validation
        return new Booking(user, boat, this.startDate, this.endDate, this.totalPrice);
    }
}