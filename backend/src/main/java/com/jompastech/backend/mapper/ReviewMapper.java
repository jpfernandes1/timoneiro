package com.jompastech.backend.mapper;


import com.jompastech.backend.model.dto.ReviewRequestDTO;
import com.jompastech.backend.model.dto.ReviewResponseDTO;
import com.jompastech.backend.model.dto.basicDTO.BoatBasicDTO;
import com.jompastech.backend.model.dto.basicDTO.UserBasicDTO;
import com.jompastech.backend.model.entity.Boat;
import com.jompastech.backend.model.entity.Review;
import com.jompastech.backend.model.entity.User;
import org.springframework.stereotype.Component;

/**
 * Mapper class for converting between Review entities and DTOs.
 *
 * <p><b>Design Decisions:</b>
 * <ul>
 *   <li>Manual mapping over reflection-based frameworks for explicit control and performance</li>
 *   <li>Separation of concerns - pure conversion logic without business rules</li>
 *   <li>Null-safe operations to prevent runtime exceptions</li>
 *   <li>Stateless and thread-safe for Spring singleton scope</li>
 * </ul>
 *
 * <p><b>Usage Note:</b> Entity to DTO conversion requires pre-fetched associations
 * to avoid LazyInitializationException. So we'll be using JOIN FETCH in queries.</p>
 */

@Component
public class ReviewMapper {

    /**
     * Converts ReviewRequestDTO to Review entity.
     * Note: User and Boat entities must be fetched separately and set on the entity.
     *
     * @param requestDTO the review creation request data
     * @return new Review entity with basic fields populated
     * @throws IllegalArgumentException if dto is null
     */

    public Review toEntity(ReviewRequestDTO requestDTO){

        if (requestDTO == null){
            throw new IllegalArgumentException("ReviewRequestDTO cannot be null");
        }

        Review review = new Review();
        review.setRating((requestDTO.getRating()));
        review.setComment(requestDTO.getComment());
        // Note: user and boat must be set separately using fetched entities
        // This separation ensures that we don't create invalid associations

        return review;
    }

    /**
     * Converts Review entity to ReviewResponseDTO with nested BasicDTOs.
     * Handles null checks for associations to prevent NullPointerException.
     *
     * @param review the review entity to convert
     * @return fully populated ReviewResponseDTO
     * @throws IllegalArgumentException if review is null
     */
    public ReviewResponseDTO toResponseDTO(Review review){
        if(review == null){
            throw new IllegalArgumentException("Review entity cannot be null");
        }

        UserBasicDTO userDTO = review.getUser() != null ?
                mapUserToBasicDTO(review.getUser()) : null;

        BoatBasicDTO boatDTO = review.getBoat() != null ?
                mapBoatBasicDTO(review.getBoat()) : null;

        return new ReviewResponseDTO(
                review.getId(),
                userDTO,
                boatDTO,
                review.getRating(),
                review.getComment(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }

    /**
     * Maps User entity to UserBasicDTO.
     * Extracted as separate method for consistency and potential reuse.
     */
    public UserBasicDTO mapUserToBasicDTO(User user){

        return new UserBasicDTO(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }

    /**
     * Maps Boat entity to BoatBasicDTO.
     * Extracted as separate method for consistency and potential reuse.
     */
    public BoatBasicDTO mapBoatBasicDTO(Boat boat){
        return new BoatBasicDTO(
                boat.getId(),
                boat.getName(),
                boat.getType(),
                boat.getAddress()
        );
    }
}
