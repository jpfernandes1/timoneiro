package com.jompastech.backend.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for sending new messages in the chat system.
 *
 * Supports both pre-booking inquiries and post-booking communications
 * through flexible association with either booking or advertisement.
 * This design allows seamless conversation flow from initial inquiry
 * through confirmed booking without changing communication channels.
 */
public record MessageRequestDTO(

        /**
         * Booking identifier for messages related to existing bookings.
         *
         * When provided, associates the message with a specific confirmed booking
         * for post-booking communication between sailor and boat owner.
         * Must be null for pre-booking conversations to avoid data inconsistency.
         */
        Long bookingId,

        /**
         * Advertisement identifier for pre-booking inquiries.
         *
         * Used when users are inquiring about a boat before making a reservation.
         * Allows boat owners to respond to potential customers and discuss
         * availability, pricing, and boat details before booking creation.
         * Must be null for post-booking conversations.
         */
        Long adId,

        /**
         * Text content of the message with validation constraints.
         *
         * Enforced length limit prevents database overflow and maintains
         * reasonable message size for chat interface display. Non-blank
         * validation ensures meaningful communication.
         */
        @NotBlank(message = "Message content cannot be empty")
        @Size(max = 2000, message = "Message cannot exceed 2000 characters")
        String content
) {}