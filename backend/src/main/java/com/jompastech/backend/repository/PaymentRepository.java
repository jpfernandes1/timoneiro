package com.jompastech.backend.repository;

import com.jompastech.backend.model.entity.Payment;
import com.jompastech.backend.model.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Payment entity operations.
 *
 * Design Decisions:
 * - Extends JpaRepository for standard CRUD operations with Spring Data JPA
 * - Includes custom queries for common payment business scenarios
 * - Uses derived query methods for simple filtering operations
 * - Supports both ID-based and transaction ID-based lookups
 *
 * Trade-offs Accepted:
 * - Some complex queries use JPQL for clarity over method naming
 * - Mixed approach between derived methods and explicit @Query annotations
 * - Pagination support deferred to service layer for flexibility
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Finds payment by gateway transaction ID for reconciliation.
     */
    Optional<Payment> findByTransactionId(String transactionId);

    /**
     * Finds all payments for a specific booking.
     */
    List<Payment> findByBookingIdOrderByCreatedAtDesc(Long bookingId);

    /**
     * Finds payments by status for batch processing.
     */
    List<Payment> findByStatus(PaymentStatus status);

    /**
     * Finds pending payments that have expired.
     * Useful for cleaning up abandoned PIX/boleto payments.
     */
    List<Payment> findByStatusAndCreatedAtBefore(
            PaymentStatus status,
            LocalDateTime createdBefore);


    /**
     * Finds payment by bookingID
     * @param bookingId
     */
    Optional<Payment> findByBookingId(Long bookingId);

    /**
     * Finds payments by user ID through booking relationship.
     */
    @Query("SELECT p FROM Payment p JOIN p.booking b WHERE b.user.id = :userId")
    List<Payment> findByUserId(@Param("userId") Long userId);

    /**
     * Checks if a booking already has a successful payment.
     * Prevents duplicate payment attempts for the same booking.
     */
    @Query("SELECT COUNT(p) > 0 FROM Payment p WHERE p.booking.id = :bookingId AND p.status = 'CONFIRMED'")
    boolean existsConfirmedPaymentByBookingId(@Param("bookingId") Long bookingId);

    /**
     * Finds payments within a date range for reporting.
     */
    List<Payment> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Finds payments by status and payment method.
     * Useful for gateway-specific reconciliation.
     */
    List<Payment> findByStatusAndPaymentMethod(
            PaymentStatus status,
            String paymentMethod);
}