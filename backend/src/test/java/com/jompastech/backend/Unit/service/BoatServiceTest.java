package com.jompastech.backend.Unit.service;

import com.jompastech.backend.mapper.BoatMapper;
import com.jompastech.backend.model.dto.BoatRequestDTO;
import com.jompastech.backend.model.dto.BoatResponseDTO;
import com.jompastech.backend.model.entity.Boat;
import com.jompastech.backend.model.entity.User;
import com.jompastech.backend.repository.AddressRepository;
import com.jompastech.backend.repository.BoatRepository;
import com.jompastech.backend.repository.UserRepository;
import com.jompastech.backend.service.BoatService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.lang.reflect.Field;
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
    private AddressRepository addressRepository;

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
        testRequestDTO = new BoatRequestDTO(
                "Nautilus",
                "Submarine",
                null,
                8,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
                );

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
                null,
                null,
                null,
                null,
                null,
                testBoat.getOwner().getName(),
                testBoatOwner.getId()
        );

        // Manual injection of UserRepository on BoatService
        // The UserRepository is not in the constructor, so we need to inject it via reflection.
        try {
            Field userRepositoryField = BoatService.class.getDeclaredField("userRepository");
            userRepositoryField.setAccessible(true);
            userRepositoryField.set(boatService, userRepository);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject userRepository into BoatService", e);
        }
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
    void save_WithValidBookingContext_ShouldSuccess() {
        // Arrange
        UserDetails mockUserDetails = mock(UserDetails.class);
        when(mockUserDetails.getUsername()).thenReturn("teste@teste.com");

        when(userRepository.findByEmail("teste@teste.com")).thenReturn(Optional.of(testBoatOwner));

        testBoat.setOwner(testBoatOwner);

        // Mock do BoatMapper para garantir que o boat tenha owner
        Boat boatWithOwner = new Boat();
        boatWithOwner.setId(1L);
        boatWithOwner.setName("Demeter");
        boatWithOwner.setType("Long Boat");
        boatWithOwner.setOwner(testBoatOwner);

        when(boatMapper.toEntity(testRequestDTO)).thenReturn(boatWithOwner);
        when(boatRepository.save(any(Boat.class))).thenReturn(boatWithOwner);
        when(boatMapper.toResponseDTO(any(Boat.class))).thenReturn(testResponseDTO);

        // Act
        BoatResponseDTO result = boatService.save(testRequestDTO, mockUserDetails);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testBoat.getId());

        verify(boatRepository).save(any(Boat.class));
        verify(boatMapper).toResponseDTO(any(Boat.class));
        verify(boatMapper).toEntity(testRequestDTO);
        verify(userRepository).findByEmail("teste@teste.com");
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
        when(boatMapper.toResponseDTO(testBoat)).thenReturn(testResponseDTO);

        // Act
        BoatResponseDTO result = boatService.findById(testBoat.getId());

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testBoat.getId());
        verify(boatRepository).findById(testBoat.getId());
        verify(boatMapper).toResponseDTO(testBoat);
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

        assertThat(exception.getMessage()).isEqualTo("Boat not found");
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
        when(boatMapper.toResponseDTO(testBoat)).thenReturn(testResponseDTO);

        // Act - First find to ensure boat exists
        BoatResponseDTO beforeDelete = boatService.findById(boatId);
        assertThat(beforeDelete).isNotNull();
        assertThat(beforeDelete.getId()).isEqualTo(boatId);

        // Delete the boat
        boatService.deleteById(boatId);
        verify(boatRepository).deleteById(boatId);

        // Now mock the repository to return empty for findById
        when(boatRepository.findById(boatId)).thenReturn(Optional.empty());

        // Act & Assert - Should Throw an exception when search after delete
        assertThrows(EntityNotFoundException.class,
                () -> boatService.findById(boatId));

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

        BoatResponseDTO dto1 = new BoatResponseDTO(
                1L,
                "Demeter",
                null,
                "Long Boat",
                80,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                testBoatOwner.getName(),
                testBoatOwner.getId());

        BoatResponseDTO dto2 = new BoatResponseDTO(
                2L,
                "Nautilus",
                null,
                "Submarine",
                12,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                testBoatOwner.getName(),
                testBoatOwner.getId());

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