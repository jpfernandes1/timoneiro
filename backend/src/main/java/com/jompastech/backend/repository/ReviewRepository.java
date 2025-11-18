package com.jompastech.backend.repository;

import com.jompastech.backend.model.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Review entities with custom queries for business operations.
 *
 * <p><b>Design Decisions:</b>
 * <ul>
 *   <li>Extends JpaRepository for standard CRUD operations with pagination support</li>
 *   <li>Uses method naming convention for simple derived queries</li>
 *   <li>Uses @Query for complex JPQL operations requiring joins or aggregations</li>
 *   <li>Includes both entity-based and projection-based queries for flexibility</li>
 * </ul>
 *
 * <p><b>Performance Note:</b> Queries use JOIN FETCH to avoid N+1 problem
 * by eagerly loading required associations in a single query.</p>
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * Finds all reviews for a specific boat with user and boat associations loaded.
     * Uses JOIN FETCH to avoid N+1 queries in list operations.
     *
     * @param boatId the boat identifier
     * @return list of reviews with user and boat data eagerly loaded
     */
    @Query("SELECT r FROM Review r JOIN FETCH r.user JOIN FETCH r.boat WHERE r.boat.id = :boatId ORDER BY r.createdAt DESC")
    List<Review> findByBoatIdWithAssociations(@Param("boatId") Long boatId);

    /**
     * Finds all reviews by a specific user with boat association loaded.
     *
     * @param userId the user identifier
     * @return list of user's reviews with boat data
     */
    @Query("SELECT r FROM Review r JOIN FETCH r.boat WHERE r.user.id = :userId ORDER BY r.createdAt DESC")
    List<Review> findByUserIdWithBoat(@Param("userId") Long userId);

    /**
     * Checks if a user has already reviewed a specific boat.
     * Used for business rule validation during review creation.
     *
     * @param userId the user identifier
     * @param boatId the boat identifier
     * @return true if review exists, false otherwise
     */
    boolean existsByUserIdAndBoatId(Long userId, Long boatId);

    /**
     * Finds a specific review by user and boat combination.
     * Useful for update operations and duplicate validation.
     *
     * @param userId the user identifier
     * @param boatId the boat identifier
     * @return optional review if found
     */
    Optional<Review> findByUserIdAndBoatId(Long userId, Long boatId);

    /**
     * Calculates average rating and total review count for a boat.
     * Uses projection interface for type-safe result mapping.
     *
     * @param boatId the boat identifier
     * @return aggregation result with average rating and count
     */
    @Query("SELECT AVG(r.rating) as averageRating, COUNT(r) as reviewCount " +
            "FROM Review r WHERE r.boat.id = :boatId")
    ReviewAggregation getReviewAggregation(@Param("boatId") Long boatId);

    /**
     * Projection interface for review aggregation results.
     * Provides type-safe access to calculated values.
     */
    interface ReviewAggregation {
        Double getAverageRating();
        Long getReviewCount();
    }
}