package com.jompastech.backend.service;

import com.jompastech.backend.exception.BusinessValidationException;
import com.jompastech.backend.exception.EntityNotFoundException;
import com.jompastech.backend.model.dto.ReviewRequestDTO;
import com.jompastech.backend.model.dto.ReviewResponseDTO;
import com.jompastech.backend.model.entity.Review;
import com.jompastech.backend.model.entity.User;
import com.jompastech.backend.model.entity.Boat;
import com.jompastech.backend.mapper.ReviewMapper;
import com.jompastech.backend.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service layer for review business operations and coordination.
 *
 * <p><b>Design Decisions:</b>
 * <ul>
 *   <li>Orchestrates business rules across multiple entities and services</li>
 *   <li>Handles transaction boundaries for complex operations</li>
 *   <li>Separates business logic from data access and presentation concerns</li>
 *   <li>Provides clear entry points for controller layer</li>
 * </ul>
 *
 * <p><b>Business Rules Implemented:</b>
 * <ul>
 *   <li>Users can only review boats they have actually rented</li>
 *   <li>Prevents duplicate reviews for the same boat by same user</li>
 *   <li>Ensures data integrity through validation before persistence</li>
 *   <li>Maintains authorization boundaries for update/delete operations</li>
 * </ul>
 */
@Service
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;
    private final UserService userService;
    private final BoatService boatService;
    private final BookingQueryService bookingQueryService;

    public ReviewService(ReviewRepository reviewRepository, ReviewMapper reviewMapper,
                         UserService userService, BoatService boatService, BookingQueryService bookingQueryService) {
        this.reviewRepository = reviewRepository;
        this.reviewMapper = reviewMapper;
        this.userService = userService;
        this.boatService = boatService;
        this.bookingQueryService = bookingQueryService;
    }

    /**
     * Creates a new review after validating all business rules.
     *
     * @param reviewRequest the review data from API
     * @param authenticatedUserId user ID from security context
     * @return the created review as response DTO
     * @throws BusinessValidationException if business rules are violated
     * @throws EntityNotFoundException if referenced entities don't exist
     */
    public ReviewResponseDTO createReview(ReviewRequestDTO reviewRequest, Long authenticatedUserId) {

        User user = userService.findById(authenticatedUserId);
        Boat boat = boatService.getBoatEntity(reviewRequest.getBoatId());

        // Business rule: user must have rented this boat to review it
        validateUserRentedBoat(authenticatedUserId, reviewRequest.getBoatId());

        // Business rule: prevent duplicate reviews
        validateNoDuplicateReview(authenticatedUserId, reviewRequest.getBoatId());

        // Create and save review
        Review review = reviewMapper.toEntity(reviewRequest);
        review.setUser(user);
        review.setBoat(boat);

        Review savedReview = reviewRepository.save(review);
        return reviewMapper.toResponseDTO(savedReview);
    }

    /**
     * Updates an existing review with authorization check.
     *
     * @param reviewId the review to update
     * @param reviewRequest updated review data
     * @param authenticatedUserId user ID for authorization
     * @return updated review as response DTO
     * @throws SecurityException if user is not the review author
     */
    public ReviewResponseDTO updateReview(Long reviewId, ReviewRequestDTO reviewRequest, Long authenticatedUserId) {
        Review existingReview = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Review not found with id: " + reviewId));

        // Authorization check: only review author can update
        if (!existingReview.getUser().getId().equals(authenticatedUserId)) {
            throw new SecurityException("User is not authorized to update this review");
        }

        // Update fields
        existingReview.setRating(reviewRequest.getRating());
        existingReview.setComment(reviewRequest.getComment());

        Review updatedReview = reviewRepository.save(existingReview);
        return reviewMapper.toResponseDTO(updatedReview);
    }

    /**
     * Retrieves a review by ID with proper error handling.
     */
    @Transactional(readOnly = true)
    public ReviewResponseDTO findById(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Review not found with id: " + reviewId));
        return reviewMapper.toResponseDTO(review);
    }

    /**
     * Finds all reviews for a specific boat with pagination support.
     */
    @Transactional(readOnly = true)
    public List<ReviewResponseDTO> findByBoatId(Long boatId) {
        List<Review> reviews = reviewRepository.findByBoatIdWithAssociations(boatId);
        return reviews.stream()
                .map(reviewMapper::toResponseDTO)
                .toList();
    }

    /**
     * Finds all reviews by a specific user.
     */
    @Transactional(readOnly = true)
    public List<ReviewResponseDTO> findByUserId(Long userId) {
        List<Review> reviews = reviewRepository.findByUserIdWithBoat(userId);
        return reviews.stream()
                .map(reviewMapper::toResponseDTO)
                .toList();
    }

    /**
     * Deletes a review with authorization check.
     */
    public void deleteReview(Long reviewId, Long authenticatedUserId, boolean isAdmin) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Review not found with id: " + reviewId));

        // Authorization: only author or admin can delete
        if (!review.getUser().getId().equals(authenticatedUserId) && !isAdmin) {
            throw new SecurityException("User is not authorized to delete this review");
        }

        reviewRepository.delete(review);
    }

    /**
     * Gets review statistics for a boat.
     */
    @Transactional(readOnly = true)
    public ReviewRepository.ReviewAggregation getReviewStats(Long boatId) {
        return reviewRepository.getReviewAggregation(boatId);
    }

    // Private validation methods
    private void validateUserRentedBoat(Long userId, Long boatId) {
        if (!bookingQueryService.hasUserRentedBoat(userId, boatId)) {
            throw new BusinessValidationException(
                    "User must have completed a rental for this boat before submitting a review"
            );
        }
    }

    private void validateNoDuplicateReview(Long userId, Long boatId) {
        if (reviewRepository.existsByUserIdAndBoatId(userId, boatId)) {
            throw new BusinessValidationException(
                    "User has already submitted a review for this boat"
            );
        }
    }
}