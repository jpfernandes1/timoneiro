package com.jompastech.backend.Unit.service;

import com.jompastech.backend.mapper.BoatMapper;
import com.jompastech.backend.model.dto.BoatRequestDTO;
import com.jompastech.backend.model.dto.BoatResponseDTO;
import com.jompastech.backend.model.entity.Boat;
import com.jompastech.backend.model.entity.User;
import com.jompastech.backend.repository.BoatRepository;
import com.jompastech.backend.repository.UserRepository;
import com.jompastech.backend.service.BoatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BoatService business logic.
 *
 * <p>Tests focus on service layer responsibilities:
 *     <ul>
 *         <li>Business rule validation and enforcement</li>
 *         <li>Repository coordination and transaction boundaries</li>
 *         <li>Authorization and permission logic</li>
 *         <li>DTO mapping delegation</li>
 *     </ul>
 * </p>
 *
 * <p>Uses consistent test data initialization to ensure test independence
 * while minimizing code duplication through shared setup.</p>
 */
@ExtendWith(MockitoExtension.class)
public class BoatServiceTest {

    @Mock
    private BoatRepository boatRepository;

    @InjectMocks
    private BoatService boatService;

    @Mock
    private BoatMapper boatMapper;

    @Mock
    private UserRepository userRepository;

    private Boat testBoat;
    private Boat testBoat2;
    private User testBoatOwner;
    private BoatRequestDTO testRequestDTO;
    private BoatResponseDTO testResponseDTO;

    /**
     * Initializes comprehensive test data before each test execution.
     *
     * <p>Creates consistent entity relationships and DTOs that represent
     * real-world scenarios for Boat contexts.</p>
     */
    @BeforeEach
    void setUp() {
        // User entities - represents the boat owner
        testBoatOwner = new User();
        testBoatOwner.setId(1L);
        testBoatOwner.setName("Test User");
        testBoatOwner.setEmail("teste@teste.com");

        // Boat entities - represents the aim of this tests
        testBoat = new Boat();
        testBoat.setId(1L);
        testBoat.setOwner(testBoatOwner);
        testBoat.setName("Demeter");
        testBoat.setType("Long Boat");

        testBoat2 = new Boat();
        testBoat2.setId(2L);
        testBoat2.setOwner(testBoatOwner);
        testBoat2.setName("Nautilus");
        testBoat2.setType("Submarine");

        // Request DTOs - represent API inputs for different contexts
        testRequestDTO = new BoatRequestDTO("Nautilus", "Submarine", null, 8, null, null, null, 1L);

        // Response DTOs - represent API output format
        testResponseDTO = new BoatResponseDTO(
                testBoat.getId(),
                testBoat.getName(),
                null,
                testBoat.getType(),
                6,
                null,
                null,
                null,
                null,
                testBoat.getOwner().getName()
        );
    }

    /**
     * Tests successful boat creation with valid data.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>Boat entity is properly converted from DTO</li>
     *   <li>Boat is persisted with correct data</li>
     *   <li>Response DTO is properly generated from saved entity</li>
     * </ul>
     */
    @Test
    void save_WithValidBookingContext_ShouldSuccess(){
        // Arrange
        when(boatMapper.toEntity(testRequestDTO)).thenReturn(testBoat);
        when(boatRepository.save(testBoat)).thenReturn(testBoat);
        when(boatMapper.toResponseDTO(testBoat)).thenReturn(testResponseDTO);

        // Act
        BoatResponseDTO result = boatService.save(testRequestDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testBoat.getId());
        verify(boatRepository).save(testBoat);
        verify(boatMapper).toResponseDTO(testBoat);
        verify(boatMapper).toEntity(testRequestDTO);
    }

    /**
     * Tests successful retrieval of boat by existing ID.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>Service returns correct boat when ID exists</li>
     *   <li>Repository findById method is properly called</li>
     * </ul>
     */
    @Test
    void findById_ShouldReturnABoatByIdIfItExists(){
        // Arrange
        when(boatRepository.findById(testBoat.getId())).thenReturn(Optional.of(testBoat));

        // Act
        Boat result = boatService.findById(testBoat.getId());

        // Assert
        assertThat(result).isEqualTo(testBoat);
        verify(boatRepository).findById(testBoat.getId());
    }

    /**
     * Tests exception handling when boat ID is not found.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>Service throws RuntimeException with proper message</li>
     *   <li>Exception is thrown when boat doesn't exist</li>
     *   <li>Repository findById method is properly called</li>
     * </ul>
     */
    @Test
    void findById_ShouldThrowAnExceptionIfNotExists(){
        // Arrange
        Long nonExistingId = 99L;
        when(boatRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> boatService.findById(nonExistingId));

        assertThat(exception.getMessage()).isEqualTo("Boat not found!");
        verify(boatRepository).findById(nonExistingId);
    }

    /**
     * Tests retrieval of all boats when they exist.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>Service returns complete list of boats</li>
     *   <li>Repository findAll method is properly called</li>
     *   <li>Returned list contains expected boats</li>
     * </ul>
     */
    @Test
    void findAll_ShouldReturnAllBoatsWhenExists(){
        // Arrange
        List<Boat> expectedBoats = Arrays.asList(testBoat, testBoat2);
        when(boatRepository.findAll()).thenReturn(expectedBoats);

        // Act
        List<Boat> result = boatService.findAll();

        // Assert
        assertThat(result).containsExactly(testBoat, testBoat2);
        verify(boatRepository).findAll();
    }

    /**
     * Tests boat deletion and subsequent search behavior.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>Boat is properly deleted from repository</li>
     *   <li>Searching for deleted boat throws exception</li>
     *   <li>Repository deleteById method is properly called</li>
     * </ul>
     */
    @Test
    void deleteByID_ShouldDeleteABoatAndReturnANotFoundExceptionWhenSearchByIt(){
        // Arrange
        Long boatId = 1L;
        when(boatRepository.findById(boatId)).thenReturn(Optional.of(testBoat));

        // Act - First find to ensure boat exists
        Boat beforeDelete = boatService.findById(boatId);
        assertThat(beforeDelete).isNotNull();

        // Delete the boat
        boatService.deleteById(boatId);

        // Now mock the repository to return empty for findById
        when(boatRepository.findById(boatId)).thenReturn(Optional.empty());

        // Act & Assert - Now findById should throw exception
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> boatService.findById(boatId));

        assertThat(exception.getMessage()).isEqualTo("Boat not found!");
        verify(boatRepository).deleteById(boatId);
    }

    /**
     * Tests retrieval of boats by specific type.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>Service returns boats filtered by type</li>
     *   <li>Repository findByType method is properly called</li>
     *   <li>Returned list contains only boats of specified type</li>
     * </ul>
     */
    @Test
    void findByType_ShouldReturnBoatsByTypeIfExists(){
        // Arrange
        String boatType = "Submarine";
        List<Boat> expectedBoats = Arrays.asList(testBoat2);
        when(boatRepository.findByType(boatType)).thenReturn(expectedBoats);

        // Act
        List<Boat> result = boatService.findByType(boatType);

        // Assert
        assertThat(result)
                .isNotNull()
                .hasSize(1)
                .containsExactly(testBoat2);
        verify(boatRepository).findByType(boatType);
    }

    /**
     * Tests retrieval of all boats belonging to a specific owner.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>Service returns all boats for given owner ID</li>
     *   <li>Repository findByOwnerId method is properly called</li>
     *   <li>Returned list contains correct owner's boats</li>
     * </ul>
     */
    @Test
    void findByOwnerId_ShouldReturnAllBoatsBelongingToAnOwner(){
        // Arrange
        List<Boat> expectedBoats = Arrays.asList(testBoat, testBoat2);
        Long ownerId = 1L;
        when(boatRepository.findByOwnerId(ownerId)).thenReturn(expectedBoats);

        // Act
        List<Boat> result = boatService.findByOwnerId(ownerId);

        // Assert
        assertThat(result)
                .isNotNull()
                .hasSize(2)
                .containsExactly(testBoat, testBoat2);
        verify(boatRepository).findByOwnerId(ownerId);
    }

    /**
     * Tests retrieval of all boats converted to Response DTO format.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>All boats are properly converted to Response DTOs</li>
     *   <li>Repository findAll and mapper toResponseDTO methods are called</li>
     *   <li>Returned list contains properly formatted DTOs</li>
     * </ul>
     */
    @Test
    void findAllBoats_ShouldReturnAllBoatsInResponseDTOFormat(){
        // Arrange
        List<Boat> mockBoats = Arrays.asList(testBoat, testBoat2);

        BoatResponseDTO dto1 = new BoatResponseDTO(1L, "Demeter", null, "Long Boat", 80, null, null, null, null, testBoatOwner.getName());
        BoatResponseDTO dto2 = new BoatResponseDTO(2L, "Nautilus", null, "Submarine", 12, null, null, null, null, testBoatOwner.getName());
        List<BoatResponseDTO> expectedDTOs = Arrays.asList(dto1, dto2);

        when(boatRepository.findAll()).thenReturn(mockBoats);
        when(boatMapper.toResponseDTO(testBoat)).thenReturn(dto1);
        when(boatMapper.toResponseDTO(testBoat2)).thenReturn(dto2);

        // Act
        List<BoatResponseDTO> result = boatService.findAllBoats();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(dto1, dto2);

        verify(boatRepository).findAll();
        verify(boatMapper).toResponseDTO(testBoat);
        verify(boatMapper).toResponseDTO(testBoat2);
    }
}