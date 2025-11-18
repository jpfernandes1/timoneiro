package com.jompastech.backend.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "reviews", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "boat_id"}, name = "uk_user_boat_review")
})
/**
 * Represents a user review for a boat rental.
 * Ensures data integrity through unique constraint preventing multiple reviews
 * from the same user for the same boat.
 */
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boat_id", nullable = false)
    private Boat boat;

    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private int rating;

    @NotBlank(message = "Review comment cannot be empty")
    @Size(max = 1000, message = "Comment must not exceed 1000 characters")
    @Column(length = 1000)
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Business logic method
    /**
     * Validates if the review meets business criteria.
     * Currently focuses on comment length constraints.
     * Future: Integrate with rental validation service.
     */
    public boolean isValidForSubmission() {
        return comment != null && !comment.trim().isEmpty() &&
                comment.length() <= 1000 && rating >= 1 && rating <= 5;
    }
}