package com.jompastech.backend.model.dto;

import com.jompastech.backend.model.dto.basicDTO.UserBasicDTO;

import java.time.LocalDateTime;

/**
 * DTO for message responses in the chat system.
 *
 * Provides complete message information for frontend display including
 * sender details, timestamps, and contextual associations. Designed
 * to support real-time chat interfaces with minimal additional data
 * processing required on the client side.
 */
public record MessageResponseDTO(

        /**
         * Unique message identifier for client-side message management.
         *
         * Used for message tracking, updates, and deletion operations
         * in the chat interface.
         */
        Long id,

        /**
         * The actual message content as sent by the user.
         *
         * Displayed directly in the chat interface with proper
         * formatting and sanitization applied.
         */
        String content,

        /**
         * Timestamp indicating when the message was sent.
         *
         * Used for chronological ordering and display of message
         * timing in the chat interface. Automatically set by the system.
         */
        LocalDateTime sentAt,

        /**
         * Basic information about the user who sent the message.
         *
         * Provides sender identification without exposing sensitive
         * user data. Includes name and identifier for display purposes.
         */
        UserBasicDTO sender,

        /**
         * Optional booking association for contextual reference.
         *
         * When present, indicates the message is part of a
         * post-booking conversation. When null, indicates a
         * pre-booking inquiry about an advertisement.
         */
        Long bookingId
) {}