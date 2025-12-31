package com.jompastech.backend.service;

import com.jompastech.backend.exception.PaymentGatewayException;
import com.jompastech.backend.exception.PaymentValidationException;
import com.jompastech.backend.model.dto.payment.*;
import com.jompastech.backend.model.entity.Booking;
import com.jompastech.backend.model.entity.Payment;
import com.jompastech.backend.model.enums.PaymentMethod;
import com.jompastech.backend.model.enums.PaymentStatus;
import com.jompastech.backend.repository.BookingRepository;
import com.jompastech.backend.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for payment processing and management with full JPA persistence integration.
 *
 * Design Decisions:
 * - Integrates payment processing with database persistence for complete audit trail
 * - Maintains separation between gateway simulation and business logic
 * - Supports both booking-based and direct payment scenarios
 * - Implements comprehensive validation and error handling with transaction management
 * - Uses optimistic locking to handle concurrent payment updates
 *
 * Trade-offs Accepted:
 * - Gateway simulation remains for sandbox environment but structured for easy replacement
 * - Some validation logic exists in both service and entity layers for defense in depth
 * - Payment status transitions handled synchronously with async reconciliation planned
 * - Mock card data retained for development and testing flexibility
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final Environment env;
    private final RestTemplate restTemplate;

    /**
     * Processes payment with full persistence integration.
     * Creates payment record, processes via gateway, and updates entity with results.
     *
     * @param paymentInfo payment details including method, amount, and context
     * @return payment result with status and transaction details
     * @throws PaymentValidationException if payment data fails validation
     * @throws PaymentGatewayException if gateway communication fails
     */
    @Transactional
    public PaymentResult processPayment(PaymentInfo paymentInfo) {
        log.info("Processing payment for amount: {} via {}, context: {}",
                paymentInfo.getAmount(),
                paymentInfo.getPaymentMethod(),
                paymentInfo.getContextType());

        try {
            // Phase 1: Validation
            validatePaymentInfo(paymentInfo);

            // Phase 2: Entity Creation
            Payment payment = createPaymentEntity(paymentInfo);
            Payment savedPayment = paymentRepository.save(payment);
            log.debug("Payment entity created with ID: {}", savedPayment.getId());

            // Phase 3: Gateway Processing
            PaymentResult gatewayResult = processWithGateway(paymentInfo);

            // Phase 4: Entity Update
            updatePaymentFromGatewayResult(savedPayment, gatewayResult);
            Payment updatedPayment = paymentRepository.save(savedPayment);

            log.info("Payment processing completed. Transaction: {}, Status: {}",
                    updatedPayment.getTransactionId(), updatedPayment.getStatus());

            return mapToPaymentResult(updatedPayment);

        } catch (PaymentValidationException e) {
            log.warn("Payment validation failed: {}", e.getMessage());
            return PaymentResult.failed("VALIDATION_ERROR", e.getMessage());
        } catch (PaymentGatewayException e) {
            log.error("Payment gateway error: {}", e.getMessage());
            return PaymentResult.failed("GATEWAY_ERROR", e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during payment processing", e);
            return PaymentResult.failed("SYSTEM_ERROR", "Payment system temporarily unavailable");
        }
    }

    /**
     * Creates and persists a new Payment entity from payment information.
     * Sets initial status to PENDING and establishes relationships.
     */
    private Payment createPaymentEntity(PaymentInfo paymentInfo) {
        Payment payment = new Payment();
        payment.setAmount(paymentInfo.getAmount());
        payment.setPaymentMethod(paymentInfo.getPaymentMethod());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentDate(LocalDateTime.now());

        // Set booking relationship
        Booking booking = bookingRepository.findById(paymentInfo.getBookingId())
                .orElseThrow(() -> new PaymentValidationException("Booking not found with ID: " + paymentInfo.getBookingId()));
        payment.setBooking(booking);

        // Set description from payment info or default
        if (paymentInfo.getDescription() != null) {
            payment.setGatewayMessage(paymentInfo.getDescription());
        } else {
            payment.setGatewayMessage("Payment for " + paymentInfo.getPaymentMethod());
        }

        return payment;
    }

    /**
     * Processes payment through external gateway (simulated for sandbox).
     * In production, this would integrate with PagSeguro, Stripe, or similar.
     */
    private PaymentResult processWithGateway(PaymentInfo paymentInfo) {
        log.debug("Processing payment with gateway simulation");

        try {
            // Simulate API call to PagSeguro sandbox
            PagSeguroSandboxResponse response = callPagSeguroSandbox(paymentInfo);
            return mapGatewayResponseToResult(response);

        } catch (RestClientException e) {
            log.error("Gateway communication failed", e);
            throw new PaymentGatewayException("Payment gateway communication failed");
        }
    }

    /**
     * Updates payment entity with results from gateway processing.
     * Handles status transitions and gateway-specific data storage.
     */
    private void updatePaymentFromGatewayResult(Payment payment, PaymentResult result) {
        payment.setTransactionId(result.getTransactionId());
        payment.setStatus(result.getStatus());
        payment.setGatewayMessage(result.getGatewayMessage());
        payment.setProcessedAt(result.getProcessedAt());

        // Store comprehensive gateway response for debugging and reconciliation
        String gatewayResponse = String.format(
                "Transaction: %s, Status: %s, Message: %s, Processed: %s",
                result.getTransactionId(),
                result.getStatus(),
                result.getGatewayMessage(),
                result.getProcessedAt()
        );
        payment.setGatewayResponse(gatewayResponse);

        log.debug("Payment entity updated with gateway result: {}", result.getStatus());
    }

    /**
     * Validates payment information according to business rules.
     * Throws PaymentValidationException with descriptive messages for client feedback.
     */
    private void validatePaymentInfo(PaymentInfo paymentInfo) {
        // Amount validation
        if (paymentInfo.getAmount() == null || paymentInfo.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentValidationException("Payment amount must be greater than zero");
        }

        // Maximum amount check (configurable)
        BigDecimal maxAmount = new BigDecimal(env.getProperty("app.payments.max-amount", "10000"));
        if (paymentInfo.getAmount().compareTo(maxAmount) > 0) {
            throw new PaymentValidationException("Payment amount exceeds maximum allowed: " + maxAmount);
        }

        // Payment method validation
        if (paymentInfo.getPaymentMethod() == null) {
            throw new PaymentValidationException("Payment method is required");
        }

        // Context validation (must have either booking or boat, not both)
        if (paymentInfo.getBookingId() != null && paymentInfo.getBoatId() != null) {
            throw new PaymentValidationException("Cannot specify both booking and boat context");
        }

        if (paymentInfo.getBookingId() == null && paymentInfo.getBoatId() == null) {
            throw new PaymentValidationException("Either booking or boat context is required");
        }

        // Credit card specific validation
        if (paymentInfo.getPaymentMethod() == PaymentMethod.CREDIT_CARD) {
            validateCreditCard(paymentInfo.getMockCardData());
        }

        // Installments validation
        if (paymentInfo.getInstallments() != null && paymentInfo.getInstallments() < 1) {
            throw new PaymentValidationException("Installments must be at least 1");
        }

        log.debug("Payment validation passed for {}", paymentInfo.getContextType());
    }

    /**
     * Validates mock credit card data for sandbox environment.
     * Adjusted to work with your MockCardData structure.
     */
    private void validateCreditCard(MockCardData cardData) {
        if (cardData == null) {
            throw new PaymentValidationException("Card data is required for credit card payments");
        }

        // Card number validation
        if (cardData.getCardNumber() == null || cardData.getCardNumber().trim().length() < 13) {
            throw new PaymentValidationException("Valid card number is required (minimum 13 digits)");
        }

        // Holder name validation
        if (cardData.getHolderName() == null || cardData.getHolderName().trim().isEmpty()) {
            throw new PaymentValidationException("Card holder name is required");
        }

        // Expiration date validation - assuming format "MM/YY" or "MM/YYYY"
        if (cardData.getExpirationDate() == null || cardData.getExpirationDate().trim().isEmpty()) {
            throw new PaymentValidationException("Card expiration date is required");
        }

        // Basic expiration date format validation
        String expirationDate = cardData.getExpirationDate().trim();
        if (!expirationDate.matches("(0[1-9]|1[0-2])/[0-9]{2,4}")) {
            throw new PaymentValidationException("Card expiration date must be in format MM/YY or MM/YYYY");
        }

        // CVV validation
        if (cardData.getCvv() == null || cardData.getCvv().trim().length() < 3) {
            throw new PaymentValidationException("Card security code (CVV) is required (3-4 digits)");
        }

        log.debug("Credit card validation passed for card ending with: {}",
                cardData.getCardNumber().substring(cardData.getCardNumber().length() - 4));
    }

    // ========== GATEWAY SIMULATION METHODS ==========

    /**
     * Simulates PagSeguro sandbox API call.
     * Determines response based on test scenarios and mock data.
     */
    private PagSeguroSandboxResponse callPagSeguroSandbox(PaymentInfo paymentInfo) {
        String sandboxUrl = env.getProperty("app.pagseguro.sandbox-url",
                "https://sandbox.pagseguro.uol.com.br/v2/transactions");

        HttpHeaders headers = createPagSeguroHeaders();

        log.debug("Simulating PagSeguro API call to: {}", sandboxUrl);

        // Simulate processing delay
        simulateProcessingDelay();

        // Determine response based on test scenarios
        String mockCardNumber = paymentInfo.getMockCardData() != null
                ? paymentInfo.getMockCardData().getCardNumber()
                : "";

        if (isApprovedScenario(mockCardNumber, paymentInfo.getAmount())) {
            return createApprovedResponse(paymentInfo);
        } else if (isPendingScenario(mockCardNumber)) {
            return createPendingResponse(paymentInfo);
        } else {
            return createDeclinedResponse(paymentInfo);
        }
    }

    /**
     * Maps gateway response to internal PaymentResult format.
     */
    private PaymentResult mapGatewayResponseToResult(PagSeguroSandboxResponse response) {
        return PaymentResult.builder()
                .transactionId(response.getCode())
                .status(mapStatus(response.getStatusCode()))
                .success("3".equals(response.getStatusCode())) // Status 3 = Paid
                .gatewayMessage(response.getMessage())
                .processedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Maps gateway status code to internal PaymentStatus enum.
     */
    private PaymentStatus mapStatus(String statusCode) {
        return switch (statusCode) {
            case "1" -> PaymentStatus.PENDING;
            case "2" -> PaymentStatus.CANCELLED;
            case "3" -> PaymentStatus.CONFIRMED;
            default -> PaymentStatus.UNKNOWN;
        };
    }

    // ========== SIMULATION HELPER METHODS ==========

    private boolean isApprovedScenario(String cardNumber, BigDecimal amount) {
        // Approved for most test scenarios, declined for specific test cases
        if ("4111111111111111".equals(cardNumber)) return true;  // Test success card
        if ("4222222222222222".equals(cardNumber)) return false; // Test failure card

        // Random approval for demo (90% success rate for amounts under limit)
        return Math.random() > 0.1 && amount.compareTo(new BigDecimal("10000")) < 0;
    }

    private boolean isPendingScenario(String cardNumber) {
        return "4333333333333333".equals(cardNumber); // Test pending card
    }

    private PagSeguroSandboxResponse createApprovedResponse(PaymentInfo paymentInfo) {
        return PagSeguroSandboxResponse.builder()
                .code(generateTransactionId())
                .status("PAID")
                .statusCode("3")
                .message("Payment approved successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }

    private PagSeguroSandboxResponse createDeclinedResponse(PaymentInfo paymentInfo) {
        return PagSeguroSandboxResponse.builder()
                .code(generateTransactionId())
                .status("DECLINED")
                .statusCode("2")
                .message("Payment declined by issuer")
                .timestamp(LocalDateTime.now())
                .build();
    }

    private PagSeguroSandboxResponse createPendingResponse(PaymentInfo paymentInfo) {
        return PagSeguroSandboxResponse.builder()
                .code(generateTransactionId())
                .status("PENDING")
                .statusCode("1")
                .message("Payment pending review")
                .timestamp(LocalDateTime.now())
                .build();
    }

    private String generateTransactionId() {
        return "PSB_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }

    private void simulateProcessingDelay() {
        try {
            Thread.sleep(1000 + (long)(Math.random() * 2000)); // 1-3 second delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Payment processing delay interrupted");
        }
    }

    private HttpHeaders createPagSeguroHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + getSandboxToken());
        return headers;
    }

    private String getSandboxToken() {
        return env.getProperty("app.pagseguro.sandbox-token", "SANDBOX_TOKEN_" + System.currentTimeMillis());
    }

    /**
     * Maps Payment entity to PaymentResult DTO for service response.
     */
    private PaymentResult mapToPaymentResult(Payment payment) {
        return PaymentResult.builder()
                .success(payment.getStatus() == PaymentStatus.CONFIRMED)
                .transactionId(payment.getTransactionId())
                .status(payment.getStatus())
                .gatewayMessage(payment.getGatewayMessage())
                .processedAt(payment.getProcessedAt())
                .build();
    }

    /**
     * Retrieves payment by transaction ID for status checking.
     *
     * @param transactionId gateway transaction identifier
     * @return payment result if found
     */
    @Transactional(readOnly = true)
    public PaymentResult getPaymentByTransactionId(String transactionId) {
        log.debug("Retrieving payment by transaction ID: {}", transactionId);

        return paymentRepository.findByTransactionId(transactionId)
                .map(this::mapToPaymentResult)
                .orElseThrow(() -> new PaymentValidationException("Payment not found for transaction: " + transactionId));
    }

    /**
     * Retrieves payment history for a user.
     *
     * @param userId user identifier
     * @return list of payment results
     */
    @Transactional(readOnly = true)
    public List<PaymentResult> getUserPaymentHistory(Long userId) {
        log.debug("Retrieving payment history for user: {}", userId);

        List<Payment> payments = paymentRepository.findByUserId(userId);
        return payments.stream()
                .map(this::mapToPaymentResult)
                .toList();
    }
}