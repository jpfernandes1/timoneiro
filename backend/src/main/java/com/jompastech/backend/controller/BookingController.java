package com.jompastech.backend.controller;

import com.jompastech.backend.exception.EntityNotFoundException;
import com.jompastech.backend.mapper.BookingMapper;
import com.jompastech.backend.model.dto.booking.BookingRequestDTO;
import com.jompastech.backend.model.dto.booking.BookingResponseDTO;
import com.jompastech.backend.model.entity.Booking;
import com.jompastech.backend.model.entity.User;
import com.jompastech.backend.model.enums.BookingStatus;
import com.jompastech.backend.repository.BookingRepository;
import com.jompastech.backend.repository.UserRepository;
import com.jompastech.backend.service.BookingApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing boat rental bookings.
 *
 * <p>This controller handles all booking-related operations including creation,
 * retrieval, and management of bookings. It follows RESTful principles and
 * integrates with Spring Security for authentication and authorization.</p>
 *
 * <p><b>Design Note:</b> All endpoints require JWT authentication and validate
 * business rules through the service layer before processing. The controller
 * delegates business logic to application services to maintain separation of
 * concerns.</p>
 */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Bookings", description = "Endpoints for managing boat rental bookings")
public class BookingController {

    private final BookingApplicationService bookingApplicationService;
    private final BookingMapper bookingMapper;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    /**
     * Creates a new boat rental booking with dynamic pricing.
     *
     * <p>This endpoint orchestrates the complete booking creation workflow:
     * 1. Validates the booking request parameters
     * 2. Checks boat availability for the requested period
     * 3. Calculates dynamic pricing based on specific availability windows
     * 4. Processes payment through the payment gateway
     * 5. Creates and persists the booking
     * 6. Sends notifications to both renter and boat owner</p>
     *
     * <p><b>Important:</b> The booking price is dynamically calculated based on the
     * specific availability window's price per hour, enabling seasonal and
     * demand-based pricing strategies.</p>
     *
     * @param bookingRequest DTO containing booking details including boatId,
     *                      dates, and payment information
     * @param email Authenticated user ID extracted from JWT token
     * @return ResponseEntity containing the created booking details with HTTP 201 status
     * @throws IllegalArgumentException if validation fails at parameter level
     * @throws IllegalStateException if business validation fails (availability, payment)
     */
    @PostMapping
    @Operation(
            summary = "Create a new booking",
            description = "Creates a new boat rental booking with dynamic pricing based on availability windows. "
                    + "The endpoint validates availability, calculates price based on the specific window's "
                    + "rate, processes payment, and sends notifications."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Booking successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid booking parameters or validation failed"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "404", description = "Boat or user not found"),
            @ApiResponse(responseCode = "409", description = "Booking conflicts with existing reservation"),
            @ApiResponse(responseCode = "402", description = "Payment processing failed"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<BookingResponseDTO> createBooking(
            @Valid @RequestBody BookingRequestDTO bookingRequest,
            @Parameter(hidden = true) @AuthenticationPrincipal String email) {

        log.info("Booking creation requested by user {} for boat {} from {} to {}",
                email,
                bookingRequest.getBoatId(),
                bookingRequest.getStartDate(),
                bookingRequest.getEndDate());

        // Search User Id by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + email));

        // Set userId on DTO
        bookingRequest.setUserId(user.getId());

        // Process booking through application service (includes payment processing)
        var booking = bookingApplicationService.createBooking(bookingRequest);

        // Convert entity to response DTO using existing mapper
        var response = bookingMapper.toResponseDTO(booking);

        log.info("Booking created successfully with ID: {} and total price: {}",
                response.getId(),
                response.getTotalPrice());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Retrieves booking details by ID with authorization check.
     *
     * <p>Returns booking details if the authenticated user is either
     * the booking owner (renter) or the boat owner. Authorization logic
     * is enforced at the service layer to prevent unauthorized access.</p>
     *
     * <p><b>Implementation Status:</b> This endpoint requires additional
     * service methods for authorization checking and is marked for
     * future implementation.</p>
     *
     * @param bookingId Unique identifier of the booking to retrieve
     * @param userId Authenticated user ID for authorization validation
     * @return ResponseEntity with booking details if authorized
     */
    @GetMapping("/{bookingId}")
    @Operation(
            summary = "Get booking by ID",
            description = "Retrieves booking details by ID. User must be either the booking owner or boat owner. "
                    + "Authorization is enforced to protect user privacy."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Booking found and user is authorized"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "403", description = "User not authorized to view this booking"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public ResponseEntity<BookingResponseDTO> getBookingById(
            @PathVariable Long bookingId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {

        log.info("Retrieving booking {} for user {}", bookingId, userId);

        // TODO: Implement service method for authorized booking retrieval
        // var booking = bookingQueryService.getBookingByIdAndAuthorize(bookingId, userId);
        // var response = bookingMapper.toResponseDTO(booking);
        // return ResponseEntity.ok(response);

        log.warn("Booking retrieval endpoint not yet implemented - returning 501");
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    /**
     * Retrieves all bookings for the currently authenticated user.
     *
     * <p>Returns a paginated list of bookings where the authenticated user
     * is either the booking owner (renter) or the boat owner. Results are
     * ordered by start date descending (most recent first).</p>
     *
     * @param page Page number for pagination (default: 0)
     * @param size Page size for pagination (default: 20)
     * @param status Optional filter by booking status
     * @return ResponseEntity with paginated list of user's bookings
     */
    @GetMapping("/my-bookings")
    @Operation(
            summary = "Get user's bookings",
            description = "Retrieves all bookings for the authenticated user (as renter or boat owner) "
                    + "with pagination support and optional status filtering."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bookings retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters")
    })
    public ResponseEntity<Page<BookingResponseDTO>> getMyBookings(
            @Parameter(hidden = true) @AuthenticationPrincipal String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) BookingStatus status) {

        log.info("📋 Retrieving bookings for user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("❌ User not found with email: {}", email);
                    return new EntityNotFoundException("User not found with email: " + email);
                });

        log.info("✅ User found: {} (ID: {})", user.getName(), user.getId());


        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startDate"));
        Page<Booking> bookings;

        if (status != null) {
            bookings = bookingRepository.findByUserIdAndStatus(user.getId(), status, pageable);
        } else {
            bookings = bookingRepository.findByUserId(user.getId(), pageable);
        }

        // Search user reservations
        Page<BookingResponseDTO> response = bookings.map(bookingMapper::toResponseDTO);
        return ResponseEntity.ok(response);

    }

    /**
     * Cancels an existing booking according to cancellation policy.
     *
     * <p>Allows users to cancel bookings within allowed timeframes.
     * May trigger partial or full refunds based on cancellation timing
     * and payment method used. Refund processing is delegated to the
     * payment service.</p>
     *
     * <p><b>Implementation Status:</b> Requires integration with payment
     * service refund capabilities and is marked for future implementation.</p>
     *
     * @param bookingId ID of the booking to cancel
     * @param userId Authenticated user ID for authorization
     * @return ResponseEntity with no content on successful cancellation
     */
    @PostMapping("/{bookingId}/cancel")
    @Operation(
            summary = "Cancel a booking",
            description = "Cancels an existing booking according to cancellation policy. "
                    + "May trigger refund processing based on cancellation timing."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Booking successfully cancelled"),
            @ApiResponse(responseCode = "400", description = "Booking cannot be cancelled (outside allowed window)"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "403", description = "User not authorized to cancel this booking"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public ResponseEntity<Void> cancelBooking(
            @PathVariable Long bookingId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId) {

        log.info("Cancellation requested for booking {} by user {}", bookingId, userId);

        // TODO: Implement cancellation service method
        // bookingService.cancelBooking(bookingId, userId);
        // return ResponseEntity.ok().build();

        log.warn("Booking cancellation endpoint not yet implemented - returning 501");
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}