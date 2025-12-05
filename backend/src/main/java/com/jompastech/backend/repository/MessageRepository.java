package com.jompastech.backend.repository;

import com.jompastech.backend.model.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Message entity operations.
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Finds all messages associated with a specific booking.
     *
     * Used for post-booking conversations where messages are related
     * to an existing boat reservation.
     *
     * @param bookingId the ID of the booking to search messages for
     * @return list of messages associated with the booking
     */
    List<Message> findByBookingId(Long bookingId);

    /**
     * Finds all messages associated with a specific boat.
     *
     * Used for pre-booking conversations where users are discussing
     * potential reservations before booking confirmation.
     *
     * @param boatId the ID of the boat to search messages for
     * @return list of messages associated with the boat
     */
    List<Message> findByBoatId(Long boatId);
}