package com.jompastech.backend.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for sending a new message in the chat system.
 *
 * Used for both pre-booking and post-booking conversations.
 * For pre-booking: boatId must be provided, bookingId is null.
 * For post-booking: bookingId must be provided, boatId is null.
 */
public record MessageRequestDTO(

        /**
         * Booking ID for post-booking conversations.
         * When provided, associates the message with an existing booking.
         * Must be null for pre-booking conversations.
         */
        Long bookingId,

        /**
         * Boat ID for pre-booking conversations.
         * When provided, associates the message with a specific boat.
         * Must be null for post-booking conversations.
         */
        Long boatId,

        /**
         * Message content. Must not be empty and has a reasonable size limit for chat messages.
         */
        @NotBlank(message = "Message content cannot be empty")
        @Size(max = 2000, message = "Message cannot exceed 2000 characters")
        String content
) {}