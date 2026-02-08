package com.jompastech.backend.unit.service;

import com.jompastech.backend.mapper.MessageMapper;
import com.jompastech.backend.model.dto.MessageRequestDTO;
import com.jompastech.backend.model.dto.MessageResponseDTO;
import com.jompastech.backend.model.dto.basicDTO.UserBasicDTO;
import com.jompastech.backend.model.entity.*;
import com.jompastech.backend.repository.*;
import com.jompastech.backend.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MessageService business logic.
 *
 * <p>Tests focus on service layer responsibilities:
 * <ul>
 *   <li>Business rule validation and enforcement</li>
 *   <li>Repository coordination and transaction boundaries</li>
 *   <li>Authorization and permission logic</li>
 *   <li>DTO mapping delegation</li>
 * </ul>
 *
 * <p>Uses consistent test data initialization to ensure test independence
 * while minimizing code duplication through shared setup.
 */
@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BoatRepository boatRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MessageMapper messageMapper;

    @InjectMocks
    private MessageService messageService;

    private User testUser;
    private User testBoatOwner;
    private Boat testBoat;
    private Booking testBooking;
    private Message testMessage;
    private MessageRequestDTO testBookingRequestDTO;
    private MessageRequestDTO testBoatRequestDTO;
    private MessageResponseDTO testResponseDTO;

    /**
     * Initializes comprehensive test data before each test execution.
     *
     * <p>Creates consistent entity relationships and DTOs that represent
     * real-world scenarios for both booking and boat conversation contexts.
     */
    @BeforeEach
    void setUp() {
        // User entities - represents different roles in the system
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");
        testUser.setEmail("user@test.com");

        testBoatOwner = new User();
        testBoatOwner.setId(2L);
        testBoatOwner.setName("Boat Owner");
        testBoatOwner.setEmail("owner@test.com");

        // Boat entity - represents the resource being discussed
        testBoat = new Boat();
        testBoat.setId(1L);
        testBoat.setName("Test Boat");
        testBoat.setOwner(testBoatOwner);

        // Booking entity - represents confirmed rental context
        testBooking = mock(Booking.class);
        lenient().when(testBooking.getId()).thenReturn(1L);
        lenient().when(testBooking.getUser()).thenReturn(testUser);
        lenient().when(testBooking.getBoat()).thenReturn(testBoat);

        // Message entity - represents persisted communication
        testMessage = new Message();
        testMessage.setId(1L);
        testMessage.setUser(testUser);
        testMessage.setContent("Hi, I would like to rent your boat!");
        testMessage.setSentAt(LocalDateTime.now());
        testMessage.setBooking(testBooking);
        testMessage.setBoat(testBoat);

        // Request DTOs - represent API inputs for different contexts
        testBookingRequestDTO = new MessageRequestDTO(1L, null, "Booking-related message");
        testBoatRequestDTO = new MessageRequestDTO(null, 1L, "Boat inquiry message");

        // Response DTO - represents API output format
        UserBasicDTO userBasicDTO = new UserBasicDTO(
                testUser.getId(),
                testUser.getName(),
                testUser.getEmail()
        );
        testResponseDTO = new MessageResponseDTO(
                1L,
                "Hi, I would like to rent your boat!",
                testMessage.getSentAt(),
                userBasicDTO,
                1L, // bookingId
                1L  // boatId
        );
    }

    /**
     * Tests successful message creation in booking context with valid permissions.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>User authorization passes for booking participant</li>
     *   <li>Message is persisted with correct booking context</li>
     *   <li>Response DTO contains expected booking identifier</li>
     * </ul>
     */
    @Test
    void sendMessage_WithValidBookingContext_ShouldSuccess() {
        // Arrange
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(bookingRepository.findById(testBooking.getId())).thenReturn(Optional.of(testBooking));
        when(messageRepository.save(any(Message.class))).thenReturn(testMessage);
        when(messageMapper.toDTO(testMessage)).thenReturn(testResponseDTO);

        // Act
        MessageResponseDTO result = messageService.sendMessage(testBookingRequestDTO, testUser.getId());

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.bookingId()).isEqualTo(testBooking.getId());
        verify(messageRepository).save(any(Message.class));
        verify(messageMapper).toDTO(testMessage);
    }

    /**
     * Tests successful message creation in boat context by non-owner user.
     *
     * <p>Validates pre-booking conversation flow where potential customers
     * can inquire about boats before making reservations.
     */
    @Test
    void sendMessage_WithValidBoatContext_ShouldSuccess() {
        // Arrange
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(boatRepository.findById(testBoat.getId())).thenReturn(Optional.of(testBoat));
        when(messageRepository.save(any(Message.class))).thenReturn(testMessage);
        when(messageMapper.toDTO(testMessage)).thenReturn(testResponseDTO);

        // Act
        MessageResponseDTO result = messageService.sendMessage(testBoatRequestDTO, testUser.getId());

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.boatId()).isEqualTo(testBoat.getId());
        verify(messageRepository).save(any(Message.class));
    }

    /**
     * Tests business rule enforcement when boat owner tries to initiate pre-booking conversation.
     *
     * <p>Ensures that boat owners can only respond to inquiries but cannot start
     * pre-booking conversations about their own boats.
     */
    @Test
    void sendMessage_WhenBoatOwnerTriesToInitiatePreBooking_ShouldThrowException() {
        // Arrange
        when(userRepository.findById(testBoatOwner.getId())).thenReturn(Optional.of(testBoatOwner));
        when(boatRepository.findById(testBoat.getId())).thenReturn(Optional.of(testBoat));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> messageService.sendMessage(testBoatRequestDTO, testBoatOwner.getId())
        );

        assertThat(exception.getMessage()).contains("owner").contains("initiate");
        verify(messageRepository, never()).save(any(Message.class));
    }

    /**
     * Tests context validation when both booking and boat identifiers are provided.
     *
     * <p>Ensures API consumers must choose exactly one conversation context
     * to maintain clear conversation threading.
     */
    @Test
    void sendMessage_WithBothBookingAndBoat_ShouldThrowException() {
        // Arrange
        MessageRequestDTO invalidRequest = new MessageRequestDTO(1L, 1L, "Ambiguous message");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> messageService.sendMessage(invalidRequest, testUser.getId())
        );

        assertThat(exception.getMessage()).contains("both");
        verifyNoInteractions(userRepository, bookingRepository, boatRepository, messageRepository);
    }

    /**
     * Tests context validation when no conversation context is provided.
     *
     * <p>Ensures all messages are associated with either a booking or boat
     * to maintain organized conversation tracking.
     */
    @Test
    void sendMessage_WithNoContext_ShouldThrowException() {
        // Arrange
        MessageRequestDTO invalidRequest = new MessageRequestDTO(null, null, "Orphan message");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> messageService.sendMessage(invalidRequest, testUser.getId())
        );

        assertThat(exception.getMessage()).contains("either");
        verifyNoInteractions(userRepository, bookingRepository, boatRepository, messageRepository);
    }

    /**
     * Tests authorization enforcement for booking message access.
     *
     * <p>Ensures users can only access conversations where they are
     * legitimate participants (either as sailor or boat owner).
     */
    @Test
    void getMessagesByBooking_WhenUserNotParticipant_ShouldThrowException() {
        // Arrange
        User unauthorizedUser = new User();
        unauthorizedUser.setId(999L);

        when(bookingRepository.findById(testBooking.getId())).thenReturn(Optional.of(testBooking));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> messageService.getMessagesByBooking(testBooking.getId(), unauthorizedUser.getId())
        );

        assertThat(exception.getMessage()).contains("authorized");
        verify(messageRepository, never()).findByBookingId(any());
    }

    /**
     * Tests successful retrieval of boat conversation messages for authorized user.
     *
     * <p>Validates that both boat owners and potential customers can access
     * pre-booking conversation history.
     */
    @Test
    void getMessagesByBoat_WhenHasMessages_ShouldReturnMessages() {
        // Arrange
        when(boatRepository.findById(testBoat.getId())).thenReturn(Optional.of(testBoat));
        when(messageRepository.findByBoatId(testBoat.getId())).thenReturn(List.of(testMessage));
        when(messageMapper.toDTO(testMessage)).thenReturn(testResponseDTO);

        // Act
        List<MessageResponseDTO> result = messageService.getMessagesByBoat(testBoat.getId(), testUser.getId());

        // Assert
        assertThat(result).hasSize(1).containsExactly(testResponseDTO);
        verify(messageRepository).findByBoatId(testBoat.getId());
        verify(messageMapper).toDTO(testMessage);
    }

    /**
     * Tests empty result handling when no messages exist for boat.
     *
     * <p>Ensures graceful handling of empty conversation history
     * without errors or exceptions.
     */
    @Test
    void getMessagesByBoat_WhenNoMessages_ShouldReturnEmptyList() {
        // Arrange
        when(boatRepository.findById(testBoat.getId())).thenReturn(Optional.of(testBoat));
        when(messageRepository.findByBoatId(testBoat.getId())).thenReturn(List.of());

        // Act
        List<MessageResponseDTO> result = messageService.getMessagesByBoat(testBoat.getId(), testUser.getId());

        // Assert
        assertThat(result).isEmpty();
        verify(messageRepository).findByBoatId(testBoat.getId());
        verify(messageMapper, never()).toDTO(any());
    }

    /**
     * Tests booking message retrieval for authorized boat owner.
     *
     * <p>Validates that boat owners can access conversations related to their
     * boat bookings in addition to sailors.
     */
    @Test
    void getMessagesByBooking_WhenUserIsBoatOwner_ShouldReturnMessages() {
        // Arrange
        when(bookingRepository.findById(testBooking.getId())).thenReturn(Optional.of(testBooking));
        when(messageRepository.findByBookingId(testBooking.getId())).thenReturn(List.of(testMessage));
        when(messageMapper.toDTO(testMessage)).thenReturn(testResponseDTO);

        // Act - Boat owner accessing their boat's booking messages
        List<MessageResponseDTO> result = messageService.getMessagesByBooking(testBooking.getId(), testBoatOwner.getId());

        // Assert
        assertThat(result).hasSize(1).containsExactly(testResponseDTO);
        verify(messageRepository).findByBookingId(testBooking.getId());
    }

    /**
     * Tests error handling when accessing non-existent booking.
     *
     * <p>Ensures consistent error messaging to prevent enumeration attacks
     * while providing clear feedback for legitimate API consumers.
     */
    @Test
    void getMessagesByBooking_WhenBookingNotFound_ShouldThrowException() {
        // Arrange
        Long nonExistentBookingId = 999L;
        when(bookingRepository.findById(nonExistentBookingId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> messageService.getMessagesByBooking(nonExistentBookingId, testUser.getId()));

        verify(messageRepository, never()).findByBookingId(any());
    }
}