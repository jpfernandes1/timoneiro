package com.jompastech.backend.Unit.service;

import com.jompastech.backend.model.dto.BoatAvailabilityRequestDTO;
import com.jompastech.backend.model.dto.BoatAvailabilityResponseDTO;
import com.jompastech.backend.model.entity.Boat;
import com.jompastech.backend.model.entity.BoatAvailability;
import com.jompastech.backend.repository.BoatAvailabilityRepository;
import com.jompastech.backend.repository.BoatRepository;
import com.jompastech.backend.service.BoatAvailabilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BoatAvailabilityService business logic.
 *
 * <p>Tests focus on service layer responsibilities for boat availability management:
 *     <ul>
 *         <li>CRUD operations for boat availability time slots</li>
 *         <li>Business logic for checking boat availability during specific periods</li>
 *         <li>Date range validations and overlapping slot detection</li>
 *         <li>Repository coordination and transaction boundaries</li>
 *         <li>Exception handling for resource not found scenarios</li>
 *     </ul>
 * </p>
 *
 * <p>Uses consistent test data initialization to ensure test independence
 * while minimizing code duplication through shared setup methods.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BoatAvailability Service Unit Tests")
class BoatAvailabilityServiceTest {

    @Mock
    private BoatAvailabilityRepository boatAvailabilityRepository;

    @Mock
    private BoatRepository boatRepository;

    @InjectMocks
    private BoatAvailabilityService boatAvailabilityService;

    private Boat testBoat;
    private BoatAvailability testAvailability;
    private BoatAvailability testAvailability2;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private BoatAvailabilityRequestDTO requestDTO;

    /**
     * Initializes comprehensive test data before each test execution.
     *
     * <p>Creates consistent entity relationships and date ranges that represent
     * real-world scenarios for boat availability contexts. This setup ensures
     * all tests work with the same baseline data while maintaining independence.</p>
     */
    @BeforeEach
    void setUp() {
        // Boat entity - represents the boat for which availability is being managed
        testBoat = new Boat();
        testBoat.setId(1L);

        // Date ranges for availability slots - represents typical rental periods
        startDate = LocalDateTime.of(2024, 1, 15, 10, 0);
        endDate = LocalDateTime.of(2024, 1, 15, 18, 0);

        // Request DTO for creating/updating availability
        requestDTO = new BoatAvailabilityRequestDTO(
                startDate,
                endDate,
                new BigDecimal("100.00")
        );

        // BoatAvailability entities - represent concrete time slots when boats are available
        testAvailability = new BoatAvailability(testBoat, startDate, endDate, new BigDecimal("100.00"));
        testAvailability.setId(1L);

        // Second availability slot for testing multiple slots scenarios
        testAvailability2 = new BoatAvailability(
                testBoat,
                startDate.plusDays(1),
                endDate.plusDays(1),
                new BigDecimal("120.00"));
        testAvailability2.setId(2L);
    }

    /**
     * Tests successful creation of a boat availability slot with valid data.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>Boat is retrieved from repository before creating availability</li>
     *   <li>Availability entity is properly created and saved</li>
     *   <li>Response DTO is returned with all data intact including generated ID</li>
     *   <li>Repository save method is called exactly once with the correct entity</li>
     *   <li>All DTO properties (startDate, endDate, pricePerHour) are preserved</li>
     * </ul>
     */
    @Test
    @DisplayName("CREATE - Should successfully create availability with DTO")
    void createAvailability_WhenValidData_ShouldSaveAndReturnDTO() {
        // Arrange
        when(boatRepository.findById(1L)).thenReturn(Optional.of(testBoat));
        when(boatAvailabilityRepository.save(any(BoatAvailability.class))).thenReturn(testAvailability);

        // Act
        BoatAvailabilityResponseDTO result = boatAvailabilityService.createAvailability(1L, requestDTO);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(1L, result.getBoatId());
        assertEquals(startDate, result.getStartDate());
        assertEquals(endDate, result.getEndDate());
        assertEquals(new BigDecimal("100.00"), result.getPricePerHour());

        verify(boatRepository, times(1)).findById(1L);
        verify(boatAvailabilityRepository, times(1)).save(any(BoatAvailability.class));
    }

    /**
     * Tests exception handling when creating availability for non-existent boat.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>Service throws RuntimeException when boat ID is not found</li>
     *   <li>Exception message includes the non-existent boat ID for debugging</li>
     *   <li>Repository save method is never called when boat doesn't exist</li>
     * </ul>
     */
    @Test
    @DisplayName("CREATE - Should throw exception when boat not found")
    void createAvailability_WhenBoatNotFound_ShouldThrowException() {
        // Arrange
        when(boatRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            boatAvailabilityService.createAvailability(999L, requestDTO);
        });

        assertEquals("Boat not found with id: 999", exception.getMessage());
        verify(boatRepository, times(1)).findById(999L);
        verify(boatAvailabilityRepository, never()).save(any());
    }

    /**
     * Tests successful retrieval of availability slot by existing ID as DTO.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>Service returns the correct availability DTO when ID exists in repository</li>
     *   <li>Repository findById method is properly called with the correct ID</li>
     *   <li>Returned DTO contains all expected data including relationships</li>
     * </ul>
     */
    @Test
    @DisplayName("READ - findById should return availability DTO when exists")
    void findById_WhenAvailabilityExists_ShouldReturnDTO() {
        // Arrange
        when(boatAvailabilityRepository.findById(1L)).thenReturn(Optional.of(testAvailability));

        // Act
        BoatAvailabilityResponseDTO result = boatAvailabilityService.findById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(1L, result.getBoatId());
        assertEquals(startDate, result.getStartDate());
        assertEquals(endDate, result.getEndDate());
        assertEquals(new BigDecimal("100.00"), result.getPricePerHour());

        verify(boatAvailabilityRepository, times(1)).findById(1L);
    }

    /**
     * Tests exception handling when attempting to retrieve non-existent availability by ID.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>Service throws RuntimeException with descriptive error message when ID not found</li>
     *   <li>Exception message includes the non-existent ID for debugging purposes</li>
     * </ul>
     */
    @Test
    @DisplayName("READ - findById should throw RuntimeException when not found")
    void findById_WhenAvailabilityNotFound_ShouldThrowRuntimeException() {
        // Arrange
        when(boatAvailabilityRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            boatAvailabilityService.findById(999L);
        });

        assertEquals("Availability not found with id: 999", exception.getMessage());
        verify(boatAvailabilityRepository, times(1)).findById(999L);
    }

    /**
     * Tests retrieval of all availability slots for a specific boat ID as DTOs.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>Service returns complete list of availability DTOs for the given boat ID</li>
     *   <li>Repository findByBoatId method is called with correct boat ID parameter</li>
     *   <li>Returned list contains all expected DTOs with correct data</li>
     * </ul>
     */
    @Test
    @DisplayName("READ - findAvailabilityByBoatId should return list of DTOs")
    void findAvailabilityByBoatId_ShouldReturnListOfDTOs() {
        // Arrange
        List<BoatAvailability> availabilityList = Arrays.asList(testAvailability, testAvailability2);
        when(boatAvailabilityRepository.findByBoatId(1L)).thenReturn(availabilityList);

        // Act
        List<BoatAvailabilityResponseDTO> result = boatAvailabilityService.findAvailabilityByBoatId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());

        // Verify first availability
        assertEquals(1L, result.get(0).getId());
        assertEquals(1L, result.get(0).getBoatId());
        assertEquals(startDate, result.get(0).getStartDate());
        assertEquals(endDate, result.get(0).getEndDate());

        // Verify second availability
        assertEquals(2L, result.get(1).getId());
        assertEquals(1L, result.get(1).getBoatId());
        assertEquals(startDate.plusDays(1), result.get(1).getStartDate());
        assertEquals(endDate.plusDays(1), result.get(1).getEndDate());

        verify(boatAvailabilityRepository, times(1)).findByBoatId(1L);
    }

    /**
     * Tests retrieval of available slots within a specific date range for a boat.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>Service returns only slots that fall completely within the specified date range</li>
     *   <li>Repository method with date range parameters is called with correct values</li>
     * </ul>
     */
    @Test
    @DisplayName("READ - findAvailableSlots should return slots within date range")
    void findAvailableSlots_ShouldReturnMatchingSlots() {
        // Arrange
        LocalDateTime queryStart = LocalDateTime.of(2024, 1, 14, 0, 0);
        LocalDateTime queryEnd = LocalDateTime.of(2024, 1, 16, 23, 59);

        List<BoatAvailability> expectedList = Arrays.asList(testAvailability);
        when(boatAvailabilityRepository.findByBoatIdAndStartDateAfterAndEndDateBefore(1L, queryStart, queryEnd))
                .thenReturn(expectedList);

        // Act
        List<BoatAvailability> result = boatAvailabilityService.findAvailableSlots(1L, queryStart, queryEnd);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(boatAvailabilityRepository, times(1))
                .findByBoatIdAndStartDateAfterAndEndDateBefore(1L, queryStart, queryEnd);
    }

    /**
     * Tests boat availability check when no overlapping slots exist for requested period.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>Service returns true when boat is completely available during requested period</li>
     *   <li>Repository exists method is called with correct parameters and date logic</li>
     * </ul>
     */
    @Test
    @DisplayName("READ - isBoatAvailable should return true when boat is available")
    void isBoatAvailable_WhenNoOverlap_ShouldReturnTrue() {
        // Arrange
        LocalDateTime checkStart = LocalDateTime.of(2024, 1, 16, 10, 0);
        LocalDateTime checkEnd = LocalDateTime.of(2024, 1, 16, 18, 0);

        when(boatAvailabilityRepository.existsByBoatIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                1L, checkEnd, checkStart)).thenReturn(false);

        // Act
        boolean isAvailable = boatAvailabilityService.isBoatAvailable(1L, checkStart, checkEnd);

        // Assert
        assertTrue(isAvailable);
        verify(boatAvailabilityRepository, times(1))
                .existsByBoatIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(1L, checkEnd, checkStart);
    }

    /**
     * Tests boat availability check when overlapping slots exist for requested period.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>Service returns false when boat is already booked during requested period</li>
     *   <li>Repository exists method correctly identifies overlapping time periods</li>
     * </ul>
     */
    @Test
    @DisplayName("READ - isBoatAvailable should return false when boat is not available")
    void isBoatAvailable_WhenOverlapExists_ShouldReturnFalse() {
        // Arrange
        when(boatAvailabilityRepository.existsByBoatIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                1L, endDate, startDate)).thenReturn(true);

        // Act
        boolean isAvailable = boatAvailabilityService.isBoatAvailable(1L, startDate, endDate);

        // Assert
        assertFalse(isAvailable);
        verify(boatAvailabilityRepository, times(1))
                .existsByBoatIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(1L, endDate, startDate);
    }

    /**
     * Tests successful update of availability slot with valid DTO data.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>Existing availability is first retrieved by ID to ensure existence</li>
     *   <li>All fields (startDate, endDate, pricePerHour) are properly updated</li>
     *   <li>Updated entity is persisted back to repository</li>
     *   <li>Updated DTO with new values is returned to caller</li>
     * </ul>
     */
    @Test
    @DisplayName("UPDATE - updateAvailability should update with DTO and save")
    void updateAvailability_ShouldUpdateWithDTOAndSave() {
        // Arrange
        LocalDateTime newStart = startDate.plusHours(2);
        LocalDateTime newEnd = endDate.plusHours(2);
        BigDecimal newPrice = new BigDecimal("150.00");

        BoatAvailabilityRequestDTO updateDTO = new BoatAvailabilityRequestDTO(newStart, newEnd, newPrice);

        when(boatAvailabilityRepository.findById(1L)).thenReturn(Optional.of(testAvailability));
        when(boatAvailabilityRepository.save(any(BoatAvailability.class))).thenAnswer(invocation -> {
            BoatAvailability saved = invocation.getArgument(0);
            saved.setStartDate(newStart);
            saved.setEndDate(newEnd);
            saved.setPricePerHour(newPrice);
            return saved;
        });

        // Act
        BoatAvailabilityResponseDTO result = boatAvailabilityService.updateAvailability(1L, updateDTO);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(newStart, result.getStartDate());
        assertEquals(newEnd, result.getEndDate());
        assertEquals(newPrice, result.getPricePerHour());

        verify(boatAvailabilityRepository, times(1)).findById(1L);
        verify(boatAvailabilityRepository, times(1)).save(any(BoatAvailability.class));
    }

    /**
     * Tests successful deletion of availability slot by its unique identifier.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>Repository deleteById method is called exactly once with correct ID</li>
     *   <li>Method executes without throwing exceptions for valid deletion</li>
     * </ul>
     */
    @Test
    @DisplayName("DELETE - deleteAvailability should delete by id")
    void deleteAvailability_ShouldCallDeleteById() {
        // Arrange
        when(boatAvailabilityRepository.existsById(1L)).thenReturn(true);

        // Act
        boatAvailabilityService.deleteAvailability(1L);

        // Assert
        verify(boatAvailabilityRepository, times(1)).existsById(1L);
        verify(boatAvailabilityRepository, times(1)).deleteById(1L);
    }

    /**
     * Tests deletion of all availability slots for a specific boat.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>All availability slots for the boat are first retrieved to determine what to delete</li>
     *   <li>Repository deleteAll method is called with the complete list of slots to delete</li>
     * </ul>
     */
    @Test
    @DisplayName("DELETE - deleteByBoatId should delete all availabilities for boat")
    void deleteByBoatId_ShouldDeleteAllForBoat() {
        // Arrange
        List<BoatAvailability> availabilities = Arrays.asList(testAvailability, testAvailability2);
        when(boatAvailabilityRepository.findByBoatId(1L)).thenReturn(availabilities);

        // Act
        boatAvailabilityService.deleteByBoatId(1L);

        // Assert
        verify(boatAvailabilityRepository, times(1)).findByBoatId(1L);
        verify(boatAvailabilityRepository, times(1)).deleteAll(availabilities);
    }

    /**
     * Tests exception handling when attempting to update non-existent availability slot.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>Service throws RuntimeException with descriptive error message when ID not found</li>
     *   <li>Repository save method is never called when entity doesn't exist</li>
     * </ul>
     */
    @Test
    @DisplayName("Edge Case - updateAvailability should throw exception when not found")
    void updateAvailability_WhenNotFound_ShouldThrowException() {
        // Arrange
        when(boatAvailabilityRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            boatAvailabilityService.updateAvailability(999L, requestDTO);
        });

        assertEquals("Availability not found with id: 999", exception.getMessage());
        verify(boatAvailabilityRepository, times(1)).findById(999L);
        verify(boatAvailabilityRepository, never()).save(any());
    }

    /**
     * Tests exception handling when attempting to delete non-existent availability.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>Service throws RuntimeException with descriptive error message when ID not found</li>
     *   <li>Repository deleteById method is never called when entity doesn't exist</li>
     * </ul>
     */
    @Test
    @DisplayName("Edge Case - deleteAvailability should throw exception when not found")
    void deleteAvailability_WhenNotFound_ShouldThrowException() {
        // Arrange
        when(boatAvailabilityRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            boatAvailabilityService.deleteAvailability(999L);
        });

        assertEquals("Availability not found with id: 999", exception.getMessage());
        verify(boatAvailabilityRepository, times(1)).existsById(999L);
        verify(boatAvailabilityRepository, never()).deleteById(anyLong());
    }

    /**
     * Tests behavior when no available slots are found within specified date range.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>Service returns empty list when no slots match the search criteria</li>
     *   <li>Empty list is properly returned (not null) and can be handled by callers</li>
     * </ul>
     */
    @Test
    @DisplayName("Edge Case - findAvailableSlots with empty result")
    void findAvailableSlots_WhenNoSlots_ShouldReturnEmptyList() {
        // Arrange
        LocalDateTime queryStart = LocalDateTime.of(2024, 2, 1, 0, 0);
        LocalDateTime queryEnd = LocalDateTime.of(2024, 2, 28, 23, 59);

        when(boatAvailabilityRepository.findByBoatIdAndStartDateAfterAndEndDateBefore(1L, queryStart, queryEnd))
                .thenReturn(Arrays.asList());

        // Act
        List<BoatAvailability> result = boatAvailabilityService.findAvailableSlots(1L, queryStart, queryEnd);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(boatAvailabilityRepository, times(1))
                .findByBoatIdAndStartDateAfterAndEndDateBefore(1L, queryStart, queryEnd);
    }

    /**
     * Tests behavior when no availability slots exist for a boat.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>Service returns empty list when boat has no availability slots</li>
     *   <li>Empty list is properly returned (not null) and can be handled by callers</li>
     * </ul>
     */
    @Test
    @DisplayName("Edge Case - findAvailabilityByBoatId with empty result")
    void findAvailabilityByBoatId_WhenNoSlots_ShouldReturnEmptyList() {
        // Arrange
        when(boatAvailabilityRepository.findByBoatId(1L)).thenReturn(Arrays.asList());

        // Act
        List<BoatAvailabilityResponseDTO> result = boatAvailabilityService.findAvailabilityByBoatId(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(boatAvailabilityRepository, times(1)).findByBoatId(1L);
    }
}