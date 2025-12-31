package com.jompastech.backend.Unit.service;

import com.jompastech.backend.exception.BookingConflictException;
import com.jompastech.backend.exception.BookingValidationException;
import com.jompastech.backend.model.entity.Boat;
import com.jompastech.backend.model.entity.BoatAvailability;
import com.jompastech.backend.model.entity.Booking;
import com.jompastech.backend.model.entity.User;
import com.jompastech.backend.repository.BoatAvailabilityRepository;
import com.jompastech.backend.repository.BookingRepository;
import com.jompastech.backend.service.BookingValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for BookingValidationService
 *
 * Focus: Test the booking validation logic including duration checks,
 * availability verification, and conflict detection.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Booking Validation Service Tests")
class BookingValidationServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BoatAvailabilityRepository boatAvailabilityRepository;

    @InjectMocks
    private BookingValidationService bookingValidationService;

    private User testUser;
    private Boat testBoat;
    private Booking validBooking;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @BeforeEach
    void setUp() {
        startDate = LocalDateTime.now().plusDays(1);
        endDate = startDate.plusHours(4); // Exactly 4 hours, minimum duration

        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");

        testBoat = new Boat();
        testBoat.setId(1L);
        testBoat.setName("Test Boat");

        validBooking = new Booking(testUser, testBoat, startDate, endDate, new BigDecimal("500.00"));
    }

    @Nested
    @DisplayName("Successful Validation Scenarios")
    class SuccessfulValidationScenarios {

        @Test
        @DisplayName("Should validate successfully with all conditions met")
        void validateBookingCreation_WithAllConditionsMet_ShouldPassValidation() {
            // Arrange
            BoatAvailability availability = new BoatAvailability(testBoat, startDate.minusHours(1), endDate.plusHours(1), new BigDecimal("100.00"));
            when(boatAvailabilityRepository.findByBoatAndDateRange(eq(testBoat), eq(startDate), eq(endDate)))
                    .thenReturn(Collections.singletonList(availability));
            when(bookingRepository.findConflictingBookings(eq(testBoat), eq(startDate), eq(endDate)))
                    .thenReturn(Collections.emptyList());

            // Act & Assert - No exception should be thrown
            bookingValidationService.validateBookingCreation(validBooking);

            // Verify interactions
            assertThat(validBooking.hasValidDuration()).isTrue();
        }

        @Test
        @DisplayName("Should validate when multiple availability periods exist")
        void validateBookingCreation_WithMultipleAvailabilityPeriods_ShouldPassValidation() {
            // Arrange
            BoatAvailability availability1 = new BoatAvailability(testBoat, startDate.minusHours(5), startDate.minusHours(1), new BigDecimal("100.00"));
            BoatAvailability availability2 = new BoatAvailability(testBoat, startDate.minusHours(1), endDate.plusHours(2), new BigDecimal("100.00"));
            List<BoatAvailability> availabilities = Arrays.asList(availability1, availability2);

            when(boatAvailabilityRepository.findByBoatAndDateRange(eq(testBoat), eq(startDate), eq(endDate)))
                    .thenReturn(availabilities);
            when(bookingRepository.findConflictingBookings(eq(testBoat), eq(startDate), eq(endDate)))
                    .thenReturn(Collections.emptyList());

            // Act & Assert - No exception should be thrown
            bookingValidationService.validateBookingCreation(validBooking);
        }

        @Test
        @DisplayName("Should validate when booking exactly matches availability period")
        void validateBookingCreation_WithExactAvailabilityMatch_ShouldPassValidation() {
            // Arrange
            BoatAvailability availability = new BoatAvailability(testBoat, startDate, endDate, new BigDecimal("100.00"));
            when(boatAvailabilityRepository.findByBoatAndDateRange(eq(testBoat), eq(startDate), eq(endDate)))
                    .thenReturn(Collections.singletonList(availability));
            when(bookingRepository.findConflictingBookings(eq(testBoat), eq(startDate), eq(endDate)))
                    .thenReturn(Collections.emptyList());

            // Act & Assert - No exception should be thrown
            bookingValidationService.validateBookingCreation(validBooking);
        }

        @Test
        @DisplayName("Should validate when no existing bookings exist")
        void validateBookingCreation_WithNoExistingBookings_ShouldPassValidation() {
            // Arrange
            BoatAvailability availability = new BoatAvailability(testBoat, startDate.minusHours(1), endDate.plusHours(1), new BigDecimal("100.00"));
            when(boatAvailabilityRepository.findByBoatAndDateRange(eq(testBoat), eq(startDate), eq(endDate)))
                    .thenReturn(Collections.singletonList(availability));
            when(bookingRepository.findConflictingBookings(eq(testBoat), eq(startDate), eq(endDate)))
                    .thenReturn(Collections.emptyList());

            // Act & Assert - No exception should be thrown
            bookingValidationService.validateBookingCreation(validBooking);
        }
    }

    @Nested
    @DisplayName("Duration Validation Failures")
    class DurationValidationFailures {

        @Test
        @DisplayName("Should throw BookingValidationException when duration is less than 4 hours")
        void validateBookingCreation_WithDurationLessThan4Hours_ShouldThrowBookingValidationException() {
            // Arrange
            LocalDateTime shortStartDate = LocalDateTime.now().plusDays(1);
            LocalDateTime shortEndDate = shortStartDate.plusHours(3); // Only 3 hours
            Booking shortBooking = new Booking(testUser, testBoat, shortStartDate, shortEndDate, new BigDecimal("300.00"));

            // Act & Assert
            assertThatThrownBy(() -> bookingValidationService.validateBookingCreation(shortBooking))
                    .isInstanceOf(BookingValidationException.class)
                    .hasMessageContaining("Booking must be at least 4 hours");
        }

        @Test
        @DisplayName("Should pass when duration is exactly 4 hours")
        void validateBookingCreation_WithDurationExactly4Hours_ShouldPass() {
            // Arrange
            LocalDateTime exactStartDate = LocalDateTime.now().plusDays(1);
            LocalDateTime exactEndDate = exactStartDate.plusHours(4); // Exactly 4 hours
            Booking exactBooking = new Booking(testUser, testBoat, exactStartDate, exactEndDate, new BigDecimal("400.00"));

            BoatAvailability availability = new BoatAvailability(testBoat, exactStartDate.minusHours(1), exactEndDate.plusHours(1), new BigDecimal("100.00"));
            when(boatAvailabilityRepository.findByBoatAndDateRange(eq(testBoat), eq(exactStartDate), eq(exactEndDate)))
                    .thenReturn(Collections.singletonList(availability));
            when(bookingRepository.findConflictingBookings(eq(testBoat), eq(exactStartDate), eq(exactEndDate)))
                    .thenReturn(Collections.emptyList());

            // Act & Assert - No exception should be thrown
            bookingValidationService.validateBookingCreation(exactBooking);
        }
    }

    @Nested
    @DisplayName("Availability Validation Failures")
    class AvailabilityValidationFailures {

        @Test
        @DisplayName("Should throw BookingValidationException when no availability periods exist")
        void validateBookingCreation_WithNoAvailabilityPeriods_ShouldThrowBookingValidationException() {
            // Arrange
            when(boatAvailabilityRepository.findByBoatAndDateRange(eq(testBoat), eq(startDate), eq(endDate)))
                    .thenReturn(Collections.emptyList());

            // Act & Assert
            assertThatThrownBy(() -> bookingValidationService.validateBookingCreation(validBooking))
                    .isInstanceOf(BookingValidationException.class)
                    .hasMessageContaining("Boat is not available for the selected dates");
        }

        @Test
        @DisplayName("Should throw BookingValidationException when booking doesn't fit within availability periods")
        void validateBookingCreation_WhenBookingDoesntFitWithinAvailability_ShouldThrowBookingValidationException() {
            // Arrange
            // Availability that starts after booking start or ends before booking end
            BoatAvailability availability = new BoatAvailability(testBoat,
                    startDate.plusHours(1),  // Availability starts 1 hour after booking
                    endDate.minusHours(1),
                    new BigDecimal("100.00"));   // Availability ends 1 hour before booking

            when(boatAvailabilityRepository.findByBoatAndDateRange(eq(testBoat), eq(startDate), eq(endDate)))
                    .thenReturn(Collections.singletonList(availability));

            // Act & Assert
            assertThatThrownBy(() -> bookingValidationService.validateBookingCreation(validBooking))
                    .isInstanceOf(BookingValidationException.class)
                    .hasMessageContaining("Booking period doesn't match boat availability");
        }

        @Test
        @DisplayName("Should throw exception when booking starts before availability")
        void validateBookingCreation_WhenBookingStartsBeforeAvailability_ShouldThrowBookingValidationException() {
            // Arrange
            BoatAvailability availability = new BoatAvailability(testBoat,
                    startDate.plusHours(1),  // Availability starts 1 hour after booking
                    endDate.plusHours(2),
                    new BigDecimal("100.00"));

            when(boatAvailabilityRepository.findByBoatAndDateRange(eq(testBoat), eq(startDate), eq(endDate)))
                    .thenReturn(Collections.singletonList(availability));

            // Act & Assert
            assertThatThrownBy(() -> bookingValidationService.validateBookingCreation(validBooking))
                    .isInstanceOf(BookingValidationException.class)
                    .hasMessageContaining("Booking period doesn't match boat availability");
        }

        @Test
        @DisplayName("Should throw exception when booking ends after availability")
        void validateBookingCreation_WhenBookingEndsAfterAvailability_ShouldThrowBookingValidationException() {
            // Arrange
            BoatAvailability availability = new BoatAvailability(testBoat,
                    startDate.minusHours(2),
                    endDate.minusHours(1),
                    new BigDecimal("100.00"));  // Availability ends 1 hour before booking

            when(boatAvailabilityRepository.findByBoatAndDateRange(eq(testBoat), eq(startDate), eq(endDate)))
                    .thenReturn(Collections.singletonList(availability));

            // Act & Assert
            assertThatThrownBy(() -> bookingValidationService.validateBookingCreation(validBooking))
                    .isInstanceOf(BookingValidationException.class)
                    .hasMessageContaining("Booking period doesn't match boat availability");
        }
    }

    @Nested
    @DisplayName("Conflict Validation Failures")
    class ConflictValidationFailures {

        @Test
        @DisplayName("Should throw BookingConflictException when conflicting booking exists")
        void validateBookingCreation_WithConflictingBooking_ShouldThrowBookingConflictException() {
            // Arrange
            BoatAvailability availability = new BoatAvailability(testBoat, startDate.minusHours(1), endDate.plusHours(1),new BigDecimal("100.00"));
            when(boatAvailabilityRepository.findByBoatAndDateRange(eq(testBoat), eq(startDate), eq(endDate)))
                    .thenReturn(Collections.singletonList(availability));

            // Create a conflicting booking
            Booking conflictingBooking = new Booking(testUser, testBoat,
                    startDate.plusHours(1),  // Overlaps with new booking
                    endDate.plusHours(1),
                    new BigDecimal("500.00"));

            when(bookingRepository.findConflictingBookings(eq(testBoat), eq(startDate), eq(endDate)))
                    .thenReturn(Collections.singletonList(conflictingBooking));

            // Act & Assert
            assertThatThrownBy(() -> bookingValidationService.validateBookingCreation(validBooking))
                    .isInstanceOf(BookingConflictException.class)
                    .hasMessageContaining("Booking conflicts with existing reservation");
        }

        @Test
        @DisplayName("Should throw exception when new booking completely overlaps existing booking")
        void validateBookingCreation_WhenCompletelyOverlapsExistingBooking_ShouldThrowBookingConflictException() {
            // Arrange
            BoatAvailability availability = new BoatAvailability(testBoat, startDate.minusHours(1), endDate.plusHours(1), new BigDecimal("100.00"));
            when(boatAvailabilityRepository.findByBoatAndDateRange(eq(testBoat), eq(startDate), eq(endDate)))
                    .thenReturn(Collections.singletonList(availability));

            // Existing booking that is completely within new booking period
            Booking existingBooking = new Booking(testUser, testBoat,
                    startDate.plusHours(1),
                    endDate.minusHours(1),
                    new BigDecimal("500.00"));

            when(bookingRepository.findConflictingBookings(eq(testBoat), eq(startDate), eq(endDate)))
                    .thenReturn(Collections.singletonList(existingBooking));

            // Act & Assert
            assertThatThrownBy(() -> bookingValidationService.validateBookingCreation(validBooking))
                    .isInstanceOf(BookingConflictException.class)
                    .hasMessageContaining("Booking conflicts with existing reservation");
        }

        @Test
        @DisplayName("Should throw exception when new booking starts during existing booking")
        void validateBookingCreation_WhenStartsDuringExistingBooking_ShouldThrowBookingConflictException() {
            // Arrange
            BoatAvailability availability = new BoatAvailability(testBoat, startDate.minusHours(1), endDate.plusHours(1), new BigDecimal("100.00"));
            when(boatAvailabilityRepository.findByBoatAndDateRange(eq(testBoat), eq(startDate), eq(endDate)))
                    .thenReturn(Collections.singletonList(availability));

            // Existing booking that starts before new booking
            Booking existingBooking = new Booking(testUser, testBoat,
                    startDate.minusHours(1),  // Starts before new booking
                    startDate.plusHours(2),   // Ends during new booking
                    new BigDecimal("500.00"));

            when(bookingRepository.findConflictingBookings(eq(testBoat), eq(startDate), eq(endDate)))
                    .thenReturn(Collections.singletonList(existingBooking));

            // Act & Assert
            assertThatThrownBy(() -> bookingValidationService.validateBookingCreation(validBooking))
                    .isInstanceOf(BookingConflictException.class)
                    .hasMessageContaining("Booking conflicts with existing reservation");
        }

        @Test
        @DisplayName("Should throw exception when new booking ends during existing booking")
        void validateBookingCreation_WhenEndsDuringExistingBooking_ShouldThrowBookingConflictException() {
            // Arrange
            BoatAvailability availability = new BoatAvailability(testBoat, startDate.minusHours(1), endDate.plusHours(1), new BigDecimal("100.00"));
            when(boatAvailabilityRepository.findByBoatAndDateRange(eq(testBoat), eq(startDate), eq(endDate)))
                    .thenReturn(Collections.singletonList(availability));

            // Existing booking that ends after new booking
            Booking existingBooking = new Booking(testUser, testBoat,
                    endDate.minusHours(2),    // Starts during new booking
                    endDate.plusHours(1),     // Ends after new booking
                    new BigDecimal("500.00"));

            when(bookingRepository.findConflictingBookings(eq(testBoat), eq(startDate), eq(endDate)))
                    .thenReturn(Collections.singletonList(existingBooking));

            // Act & Assert
            assertThatThrownBy(() -> bookingValidationService.validateBookingCreation(validBooking))
                    .isInstanceOf(BookingConflictException.class)
                    .hasMessageContaining("Booking conflicts with existing reservation");
        }

        @Test
        @DisplayName("Should handle multiple conflicting bookings")
        void validateBookingCreation_WithMultipleConflictingBookings_ShouldThrowBookingConflictException() {
            // Arrange
            BoatAvailability availability = new BoatAvailability(testBoat, startDate.minusHours(1), endDate.plusHours(1), new BigDecimal("100.00"));
            when(boatAvailabilityRepository.findByBoatAndDateRange(eq(testBoat), eq(startDate), eq(endDate)))
                    .thenReturn(Collections.singletonList(availability));

            // Create multiple conflicting bookings
            Booking conflict1 = new Booking(testUser, testBoat, startDate.minusHours(1), startDate.plusHours(1), new BigDecimal("500.00"));
            Booking conflict2 = new Booking(testUser, testBoat, endDate.minusHours(1), endDate.plusHours(1), new BigDecimal("500.00"));

            when(bookingRepository.findConflictingBookings(eq(testBoat), eq(startDate), eq(endDate)))
                    .thenReturn(Arrays.asList(conflict1, conflict2));

            // Act & Assert
            assertThatThrownBy(() -> bookingValidationService.validateBookingCreation(validBooking))
                    .isInstanceOf(BookingConflictException.class)
                    .hasMessageContaining("Booking conflicts with existing reservation");
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Conditions")
    class EdgeCasesAndBoundaryConditions {

        @Test
        @DisplayName("Should pass when booking touches availability boundaries exactly")
        void validateBookingCreation_WhenBookingTouchesAvailabilityBoundariesExactly_ShouldPass() {
            // Arrange
            LocalDateTime availabilityStart = startDate;
            LocalDateTime availabilityEnd = endDate;
            BoatAvailability availability = new BoatAvailability(testBoat, availabilityStart, availabilityEnd, new BigDecimal("100.00"));

            when(boatAvailabilityRepository.findByBoatAndDateRange(eq(testBoat), eq(startDate), eq(endDate)))
                    .thenReturn(Collections.singletonList(availability));
            when(bookingRepository.findConflictingBookings(eq(testBoat), eq(startDate), eq(endDate)))
                    .thenReturn(Collections.emptyList());

            // Act & Assert - No exception should be thrown
            bookingValidationService.validateBookingCreation(validBooking);
        }

        @Test
        @DisplayName("Should not consider cancelled bookings as conflicts")
        void validateBookingCreation_WhenOnlyCancelledBookingsExist_ShouldPass() {
            // Arrange
            // This test relies on the repository method excluding CANCELLED bookings
            // The repository method uses: "b.status != 'CANCELLED'"
            BoatAvailability availability = new BoatAvailability(testBoat, startDate.minusHours(1), endDate.plusHours(1), new BigDecimal("100.00"));
            when(boatAvailabilityRepository.findByBoatAndDateRange(eq(testBoat), eq(startDate), eq(endDate)))
                    .thenReturn(Collections.singletonList(availability));

            // Repository should return empty list because canceled bookings are excluded
            when(bookingRepository.findConflictingBookings(eq(testBoat), eq(startDate), eq(endDate)))
                    .thenReturn(Collections.emptyList());

            // Act & Assert - No exception should be thrown
            bookingValidationService.validateBookingCreation(validBooking);
        }

        @Test
        @DisplayName("Should validate booking with long duration")
        void validateBookingCreation_WithLongDuration_ShouldPass() {
            // Arrange
            LocalDateTime longStartDate = LocalDateTime.now().plusDays(1);
            LocalDateTime longEndDate = longStartDate.plusDays(7); // 7 days = 168 hours
            Booking longBooking = new Booking(testUser, testBoat, longStartDate, longEndDate, new BigDecimal("5000.00"));

            BoatAvailability availability = new BoatAvailability(testBoat, longStartDate.minusHours(1), longEndDate.plusHours(1), new BigDecimal("100.00"));
            when(boatAvailabilityRepository.findByBoatAndDateRange(eq(testBoat), eq(longStartDate), eq(longEndDate)))
                    .thenReturn(Collections.singletonList(availability));
            when(bookingRepository.findConflictingBookings(eq(testBoat), eq(longStartDate), eq(longEndDate)))
                    .thenReturn(Collections.emptyList());

            // Act & Assert - No exception should be thrown
            bookingValidationService.validateBookingCreation(longBooking);
        }
    }

    @Test
    @DisplayName("Should throw exception when availability repository returns null")
    void validateBookingCreation_WhenAvailabilityRepositoryReturnsNull_ShouldThrowException() {
        // Arrange
        when(boatAvailabilityRepository.findByBoatAndDateRange(eq(testBoat), eq(startDate), eq(endDate)))
                .thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> bookingValidationService.validateBookingCreation(validBooking))
                .isInstanceOf(NullPointerException.class); // Will throw NPE when trying to call isEmpty() on null
    }

    @Test
    @DisplayName("Should throw exception when booking repository returns null")
    void validateBookingCreation_WhenBookingRepositoryReturnsNull_ShouldThrowException() {
        // Arrange
        BoatAvailability availability = new BoatAvailability(testBoat, startDate.minusHours(1), endDate.plusHours(1), new BigDecimal("100.00"));
        when(boatAvailabilityRepository.findByBoatAndDateRange(eq(testBoat), eq(startDate), eq(endDate)))
                .thenReturn(Collections.singletonList(availability));
        when(bookingRepository.findConflictingBookings(eq(testBoat), eq(startDate), eq(endDate)))
                .thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> bookingValidationService.validateBookingCreation(validBooking))
                .isInstanceOf(NullPointerException.class); // Will throw NPE when trying to iterate over null
    }
}