package com.jompastech.backend.model.dto;

import com.jompastech.backend.model.dto.basicDTO.BoatBasicDTO;
import com.jompastech.backend.model.dto.basicDTO.UserBasicDTO;
import com.jompastech.backend.model.enums.BookingStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for booking API responses.
 *
 * <p>Provides a complete view of booking data including nested user and boat information
 * for client consumption without exposing internal entity relationships.</p>
 *
 * <p><b>Design Rationale:</b> Uses basic DTOs for nested objects to prevent over-fetching
 * and maintain consistent data exposure patterns across the API.</p>
 */
@Data
public class BookingResponseDTO {

    /**
     * Unique identifier of the booking.
     */
    private Long id;

    /**
     * Basic user information associated with the booking.
     *
     * <p>Includes minimal user details to identify the booking owner
     * without exposing sensitive user information.</p>
     */
    private UserBasicDTO user;

    /**
     * Basic boat information for the booked boat.
     *
     * <p>Provides essential boat details needed for booking display
     * while excluding operational and maintenance data.</p>
     */
    private BoatBasicDTO boat;

    /**
     * Start date and time of the booking period.
     */
    private LocalDateTime startDate;

    /**
     * End date and time of the booking period.
     */
    private LocalDateTime endDate;

    /**
     * Current status of the booking in the lifecycle.
     */
    private BookingStatus status;

    /**
     * Total price for the booking period.
     *
     * <p>Stored with precision for financial calculations
     * and formatted for display purposes.</p>
     */
    private BigDecimal totalPrice;

    /**
     * All-args constructor for manual DTO creation and mapping.
     *
     * <p>Facilitates clean conversion from entity to DTO in mapper classes
     * while maintaining immutability where possible.</p>
     *
     * @param id the booking identifier
     * @param user basic user information
     * @param boat basic boat information
     * @param startDate booking start date
     * @param endDate booking end date
     * @param status current booking status
     * @param totalPrice calculated total price
     */
    public BookingResponseDTO(Long id, UserBasicDTO user, BoatBasicDTO boat,
                              LocalDateTime startDate, LocalDateTime endDate,
                              BookingStatus status, BigDecimal totalPrice) {
        this.id = id;
        this.user = user;
        this.boat = boat;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.totalPrice = totalPrice;
    }
}