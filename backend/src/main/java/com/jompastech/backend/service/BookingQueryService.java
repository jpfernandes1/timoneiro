package com.jompastech.backend.service;

import com.jompastech.backend.model.enums.BookingStatus;
import com.jompastech.backend.repository.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service dedicated to booking query operations and business intelligence.
 *
 * <p><b>Design Decisions:</b>
 * <ul>
 *   <li>Separates read operations from write operations (CQRS-inspired)</li>
 *   <li>Optimized for query performance with read-only transactions</li>
 *   <li>Provides business-focused query methods for other services</li>
 *   <li>Maintains clear separation of concerns from booking creation logic</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class BookingQueryService {

    private final BookingRepository bookingRepository;

    public BookingQueryService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    /**
     * Validates if a user has completed a rental for a specific boat.
     * Essential business rule for review eligibility.
     *
     * @param userId the user identifier to check
     * @param boatId the boat identifier to check
     * @return true if user has completed a booking for this boat, false otherwise
     */
    public boolean hasUserRentedBoat(Long userId, Long boatId) {
        return bookingRepository.existsByUserIdAndBoatIdAndStatus(userId, boatId, BookingStatus.FINISHED);
    }

    /**
     * Finds completed bookings by user and boat for additional validation context.
     *
     * @param userId the user identifier
     * @param boatId the boat identifier
     * @return number of completed bookings matching the criteria
     */
    public long countCompletedBookingsByUserAndBoat(Long userId, Long boatId) {
        return bookingRepository.countByUserIdAndBoatIdAndStatus(userId, boatId, BookingStatus.FINISHED);
    }
}