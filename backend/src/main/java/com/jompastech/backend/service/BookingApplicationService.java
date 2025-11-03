package com.jompastech.backend.service;

import com.jompastech.backend.exception.BookingCreationException;
import com.jompastech.backend.exception.PaymentProcessingException;
import com.jompastech.backend.model.dto.booking.CreateBookingCommand;
import com.jompastech.backend.model.dto.payment.PaymentInfo;
import com.jompastech.backend.model.dto.payment.PaymentResult;
import com.jompastech.backend.model.entity.Boat;
import com.jompastech.backend.model.entity.Booking;
import com.jompastech.backend.model.entity.User;
import com.jompastech.backend.repository.BoatRepository;
import com.jompastech.backend.repository.BookingRepository;
import com.jompastech.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Application service that orchestrates the complete booking creation process.
 *
 * Coordinates multiple domain services and repositories to execute the booking workflow,
 * ensuring transactional consistency and business rule enforcement throughout the process.
 *
 * Follows the Command pattern by accepting a CreateBookingCommand that encapsulates
 * all required data for booking creation, promoting clear separation of concerns.
 */
@Service
@Transactional
public class BookingApplicationService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final BoatRepository boatRepository;
    private final BookingValidationService bookingValidationService;
    private final PaymentService paymentService;
    private final NotificationService notificationService;

    /**
     * Constructs the BookingApplicationService with required dependencies.
     *
     * Uses constructor injection to ensure all dependencies are provided and
     * the service is in a valid state upon instantiation.
     */
    public BookingApplicationService(BookingRepository bookingRepository,
                                     UserRepository userRepository,
                                     BoatRepository boatRepository,
                                     BookingValidationService bookingValidationService,
                                     PaymentService paymentService,
                                     NotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.boatRepository = boatRepository;
        this.bookingValidationService = bookingValidationService;
        this.paymentService = paymentService;
        this.notificationService = notificationService;
    }

    /**
     * Creates a new booking by orchestrating the complete workflow from validation to confirmation.
     *
     * Executes a seven-step process:
     * 1. Fetches user and boat entities to ensure they exist
     * 2. Converts the command to a booking entity with domain validation
     * 3. Validates business rules and availability conflicts
     * 4. Builds payment information from command data
     * 5. Processes payment through the payment service
     * 6. Confirms and persists the booking
     * 7. Notifies both boat owner and renter
     *
     * The entire process is transactional, ensuring data consistency across all operations.
     *
     * @param command the booking creation command containing all required booking data
     * @return the created and confirmed booking entity
     * @throws BookingCreationException if user, boat not found or validation fails
     * @throws PaymentProcessingException if payment processing fails
     */
    public Booking createBooking(CreateBookingCommand command) {
        // Step 1: Fetch required entities
        User user = userRepository.findById(command.getUserId())
                .orElseThrow(() -> new BookingCreationException("User not found with id: " + command.getUserId()));

        Boat boat = boatRepository.findById(command.getBoatId())
                .orElseThrow(() -> new BookingCreationException("Boat not found with id: " + command.getBoatId()));

        // Step 2: Convert command to domain entity
        Booking booking = command.toBooking(user, boat);

        // Step 3: Validate business rules and availability
        bookingValidationService.validateBookingCreation(booking);

        // Step 4: Prepare payment information
        PaymentInfo paymentInfo = buildPaymentInfo(command, user, booking.getTotalPrice());

        // Step 5: Process payment (sandbox mode for portfolio demonstration)
        PaymentResult paymentResult = paymentService.processPayment(paymentInfo);

        if (!paymentResult.isSuccessful()) {
            throw new PaymentProcessingException("Payment failed: " + paymentResult.getErrorMessage());
        }

        // Step 6: Confirm and persist the booking
        booking.confirm(); // Transition from PENDING to CONFIRMED status
        Booking savedBooking = bookingRepository.save(booking);

        // Step 7: Notify involved parties
        notificationService.notifyOwner(savedBooking);
        notificationService.notifyRenter(savedBooking);

        return savedBooking;
    }

    /**
     * Builds payment information from command data and user details.
     *
     * Aggregates data from multiple sources to create a complete payment request,
     * including user email for receipt purposes and descriptive information for
     * transaction tracking.
     *
     * @param command the original booking creation command
     * @param user the user entity for contact information
     * @param amount the total amount to be charged
     * @return complete payment information ready for processing
     */
    private PaymentInfo buildPaymentInfo(CreateBookingCommand command, User user, BigDecimal amount) {
        return PaymentInfo.builder()
                .amount(amount)
                .paymentMethod(command.getPaymentMethod())
                .userEmail(user.getEmail())
                .mockCardData(command.getMockCardData()) // For sandbox testing environment
                .description("Boat rental - " + command.getBoatId())
                .build();
    }
}