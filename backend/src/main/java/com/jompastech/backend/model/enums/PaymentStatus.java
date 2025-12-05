package com.jompastech.backend.model.enums;

/**
 * Represents the lifecycle states of a payment transaction.
 *
 * Design Decisions:
 * - Covers complete payment lifecycle from initiation to terminal states
 * - Includes both success and failure scenarios with clear distinctions
 * - Supports refund workflow for customer protection
 * - Aligns with major payment gateway status mappings (PagSeguro, Stripe, etc.)
 *
 * Trade-offs Accepted:
 * - Some states like EXPIRED and FAILED may overlap but serve different business contexts
 * - UNKNOWN state exists for gateway communication failures despite being non-ideal
 * - REFUNDED state is terminal even though partial refunds could be represented differently
 */
public enum PaymentStatus {

    /**
     * Payment has been initiated but not yet processed by the gateway.
     * Typical for PIX/Boleto where user needs to complete the payment.
     */
    PENDING("Payment is pending customer action"),

    /**
     * Payment is being processed by the gateway.
     * Intermediate state between initiation and completion.
     */
    PROCESSING("Payment is being processed by the payment gateway"),

    /**
     * Payment has been successfully completed and funds are available.
     * Terminal success state - payment is considered successful.
     */
    CONFIRMED("Payment has been confirmed and completed successfully"),

    /**
     * Payment was declined by the payment processor or issuer.
     * Terminal failure state - requires customer retry with different method.
     */
    DECLINED("Payment was declined by the payment processor"),

    /**
     * Payment failed due to technical issues or invalid data.
     * Terminal failure state - may require system investigation.
     */
    FAILED("Payment failed due to technical issues or invalid data"),

    /**
     * Payment was canceled by the user or system before completion.
     * Terminal state - user explicitly abandoned the payment.
     */
    CANCELLED("Payment was cancelled before completion"),

    /**
     * Payment expired due to timeout (common for PIX/Boleto).
     * Terminal state - user did not complete payment within timeframe.
     */
    EXPIRED("Payment expired due to timeout"),

    /**
     * Payment was successfully refunded to the customer.
     * Terminal state - reversal of confirmed payment.
     */
    REFUNDED("Payment has been refunded to the customer"),

    /**
     * Payment status could not be determined due to communication issues.
     * Non-terminal state - requires manual investigation and reconciliation.
     */
    UNKNOWN("Payment status could not be determined");

    private final String description;

    PaymentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Checks if this status represents a terminal state (no further transitions expected).
     * Terminal states are typically the end of payment lifecycle.
     */
    public boolean isTerminal() {
        return this == CONFIRMED ||
                this == DECLINED ||
                this == FAILED ||
                this == CANCELLED ||
                this == EXPIRED ||
                this == REFUNDED;
    }

    /**
     * Checks if this status represents a successful payment.
     * Only CONFIRMED is considered truly successful for business operations.
     */
    public boolean isSuccessful() {
        return this == CONFIRMED;
    }

    /**
     * Checks if this status allows refund operations.
     * Only confirmed payments can be refunded in standard workflow.
     */
    public boolean isRefundable() {
        return this == CONFIRMED;
    }

    /**
     * Checks if this status represents a pending or processing state.
     * Useful for determining if payment outcome is still uncertain.
     */
    public boolean isPendingOrProcessing() {
        return this == PENDING || this == PROCESSING;
    }
}