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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReviewService business logic and operations.
 *
 * <p><b>Testing Strategy:</b>
 * <ul>
 *   <li>Uses Mockito for complete dependency isolation</li>
 *   <li>Covers both positive and negative test scenarios</li>
 *   <li>Validates business rule enforcement and error handling</li>
 *   <li>Tests authorization and security boundaries</li>
 *   <li>Ensures proper data flow between service layers</li>
 * </ul>
 *
 * <p><b>Mock Configuration:</b> All external dependencies are mocked to test
 * ReviewService in isolation. Services return entities directly (not Optional)
 * to maintain consistency with actual implementation.</p>
 */
@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private UserService userService;

    @Mock
    private BoatService boatService;

    @Mock
    private BookingQueryService bookingQueryService;

    @InjectMocks
    private ReviewService reviewService;

    private User testUser;
    private Boat testBoat;
    private ReviewRequestDTO testRequestDTO;
    private Review testReview;
    private ReviewResponseDTO testResponseDTO;

    /**
     * Initializes test data before each test method execution.
     * Creates consistent test entities and DTOs for all test scenarios.
     */
    @BeforeEach
    void setUp() {
        // Test user setup - represents authenticated user in test scenarios
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");

        // Test boat setup - represents the boat being reviewed
        testBoat = new Boat();
        testBoat.setId(1L);
        testBoat.setName("Test Boat");

        // Test request DTO - represents API input for review creation
        testRequestDTO = new ReviewRequestDTO();
        testRequestDTO.setBoatId(1L);
        testRequestDTO.setRating(5);
        testRequestDTO.setComment("Excellent experience!");

        // Test review entity - represents persisted review with all relationships
        testReview = new Review();
        testReview.setId(1L);
        testReview.setUser(testUser);
        testReview.setBoat(testBoat);
        testReview.setRating(5);
        testReview.setComment("Excellent experience!");

        // Test response DTO - represents API output with aggregated data
        testResponseDTO = new ReviewResponseDTO(
                1L, null, null, 5, "Excellent experience!", null, null
        );
    }

    /**
     * Tests successful review creation with valid data and proper authorization.
     * Verifies complete flow from request to response with all business validations.
     */
    @Test
    void createReview_WithValidData_ShouldReturnReviewResponse() {
        // Arrange - Configure all required mocks for successful flow
        when(userService.findById(1L)).thenReturn(testUser);
        when(boatService.findById(1L)).thenReturn(testBoat);
        when(bookingQueryService.hasUserRentedBoat(1L, 1L)).thenReturn(true);
        when(reviewRepository.existsByUserIdAndBoatId(1L, 1L)).thenReturn(false);

        // Mock mapper to return basic review (without relationships)
        Review basicReview = new Review();
        basicReview.setRating(5);
        basicReview.setComment("Excellent experience!");
        when(reviewMapper.toEntity(testRequestDTO)).thenReturn(basicReview);

        // Mock repository to return complete review (with relationships)
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
        when(reviewMapper.toResponseDTO(testReview)).thenReturn(testResponseDTO);

        // Act - Execute the service method
        ReviewResponseDTO result = reviewService.createReview(testRequestDTO, 1L);

        // Assert - Verify expected outcomes
        assertNotNull(result, "Response should not be null");
        assertEquals(1L, result.getId(), "Review ID should match");
        assertEquals(5, result.getRating(), "Rating should match input");

        // Verify interactions with dependencies
        verify(userService, times(1)).findById(1L);
        verify(boatService, times(1)).findById(1L);
        verify(bookingQueryService, times(1)).hasUserRentedBoat(1L, 1L);
        verify(reviewRepository, times(1)).existsByUserIdAndBoatId(1L, 1L);
        verify(reviewRepository, times(1)).save(any(Review.class));
    }

    /**
     * Tests review creation failure when user doesn't exist.
     * Validates proper exception handling for missing user entity.
     */
    @Test
    void createReview_WhenUserNotFound_ShouldThrowEntityNotFoundException() {
        // Arrange - Mock user service to throw exception
        when(userService.findById(1L)).thenThrow(new EntityNotFoundException("User not found"));

        // Act & Assert - Verify exception is thrown
        assertThrows(EntityNotFoundException.class,
                () -> reviewService.createReview(testRequestDTO, 1L),
                "Should throw EntityNotFoundException when user doesn't exist"
        );
    }

    /**
     * Tests review creation failure when boat doesn't exist.
     * Validates proper exception handling for missing boat entity.
     */
    @Test
    void createReview_WhenBoatNotFound_ShouldThrowEntityNotFoundException() {
        // Arrange - Mock boat service to throw exception
        when(userService.findById(1L)).thenReturn(testUser);
        when(boatService.findById(1L)).thenThrow(new EntityNotFoundException("Boat not found"));

        // Act & Assert - Verify exception is thrown
        assertThrows(EntityNotFoundException.class,
                () -> reviewService.createReview(testRequestDTO, 1L),
                "Should throw EntityNotFoundException when boat doesn't exist"
        );
    }

    /**
     * Tests review creation failure when user hasn't rented the boat.
     * Validates business rule enforcement for review eligibility.
     */
    @Test
    void createReview_WhenUserNotRentedBoat_ShouldThrowBusinessValidationException() {
        // Arrange - Mock rental validation to return false
        when(userService.findById(1L)).thenReturn(testUser);
        when(boatService.findById(1L)).thenReturn(testBoat);
        when(bookingQueryService.hasUserRentedBoat(1L, 1L)).thenReturn(false);

        // Act & Assert - Verify business rule violation
        assertThrows(BusinessValidationException.class,
                () -> reviewService.createReview(testRequestDTO, 1L),
                "Should throw BusinessValidationException when user hasn't rented the boat"
        );
    }

    /**
     * Tests review creation failure when duplicate review exists.
     * Validates unique constraint enforcement for user-boat combinations.
     */
    @Test
    void createReview_WhenDuplicateReview_ShouldThrowBusinessValidationException() {
        // Arrange - Mock duplicate review check to return true
        when(userService.findById(1L)).thenReturn(testUser);
        when(boatService.findById(1L)).thenReturn(testBoat);
        when(bookingQueryService.hasUserRentedBoat(1L, 1L)).thenReturn(true);
        when(reviewRepository.existsByUserIdAndBoatId(1L, 1L)).thenReturn(true);

        // Act & Assert - Verify duplicate prevention
        assertThrows(BusinessValidationException.class,
                () -> reviewService.createReview(testRequestDTO, 1L),
                "Should throw BusinessValidationException for duplicate reviews"
        );
    }

    /**
     * Tests successful review update when user is the review author.
     * Verifies authorization check and field updates.
     */
    @Test
    void updateReview_WhenUserIsOwner_ShouldUpdateSuccessfully() {
        // Arrange - Mock existing review owned by authenticated user
        testReview.setUser(testUser); // User owns the review
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));
        when(reviewRepository.save(testReview)).thenReturn(testReview);
        when(reviewMapper.toResponseDTO(testReview)).thenReturn(testResponseDTO);

        // Act - Execute update
        ReviewResponseDTO result = reviewService.updateReview(1L, testRequestDTO, 1L);

        // Assert - Verify update occurred
        assertNotNull(result, "Updated review should not be null");
        verify(reviewRepository, times(1)).save(testReview);

        // Verify review fields were updated
        assertEquals(5, testReview.getRating(), "Rating should be updated");
        assertEquals("Excellent experience!", testReview.getComment(), "Comment should be updated");
    }

    /**
     * Tests review update failure when user is not the review author.
     * Validates authorization boundary enforcement.
     */
    @Test
    void updateReview_WhenUserNotOwner_ShouldThrowSecurityException() {
        // Arrange - Mock review owned by different user
        User differentUser = new User();
        differentUser.setId(2L);
        testReview.setUser(differentUser);

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));

        // Act & Assert - Verify authorization failure
        assertThrows(SecurityException.class,
                () -> reviewService.updateReview(1L, testRequestDTO, 1L),
                "Should throw SecurityException when user is not review owner"
        );
    }

    /**
     * Tests successful review retrieval by ID.
     * Verifies entity to DTO conversion and proper error handling.
     */
    @Test
    void findById_WhenReviewExists_ShouldReturnReview() {
        // Arrange - Mock existing review
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));
        when(reviewMapper.toResponseDTO(testReview)).thenReturn(testResponseDTO);

        // Act - Execute retrieval
        ReviewResponseDTO result = reviewService.findById(1L);

        // Assert - Verify successful retrieval
        assertNotNull(result, "Review should be found");
        assertEquals(1L, result.getId(), "Review ID should match");
    }

    /**
     * Tests review retrieval failure when review doesn't exist.
     * Validates proper exception handling for missing reviews.
     */
    @Test
    void findById_WhenReviewNotExists_ShouldThrowEntityNotFoundException() {
        // Arrange - Mock missing review
        when(reviewRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert - Verify exception for missing entity
        assertThrows(EntityNotFoundException.class,
                () -> reviewService.findById(1L),
                "Should throw EntityNotFoundException when review doesn't exist"
        );
    }

    /**
     * Tests successful review deletion by review owner.
     * Verifies authorization check and repository interaction.
     */
    @Test
    void deleteReview_WhenUserIsOwner_ShouldDeleteSuccessfully() {
        // Arrange - Mock review owned by authenticated user
        testReview.setUser(testUser);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));

        // Act - Execute deletion
        reviewService.deleteReview(1L, 1L, false);

        // Assert - Verify deletion occurred
        verify(reviewRepository, times(1)).delete(testReview);
    }

    /**
     * Tests review deletion by admin user regardless of ownership.
     * Verifies admin privilege override for deletion operations.
     */
    @Test
    void deleteReview_WhenUserIsAdmin_ShouldDeleteSuccessfully() {
        // Arrange - Mock review owned by different user, but user is admin
        User differentUser = new User();
        differentUser.setId(2L);
        testReview.setUser(differentUser);

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));

        // Act - Execute deletion with admin privileges
        reviewService.deleteReview(1L, 1L, true);

        // Assert - Verify admin override worked
        verify(reviewRepository, times(1)).delete(testReview);
    }

    /**
     * Tests review deletion failure when user is not owner and not admin.
     * Validates authorization boundary for deletion operations.
     */
    @Test
    void deleteReview_WhenUserNotOwnerAndNotAdmin_ShouldThrowSecurityException() {
        // Arrange - Mock review owned by different user, user is not admin
        User differentUser = new User();
        differentUser.setId(2L);
        testReview.setUser(differentUser);

        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));

        // Act & Assert - Verify authorization failure
        assertThrows(SecurityException.class,
                () -> reviewService.deleteReview(1L, 1L, false),
                "Should throw SecurityException when user is not owner and not admin"
        );
    }

    /**
     * Tests retrieval of reviews by boat ID.
     * Verifies proper repository method call and list processing.
     */
    @Test
    void findByBoatId_ShouldReturnReviewList() {
        // Arrange - Mock review list for boat
        List<Review> reviews = List.of(testReview);
        when(reviewRepository.findByBoatIdWithAssociations(1L)).thenReturn(reviews);
        when(reviewMapper.toResponseDTO(testReview)).thenReturn(testResponseDTO);

        // Act - Execute retrieval
        List<ReviewResponseDTO> results = reviewService.findByBoatId(1L);

        // Assert - Verify list processing
        assertNotNull(results, "Results should not be null");
        assertEquals(1, results.size(), "Should return one review");
        verify(reviewMapper, times(1)).toResponseDTO(testReview);
    }

    /**
     * Tests retrieval of review statistics for a boat.
     * Verifies aggregation query and result mapping.
     */
    @Test
    void getReviewStats_ShouldReturnAggregationData() {
        // Arrange - Mock aggregation result
        ReviewRepository.ReviewAggregation mockAggregation =
                new ReviewRepository.ReviewAggregation() {
                    public Double getAverageRating() { return 4.5; }
                    public Long getReviewCount() { return 10L; }
                };
        when(reviewRepository.getReviewAggregation(1L)).thenReturn(mockAggregation);

        // Act - Execute stats retrieval
        var result = reviewService.getReviewStats(1L);

        // Assert - Verify aggregation data
        assertNotNull(result, "Stats should not be null");
        assertEquals(4.5, result.getAverageRating(), 0.01, "Average rating should match");
        assertEquals(10L, result.getReviewCount(), "Review count should match");
    }

    /**
     * Tests that findByUserId returns all reviews when user has existing reviews.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>Repository method is called with correct user ID</li>
     *   <li>Mapper converts entities to DTOs properly</li>
     *   <li>Returns list with correct size and content</li>
     * </ul>
     *
     * <p>Uses shared test fixtures from setUp method to ensure consistency
     * across test scenarios.
     */

    @Test
    void shouldReturnAllReviewsByUserId(){

        // Arrange

        when(reviewRepository.findByUserIdWithBoat(testUser.getId()))
                .thenReturn(List.of(testReview));
        when(reviewMapper.toResponseDTO(testReview))
                .thenReturn(testResponseDTO);

        // Act & Assert
        assertThat(reviewService.findByUserId(testUser.getId()))
            .hasSize(1)
                .containsExactly(testResponseDTO);
    }

    /**
     * Tests that findByUserId returns empty list when user has no reviews.
     *
     * <p>Validates edge case behavior where:
     * <ul>
     *   <li>Repository returns empty list for user with no reviews</li>
     *   <li>Service returns empty list instead of null</li>
     *   <li>Mapper is not called unnecessarily (performance optimization)</li>
     * </ul>
     */

    @Test
    void findByUserId_WhenNoReviews_ShouldReturnEmptyList(){
        // Arrange
        when(reviewRepository.findByUserIdWithBoat(testUser.getId()))
                .thenReturn(Collections.emptyList());

        // Act & Assert
        assertThat(reviewService.findByUserId(testUser.getId())).isEmpty();

        // Bonus: garante que o mapper não foi chamado desnecessariamente
        verify(reviewMapper, never()).toResponseDTO(any());
    }

}