package com.jompastech.backend.model.entity;

import com.jompastech.backend.model.enums.PaymentMethod;
import com.jompastech.backend.model.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a payment transaction in the system.
 *
 * Design Decisions:
 * - Stores both internal payment details and gateway transaction data
 * - Maintains audit trail with created/updated timestamps
 * - Supports multiple payment methods while preserving gateway-specific data
 * - Uses optimistic locking to handle concurrent updates
 *
 * Trade-offs Accepted:
 * - Some gateway-specific fields are nullable to accommodate different payment processors
 * - Payment method enum includes both Brazilian and international options for future scalability
 * - Storing raw gateway response for debugging despite potential data redundancy
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
public class Payment {

    /**
     * Primary identifier for the payment record.
     * Uses database-generated identity for consistency across distributed systems.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    /**
     * Associated booking for this payment.
     * Many-to-one relationship as multiple payments could theoretically exist per booking
     * (e.g., partial payments, refunds) though business rules may restrict to one active payment.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    /**
     * Payment amount in Brazilian Real (BRL).
     * Precision and scale optimized for financial calculations.
     */
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    /**
     * Current payment status following the payment lifecycle.
     * Defaults to PENDING to handle asynchronous payment processing.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    /**
     * Payment method selected by the user.
     * Supports Brazilian-specific methods (PIX, Boleto) alongside international cards.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    /**
     * Gateway-specific transaction identifier.
     * Essential for reconciliation with external payment processors.
     */
    @Column(name = "transaction_id", unique = true, length = 100)
    private String transactionId;

    /**
     * Raw message or status description from payment gateway.
     * Useful for debugging and customer support scenarios.
     */
    @Column(name = "gateway_message", length = 500)
    private String gatewayMessage;

    /**
     * Timestamp when the payment was successfully processed by the gateway.
     * Different from paymentDate which represents when user initiated payment.
     */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /**
     * Timestamp when user initiated the payment process.
     * Used for business rules like payment expiration (especially for PIX/Boleto).
     */
    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    /**
     * Audit field - when this payment record was created.
     * Never updated after initial persistence.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Audit field - when this payment record was last updated.
     * Automatically updated on every persistence operation.
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * Raw response data from payment gateway in JSON format.
     * Trade-off: Storing unstructured data for debugging vs. normalizing into separate tables.
     * Chosen for simplicity and complete audit trail of gateway interactions.
     */
    @Column(name = "gateway_response", columnDefinition = "TEXT")
    private String gatewayResponse;

    /**
     * Version field for optimistic locking.
     * Prevents concurrent updates from overwriting each other.
     */
    @Version
    private Long version;

    /**
     * Pre-persistence callback to set audit timestamps.
     * Ensures updatedAt is always current without manual intervention.
     */
    @PrePersist
    @PreUpdate
    public void updateTimestamps() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    /**
     * Business method to check if payment is in a terminal state.
     * Terminal states cannot transition to other statuses.
     */
    public boolean isTerminalStatus() {
        return status == PaymentStatus.CONFIRMED ||
                status == PaymentStatus.CANCELLED ||
                status == PaymentStatus.REFUNDED;
    }

    /**
     * Business method to check if payment can be refunded.
     * Only confirmed payments within reasonable time frame are refundable.
     */
    public boolean isRefundable() {
        return status == PaymentStatus.CONFIRMED &&
                processedAt != null &&
                processedAt.isAfter(LocalDateTime.now().minusDays(30));
    }
}