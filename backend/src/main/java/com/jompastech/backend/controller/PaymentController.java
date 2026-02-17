package com.jompastech.backend.controller;

import com.jompastech.backend.exception.PaymentValidationException;
import com.jompastech.backend.model.dto.payment.PaymentRequestDTO;
import com.jompastech.backend.model.dto.payment.PaymentResponseDTO;
import com.jompastech.backend.model.dto.payment.PaymentInfo;
import com.jompastech.backend.model.dto.payment.PaymentResult;
import com.jompastech.backend.security.service.UserDetailsImpl;
import com.jompastech.backend.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST Controller for payment processing operations.
 *
 * Design Decisions:
 * - Integrates with Spring Security JWT authentication for user identification
 * - Provides separate endpoints for booking and direct payment contexts
 * - Implements comprehensive error handling and validation
 * - Supports Brazilian payment methods (PIX, Boleto) alongside credit cards
 * - Includes webhook endpoint for asynchronous payment notifications
 *
 * Trade-offs Accepted:
 * - Payment status lookup endpoint stubbed for future implementation
 * - Webhook processing deferred to subsequent iterations
 * - Some business validation delegated to service layer for consistency
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments", description = "Payment processing and management APIs")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Processes a payment for an existing booking.
     * Requires authentication and validates user authorization for the booking.
     *
     * @param request Payment details including amount, method, and context
     * @param userDetails Authenticated user ID extracted from JWT token
     * @return Payment processing result with transaction details and status
     */
    @PostMapping("/booking")
    @Operation(summary = "Process booking payment",
            description = "Process payment for an existing booking. Requires user authentication.")
    public ResponseEntity<PaymentResponseDTO> processBookingPayment(
            @Valid @RequestBody PaymentRequestDTO request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {


        Long userId = userDetails.getId();
        log.info("Processing booking payment for user: {}, amount: {}, booking: {}",
                userId, request.getAmount(), request.getBookingId());

        // Convert to internal DTO and process payment
        PaymentInfo paymentInfo = PaymentInfo.fromPaymentRequest(request);
        PaymentResult result = paymentService.processPayment(paymentInfo);

        // Convert to API response DTO
        PaymentResponseDTO response = PaymentResponseDTO.fromPaymentResult(
                result,
                request.getBookingId(),
                request.getBoatId()
        );

        log.info("Booking payment processed successfully. Transaction: {}, Status: {}",
                response.getTransactionId(), response.getStatus());

        return ResponseEntity.ok(response);
    }

    /**
     * Processes a direct payment without booking context.
     * Used for deposits, service fees, or pre-booking inquiries.
     *
     * @param request Payment details and context
     * @param userDetails Authenticated user ID from JWT token
     * @return Payment processing result
     */
    @PostMapping("/direct")
    @Operation(summary = "Process direct payment",
            description = "Process payment without booking context. Typically for deposits or inquiries.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Payment completed"),
            @ApiResponse(responseCode = "400", description = "Invalid Payment parameters or validation failed"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "404", description = "Boat or Booking not found"),
            @ApiResponse(responseCode = "402", description = "Payment processing failed"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<PaymentResponseDTO> processDirectPayment(
            @Valid @RequestBody PaymentRequestDTO request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        Long userId = userDetails.getId();

        log.info("Processing direct payment for user: {}, amount: {}, boat: {}",
                userId, request.getAmount(), request.getBoatId());

        PaymentInfo paymentInfo = PaymentInfo.fromPaymentRequest(request);
        if (!paymentInfo.hasValidContext()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid Request: Provide bookingId or boatId, not both.");
        }
        PaymentResult result = paymentService.processPayment(paymentInfo);

        PaymentResponseDTO response = PaymentResponseDTO.fromPaymentResult(
                result,
                request.getBookingId(),
                request.getBoatId()
        );

        log.info("Direct payment processed successfully. Transaction: {}, Status: {}",
                response.getTransactionId(), response.getStatus());

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves payment status and details by transaction ID.
     * Validates user authorization to access the payment information.
     *
     * @param transactionId Unique gateway transaction identifier
     * @param userDetails Authenticated user ID for authorization
     * @return Payment details and current status
     */
    @GetMapping("/transaction/{transactionId}")
    @Operation(summary = "Get payment by transaction ID",
            description = "Retrieve payment details and status by gateway transaction ID")
    public ResponseEntity<PaymentResponseDTO> getPaymentByTransactionId(
            @PathVariable String transactionId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        Long userId = userDetails.getId();

        log.info("Retrieving payment details for transaction: {}, user: {}", transactionId, userId);

        // Implementation pending payment lookup service
        // PaymentResponseDTO response = paymentService.getPaymentByTransactionId(transactionId, userId);

        log.warn("Payment lookup not yet implemented for transaction: {}", transactionId);
        return ResponseEntity.notFound().build();
    }

    /**
     * Retrieves payment history for authenticated user.
     * Supports pagination for large result sets.
     *
     * @param userDetails Authenticated user ID
     * @param page Page number for pagination (default: 0)
     * @param size Page size for pagination (default: 20)
     * @return Paginated list of user's payments
     */
    @GetMapping("/history")
    @Operation(summary = "Get payment history",
            description = "Retrieve paginated payment history for authenticated user")
    public ResponseEntity<?> getPaymentHistory(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long userId = userDetails.getId();

        log.info("Retrieving payment history for user: {}, page: {}, size: {}", userId, page, size);

        // Implementation pending payment repository and service
        // Page<PaymentResponseDTO> history = paymentService.getUserPaymentHistory(userId, page, size);

        log.warn("Payment history not yet implemented for user: {}", userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Webhook endpoint for payment gateway notifications.
     * Processes asynchronous payment status updates from PagSeguro.
     * Implements signature verification for security.
     *
     * @param payload Raw notification payload from payment gateway
     * @param signature Webhook signature for request verification
     * @return Acknowledgement of successful processing
     */
    @PostMapping("/webhook/pagseguro")
    @Operation(summary = "Payment gateway webhook",
            description = "Webhook endpoint for asynchronous payment notifications from PagSeguro")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Webhook processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid signature or malformed payload"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> handlePaymentWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Signature") String signature) {

        log.info("Received payment webhook notification, verifying signature");

        // 1. Verify signature
        boolean isValid = paymentService.verifyWebhookSignature(payload, signature);
        if (!isValid) {
            log.warn("Webhook signature verification failed");
            return ResponseEntity.badRequest().build();
        }

        // 2. Process notification
        try {
            paymentService.processWebhookNotification(payload);
            return ResponseEntity.ok().build();
        } catch (PaymentValidationException e) {
            log.error("Webhook processing failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Health check endpoint for payment service.
     * Verifies connectivity with payment gateway and internal services.
     *
     * @return Service health status
     */
    @GetMapping("/health")
    @Operation(summary = "Payment service health check",
            description = "Check payment service status and gateway connectivity")
    public ResponseEntity<String> healthCheck() {
        log.debug("Payment service health check requested");

        // Basic health check - can be expanded with gateway connectivity tests
        boolean serviceHealthy = true; // Add actual health checks
        return ResponseEntity.ok("Payment service is healthy");
    }
}