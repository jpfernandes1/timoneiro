package com.jompastech.backend.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Entity that represents a message in Timoneiro's chat system.
 *
 * A message can be associated with a booking or be a pre-booking conversation
 * between a sailor and a boat owner. The booking field is optional
 * to allow initial conversations before booking confirmation.
 *
 * MVP Note: This simplified approach meets MVP requirements. Future evolutions
 * may consider a separate Conversation entity for better organization.
 */
@Entity
@Data
@Table(name = "messages")
public class Message {

    /**
     * Unique identifier for the message.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long id;

    /**
     * Booking associated with the message. Can be null for pre-booking conversations.
     *
     * When null, it indicates the conversation is happening before booking confirmation
     * between the sailor and the boat owner.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    /**
     * User who sent the message.
     *
     * Can be either the sailor or the boat owner. Role differentiation
     * is handled in the access and security context.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Text content of the message.
     *
     * Should be validated for maximum length and malicious content
     * at the service/controller layer.
     */
    @Column(nullable = false, length = 2000)
    private String content;

    /**
     * Timestamp of when the message was sent.
     *
     * Automatically set by the system when the entity is created.
     */
    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    /**
     * JPA callback method to automatically set the timestamp
     * before entity persistence.
     */
    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
    }
}