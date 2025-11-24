package com.jompastech.backend.service;

import com.jompastech.backend.mapper.MessageMapper;
import com.jompastech.backend.mapper.UserMapper;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service layer for message business operations.
 *
 * Orchestrates message operations and enforces business rules including
 * permission validation and conversation context management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MessageService {

    private final MessageRepository messageRepository;
    private final BookingRepository bookingRepository;
    private final BoatRepository boatRepository;
    private final UserRepository userRepository;
    private final MessageMapper messageMapper;

    /**
     * Sends a new message after validating business rules and permissions.
     *
     * Validates that only one context (booking or boat) is provided and that
     * the authenticated user has permission to send messages in that context.
     *
     * @param request the message data transfer object
     * @param authenticatedUserId the ID of the currently authenticated user
     * @return the created message as DTO
     * @throws IllegalArgumentException if business rules are violated
     */
    public MessageResponseDTO sendMessage(MessageRequestDTO request, Long authenticatedUserId) {
        log.info("Sending message for user: {}, context: booking={}, boat={}",
                authenticatedUserId, request.bookingId(), request.boatId());

        validateMessageContext(request);

        User user = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        validateUserPermission(request, authenticatedUserId);

        Message message = new Message();
        message.setUser(user);
        message.setContent(request.content());

        if (request.bookingId() != null) {
            var booking = bookingRepository.findById(request.bookingId())
                    .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
            message.setBooking(booking);
        } else {
            var boat = boatRepository.findById(request.boatId())
                    .orElseThrow(() -> new IllegalArgumentException("Boat not found"));
            message.setBoat(boat);
        }

        Message savedMessage = messageRepository.save(message);
        log.debug("Message sent successfully with ID: {}", savedMessage.getId());

        return messageMapper.toDTO(savedMessage);
    }

    /**
     * Retrieves messages for a specific booking with permission validation.
     *
     * Ensures the authenticated user is either the sailor or boat owner
     * before returning the message history.
     *
     * @param bookingId the booking identifier
     * @param authenticatedUserId the user requesting the messages
     * @return list of messages for the booking
     */
    @Transactional(readOnly = true)
    public List<MessageResponseDTO> getMessagesByBooking(Long bookingId, Long authenticatedUserId) {
        log.debug("Fetching messages for booking: {}, user: {}", bookingId, authenticatedUserId);

        var booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (!isUserParticipantInBooking(booking, authenticatedUserId)) {
            throw new IllegalArgumentException("User not authorized to view these messages");
        }

        var messages = messageRepository.findByBookingId(bookingId);
        return messages.stream()
                .map(messageMapper::toDTO)
                .toList();
    }

    /**
     * Retrieves messages for a specific boat with permission validation.
     *
     * Used for pre-booking conversations. Ensures the authenticated user
     * has permission to view messages for this boat.
     *
     * @param boatId the boat identifier
     * @param authenticatedUserId the user requesting the messages
     * @return list of messages for the boat
     */
    @Transactional(readOnly = true)
    public List<MessageResponseDTO> getMessagesByBoat(Long boatId, Long authenticatedUserId) {
        log.debug("Fetching messages for boat: {}, user: {}", boatId, authenticatedUserId);

        var boat = boatRepository.findById(boatId)
                .orElseThrow(() -> new IllegalArgumentException("Boat not found"));

        if (!isUserParticipantInBoatConversation(boat, authenticatedUserId)) {
            throw new IllegalArgumentException("User not authorized to view these messages");
        }

        var messages = messageRepository.findByBoatId(boatId);
        return messages.stream()
                .map(messageMapper::toDTO)
                .toList();
    }

    /**
     * Validates that only one context (bookingId OR boatId) is provided.
     *
     * This business rule ensures clear conversation context and simplifies
     * permission validation logic throughout the system.
     */
    private void validateMessageContext(MessageRequestDTO request) {
        boolean hasBooking = request.bookingId() != null;
        boolean hasBoat = request.boatId() != null;

        if (hasBooking && hasBoat) {
            throw new IllegalArgumentException("Cannot specify both bookingId and boatId");
        }

        if (!hasBooking && !hasBoat) {
            throw new IllegalArgumentException("Must specify either bookingId or boatId");
        }
    }

    /**
     * Validates user has permission to send message in the specified context.
     *
     * For booking context: user must be participant in the booking.
     * For boat context: user cannot be the boat owner (owners can only respond).
     */
    private void validateUserPermission(MessageRequestDTO request, Long userId) {
        if (request.bookingId() != null) {
            var booking = bookingRepository.findById(request.bookingId())
                    .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
            if (!isUserParticipantInBooking(booking, userId)) {
                throw new IllegalArgumentException("User not authorized for this booking");
            }
        } else {
            var boat = boatRepository.findById(request.boatId())
                    .orElseThrow(() -> new IllegalArgumentException("Boat not found"));
            if (boat.getOwner().getId().equals(userId)) {
                throw new IllegalArgumentException("Boat owners cannot initiate pre-booking conversations");
            }
        }
    }

    /**
     * Checks if user is participant in booking (either sailor or boat owner).
     *
     * Uses booking.getUser() for sailor and booking.getBoat().getOwner() for boat owner.
     */
    private boolean isUserParticipantInBooking(Booking booking, Long userId) {
        return booking.getUser().getId().equals(userId) ||
                booking.getBoat().getOwner().getId().equals(userId);
    }

    /**
     * Checks if user can participate in boat conversation.
     *
     * For pre-booking conversations, any authenticated user can view messages
     * about a boat as long as they are either the boat owner or a potential sailor.
     * In MVP, we allow any authenticated user to view boat messages for simplicity.
     */
    private boolean isUserParticipantInBoatConversation(Boat boat, Long userId) {
        return boat.getOwner().getId().equals(userId) ||
                !boat.getOwner().getId().equals(userId); // Any non-owner can view
    }
}