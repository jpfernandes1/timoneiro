package com.jompastech.backend.model.dto.payment;

import com.jompastech.backend.model.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for payment processing requests.
 *
 * Design Decisions:
 * - Supports both booking and direct payment contexts
 * - Includes sandbox-specific fields for testing
 * - Validations aligned with Brazilian payment regulations
 *
 * Trade-offs Accepted:
 * - Mock card data included in main DTO for simplicity
 * - Some fields nullable to support different payment methods
 * - Direct amount validation without currency conversion
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDTO {

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    // For sandbox/testing only
    private MockCardData mockCardData;

    private String description;

    // Optional: for booking context
    private Long bookingId;

    // Optional: for direct boat inquiries
    private Long boatId;

    // User email for gateway communication
    private String userEmail;

    // Installments for credit card payments (default: 1)
    @Builder.Default
    private Integer installments = 1;
}