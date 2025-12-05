package com.jompastech.backend.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for review creation and update requests.
 *
 * <p><b>Security Note:</b> User ID is intentionally omitted - it should be extracted
 * from the security context (JWT token) to prevent impersonation attacks.</p>
 *
 * <p><b>Validation strategy:</b> Uses JSR-303 annotations for basic data integrity
 * with additional business rule validation in the service layer.</p>
 */
@Data
public class ReviewRequestDTO {

    /**
     * Id of the user making the review
     * Required to associate the review with an existing user.
     */
    @NotNull(message = "User ID is required")
    private Long userID;

    /**
     * ID of the boat being reviewed.
     * Required to associate the review with the correct boat rental experience.
     * Must reference an existing boat that the authenticated user has rented.
     */
    @NotNull(message = "Boat ID is required")
    private Long boatId;

    /**
     * Rating score from 1 to 5 stars.
     * Represents overall satisfaction with the boat rental experience.
     */
    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;

    /**
     * Detailed feedback about the rental experience.
     * Provides valuable insights for future customers and boat owners.
     */
    @NotBlank(message = "Review comment cannot be empty")
    @Size(max = 1000, message = "Comment must not exceed 1000 characters")
    private String comment;
}