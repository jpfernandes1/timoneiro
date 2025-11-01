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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MessageService.
 *
 * Tests business logic and validation rules in isolation using mocks.
 *
 * Design Note: Tests respect the entity design patterns with protected constructors
 * and immutable properties where applicable.
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

    @Test
    void sendMessage_WithValidBookingContext_ShouldSuccess() {
        // Arrange
        Long userId = 1L;
        Long bookingId = 1L;
        MessageRequestDTO request = new MessageRequestDTO(bookingId, null, "Hello");

        User user = new User();
        user.setId(userId);

        User boatOwner = new User();
        boatOwner.setId(2L);

        Boat boat = new Boat();
        boat.setOwner(boatOwner);

        // Use reflection or test-specific factory method to create Booking
        // Since Booking has protected constructor, we'll mock the repository response
        Booking booking = mock(Booking.class);
        when(booking.getUser()).thenReturn(user);
        when(booking.getBoat()).thenReturn(boat);
        when(booking.getId()).thenReturn(bookingId);

        Message savedMessage = new Message();
        MessageResponseDTO responseDTO = new MessageResponseDTO(1L, "Hello", null, null, bookingId, null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);
        when(messageMapper.toDTO(savedMessage)).thenReturn(responseDTO);

        // Act
        MessageResponseDTO result = messageService.sendMessage(request, userId);

        // Assert
        assertNotNull(result);
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void sendMessage_WithValidBoatContext_ShouldSuccess() {
        // Arrange
        Long userId = 1L;
        Long boatId = 1L;
        MessageRequestDTO request = new MessageRequestDTO(null, boatId, "Hello");

        User user = new User();
        user.setId(userId);

        Boat boat = new Boat();
        User owner = new User();
        owner.setId(2L); // Different from userId
        boat.setOwner(owner);

        Message savedMessage = new Message();
        MessageResponseDTO responseDTO = new MessageResponseDTO(1L, "Hello", null, null, null, boatId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(boatRepository.findById(boatId)).thenReturn(Optional.of(boat));
        when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);
        when(messageMapper.toDTO(savedMessage)).thenReturn(responseDTO);

        // Act
        MessageResponseDTO result = messageService.sendMessage(request, userId);

        // Assert
        assertNotNull(result);
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void sendMessage_WithBothBookingAndBoat_ShouldThrowException() {
        // Arrange
        Long userId = 1L;
        MessageRequestDTO request = new MessageRequestDTO(1L, 1L, "Hello");

        User user = new User();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                messageService.sendMessage(request, userId));
    }

    @Test
    void sendMessage_WithNoContext_ShouldThrowException() {
        // Arrange
        Long userId = 1L;
        MessageRequestDTO request = new MessageRequestDTO(null, null, "Hello");

        User user = new User();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                messageService.sendMessage(request, userId));
    }

    @Test
    void sendMessage_WhenBoatOwnerTriesToInitiatePreBooking_ShouldThrowException() {
        // Arrange
        Long ownerId = 1L;
        Long boatId = 1L;
        MessageRequestDTO request = new MessageRequestDTO(null, boatId, "Hello");

        User owner = new User();
        owner.setId(ownerId);

        Boat boat = new Boat();
        boat.setOwner(owner);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(boatRepository.findById(boatId)).thenReturn(Optional.of(boat));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                messageService.sendMessage(request, ownerId));
    }

    @Test
    void getMessagesByBooking_WhenUserNotParticipant_ShouldThrowException() {
        // Arrange
        Long userId = 1L;
        Long bookingId = 1L;

        User differentUser = new User();
        differentUser.setId(999L); // Different user

        User boatOwner = new User();
        boatOwner.setId(888L); // Different owner

        Boat boat = new Boat();
        boat.setOwner(boatOwner);

        Booking booking = mock(Booking.class);
        when(booking.getUser()).thenReturn(differentUser);
        when(booking.getBoat()).thenReturn(boat);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                messageService.getMessagesByBooking(bookingId, userId));
    }
}