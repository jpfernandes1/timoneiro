package com.jompastech.backend.model.dto;

import com.jompastech.backend.model.dto.basicDTO.BoatBasicDTO;
import com.jompastech.backend.model.dto.basicDTO.UserBasicDTO;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO for review API responses.
 *
 * <p>Provides a complete view of review data including user and boat information
 * for client consumption without exposing internal entity relationships.</p>
 *
 */

@Data
public class ReviewResponseDTO {

    /**
     * Unique identifier of the review
     */
    private Long id;

    /**
     * Basic user information associated with the review
     * Includes minimal user details to identify the reviewer
     * without exposing sensitive information like email or phone
     */
    private UserBasicDTO user;

    /**
     * Basic boat information for the reviewed boat
     * Provides essential boat context for the review
     * while excluding owner details and sensitive pricing information
     */
    private BoatBasicDTO boat;

    /**
     * Rating score from 1 to 5 stars
     * Represents user satisfaction level
     */
    private int rating;

    /**
     * Personalized comment describing the rental experience
     * May contain constructive feedback or recommendations
     */
    private String comment;

    /**
     * Timestamp when the review was originally created
     * Useful for sorting and displaying review chronology
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the review was last updated
     * Null if review has never been modified after creation
     * Important for tracking review modifications
     */
    private LocalDateTime updatedAt;

    /**
     * All-args constructor for manual DTO creation and mapping
     * @param id unique review identifier
     * @param user basic reviewer information
     * @param boat basic boat information
     * @param rating numerical rating from 1-5
     * @param comment detailed review text
     * @param createdAt review creation timestamp
     * @param updatedAt review last modification timestamp
     */
    public ReviewResponseDTO(Long id, UserBasicDTO user, BoatBasicDTO boat,
                             int rating, String comment, LocalDateTime createdAt,
                             LocalDateTime updatedAt) {
        this.id = id;
        this.user = user;
        this.boat = boat;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}