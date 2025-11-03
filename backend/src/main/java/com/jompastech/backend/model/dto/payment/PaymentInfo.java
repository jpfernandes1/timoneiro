package com.jompastech.backend.model.dto.payment;

import com.jompastech.backend.model.enums.PaymentMethod;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Internal DTO for payment processing within the service layer.
 * Contains all necessary data for payment gateway communication.
 *
 * Design Decisions:
 * - Separated from API DTO to maintain service layer independence
 * - Includes both booking and boat contexts for flexibility
 * - Sandbox-friendly with mock data support
 *
 * Trade-offs Accepted:
 * - Some fields nullable to support different payment scenarios
 * - Mock card data included for testing simplicity
 * - Direct mapping from PaymentRequestDTO in controller
 */
@Data
@Builder
public class PaymentInfo {
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private MockCardData mockCardData; // For sandbox only
    private String description;
    private String userEmail;

    // Context information - one should be present
    private Long bookingId;
    private Long boatId;

    // Installments for credit card payments
    @Builder.Default
    private Integer installments = 1;

    /**
     * Factory method to create PaymentInfo from PaymentRequestDTO
     */
    public static PaymentInfo fromPaymentRequest(PaymentRequestDTO request) {
        return PaymentInfo.builder()
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .mockCardData(request.getMockCardData())
                .description(request.getDescription())
                .userEmail(request.getUserEmail())
                .bookingId(request.getBookingId())
                .boatId(request.getBoatId())
                .installments(request.getInstallments())
                .build();
    }

    /**
     * Validates that the payment info has proper context
     */
    public boolean hasValidContext() {
        return (bookingId != null && boatId == null) ||
                (bookingId == null && boatId != null);
    }

    /**
     * Gets the context type for logging and validation
     */
    public String getContextType() {
        if (bookingId != null && boatId == null) return "BOOKING";
        if (bookingId == null && boatId != null) return "BOAT";
        if (bookingId != null && boatId != null) return "AMBIGUOUS";
        return "NONE";
    }
}