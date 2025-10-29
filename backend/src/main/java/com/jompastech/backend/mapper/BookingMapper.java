package com.jompastech.backend.mapper;

import com.jompastech.backend.model.dto.BookingRequestDTO;
import com.jompastech.backend.model.dto.BookingResponseDTO;
import com.jompastech.backend.model.dto.basicDTO.BoatBasicDTO;
import com.jompastech.backend.model.dto.basicDTO.UserBasicDTO;
import com.jompastech.backend.model.entity.Booking;
import com.jompastech.backend.model.entity.Boat;
import com.jompastech.backend.model.entity.User;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between Booking entities and DTOs.
 *
 * <p>Handles the transformation of booking data between different representation layers,
 * ensuring proper separation of concerns and maintaining data integrity.</p>
 *
 * <p><b>Design Note:</b> This mapper intentionally delegates price calculation to the service layer
 * to maintain business logic cohesion and avoid scattering pricing rules across multiple classes.</p>
 */
@Component
public class BookingMapper {

    /**
     * Converts BookingRequestDTO and related entities to Booking entity.
     *
     * <p><b>Important:</b> The totalPrice is intentionally set to null because price calculation
     * involves complex business rules that belong in the service layer. This design prevents
     * business logic leakage into the mapper and ensures pricing consistency across the application.</p>
     *
     * @param requestDTO the booking request data transfer object
     * @param user the user entity associated with the booking
     * @param boat the boat entity being booked
     * @return Booking entity with basic data (price calculation pending service layer processing)
     * @throws IllegalArgumentException if required parameters are null or invalid
     */
    public Booking toEntity(BookingRequestDTO requestDTO, User user, Boat boat) {
        // Delegates entity creation to Booking's constructor which contains domain validation
        // Pricing service will calculate and set totalPrice separately to maintain separation of concerns
        return new Booking(
                user,
                boat,
                requestDTO.getStartDate(),
                requestDTO.getEndDate(),
                null // totalPrice calculated by PricingService to centralize business logic
        );
    }

    /**
     * Converts Booking entity to BookingResponseDTO for API responses.
     *
     * <p>Includes nested UserBasicDTO and BoatBasicDTO to provide complete booking context
     * without exposing full entity details, following the principle of minimal data exposure.</p>
     *
     * @param booking the booking entity to convert
     * @return BookingResponseDTO with complete booking information including nested user and boat data
     */
    public BookingResponseDTO toResponseDTO(Booking booking) {
        if (booking == null) {
            return null;
        }

        return new BookingResponseDTO(
                booking.getId(),
                toUserBasicDTO(booking.getUser()),
                toBoatBasicDTO(booking.getBoat()),
                booking.getStartDate(),
                booking.getEndDate(),
                booking.getStatus(),
                booking.getTotalPrice()
        );
    }

    /**
     * Converts User entity to UserBasicDTO for nested representation.
     *
     * <p>Uses basic DTO pattern to expose only essential user information,
     * protecting sensitive data and reducing payload size.</p>
     *
     * @param user the user entity to convert
     * @return UserBasicDTO with minimal user information, or null if input is null
     */
    private UserBasicDTO toUserBasicDTO(User user) {
        if (user == null) {
            return null;
        }
        return new UserBasicDTO(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }

    /**
     * Converts Boat entity to BoatBasicDTO for nested representation.
     *
     * <p>Provides essential boat information while excluding complex relationships
     * and operational data not required for booking context.</p>
     *
     * @param boat the boat entity to convert
     * @return BoatBasicDTO with essential boat information, or null if input is null
     */
    private BoatBasicDTO toBoatBasicDTO(Boat boat) {
        if (boat == null) {
            return null;
        }
        return new BoatBasicDTO(
                boat.getId(),
                boat.getName(),
                boat.getType(),
                boat.getAddress()
        );
    }
}