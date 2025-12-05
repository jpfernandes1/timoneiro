package com.jompastech.backend.model.dto.payment;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO for PagSeguro sandbox API integration.
 * Models the complete contract for payment processing responses.
 *
 * Design Decisions:
 * - Exact mapping to PagSeguro API response structure for seamless parsing
 * - Lombok annotations for concise implementation while maintaining clarity
 * - Includes both human-readable status and machine-readable status codes
 * - Prepared for webhook handling and asynchronous status updates
 *
 * Trade-offs Accepted:
 * - Limited to sandbox response structure, may require expansion for production
 * - Some PagSeguro-specific response fields omitted for initial implementation
 * - Lombok usage for simplicity in read-heavy response object
 * - Timestamp uses LocalDateTime instead of Instant for simplicity
 */
@Data
@Builder
public class PagSeguroSandboxResponse {

    /**
     * Gateway transaction identifier for reconciliation.
     * Format: PSB_{timestamp}_{random} in sandbox, actual ID in production.
     */
    private String code;

    /**
     * Human-readable payment status description.
     * Examples: "PAID", "DECLINED", "PENDING", "CANCELLED"
     */
    private String status;

    /**
     * Machine-readable status code for programmatic handling.
     * Mapping: "1"=PENDING, "2"=CANCELLED, "3"=CONFIRMED
     */
    private String statusCode;

    /**
     * Detailed message from payment gateway for user display.
     * Provides context for status like decline reasons or pending requirements.
     */
    private String message;

    /**
     * Response timestamp from gateway for auditing and synchronization.
     * Used for reconciliation and duplicate detection.
     */
    private LocalDateTime timestamp;

    /**
     * Additional gateway reference data for debugging.
     * Optional field for gateway-specific metadata.
     */
    private String reference;

    /**
     * Payment method used in the transaction.
     * Examples: "CREDIT_CARD", "PIX", "BOLETO"
     */
    private String paymentMethod;

    /**
     * Checks if payment was successfully processed and confirmed.
     *
     * @return true if status code indicates confirmed payment (3)
     */
    public boolean isApproved() {
        return "3".equals(statusCode);
    }

    /**
     * Checks if payment is in pending state awaiting completion.
     * Common for PIX and boleto payments requiring user action.
     *
     * @return true if status code indicates pending payment (1)
     */
    public boolean isPending() {
        return "1".equals(statusCode);
    }

    /**
     * Checks if payment was declined or cancelled.
     *
     * @return true if status code indicates cancelled payment (2)
     */
    public boolean isDeclined() {
        return "2".equals(statusCode);
    }

    /**
     * Maps gateway status code to internal PaymentStatus enum.
     *
     * @return Corresponding PaymentStatus or UNKNOWN if unmapped
     */
    public com.jompastech.backend.model.enums.PaymentStatus getMappedStatus() {
        return switch (statusCode) {
            case "1" -> com.jompastech.backend.model.enums.PaymentStatus.PENDING;
            case "2" -> com.jompastech.backend.model.enums.PaymentStatus.CANCELLED;
            case "3" -> com.jompastech.backend.model.enums.PaymentStatus.CONFIRMED;
            default -> com.jompastech.backend.model.enums.PaymentStatus.UNKNOWN;
        };
    }

    /**
     * Creates a failed response for error scenarios.
     *
     * @param errorMessage Descriptive error message
     * @return Failed response with current timestamp
     */
    public static PagSeguroSandboxResponse failed(String errorMessage) {
        return PagSeguroSandboxResponse.builder()
                .status("ERROR")
                .statusCode("2")
                .message(errorMessage)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a pending response for asynchronous payment methods.
     *
     * @param transactionId Gateway transaction identifier
     * @param pendingMessage Instructions for completing payment
     * @return Pending response with current timestamp
     */
    public static PagSeguroSandboxResponse pending(String transactionId, String pendingMessage) {
        return PagSeguroSandboxResponse.builder()
                .code(transactionId)
                .status("PENDING")
                .statusCode("1")
                .message(pendingMessage)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a successful response for approved payments.
     *
     * @param transactionId Gateway transaction identifier
     * @param successMessage Confirmation message
     * @return Approved response with current timestamp
     */
    public static PagSeguroSandboxResponse approved(String transactionId, String successMessage) {
        return PagSeguroSandboxResponse.builder()
                .code(transactionId)
                .status("PAID")
                .statusCode("3")
                .message(successMessage)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Gets a descriptive string for logging (without sensitive data).
     *
     * @return Safe log representation
     */
    public String toLogString() {
        return String.format(
                "PagSeguroResponse{code='%s', status=%s, statusCode=%s}",
                code,
                status,
                statusCode
        );
    }

    /**
     * Validates that response contains required fields.
     *
     * @return true if response has status code and timestamp
     */
    public boolean isValid() {
        return statusCode != null && timestamp != null;
    }
}