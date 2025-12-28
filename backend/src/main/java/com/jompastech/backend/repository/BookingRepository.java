package com.jompastech.backend.repository;

import com.jompastech.backend.model.dto.booking.BookingResponseDTO;
import com.jompastech.backend.model.entity.Boat;
import com.jompastech.backend.model.entity.Booking;
import com.jompastech.backend.model.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Booking entity operations and complex booking queries.
 *
 * Provides data access methods for booking management with specialized queries
 * for conflict detection and availability validation. Extends JpaRepository to
 * inherit standard CRUD operations while adding domain-specific search capabilities.
 *
 * The custom query implementations use JPQL for database-agnostic date range
 * comparisons and conflict detection logic that aligns with business rules.
 */
public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * Finds active bookings that conflict with a specified date range for a given boat.

     * This query detects overlapping bookings by evaluating multiple temporal scenarios:
     * - Booking starts within the target period
     * - Booking ends within the target period
     * - Booking completely encompasses the target period

     * Business Rule: Excludes cancelled bookings since they don't represent actual
     * reservations and shouldn't block availability. This ensures that cancelled
     * bookings don't interfere with new booking requests.

     * @param boat the boat to check for booking conflicts
     * @param startDate the start date of the potential booking period
     * @param endDate the end date of the potential booking period
     * @return list of non-cancelled bookings that overlap with the specified date range
     */
    @Query("SELECT b FROM Booking b WHERE b.boat = :boat AND b.status != 'CANCELLED' " +
            "AND ((b.startDate BETWEEN :startDate AND :endDate) OR " +
            "(b.endDate BETWEEN :startDate AND :endDate) OR " +
            "(b.startDate <= :startDate AND b.endDate >= :endDate))")
    List<Booking> findConflictingBookings(@Param("boat") Boat boat,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    /**
     * Checks if a completed booking exists for the user-boat combination.
     * Used for review eligibility validation.
     */
    boolean existsByUserIdAndBoatIdAndStatus(Long userId, Long boatId, String status);

    /**
     * Counts completed bookings for user-boat combination.
     * Provides additional business context for validation.
     */
    long countByUserIdAndBoatIdAndStatus(Long userId, Long boatId, String status);

    // Search for all of a user's bookings.
    Page<Booking> findByUserId(Long userId, Pageable pageable);

    // Search for a user's bookings by status.
    Page<Booking> findByUserIdAndStatus(Long userId, BookingStatus status, Pageable pageable);

    // Search for bookings where the user owns the boat.
    @Query("SELECT b FROM Booking b WHERE b.boat.owner.id = :userId")
    Page<Booking> findByBoatOwnerId(@Param("userId") Long userId, Pageable pageable);

}