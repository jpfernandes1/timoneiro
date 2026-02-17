package com.jompastech.backend.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jompastech.backend.integration.controller.util.*;
import com.jompastech.backend.model.entity.*;
import com.jompastech.backend.model.enums.BookingStatus;
import com.jompastech.backend.repository.*;
import com.jompastech.backend.security.dto.AuthRequestDTO;
import com.jompastech.backend.security.filter.JwtAuthenticationFilter;
import com.jompastech.backend.service.CloudinaryService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReviewControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private BoatRepository boatRepository;
    @Autowired private BoatAvailabilityRepository boatAvailabilityRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private CloudinaryService cloudinaryService;

    private UserGenerator userGenerator;
    private BoatGenerator boatGenerator;
    private BookingGenerator bookingGenerator;
    private AvailabilityGenerator availabilityGenerator;
    private ReviewGenerator reviewGenerator;
    private String jwtToken;
    private User testUser;
    private Boat testBoat;
    private Long boatId;

    private final LocalDateTime startDate = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0);
    private final LocalDateTime endDate = startDate.plusHours(4);

    @BeforeEach
    void setup() {
        userGenerator = new UserGenerator(mockMvc);
        boatGenerator = new BoatGenerator(mockMvc);
        bookingGenerator = new BookingGenerator(mockMvc);
        availabilityGenerator = new AvailabilityGenerator(mockMvc);
        reviewGenerator = new ReviewGenerator(mockMvc);
    }

    @AfterEach
    void cleanup() {
        paymentRepository.deleteAll();
        reviewRepository.deleteAll();
        bookingRepository.deleteAll();
        boatAvailabilityRepository.deleteAll();
        boatRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==========================================================
    // TEST
    // ==========================================================

    @Test
    void shouldCreateReviewWhenBookingFinished() throws Exception {

        var owner = userGenerator.createAndAuthenticateUser();
        var renter = userGenerator.createAndAuthenticateUser();

        Long boatId = boatGenerator.createBoat("Bearer " + owner.token());

        availabilityGenerator.createAvailability(
                "Bearer " + owner.token(),
                boatId,
                LocalDateTime.now().plusDays(5),
                LocalDateTime.now().plusDays(30),
                BigDecimal.valueOf(300)
        );

        Long bookingId = bookingGenerator.createBooking(
                "Bearer " + renter.token(),
                boatId,
                LocalDateTime.now().plusDays(6),
                LocalDateTime.now().plusDays(7)
        );

        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        booking.finish();
        bookingRepository.saveAndFlush(booking);

        String json = """
            {
              "boatId": %d,
              "rating": 5,
              "comment": "Excellent experience"
            }
            """.formatted(boatId);

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + renter.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(5));
    }

    @Test
    void shouldNotCreateReviewIfBookingNotFinished() throws Exception {

        var owner = userGenerator.createAndAuthenticateUser();
        var renter = userGenerator.createAndAuthenticateUser();

        Long boatId = boatGenerator.createBoat("Bearer " + owner.token());

        availabilityGenerator.createAvailability(
                "Bearer " + owner.token(),
                boatId,
                LocalDateTime.now().plusDays(5),
                LocalDateTime.now().plusDays(30),
                BigDecimal.valueOf(300)
        );

        bookingGenerator.createBooking(
                "Bearer " + renter.token(),
                boatId,
                LocalDateTime.now().plusDays(6),
                LocalDateTime.now().plusDays(7)
        );

        String json = """
        {
          "boatId": %d,
          "rating": 5,
          "comment": "Should fail"
        }
        """.formatted(boatId);

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + renter.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUpdateReview() throws Exception {

        var owner = userGenerator.createAndAuthenticateUser();
        var renter = userGenerator.createAndAuthenticateUser();

        Long boatId = boatGenerator.createBoat("Bearer " + owner.token());

        availabilityGenerator.createAvailability(
                "Bearer " + owner.token(),
                boatId,
                LocalDateTime.now().plusDays(5),
                LocalDateTime.now().plusDays(30),
                BigDecimal.valueOf(300)
        );

        Long bookingId = bookingGenerator.createBooking(
                "Bearer " + renter.token(),
                boatId,
                LocalDateTime.now().plusDays(6),
                LocalDateTime.now().plusDays(7)
        );

        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        booking.finish();
        bookingRepository.saveAndFlush(booking);

        Long reviewId = reviewGenerator.createReview(
                "Bearer " + renter.token(),
                boatId,
                5,
                "Original"
        );

        String updateJson = """
        {
          "boatId": %d,
          "rating": 3,
          "comment": "Updated comment"
        }
        """.formatted(boatId);

        mockMvc.perform(put("/api/reviews/{id}", reviewId)
                        .header("Authorization", "Bearer " + renter.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(3))
                .andExpect(jsonPath("$.comment").value("Updated comment"));
    }
    @Test
    void shouldNotUpdateReviewFromAnotherUser() throws Exception {

        var owner = userGenerator.createAndAuthenticateUser();
        var renter = userGenerator.createAndAuthenticateUser();
        var otherUser = userGenerator.createAndAuthenticateUser();

        Long boatId = boatGenerator.createBoat("Bearer " + owner.token());

        availabilityGenerator.createAvailability(
                "Bearer " + owner.token(),
                boatId,
                LocalDateTime.now().plusDays(5),
                LocalDateTime.now().plusDays(30),
                BigDecimal.valueOf(300)
        );

        Long bookingId = bookingGenerator.createBooking(
                "Bearer " + renter.token(),
                boatId,
                LocalDateTime.now().plusDays(6),
                LocalDateTime.now().plusDays(7)
        );

        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        booking.finish();
        bookingRepository.saveAndFlush(booking);

        Long reviewId = reviewGenerator.createReview(
                "Bearer " + renter.token(),
                boatId,
                5,
                "Owner review"
        );

        String updateJson = """
        {
          "boatId": %d,
          "rating": 1,
          "comment": "Hack attempt"
        }
        """.formatted(boatId);

        mockMvc.perform(put("/api/reviews/{id}", reviewId)
                        .header("Authorization", "Bearer " + otherUser.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldGetReviewById() throws Exception {

        var owner = userGenerator.createAndAuthenticateUser();
        var renter = userGenerator.createAndAuthenticateUser();

        Long boatId = boatGenerator.createBoat("Bearer " + owner.token());

        availabilityGenerator.createAvailability(
                "Bearer " + owner.token(),
                boatId,
                LocalDateTime.now().plusDays(5),
                LocalDateTime.now().plusDays(30),
                BigDecimal.valueOf(300)
        );

        Long bookingId = bookingGenerator.createBooking(
                "Bearer " + renter.token(),
                boatId,
                LocalDateTime.now().plusDays(6),
                LocalDateTime.now().plusDays(7)
        );

        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        booking.finish();
        bookingRepository.saveAndFlush(booking);

        Long reviewId = reviewGenerator.createReview(
                "Bearer " + renter.token(),
                boatId,
                5,
                "Nice"
        );

        mockMvc.perform(get("/api/reviews/{id}", reviewId)
                .header("Authorization", "Bearer " + renter.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reviewId));
    }
    @Test
    void shouldDeleteReviewAsOwner() throws Exception {

        var owner = userGenerator.createAndAuthenticateUser();
        var renter = userGenerator.createAndAuthenticateUser();

        Long boatId = boatGenerator.createBoat("Bearer " + owner.token());

        availabilityGenerator.createAvailability(
                "Bearer " + owner.token(),
                boatId,
                LocalDateTime.now().plusDays(5),
                LocalDateTime.now().plusDays(30),
                BigDecimal.valueOf(300)
        );

        Long bookingId = bookingGenerator.createBooking(
                "Bearer " + renter.token(),
                boatId,
                LocalDateTime.now().plusDays(6),
                LocalDateTime.now().plusDays(7)
        );

        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        booking.finish();
        bookingRepository.saveAndFlush(booking);

        Long reviewId = reviewGenerator.createReview(
                "Bearer " + renter.token(),
                boatId,
                5,
                "To delete"
        );

        mockMvc.perform(delete("/api/reviews/{id}", reviewId)
                        .header("Authorization", "Bearer " + renter.token()))
                .andExpect(status().isNoContent());

        assertFalse(reviewRepository.findById(reviewId).isPresent());
    }

    @Test
    void shouldReturnReviewStats() throws Exception {

        var owner = userGenerator.createAndAuthenticateUser();
        var renter = userGenerator.createAndAuthenticateUser();

        Long boatId = boatGenerator.createBoat("Bearer " + owner.token());

        availabilityGenerator.createAvailability(
                "Bearer " + owner.token(),
                boatId,
                LocalDateTime.now().plusDays(5),
                LocalDateTime.now().plusDays(30),
                BigDecimal.valueOf(300)
        );

        Long bookingId = bookingGenerator.createBooking(
                "Bearer " + renter.token(),
                boatId,
                LocalDateTime.now().plusDays(6),
                LocalDateTime.now().plusDays(7)
        );

        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        booking.finish();
        bookingRepository.saveAndFlush(booking);

        reviewGenerator.createReview(
                "Bearer " + renter.token(),
                boatId,
                5,
                "Great"
        );

        mockMvc.perform(get("/api/reviews/boat/{id}/stats", boatId)
                        .header("Authorization", "Bearer " + renter.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").exists());
    }

    @Test
    void shouldReturnReviewsByBoat() throws Exception {

        var owner = userGenerator.createAndAuthenticateUser();
        var renter = userGenerator.createAndAuthenticateUser();

        Long boatId = boatGenerator.createBoat("Bearer " + owner.token());

        availabilityGenerator.createAvailability(
                "Bearer " + owner.token(),
                boatId,
                LocalDateTime.now().plusDays(5),
                LocalDateTime.now().plusDays(30),
                BigDecimal.valueOf(300)
        );

        Long bookingId = bookingGenerator.createBooking(
                "Bearer " + renter.token(),
                boatId,
                LocalDateTime.now().plusDays(6),
                LocalDateTime.now().plusDays(7)
        );

        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        booking.finish();
        bookingRepository.saveAndFlush(booking);

        reviewGenerator.createReview(
                "Bearer " + renter.token(),
                boatId,
                5,
                "Amazing boat"
        );

        mockMvc.perform(get("/api/reviews/boat/{boatId}", boatId)
                        .header("Authorization", "Bearer " + renter.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].boat.id").value(boatId))
                .andExpect(jsonPath("$[0].rating").value(5));
    }

    @Test
    void shouldReturnMyReviews() throws Exception {

        var owner = userGenerator.createAndAuthenticateUser();
        var renter = userGenerator.createAndAuthenticateUser();
        var otherUser = userGenerator.createAndAuthenticateUser();

        Long boatId = boatGenerator.createBoat("Bearer " + owner.token());

        availabilityGenerator.createAvailability(
                "Bearer " + owner.token(),
                boatId,
                LocalDateTime.now().plusDays(5),
                LocalDateTime.now().plusDays(30),
                BigDecimal.valueOf(300)
        );

        Long bookingId = bookingGenerator.createBooking(
                "Bearer " + renter.token(),
                boatId,
                LocalDateTime.now().plusDays(6),
                LocalDateTime.now().plusDays(7)
        );

        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        booking.finish();
        bookingRepository.saveAndFlush(booking);

        reviewGenerator.createReview(
                "Bearer " + renter.token(),
                boatId,
                4,
                "My review"
        );

        // Outra review de outro usuário (se permitido no seu domínio)
        Long boatId2 = boatGenerator.createBoat("Bearer " + owner.token());

        availabilityGenerator.createAvailability(
                "Bearer " + owner.token(),
                boatId2,
                LocalDateTime.now().plusDays(10),
                LocalDateTime.now().plusDays(20),
                BigDecimal.valueOf(200)
        );

        Long bookingId2 = bookingGenerator.createBooking(
                "Bearer " + otherUser.token(),
                boatId2,
                LocalDateTime.now().plusDays(11),
                LocalDateTime.now().plusDays(12)
        );

        Booking booking2 = bookingRepository.findById(bookingId2).orElseThrow();
        booking2.finish();
        bookingRepository.saveAndFlush(booking2);

        reviewGenerator.createReview(
                "Bearer " + otherUser.token(),
                boatId2,
                2,
                "Other review"
        );

        mockMvc.perform(get("/api/reviews/my-reviews")
                        .header("Authorization", "Bearer " + renter.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].rating").value(4));
    }





    // ==========================================================
    // TEST SECURITY CONFIG
    // ==========================================================

    @TestConfiguration
    @Slf4j
    static class TestSecurityConfig {

        @Bean
        @Primary
        public JwtAuthenticationFilter testJwtAuthenticationFilter() {
            return new JwtAuthenticationFilter() {
                @Override
                protected void doFilterInternal(HttpServletRequest request,
                                                HttpServletResponse response,
                                                FilterChain filterChain)
                        throws ServletException, IOException {

                    String authHeader = request.getHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        String email = extractEmailFromToken(token);
                        if (email != null) {
                            var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
                            var authentication =
                                    new UsernamePasswordAuthenticationToken(email, null, authorities);
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        }
                    }

                    filterChain.doFilter(request, response);
                }

                private String extractEmailFromToken(String token) {
                    try {
                        String[] chunks = token.split("\\.");
                        String payload = new String(java.util.Base64.getUrlDecoder().decode(chunks[1]));
                        int subIndex = payload.indexOf("\"sub\":\"");
                        if (subIndex != -1) {
                            int start = subIndex + 7;
                            int end = payload.indexOf("\"", start);
                            return payload.substring(start, end);
                        }
                    } catch (Exception ignored) {}
                    return null;
                }
            };
        }
    }
}
