package com.jompastech.backend.service;

import com.jompastech.backend.mapper.MessageMapper;
import com.jompastech.backend.model.dto.MessageRequestDTO;
import com.jompastech.backend.model.dto.MessageResponseDTO;
import com.jompastech.backend.model.entity.Booking;
import com.jompastech.backend.model.entity.Message;
import com.jompastech.backend.model.entity.Boat;
import com.jompastech.backend.model.entity.User;
import com.jompastech.backend.repository.MessageRepository;
import com.jompastech.backend.repository.BookingRepository;
import com.jompastech.backend.repository.BoatRepository;
import com.jompastech.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MessageService.
 *
 * Design Decisions:
 * - Uses consistent mocking strategy to respect entity immutability patterns
 * - Focuses on testing service behavior rather than entity construction
 * - Validates business rules and authorization logic without coupling to entity internals
 * - Each test verifies one specific behavior or validation rule
 *
 * Trade-offs Accepted:
 * - Mocking may not catch all persistence issues, but provides fast, isolated unit tests
 * - Some domain logic testing is delegated to integration tests
 * - Test clarity is prioritized over testing implementation details
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

    /**
     * Tests successful message creation in booking context.
     *
     * Key Assertions:
     * - Message is saved when all validations pass
     * - User authorization is verified
     * - Booking context is properly handled
     *
     * Design Note: Uses consistent mocking to avoid coupling with entity construction.
     * This respects the immutable entity design while testing service logic.
     */
    @Test
    void sendMessage_WithValidBookingContext_ShouldSuccess() {
        // Arrange
        Long userId = 1L;
        Long bookingId = 1L;
        MessageRequestDTO request = new MessageRequestDTO(bookingId, null, "Test message");

        // Mock entity interactions without violating immutability
        User user = mock(User.class);
        User boatOwner = mock(User.class);
        Boat boat = mock(Boat.class);
        Booking booking = mock(Booking.class);
        Message savedMessage = mock(Message.class);
        MessageResponseDTO responseDTO = new MessageResponseDTO(1L, "Test message", null, null, bookingId, null);

        // Configure mock behaviors to simulate valid business scenario
        when(user.getId()).thenReturn(userId);
        when(booking.getUser()).thenReturn(user);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);
        when(messageMapper.toDTO(savedMessage)).thenReturn(responseDTO);

        // Act
        MessageResponseDTO result = messageService.sendMessage(request, userId);

        // Assert
        assertNotNull(result, "Should return response DTO");
        assertEquals(bookingId, result.bookingId(), "Should maintain booking context");
        verify(messageRepository, times(1)).save(any(Message.class));
        verify(bookingRepository, times(2)).findById(1L);
    }

    /**
     * Tests successful message creation in boat context (pre-booking inquiry).
     *
     * Key Assertions:
     * - Non-owner can initiate pre-booking conversation
     * - Boat context is properly handled
     * - Owner cannot message their own boat (tested separately)
     */
    @Test
    void sendMessage_WithValidBoatContext_ShouldSuccess() {
        // Arrange
        Long userId = 1L;
        Long boatId = 1L;
        MessageRequestDTO request = new MessageRequestDTO(null, boatId, "Inquiry about boat");

        User user = mock(User.class);
        Boat boat = mock(Boat.class);
        User owner = mock(User.class);
        Message savedMessage = mock(Message.class);
        MessageResponseDTO responseDTO = new MessageResponseDTO(1L, "Inquiry about boat", null, null, null, boatId);

        when(owner.getId()).thenReturn(2L); // Different from userId
        when(boat.getOwner()).thenReturn(owner);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(boatRepository.findById(boatId)).thenReturn(Optional.of(boat));
        when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);
        when(messageMapper.toDTO(savedMessage)).thenReturn(responseDTO);

        // Act
        MessageResponseDTO result = messageService.sendMessage(request, userId);

        // Assert
        assertNotNull(result);
        assertEquals(boatId, result.boatId(), "Should maintain boat context");
        verify(messageRepository).save(any(Message.class));
        // Verify boat owner validation logic was executed
        verify(boatRepository, times(2)).findById(1L);
    }

    /**
     * Tests validation when both booking and boat contexts are provided.
     *
     * Key Assertions:
     * - Service rejects ambiguous context requests
     * - Clear exception message for API consumers
     * - No persistence operations occur
     */
    @Test
    void sendMessage_WithBothBookingAndBoat_ShouldThrowException() {
        // Arrange
        Long userId = 1L;
        MessageRequestDTO request = new MessageRequestDTO(1L, 1L, "Ambiguous message");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> messageService.sendMessage(request, userId)
        );

        assertTrue(exception.getMessage().contains("booking") || exception.getMessage().contains("boat"),
                "Exception should mention context ambiguity");

        // Verify no interactions with persistence layer
        verify(messageRepository, never()).save(any());
        verify(bookingRepository, never()).findById(any());
        verify(boatRepository, never()).findById(any());
        verify(userRepository, never()).findById(any());
    }

    /**
     * Tests validation when no context is provided.
     *
     * Key Assertions:
     * - Service requires either booking or boat context
     * - Clear error messaging for API consumers
     * - Early validation prevents unnecessary processing
     */
    @Test
    void sendMessage_WithNoContext_ShouldThrowException() {
        // Arrange
        Long userId = 1L;
        MessageRequestDTO request = new MessageRequestDTO(null, null, "Orphan message");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> messageService.sendMessage(request, userId)
        );

        assertNotNull(exception.getMessage(), "Should provide meaningful error message");

        // Verify user was validated but no further processing occurred
        verify(userRepository, never()).findById(any());
        verify(bookingRepository, never()).findById(any());
        verify(boatRepository, never()).findById(any());
        verify(messageRepository, never()).save(any());
    }

    /**
     * Tests business rule: boat owners cannot initiate pre-booking conversations.
     *
     * Key Assertions:
     * - Authorization rule is properly enforced
     * - Clear exception indicates authorization failure
     * - Prevents spam and maintains conversation flow integrity
     */
    @Test
    void sendMessage_WhenBoatOwnerTriesToInitiatePreBooking_ShouldThrowException() {
        // Arrange
        Long ownerId = 1L;
        Long boatId = 1L;
        MessageRequestDTO request = new MessageRequestDTO(null, boatId, "Owner message");

        User owner = mock(User.class);
        Boat boat = mock(Boat.class);

        when(owner.getId()).thenReturn(ownerId);
        when(boat.getOwner()).thenReturn(owner);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(boatRepository.findById(boatId)).thenReturn(Optional.of(boat));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> messageService.sendMessage(request, ownerId)
        );

        assertTrue(exception.getMessage().contains("owner") || exception.getMessage().contains("initiate"),
                "Exception should indicate owner restriction");

        verify(messageRepository, never()).save(any(Message.class));
    }

    /**
     * Tests authorization: users can only access conversations they participate in.
     *
     * Key Assertions:
     * - Security boundary is properly enforced
     * - Users cannot access other users' conversations
     * - Clear exception indicates authorization failure
     *
     * Scalability Note: This validation prevents data leakage in multi-tenant scenarios.
     */
    @Test
    void getMessagesByBooking_WhenUserNotParticipant_ShouldThrowException() {
        // Arrange
        Long userId = 1L;
        Long bookingId = 1L;

        User differentUser = mock(User.class);
        User boatOwner = mock(User.class);
        Boat boat = mock(Boat.class);
        Booking booking = mock(Booking.class);

        when(differentUser.getId()).thenReturn(999L); // Different user
        when(boatOwner.getId()).thenReturn(888L); // Different owner
        when(boat.getOwner()).thenReturn(boatOwner);
        when(booking.getUser()).thenReturn(differentUser);
        when(booking.getBoat()).thenReturn(boat);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> messageService.getMessagesByBooking(bookingId, userId)
        );

        assertTrue(exception.getMessage().contains("not authorized") ||
                        exception.getMessage().contains("authorized"),
                "Exception should indicate authorization failure. Actual message: " + exception.getMessage());

        // Verify no message retrieval occurred
        verify(messageRepository, never()).findByBookingId(any());
    }

    /**
     * Tests edge case: user tries to access non-existent booking.
     *
     * Security Implication: Returns same error as unauthorized access to prevent enumeration attacks.
     */
    @Test
    void getMessagesByBooking_WhenBookingNotFound_ShouldThrowException() {
        // Arrange
        Long userId = 1L;
        Long bookingId = 999L;

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> messageService.getMessagesByBooking(bookingId, userId));

        verify(messageRepository, never()).findByBookingId(any());
    }
}