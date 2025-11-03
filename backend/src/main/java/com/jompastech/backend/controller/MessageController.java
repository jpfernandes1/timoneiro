package com.jompastech.backend.controller;

import com.jompastech.backend.model.dto.MessageRequestDTO;
import com.jompastech.backend.model.dto.MessageResponseDTO;
import com.jompastech.backend.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for message operations.
 *
 * Exposes endpoints for sending and retrieving messages in both
 * booking and boat conversation contexts.
 *
 * Uses JWT authentication via Spring Security context.
 */
@Slf4j
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /**
     * Sends a new message in either booking or boat conversation context.
     *
     * For pre-booking conversations: provide boatId, leave bookingId null
     * For post-booking conversations: provide bookingId, leave boatId null
     *
     * @param request the message request DTO
     * @param userId the authenticated user ID from JWT token
     * @return the created message with HTTP 200 status
     */
    @PostMapping
    public ResponseEntity<MessageResponseDTO> sendMessage(
            @Valid @RequestBody MessageRequestDTO request,
            @AuthenticationPrincipal Long userId) {

        log.info("Received message send request from user: {}", userId);
        MessageResponseDTO response = messageService.sendMessage(request, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all messages for a specific booking.
     *
     * Requires that the authenticated user is either the sailor
     * or the boat owner associated with the booking.
     *
     * @param bookingId the booking identifier
     * @param userId the authenticated user ID from JWT token
     * @return list of messages with HTTP 200 status
     */
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<List<MessageResponseDTO>> getMessagesByBooking(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal Long userId) {

        log.debug("Fetching messages for booking: {}, user: {}", bookingId, userId);
        List<MessageResponseDTO> messages = messageService.getMessagesByBooking(bookingId, userId);
        return ResponseEntity.ok(messages);
    }

    /**
     * Retrieves all messages for a specific boat (pre-booking conversations).
     *
     * Used for conversations that happen before a booking is confirmed.
     * Any authenticated user can access these messages in the MVP.
     *
     * @param boatId the boat identifier
     * @param userId the authenticated user ID from JWT token
     * @return list of messages with HTTP 200 status
     */
    @GetMapping("/boat/{boatId}")
    public ResponseEntity<List<MessageResponseDTO>> getMessagesByBoat(
            @PathVariable Long boatId,
            @AuthenticationPrincipal Long userId) {

        log.debug("Fetching messages for boat: {}, user: {}", boatId, userId);
        List<MessageResponseDTO> messages = messageService.getMessagesByBoat(boatId, userId);
        return ResponseEntity.ok(messages);
    }
}