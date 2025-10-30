package com.jompastech.backend.service;

import com.jompastech.backend.exception.BookingConflictException;
import com.jompastech.backend.exception.BookingValidationException;
import com.jompastech.backend.model.entity.BoatAvailability;
import com.jompastech.backend.model.entity.Booking;
import com.jompastech.backend.repository.BoatAvailabilityRepository;
import com.jompastech.backend.repository.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service responsible for validating booking business rules and constraints.
 *
 * Performs comprehensive validation including duration checks, availability verification,
 * and conflict detection to ensure booking integrity before creation.
 *
 * Uses read-only transactions for validation queries to optimize database performance
 * while maintaining data consistency during the validation process.
 */
@Service
@Transactional(readOnly = true)
public class BookingValidationService {

    private final BookingRepository bookingRepository;
    private final BoatAvailabilityRepository boatAvailabilityRepository;

    /**
     * Constructs the validation service with required repository dependencies.
     *
     * @param bookingRepository repository for booking data access
     * @param boatAvailabilityRepository repository for boat availability checks
     */
    public BookingValidationService(BookingRepository bookingRepository,
                                    BoatAvailabilityRepository boatAvailabilityRepository) {
        this.bookingRepository = bookingRepository;
        this.boatAvailabilityRepository = boatAvailabilityRepository;
    }

    /**
     * Validates if a new booking can be created without violating business rules.
     *
     * Performs a three-step validation process:
     * 1. Basic domain rules (duration, timing)
     * 2. Boat availability periods
     * 3. Existing booking conflicts
     *
     * @param newBooking the booking to validate
     * @throws BookingValidationException if basic business rules are violated
     * @throws BookingConflictException if temporal conflicts with existing bookings are detected
     */
    public void validateBookingCreation(Booking newBooking) {
        // Step 1: Check basic domain rules (no external dependencies)
        if (!newBooking.hasValidDuration()) {
            throw new BookingValidationException("Booking must be at least 4 hours");
        }

        // Step 2: Check boat availability periods (external dependency - database)
        validateBoatAvailability(newBooking);

        // Step 3: Check for overlapping bookings (external dependency - database)
        validateNoConflictingBookings(newBooking);
    }

    /**
     * Validates that the boat is available during the requested booking period.
     *
     * Checks both existence of availability periods and exact period matching
     * to ensure the booking fits within predefined availability windows.
     *
     * @param booking the booking to validate against availability
     * @throws BookingValidationException if no availability periods exist or don't match
     */
    private void validateBoatAvailability(Booking booking) {
        List<BoatAvailability> availablePeriods = boatAvailabilityRepository
                .findByBoatAndDateRange(booking.getBoat(), booking.getStartDate(), booking.getEndDate());

        if (availablePeriods.isEmpty()) {
            throw new BookingValidationException("Boat is not available for the selected dates");
        }

        // Additional logic: verify the booking fits within available periods
        boolean isWithinAnyAvailability = availablePeriods.stream()
                .anyMatch(availability ->
                        !booking.getStartDate().isBefore(availability.getStartDate()) &&
                                !booking.getEndDate().isAfter(availability.getEndDate()));

        if (!isWithinAnyAvailability) {
            throw new BookingValidationException("Booking period doesn't match boat availability");
        }
    }

    /**
     * Validates that no existing bookings conflict with the new booking timeframe.
     *
     * Uses the repository's conflict detection query to find overlapping bookings
     * and performs additional domain-level overlap verification for safety.
     *
     * @param newBooking the new booking to check for conflicts
     * @throws BookingConflictException if overlapping bookings are detected
     */
    private void validateNoConflictingBookings(Booking newBooking) {
        List<Booking> existingBookings = bookingRepository
                .findConflictingBookings(newBooking.getBoat(), newBooking.getStartDate(), newBooking.getEndDate());

        for (Booking existing : existingBookings) {
            if (newBooking.overlapsWith(existing.getStartDate(), existing.getEndDate())) {
                throw new BookingConflictException(
                        "Booking conflicts with existing reservation from " +
                                existing.getStartDate() + " to " + existing.getEndDate());
            }
        }
    }
}