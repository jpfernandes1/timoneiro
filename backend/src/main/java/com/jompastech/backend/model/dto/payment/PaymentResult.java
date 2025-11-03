package com.jompastech.backend.model.dto.payment;

import com.jompastech.backend.model.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Internal DTO for payment processing results within the service layer.
 * Used for business logic and gateway communication.
 *
 * Design Decisions:
 * - Simple structure focused on processing outcome
 * - Contains gateway-specific data and error handling
 * - Not exposed directly to API clients
 *
 * Trade-offs Accepted:
 * - Limited context information (no booking/boat IDs)
 * - Focused on transaction processing status
 */
@Data
@Builder
public class PaymentResult {
    private boolean success;
    private String transactionId;
    private PaymentStatus status;
    private String gatewayMessage;
    private String errorMessage;
    private LocalDateTime processedAt;

    public static PaymentResult failed(String errorCode, String message) {
        return PaymentResult.builder()
                .success(false)
                .status(PaymentStatus.CANCELLED)
                .errorMessage(message)
                .processedAt(LocalDateTime.now())
                .build();
    }

    public boolean isSuccessful() {
        return success;
    }
}