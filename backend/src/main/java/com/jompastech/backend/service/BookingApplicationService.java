package com.jompastech.backend.service;

import com.jompastech.backend.exception.BookingCreationException;
import com.jompastech.backend.exception.PaymentProcessingException;
import com.jompastech.backend.model.dto.booking.BookingRequestDTO;
import com.jompastech.backend.model.dto.payment.MockCardData;
import com.jompastech.backend.model.dto.payment.PaymentInfo;
import com.jompastech.backend.model.dto.payment.PaymentResult;
import com.jompastech.backend.model.entity.Boat;
import com.jompastech.backend.model.entity.BoatAvailability;
import com.jompastech.backend.model.entity.Booking;
import com.jompastech.backend.model.entity.User;
import com.jompastech.backend.repository.BoatAvailabilityRepository;
import com.jompastech.backend.repository.BoatRepository;
import com.jompastech.backend.repository.BookingRepository;
import com.jompastech.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class BookingApplicationService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final BoatRepository boatRepository;
    private final BoatAvailabilityRepository boatAvailabilityRepository;
    private final BookingValidationService bookingValidationService;
    private final PaymentService paymentService;
    private final NotificationService notificationService;

    public BookingApplicationService(
            BookingRepository bookingRepository,
            UserRepository userRepository,
            BoatRepository boatRepository,
            BoatAvailabilityRepository boatAvailabilityRepository,
            BookingValidationService bookingValidationService,
            PaymentService paymentService,
            NotificationService notificationService) {

        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.boatRepository = boatRepository;
        this.boatAvailabilityRepository = boatAvailabilityRepository;
        this.bookingValidationService = bookingValidationService;
        this.paymentService = paymentService;
        this.notificationService = notificationService;
    }

    /**
     * Creates a new booking with dynamic pricing calculation.
     *
     * @param bookingRequest the booking request DTO containing all required data
     * @return the created and confirmed booking entity with dynamic pricing
     */
    public Booking createBooking(BookingRequestDTO bookingRequest) {
        // Step 1: Fetch required entities
        User user = userRepository.findById(bookingRequest.getUserId())
                .orElseThrow(() -> new BookingCreationException(
                        "User not found with id: " + bookingRequest.getUserId()));

        Boat boat = boatRepository.findById(bookingRequest.getBoatId())
                .orElseThrow(() -> new BookingCreationException(
                        "Boat not found with id: " + bookingRequest.getBoatId()));

        // Step 2: Find specific availability window for dynamic pricing
        BoatAvailability availabilityWindow = findAvailabilityWindowForBooking(
                boat, bookingRequest.getStartDate(), bookingRequest.getEndDate());

        // Step 3: Calculate price based on the specific window's price per hour
        BigDecimal totalPrice = availabilityWindow.calculatePriceForPeriod(
                bookingRequest.getStartDate(), bookingRequest.getEndDate());

        // Step 4: Create booking entity directly with total price
        Booking booking = new Booking(
                user,
                boat,
                bookingRequest.getStartDate(),
                bookingRequest.getEndDate(),
                totalPrice
        );

        // Step 5: Validate business rules and availability
        bookingValidationService.validateBookingCreation(booking);

        // Step 6: Prepare and process payment
        PaymentInfo paymentInfo = buildPaymentInfo(bookingRequest, user, totalPrice);
        PaymentResult paymentResult = paymentService.processPayment(paymentInfo);

        if (!paymentResult.isSuccessful()) {
            throw new PaymentProcessingException(
                    "Payment failed: " + paymentResult.getErrorMessage());
        }

        // Step 7: Confirm and persist the booking
        booking.confirm();
        Booking savedBooking = bookingRepository.save(booking);

        // Step 8: Send notifications
        notificationService.notifyOwner(savedBooking);
        notificationService.notifyRenter(savedBooking);

        return savedBooking;
    }

    /**
     * Finds the specific availability window that covers the requested booking period.
     *
     * This method is critical for implementing dynamic pricing based on
     * different availability windows with different price rates.
     *
     * @param boat The boat being booked
     * @param startDate Start of the booking period
     * @param endDate End of the booking period
     * @return The specific BoatAvailability window covering the period
     * @throws BookingCreationException if no suitable availability window is found
     */
    private BoatAvailability findAvailabilityWindowForBooking(
            Boat boat, LocalDateTime startDate, LocalDateTime endDate) {

        List<BoatAvailability> availabilityWindows = boatAvailabilityRepository
                .findCoveringAvailabilityWindow(boat, startDate, endDate);

        return availabilityWindows.stream()
                .filter(window -> window.coversPeriod(startDate, endDate))
                .findFirst()
                .orElseThrow(() -> new BookingCreationException(
                        "No availability window with dynamic pricing found for the selected period. " +
                                "The boat may not be available or the period spans multiple pricing windows."));
    }

    /**
     * Builds payment information from booking data and user details.
     *
     * @param bookingRequest the original booking request DTO
     * @param user the user entity for contact information
     * @param amount the dynamically calculated total amount
     * @return complete payment information ready for processing
     */
    private PaymentInfo buildPaymentInfo(
            BookingRequestDTO bookingRequest, User user, BigDecimal amount) {

        return PaymentInfo.builder()
                .amount(amount)
                .paymentMethod(bookingRequest.getPaymentMethod())
                .userEmail(user.getEmail())
                .mockCardData(bookingRequest.getMockCardData())
                .description(String.format(
                        "Boat rental: %s (%s to %s)",
                        bookingRequest.getBoatId(),
                        bookingRequest.getStartDate().toLocalDate(),
                        bookingRequest.getEndDate().toLocalDate()))
                .build();
    }
}