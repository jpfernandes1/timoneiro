package com.jompastech.backend.unit.service;

import com.jompastech.backend.model.enums.BookingStatus;
import com.jompastech.backend.repository.BookingRepository;
import com.jompastech.backend.service.BookingQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for {@link BookingQueryService}.
 *
 * <p><b>Testing Strategy:</b>
 * <ul>
 *   <li>Uses Mockito for mocking dependencies</li>
 *   <li>Focuses on business logic validation</li>
 *   <li>Tests both positive and negative scenarios</li>
 *   <li>Validates repository interaction contracts</li>
 *   <li>Tests edge cases and boundary conditions</li>
 * </ul>
 *
 * <p><b>Key Test Scenarios:</b>
 * <ul>
 *   <li>User rental eligibility validation</li>
 *   <li>Completed booking counting logic</li>
 *   <li>Null parameter handling</li>
 *   <li>Repository method invocation verification</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class BookingQueryServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private BookingQueryService bookingQueryService;

    /**
     * Tests that {@code hasUserRentedBoat} returns true when a user has completed
     * a rental for the specified boat.
     *
     * <p><b>Scenario:</b> User has finished booking for the boat
     * <p><b>Expected:</b> Method returns true
     * <p><b>Verifies:</b> Repository method is called with correct parameters
     */
    @Test
    void hasUserRentedBoat_WhenUserHasFinishedBooking_ReturnsTrue() {
        // Arrange
        Long userId = 1L;
        Long boatId = 2L;
        when(bookingRepository.existsByUserIdAndBoatIdAndStatus(userId, boatId, BookingStatus.FINISHED))
                .thenReturn(true);

        // Act
        boolean result = bookingQueryService.hasUserRentedBoat(userId, boatId);

        // Assert
        assertTrue(result);
        verify(bookingRepository).existsByUserIdAndBoatIdAndStatus(userId, boatId, BookingStatus.FINISHED);
    }

    /**
     * Tests that {@code hasUserRentedBoat} returns false when a user has not
     * completed any rental for the specified boat.
     *
     * <p><b>Scenario:</b> User has no finished booking for the boat
     * <p><b>Expected:</b> Method returns false
     * <p><b>Verifies:</b> Repository method is called with correct parameters
     */
    @Test
    void hasUserRentedBoat_WhenUserHasNoFinishedBooking_ReturnsFalse() {
        // Arrange
        Long userId = 1L;
        Long boatId = 2L;
        when(bookingRepository.existsByUserIdAndBoatIdAndStatus(userId, boatId, BookingStatus.FINISHED))
                .thenReturn(false);

        // Act
        boolean result = bookingQueryService.hasUserRentedBoat(userId, boatId);

        // Assert
        assertFalse(result);
        verify(bookingRepository).existsByUserIdAndBoatIdAndStatus(userId, boatId, BookingStatus.FINISHED);
    }

    /**
     * Tests that {@code hasUserRentedBoat} handles null parameters gracefully
     * without throwing exceptions.
     *
     * <p><b>Scenario:</b> Both user ID and boat ID are null
     * <p><b>Expected:</b> Method returns false without throwing exception
     * <p><b>Verifies:</b> Robust error handling for null inputs
     */
    @Test
    void hasUserRentedBoat_WhenNullParameters_ShouldHandleGracefully() {
        // Arrange
        when(bookingRepository.existsByUserIdAndBoatIdAndStatus(null, null, BookingStatus.FINISHED))
                .thenReturn(false);

        // Act
        boolean result = bookingQueryService.hasUserRentedBoat(null, null);

        // Assert
        assertFalse(result);
    }

    /**
     * Tests that {@code countCompletedBookingsByUserAndBoat} returns the correct
     * count when multiple finished bookings exist for the user-boat combination.
     *
     * <p><b>Scenario:</b> User has multiple finished bookings for the same boat
     * <p><b>Expected:</b> Method returns exact count of completed bookings
     * <p><b>Verifies:</b> Repository count method is called with correct parameters
     */
    @Test
    void countCompletedBookingsByUserAndBoat_WhenMultipleFinishedBookings_ReturnsCount() {
        // Arrange
        Long userId = 1L;
        Long boatId = 2L;
        long expectedCount = 3L;
        when(bookingRepository.countByUserIdAndBoatIdAndStatus(userId, boatId, BookingStatus.FINISHED))
                .thenReturn(expectedCount);

        // Act
        long result = bookingQueryService.countCompletedBookingsByUserAndBoat(userId, boatId);

        // Assert
        assertEquals(expectedCount, result);
        verify(bookingRepository).countByUserIdAndBoatIdAndStatus(userId, boatId, BookingStatus.FINISHED);
    }

    /**
     * Tests that {@code countCompletedBookingsByUserAndBoat} returns zero when
     * no finished bookings exist for the user-boat combination.
     *
     * <p><b>Scenario:</b> User has no finished bookings for the specified boat
     * <p><b>Expected:</b> Method returns zero
     * <p><b>Verifies:</b> Repository returns zero count for non-existent bookings
     */
    @Test
    void countCompletedBookingsByUserAndBoat_WhenNoFinishedBookings_ReturnsZero() {
        // Arrange
        Long userId = 1L;
        Long boatId = 2L;
        when(bookingRepository.countByUserIdAndBoatIdAndStatus(userId, boatId, BookingStatus.FINISHED))
                .thenReturn(0L);

        // Act
        long result = bookingQueryService.countCompletedBookingsByUserAndBoat(userId, boatId);

        // Assert
        assertEquals(0L, result);
    }

    /**
     * Tests that {@code countCompletedBookingsByUserAndBoat} handles null parameters
     * gracefully and returns zero without throwing exceptions.
     *
     * <p><b>Scenario:</b> Both user ID and boat ID are null
     * <p><b>Expected:</b> Method returns zero without throwing exception
     * <p><b>Verifies:</b> Robust error handling for null inputs
     */
    @Test
    void countCompletedBookingsByUserAndBoat_WithNullParameters_ShouldHandleGracefully() {
        // Arrange
        when(bookingRepository.countByUserIdAndBoatIdAndStatus(null, null, BookingStatus.FINISHED))
                .thenReturn(0L);

        // Act
        long result = bookingQueryService.countCompletedBookingsByUserAndBoat(null, null);

        // Assert
        assertEquals(0L, result);
    }

    /**
     * Tests the service's read-only transaction configuration by ensuring
     * that no write operations are performed during query execution.
     *
     * <p><b>Scenario:</b> Any query method execution
     * <p><b>Expected:</b> Only repository read methods are invoked
     * <p><b>Verifies:</b> Compliance with CQRS read-only design principle
     */
    @Test
    void service_ShouldOnlyPerformReadOperations() {
        // Arrange
        Long userId = 1L;
        Long boatId = 2L;
        when(bookingRepository.existsByUserIdAndBoatIdAndStatus(userId, boatId, BookingStatus.FINISHED))
                .thenReturn(true);

        // Act
        bookingQueryService.hasUserRentedBoat(userId, boatId);

        // Assert - Verify no save/delete methods are called
        verify(bookingRepository, never()).save(any());
        verify(bookingRepository, never()).delete(any());
        verify(bookingRepository, never()).deleteAll();
        verify(bookingRepository, never()).deleteById(any());
    }
}