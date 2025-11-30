package com.jompastech.backend.service;

import com.jompastech.backend.model.entity.Boat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Test class for {@link PriceCalculationService}.
 *
 * <p><b>Testing Strategy:</b>
 * <ul>
 *   <li>Tests price calculation for various booking durations</li>
 *   <li>Validates minimum booking hour enforcement</li>
 *   <li>Tests edge cases and boundary conditions</li>
 *   <li>Verifies proper exception handling for invalid inputs</li>
 *   <li>Uses parameterized tests for comprehensive scenario coverage</li>
 * </ul>
 *
 * <p><b>Key Test Scenarios:</b>
 * <ul>
 *   <li>Normal price calculation above minimum hours</li>
 *   <li>Minimum booking hour enforcement</li>
 *   <li>Null parameter validation</li>
 *   <li>Invalid date range handling</li>
 *   <li>Boundary condition testing</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class PriceCalculationServiceTest {

    private final PriceCalculationService priceCalculationService = new PriceCalculationService();

    /**
     * Tests that price calculation works correctly for durations above minimum booking hours.
     *
     * <p><b>Scenario:</b> Booking duration exceeds minimum required hours
     * <p><b>Expected:</b> Price calculated based on actual hours
     * <p><b>Verifies:</b> Correct multiplication of hourly rate by actual duration
     */
    @Test
    void calculateBookingPrice_WhenDurationAboveMinimum_ReturnsCorrectPrice() {
        // Arrange
        Boat boat = createBoat(new BigDecimal("50.00"));
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 10, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 1, 1, 15, 0); // 5 hours

        BigDecimal expectedPrice = new BigDecimal("250.00"); // 5 * 50.00

        // Act
        BigDecimal result = priceCalculationService.calculateBookingPrice(boat, startDate, endDate);

        // Assert
        assertEquals(expectedPrice, result);
    }

    /**
     * Tests that minimum booking hours are enforced for short durations.
     *
     * <p><b>Scenario:</b> Booking duration below minimum required hours
     * <p><b>Expected:</b> Price calculated based on minimum 4 hours
     * <p><b>Verifies:</b> Minimum booking policy enforcement
     */
    @Test
    void calculateBookingPrice_WhenDurationBelowMinimum_EnforcesMinimumHours() {
        // Arrange
        Boat boat = createBoat(new BigDecimal("75.00"));
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 10, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 1, 1, 11, 30); // 1.5 hours

        BigDecimal expectedPrice = new BigDecimal("300.00"); // 4 * 75.00 (minimum)

        // Act
        BigDecimal result = priceCalculationService.calculateBookingPrice(boat, startDate, endDate);

        // Assert
        assertEquals(expectedPrice, result);
    }

    /**
     * Tests price calculation for exactly minimum booking duration.
     *
     * <p><b>Scenario:</b> Booking duration equals minimum required hours
     * <p><b>Expected:</b> Price calculated based on exact 4 hours
     * <p><b>Verifies:</b> Boundary condition handling at minimum threshold
     */
    @Test
    void calculateBookingPrice_WhenDurationAtMinimum_ReturnsCorrectPrice() {
        // Arrange
        Boat boat = createBoat(new BigDecimal("100.00"));
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 10, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 1, 1, 14, 0); // Exactly 4 hours

        BigDecimal expectedPrice = new BigDecimal("400.00"); // 4 * 100.00

        // Act
        BigDecimal result = priceCalculationService.calculateBookingPrice(boat, startDate, endDate);

        // Assert
        assertEquals(expectedPrice, result);
    }

    /**
     * Tests various booking scenarios using parameterized testing.
     *
     * <p><b>Scenarios:</b> Multiple duration and rate combinations
     * <p><b>Expected:</b> Correct price calculation for each scenario
     * <p><b>Verifies:</b> Comprehensive business rule validation
     */
    @ParameterizedTest
    @MethodSource("bookingScenariosProvider")
    void calculateBookingPrice_WithVariousScenarios_ReturnsExpectedPrice(
            BigDecimal hourlyRate,
            int startHour,
            int endHour,
            BigDecimal expectedPrice) {

        // Arrange
        Boat boat = createBoat(hourlyRate);
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, startHour, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 1, 1, endHour, 0);

        // Act
        BigDecimal result = priceCalculationService.calculateBookingPrice(boat, startDate, endDate);

        // Assert
        assertEquals(expectedPrice, result);
    }

    /**
     * Provides test data for parameterized booking scenarios.
     *
     * @return Stream of test arguments: hourlyRate, startHour, endHour, expectedPrice
     */
    private static Stream<Arguments> bookingScenariosProvider() {
        return Stream.of(
                arguments(new BigDecimal("50.00"), 10, 16, new BigDecimal("300.00")),  // 6 hours
                arguments(new BigDecimal("25.00"), 9, 11, new BigDecimal("100.00")),   // 2 hours -> min 4
                arguments(new BigDecimal("80.00"), 14, 18, new BigDecimal("320.00")),  // 4 hours exact
                arguments(new BigDecimal("150.00"), 8, 20, new BigDecimal("1800.00")) // 12 hours
        );
    }

    /**
     * Tests that null boat parameter throws appropriate exception.
     *
     * <p><b>Scenario:</b> Boat parameter is null
     * <p><b>Expected:</b> IllegalArgumentException with descriptive message
     * <p><b>Verifies:</b> Input validation for required parameters
     */
    @Test
    void calculateBookingPrice_WhenBoatIsNull_ThrowsIllegalArgumentException() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = startDate.plusHours(5);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> priceCalculationService.calculateBookingPrice(null, startDate, endDate));

        assertEquals("Boat and price per hour must not be null", exception.getMessage());
    }

    /**
     * Tests that boat with null price per hour throws appropriate exception.
     *
     * <p><b>Scenario:</b> Boat's pricePerHour is null
     * <p><b>Expected:</b> IllegalArgumentException with descriptive message
     * <p><b>Verifies:</b> Validation of boat's pricing information
     */
    @Test
    void calculateBookingPrice_WhenBoatPriceIsNull_ThrowsIllegalArgumentException() {
        // Arrange
        Boat boat = createBoat(null);
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = startDate.plusHours(5);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> priceCalculationService.calculateBookingPrice(boat, startDate, endDate));

        assertEquals("Boat and price per hour must not be null", exception.getMessage());
    }

    /**
     * Tests that null start date throws appropriate exception.
     *
     * <p><b>Scenario:</b> Start date parameter is null
     * <p><b>Expected:</b> IllegalArgumentException with descriptive message
     * <p><b>Verifies:</b> Date parameter validation
     */
    @Test
    void calculateBookingPrice_WhenStartDateIsNull_ThrowsIllegalArgumentException() {
        // Arrange
        Boat boat = createBoat(new BigDecimal("50.00"));
        LocalDateTime endDate = LocalDateTime.now().plusHours(5);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> priceCalculationService.calculateBookingPrice(boat, null, endDate));

        assertEquals("Invalid date range", exception.getMessage());
    }

    /**
     * Tests that null end date throws appropriate exception.
     *
     * <p><b>Scenario:</b> End date parameter is null
     * <p><b>Expected:</b> IllegalArgumentException with descriptive message
     * <p><b>Verifies:</b> Date parameter validation
     */
    @Test
    void calculateBookingPrice_WhenEndDateIsNull_ThrowsIllegalArgumentException() {
        // Arrange
        Boat boat = createBoat(new BigDecimal("50.00"));
        LocalDateTime startDate = LocalDateTime.now();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> priceCalculationService.calculateBookingPrice(boat, startDate, null));

        assertEquals("Invalid date range", exception.getMessage());
    }

    /**
     * Tests that invalid date range (start after end) throws appropriate exception.
     *
     * <p><b>Scenario:</b> Start date is after end date
     * <p><b>Expected:</b> IllegalArgumentException with descriptive message
     * <p><b>Verifies:</b> Temporal logic validation
     */
    @Test
    void calculateBookingPrice_WhenStartDateAfterEndDate_ThrowsIllegalArgumentException() {
        // Arrange
        Boat boat = createBoat(new BigDecimal("50.00"));
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.plusHours(5); // Start after end

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> priceCalculationService.calculateBookingPrice(boat, startDate, endDate));

        assertEquals("Invalid date range", exception.getMessage());
    }

    /**
     * Tests price calculation with different time precisions (including minutes).
     *
     * <p><b>Scenario:</b> Booking with partial hours (minutes)
     * <p><b>Expected:</b> Price calculated based on full hours (duration.toHours())
     * <p><b>Verifies:</b> Hour truncation behavior
     */
    @Test
    void calculateBookingPrice_WithPartialHours_TruncatesToFullHours() {
        // Arrange
        Boat boat = createBoat(new BigDecimal("60.00"));
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 10, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 1, 1, 13, 30); // 3.5 hours -> 3 hours

        BigDecimal expectedPrice = new BigDecimal("240.00"); // 4 * 60.00 (minimum)

        // Act
        BigDecimal result = priceCalculationService.calculateBookingPrice(boat, startDate, endDate);

        // Assert
        assertEquals(expectedPrice, result);
    }

    /**
     * Creates a Boat instance with specified hourly rate for testing.
     *
     * @param pricePerHour the hourly rate to set
     * @return configured Boat instance
     */
    private Boat createBoat(BigDecimal pricePerHour) {
        Boat boat = new Boat();
        boat.setId(1L);
        boat.setName("Test Boat");
        boat.setPricePerHour(pricePerHour);
        boat.setCapacity(4);
        return boat;
    }
}