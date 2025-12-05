package com.jompastech.backend.controller;

import com.jompastech.backend.model.dto.ReviewRequestDTO;
import com.jompastech.backend.model.dto.ReviewResponseDTO;
import com.jompastech.backend.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API controller for review operations.
 *
 * <p><b>API Design Decisions:</b>
 * <ul>
 *   <li>Follows RESTful conventions with proper HTTP methods and status codes</li>
 *   <li>Uses DTOs for request/response to decouple API contract from domain model</li>
 *   <li>Extracts user identity from security context to prevent impersonation</li>
 *   <li>Validates input at API boundary with JSR-303 annotations</li>
 *   <li>Provides clear, consistent error responses</li>
 * </ul>
 *
 * <p><b>Security:</b> All endpoints require authentication and use the
 * authenticated user's identity from Spring Security context.</p>
 */
@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * Creates a new review for a boat.
     *
     * @param reviewRequest the review data
     * @param userId authenticated user ID from security context
     * @return created review with 201 status
     */
    @PostMapping
    public ResponseEntity<ReviewResponseDTO> createReview(
            @Valid @RequestBody ReviewRequestDTO reviewRequest,
            @AuthenticationPrincipal Long userId) {
        ReviewResponseDTO createdReview = reviewService.createReview(reviewRequest, userId);
        return new ResponseEntity<>(createdReview, HttpStatus.CREATED);
    }

    /**
     * Updates an existing review.
     *
     * @param reviewId the review ID to update
     * @param reviewRequest updated review data
     * @param userId authenticated user ID for authorization
     * @return updated review
     */
    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewResponseDTO> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewRequestDTO reviewRequest,
            @AuthenticationPrincipal Long userId) {
        ReviewResponseDTO updatedReview = reviewService.updateReview(reviewId, reviewRequest, userId);
        return ResponseEntity.ok(updatedReview);
    }

    /**
     * Retrieves a specific review by ID.
     */
    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewResponseDTO> getReview(@PathVariable Long reviewId) {
        ReviewResponseDTO review = reviewService.findById(reviewId);
        return ResponseEntity.ok(review);
    }

    /**
     * Retrieves all reviews for a specific boat.
     */
    @GetMapping("/boat/{boatId}")
    public ResponseEntity<List<ReviewResponseDTO>> getReviewsByBoat(@PathVariable Long boatId) {
        List<ReviewResponseDTO> reviews = reviewService.findByBoatId(boatId);
        return ResponseEntity.ok(reviews);
    }

    /**
     * Retrieves all reviews by the authenticated user.
     */
    @GetMapping("/my-reviews")
    public ResponseEntity<List<ReviewResponseDTO>> getMyReviews(@AuthenticationPrincipal Long userId) {
        List<ReviewResponseDTO> reviews = reviewService.findByUserId(userId);
        return ResponseEntity.ok(reviews);
    }

    /**
     * Deletes a review.
     *
     * @param reviewId the review to delete
     * @param userId authenticated user ID for authorization
     * @param isAdmin whether user has admin role (from security context)
     * @return 204 No Content on success
     */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false, defaultValue = "false") Boolean isAdmin) {
        reviewService.deleteReview(reviewId, userId, isAdmin);
        return ResponseEntity.noContent().build();
    }

    /**
     * Gets review statistics for a boat.
     */
    @GetMapping("/boat/{boatId}/stats")
    public ResponseEntity<Object> getReviewStats(@PathVariable Long boatId) {
        var stats = reviewService.getReviewStats(boatId);
        return ResponseEntity.ok(stats);
    }
}