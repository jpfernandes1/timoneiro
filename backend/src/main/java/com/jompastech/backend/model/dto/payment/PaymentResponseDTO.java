// PaymentResponseDTO - API RESPONSE
package com.jompastech.backend.model.dto.payment;

import com.jompastech.backend.model.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * API Response DTO for payment operations.
 * Exposed to clients with complete payment context.
 *
 * Design Decisions:
 * - Rich structure with full context (booking, boat, user)
 * - Includes additional data for specific payment methods (PIX, boleto)
 * - Designed for client application consumption
 *
 * Trade-offs Accepted:
 * - More comprehensive than internal processing needs
 * - Includes client-specific formatting and URLs
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDTO {

    // Core payment information
    private boolean success;
    private String transactionId;
    private PaymentStatus status;
    private String gatewayMessage;
    private String errorMessage;
    private LocalDateTime processedAt;

    // Extended context for client applications
    private Long paymentId;
    private BigDecimal amount;
    private String paymentMethod;
    private Long bookingId;
    private Long boatId;

    // Payment method specific data
    private String pixQrCode;
    private String boletoUrl;
    private String paymentUrl;
    private LocalDateTime expiresAt;

    // Factory method to create from PaymentResult
    public static PaymentResponseDTO fromPaymentResult(PaymentResult result, Long bookingId, Long boatId) {
        return PaymentResponseDTO.builder()
                .success(result.isSuccessful())
                .transactionId(result.getTransactionId())
                .status(result.getStatus())
                .gatewayMessage(result.getGatewayMessage())
                .errorMessage(result.getErrorMessage())
                .processedAt(result.getProcessedAt())
                .bookingId(bookingId)
                .boatId(boatId)
                .build();
    }
}