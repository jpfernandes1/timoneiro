package com.jompastech.backend.integration.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jompastech.backend.model.dto.payment.MockCardData;
import com.jompastech.backend.model.entity.Boat;
import com.jompastech.backend.model.entity.BoatAvailability;
import com.jompastech.backend.model.entity.Booking;
import com.jompastech.backend.model.entity.User;
import com.jompastech.backend.model.enums.BookingStatus;
import com.jompastech.backend.repository.*;
import com.jompastech.backend.security.dto.AuthRequestDTO;
import com.jompastech.backend.security.filter.JwtAuthenticationFilter;
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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.jompastech.backend.integration.controller.util.CpfGenerator.generateValidCpf;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookingControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BoatRepository boatRepository;

    @Autowired
    private BoatAvailabilityRepository boatAvailabilityRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private String jwtToken;
    private Long boatId;
    private Long availabilityId;
    private String userEmail;
    private User testUser;
    private Boat testBoat;
    private BoatAvailability testAvailability;

    // Dates for the tests (always in the future)
    private final LocalDateTime startDate = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0);
    private final LocalDateTime endDate = startDate.plusHours(4);
    String bookingStartStr = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    String bookingEndStr = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

    // Availability
    LocalDateTime availabilityStart = startDate.minusDays(1);
    LocalDateTime availabilityEnd = endDate.plusDays(2);

    String startDateStr = availabilityStart.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    String endDateStr = availabilityEnd.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

    // JSON to create a boat (without images)
    private final String boatJson = """
        {
          "name": "Boat for Booking Tests",
          "description": "Boat used in booking integration tests",
          "type": "LANCHA",
          "capacity": 8,
          "pricePerHour": 300,
          "cep": "12345-000",
          "number": "10",
          "street": "Test Street",
          "neighborhood": "Test Neighborhood",
          "city": "Test City",
          "state": "TS",
          "marina": "Test Marina"
        }
        """;

    @BeforeEach
    void setup() throws Exception {
        // 1. Creates a unique user and obtains a JWT token.
        this.userEmail = "booking_test_" + System.nanoTime() + "@boat.com";
        String validCpf = generateValidCpf();

        String userJson = String.format("""
            {
              "name": "Booking Test User",
              "email": "%s",
              "password": "asd@12345",
              "cpf": "%s",
              "phone": "11999999999"
            }
            """, userEmail, validCpf);

        // Registration
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isCreated());

        // Login
        String loginJson = String.format("""
            {
              "email": "%s",
              "password": "asd@12345"
            }
            """, userEmail);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String loginResponse = loginResult.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(loginResponse);
        this.jwtToken = root.get("token").asText();
        this.testUser = userRepository.findByEmail(userEmail).orElseThrow();

        // 2. Creates a boat (via API, authenticated)
        MockMultipartFile boatPart = new MockMultipartFile(
                "boat",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                boatJson.getBytes()
        );

        MvcResult boatCreateResult = mockMvc.perform(
                        multipart("/api/boats")
                                .file(boatPart)
                                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isCreated())
                .andReturn();

        String boatResponse = boatCreateResult.getResponse().getContentAsString();
        this.boatId = ((Number) JsonPath.read(boatResponse, "$.id")).longValue();
        this.testBoat = boatRepository.findById(boatId).orElseThrow();

        // 3. Creates availability for the boat (via API, authenticated)
        String availabilityJson = String.format("""
            {
                "startDate": "%s",
                "endDate": "%s",
                "pricePerHour": 250.00
            }
            """, startDateStr, endDateStr);

        MvcResult availabilityResult = mockMvc.perform(post("/api/boats/{boatId}/availability", boatId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(availabilityJson))
                .andExpect(status().isOk())
                .andReturn();

        String availabilityResponse = availabilityResult.getResponse().getContentAsString();
        this.availabilityId = ((Number) JsonPath.read(availabilityResponse, "$.id")).longValue();
        this.testAvailability = boatAvailabilityRepository.findById(availabilityId).orElseThrow();
    }

    @AfterEach
    void cleanup() {
        // Correct order respecting the foreign keys: bookings → availabilities → boats → users
        paymentRepository.deleteAll();
        bookingRepository.deleteAll();
        boatAvailabilityRepository.deleteAll();
        boatRepository.deleteAll();
        userRepository.deleteAll();
    }

    // Creates a MockCardData with valid card details (payment approved).
    private MockCardData validCard() {
        MockCardData card = new MockCardData(
                "4111111111111111",
                "Jose da Silva",
                "03/2026",
                "123"
        );
        return card;
    }

    // Creates a MockCardData that forces payment rejection.
    private MockCardData refusedCard() {
        MockCardData card = new MockCardData(
                "4222222222222222",
                "Jose da Silva",
                "12/2026",
                "123"
        );
        return card;
    }

    // Convert MockCardData to JSON manually (simple)
    private String cardToJson(MockCardData card) throws Exception {
        return objectMapper.writeValueAsString(card);
    }

    // ----------------------------------------------------------------
    //  POST /api/bookings
    // ----------------------------------------------------------------

    @Test
    void shouldCreateBookingSuccessfully() throws Exception {
        String bookingRequestJson = String.format("""
            {
                "boatId": %d,
                "startDate": "%s",
                "endDate": "%s",
                "paymentMethod": "CREDIT_CARD",
                "mockCardData": %s
            }
            """, boatId, bookingStartStr, bookingEndStr, cardToJson(validCard()));

        MvcResult result = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingRequestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.boat.id").value(boatId))
                .andExpect(jsonPath("$.user.id").value(testUser.getId()))
                .andExpect(jsonPath("$.startDate").value(bookingStartStr))
                .andExpect(jsonPath("$.endDate").value(bookingEndStr))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalPrice").value(1000.00))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        Long bookingId = ((Number) JsonPath.read(response, "$.id")).longValue();

        // Check at the bank
        Booking saved = bookingRepository.findById(bookingId).orElseThrow();
        assertEquals(BookingStatus.CONFIRMED, saved.getStatus());
        assertEquals(new BigDecimal("1000.00"), saved.getTotalPrice());
    }

    @Test
    void shouldReturn400WhenInvalidBookingRequest() throws Exception {
        // Start date after end date
        String invalidDatesJson = String.format("""
            {
                "boatId": %d,
                "startDate": "%s",
                "endDate": "%s",
                "paymentMethod": "CREDIT_CARD",
                "mockCardData": %s
            }
            """, boatId, endDateStr, startDateStr, cardToJson(validCard()));

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidDatesJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn401WhenNoTokenProvided() throws Exception {
        String bookingRequestJson = String.format("""
            {
                "boatId": %d,
                "startDate": "%s",
                "endDate": "%s",
                "paymentMethod": "CREDIT_CARD",
                "mockCardData": %s
            }
            """, boatId, startDateStr, endDateStr, cardToJson(validCard()));

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingRequestJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn404WhenBoatNotFound() throws Exception {
        Long nonExistentBoatId = 99999L;

        String bookingRequestJson = String.format("""
            {
                "boatId": %d,
                "startDate": "%s",
                "endDate": "%s",
                "paymentMethod": "CREDIT_CARD",
                "mockCardData": %s
            }
            """, nonExistentBoatId, bookingStartStr, bookingEndStr, cardToJson(validCard()));

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingRequestJson))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn409WhenBoatNotAvailable() throws Exception {
        // 1. Create a successful booking.
        String bookingRequestJson = String.format("""
            {
                "boatId": %d,
                "startDate": "%s",
                "endDate": "%s",
                "paymentMethod": "CREDIT_CARD",
                "mockCardData": %s
            }
            """, boatId, bookingStartStr, bookingEndStr, cardToJson(validCard()));

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingRequestJson))
                .andExpect(status().isCreated());

        // 2. Try creating another booking with an overlapping period (within the same time frame).
        String overlappingRequestJson = String.format("""
            {
                "boatId": %d,
                "startDate": "%s",
                "endDate": "%s",
                "paymentMethod": "CREDIT_CARD",
                "mockCardData": %s
            }
            """, boatId, startDateStr, endDateStr, cardToJson(validCard()));

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(overlappingRequestJson))
                .andExpect(status().isConflict());   // 409 Conflict
    }

    @Test
    void shouldReturn402WhenPaymentFails() throws Exception {
        String bookingRequestJson = String.format("""
            {
                "boatId": %d,
                "startDate": "%s",
                "endDate": "%s",
                "paymentMethod": "CREDIT_CARD",
                "mockCardData": %s
            }
            """, boatId, bookingStartStr, bookingEndStr, cardToJson(refusedCard()));

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingRequestJson))
                .andExpect(status().isPaymentRequired()); // 402
    }

    // ----------------------------------------------------------------
    //  GET /api/bookings/my-bookings
    // ----------------------------------------------------------------

    @Test
    void shouldGetMyBookings() throws Exception {

        // Create second availability
        String nextDayAvailabilityStartStr = availabilityStart.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String nextDayAvailabilityEndStr = availabilityEnd.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String availabilityJson = String.format("""
    {
        "startDate": "%s",
        "endDate": "%s",
        "pricePerHour": 250.00
    }
    """, nextDayAvailabilityStartStr, nextDayAvailabilityEndStr);

        mockMvc.perform(post("/api/boats/{boatId}/availability", boatId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(availabilityJson))
                .andExpect(status().isOk());

        // Creates two bookings for the user.
        createBookingForUser(boatId, startDate, endDate, validCard());
        createBookingForUser(boatId, startDate.plusDays(1), endDate.plusDays(1), validCard());

        mockMvc.perform(get("/api/bookings/my-bookings")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].user.id").value(testUser.getId()))
                .andExpect(jsonPath("$.content[1].user.id").value(testUser.getId()));
    }

    @Test
    void shouldReturn401WhenGetMyBookingsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/bookings/my-bookings"))
                .andExpect(status().isUnauthorized());
    }

    // ----------------------------------------------------------------
    //  POST /api/bookings/{bookingId}/cancel (not implemented)
    // ----------------------------------------------------------------

    @Test
    void shouldReturn501ForCancelBooking() throws Exception {
        // Create a booking to have a valid ID.
        Long bookingId = createBookingForUser(boatId, startDate, endDate, validCard());

        mockMvc.perform(post("/api/bookings/{bookingId}/cancel", bookingId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotImplemented());  // 501
    }

    @Test
    void shouldGetBookingByIdWhenUserIsBookingOwner() throws Exception {
        Long bookingId = createBookingForUser(boatId, startDate, endDate, validCard());

        mockMvc.perform(get("/api/bookings/{bookingId}", bookingId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookingId))
                .andExpect(jsonPath("$.user.id").value(testUser.getId()))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void shouldGetBookingByIdWhenUserIsBoatOwner() throws Exception {
        // Creates a second user (tenant).
        String renterEmail = "renter_" + System.nanoTime() + "@boat.com";
        createUser(renterEmail);
        String renterToken = doLogin(renterEmail, "asd@12345");

        // Tenant makes the reservation.
        Long bookingId = createBookingForUserWithToken(boatId, startDate, endDate, validCard(), renterToken);

        // The boat owner (testUser) tries to access
        mockMvc.perform(get("/api/bookings/{bookingId}", bookingId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookingId))
                .andExpect(jsonPath("$.user.email").value(renterEmail));
    }

    @Test
    void shouldReturn404WhenBookingNotFound() throws Exception {
        mockMvc.perform(get("/api/bookings/{bookingId}", 99999L)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403WhenUserIsNotAuthorized() throws Exception {
        // Creates a third user (intruder).
        String intruderEmail = "intruder_" + System.nanoTime() + "@boat.com";
        createUser(intruderEmail);
        String intruderToken = doLogin(intruderEmail, "asd@12345");

        // Create a reservation using the default user (reservation owner).
        Long bookingId = createBookingForUser(boatId, startDate, endDate, validCard());

        // Intruder attempts to access
        mockMvc.perform(get("/api/bookings/{bookingId}", bookingId)
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn401WhenAuthenticatedUserNotFound() throws Exception {
        // 1. Creates a temporary user (no boat, no reservations)
        String tempEmail = "temp_" + System.nanoTime() + "@boat.com";
        String validCpf = generateValidCpf();
        String userJson = String.format("""
        {
            "name": "Temp User",
            "email": "%s",
            "password": "asd@12345",
            "cpf": "%s",
            "phone": "11999999999"
        }
        """, tempEmail, validCpf);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isCreated());

        // 2. Log in to obtain the JWT token.
        String tempToken = doLogin(tempEmail, "asd@12345");

        // 3. Delete the user from the database (there are no pending foreign keys).
        User tempUser = userRepository.findByEmail(tempEmail).orElseThrow();
        userRepository.delete(tempUser);
        userRepository.flush();

        // 4. Request with valid token, but non-existent user → 404
        mockMvc.perform(get("/api/bookings/my-bookings")
                        .header("Authorization", "Bearer " + tempToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldGetMyBookingsWithStatusFilter() throws Exception {
        // Create a CONFIRMED reservation (card approved)
        Long bookingId = createBookingForUser(boatId, startDate, endDate, validCard());

        mockMvc.perform(get("/api/bookings/my-bookings")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("status", "CONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(bookingId))
                .andExpect(jsonPath("$.content[0].status").value("CONFIRMED"));
    }

    // ----------------------------------------------------------------
    //  Auxiliary Method: Creates a booking and returns the ID.
    // ----------------------------------------------------------------

    /**
    * Log in and return the JWT token.
    */
    private String doLogin(String email, String password) throws Exception {
        AuthRequestDTO authRequest = new AuthRequestDTO();
        authRequest.setEmail(email);
        authRequest.setPassword(password);
        String authResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(authResponse).get("token").asText();
    }

    /**
     * Creates a new user with a default email and password.
     */
    private void createUser(String email) throws Exception {
        String validCpf = generateValidCpf();
        String userJson = String.format("""
        {
            "name": "Test User",
            "email": "%s",
            "password": "asd@12345",
            "cpf": "%s",
            "phone": "11999999999"
        }
        """, email, validCpf);
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isCreated());
    }

    /**
     * Creates a reservation using a specific token (useful for scenarios with multiple users).
     */
    private Long createBookingForUserWithToken(Long boatId, LocalDateTime start, LocalDateTime end,
                                               MockCardData card, String token) throws Exception {
        String startStr = start.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String endStr = end.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String requestJson = String.format("""
        {
            "boatId": %d,
            "startDate": "%s",
            "endDate": "%s",
            "paymentMethod": "CREDIT_CARD",
            "mockCardData": %s
        }
        """, boatId, startStr, endStr, cardToJson(card));

        MvcResult result = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private Long createBookingForUser(Long boatId, LocalDateTime start, LocalDateTime end,
                                      MockCardData card) throws Exception {
        String startStr = start.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String endStr = end.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        String requestJson = String.format("""
            {
                "boatId": %d,
                "startDate": "%s",
                "endDate": "%s",
                "paymentMethod": "CREDIT_CARD",
                "mockCardData": %s
            }
            """, boatId, startStr, endStr, cardToJson(card));

        MvcResult result;
        if (card.getCardNumber().equals("4000000000000002")) {
            // Payment declined → returns a 402 error, but the booking is created and then canceled.
            result = mockMvc.perform(post("/api/bookings")
                            .header("Authorization", "Bearer " + jwtToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isPaymentRequired())
                    .andReturn();
        } else {
            result = mockMvc.perform(post("/api/bookings")
                            .header("Authorization", "Bearer " + jwtToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isCreated())
                    .andReturn();
        }

        String response = result.getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    // ==============================================
    // TEST CONFIGURATION FOR SECURITY (mesma do BoatAvailabilityControllerIT)
    // ==============================================
    private static final ThreadLocal<String> currentUserEmail = new ThreadLocal<>();
    private static final ThreadLocal<List<String>> currentUserRoles = new ThreadLocal<>();

    public static void setCurrentUser(String email, String role) {
        currentUserEmail.set(email);
        currentUserRoles.set(List.of(role));
    }

    public static void setCurrentUser(String email, List<String> roles) {
        currentUserEmail.set(email);
        currentUserRoles.set(roles);
    }

    public static void clearCurrentUser() {
        currentUserEmail.remove();
        currentUserRoles.remove();
    }

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

                    // 1. If a test context has been explicitly defined, use it.
                    String testEmail = currentUserEmail.get();
                    List<String> testRoles = currentUserRoles.get();

                    if (testEmail != null) {
                        List<GrantedAuthority> authorities = testRoles.stream()
                                .map(SimpleGrantedAuthority::new)
                                .map(a -> (GrantedAuthority) a)
                                .toList();

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(testEmail, null, authorities);

                        SecurityContext context = SecurityContextHolder.createEmptyContext();
                        context.setAuthentication(authentication);
                        SecurityContextHolder.setContext(context);

                        filterChain.doFilter(request, response);
                        return;
                    }

                    // 2. Otherwise, it attempts to extract the email from the token (real behavior).
                    String authHeader = request.getHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        try {
                            String email = extractEmailFromToken(token); // dummy helper method
                            if (email != null) {
                                // Regular user for testing (ROLE_USER)
                                List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
                                UsernamePasswordAuthenticationToken authentication =
                                        new UsernamePasswordAuthenticationToken(email, null, authorities);
                                SecurityContextHolder.getContext().setAuthentication(authentication);
                            }
                        } catch (Exception e) {
                            log.warn("Failed to process token in test, using fallback.");
                        }
                    }

                    filterChain.doFilter(request, response);
                }

                private String extractEmailFromToken(String token) {
                    // Simplified implementation for testing - extracts the subject from the JWT without verifying the signature.
                    try {
                        String[] chunks = token.split("\\.");
                        if (chunks.length > 1) {
                            String payload = new String(java.util.Base64.getUrlDecoder().decode(chunks[1]));
                            // Search for the "sub" field
                            int subIndex = payload.indexOf("\"sub\":\"");
                            if (subIndex != -1) {
                                int start = subIndex + 7;
                                int end = payload.indexOf("\"", start);
                                return payload.substring(start, end);
                            }
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                    return null;
                }
            };
        }
    }

}