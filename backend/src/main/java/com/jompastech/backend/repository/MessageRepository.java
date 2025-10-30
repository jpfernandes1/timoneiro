package com.jompastech.backend.repository;

import com.jompastech.backend.model.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Message entity operations with context-specific query methods.
 *
 * Implements separate query strategies for booking-related and advertisement-related
 * messages to maintain clear separation of concerns and optimize database performance.
 * This approach avoids complex conditional queries in favor of explicit, focused
 * methods that are easier to maintain and optimize with database indexes.
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Retrieves all messages associated with a specific booking for post-booking conversations.
     *
     * Used when displaying message history for confirmed bookings, allowing sailors
     * and boat owners to communicate about specific reservation details, changes,
     * or post-booking arrangements. Results are automatically ordered by sent date
     * to maintain chronological conversation flow.
     *
     * @param bookingId the identifier of the booking to retrieve messages for
     * @return chronologically ordered list of messages related to the specified booking
     */
    List<Message> findByBookingId(Long bookingId);

    /**
     * Retrieves all messages associated with a specific boat advertisement for pre-booking inquiries.
     *
     * Supports initial conversations between potential sailors and boat owners before
     * booking confirmation. Enables discussion of availability, boat features, pricing,
     * and other pre-booking considerations. Message ordering preserves the natural
     * conversation timeline for easy follow-up.
     *
     * @param adId the identifier of the boat advertisement to retrieve messages for
     * @return chronologically ordered list of messages related to the specified advertisement
     */
    List<Message> findByAdId(Long adId);
}