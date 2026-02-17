package com.jompastech.backend.integration.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jompastech.backend.model.dto.BoatAvailabilityRequestDTO;
import com.jompastech.backend.model.dto.BoatAvailabilityResponseDTO;
import com.jompastech.backend.model.entity.Boat;
import com.jompastech.backend.model.entity.BoatAvailability;
import com.jompastech.backend.model.entity.User;
import com.jompastech.backend.repository.BoatAvailabilityRepository;
import com.jompastech.backend.repository.BoatRepository;
import com.jompastech.backend.repository.UserRepository;
import com.jompastech.backend.security.dto.AuthRequestDTO;
import com.jompastech.backend.security.filter.JwtAuthenticationFilter;
import com.jompastech.backend.service.BoatAvailabilityService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
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
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(BoatAvailabilityControllerIT.TestServiceConfig.class)
class BoatAvailabilityControllerIT {

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

    private String jwtToken;
    private Long boatId;
    private String userEmail;
    private User testUser;
    private Boat testBoat;

    // Boat JSON for creation
    private final String boatJson = """
        {
          "name": "Test Boat for Availability",
          "description": "Boat for availability testing",
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
        // Create a unique user for each test
        this.userEmail = "availability_test_" + System.nanoTime() + "@boat.com";
        String validCpf = generateValidCpf();

        // User registration JSON
        String userJson = String.format("""
        {
          "name": "Availability Test User",
          "email": "%s",
          "password": "asd@12345",
          "cpf": "%s",
          "phone": "11999999999"
        }
        """, userEmail, validCpf);

        // Register user via API
        MvcResult registerResult = mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isCreated())
                .andReturn();

        // Login to get JWT token
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

        // Create a boat for availability testing (without images for simplicity)
        MockMultipartFile boatPart = new MockMultipartFile(
                "boat",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                boatJson.getBytes()
        );

        // Create boat via API
        MvcResult boatCreateResult = mockMvc.perform(
                        multipart("/api/boats")
                                .file(boatPart)
                                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isCreated())
                .andReturn();

        String boatResponse = boatCreateResult.getResponse().getContentAsString();
        this.boatId = ((Number) JsonPath.read(boatResponse, "$.id")).longValue();
        this.testBoat = boatRepository.findById(boatId).orElseThrow();
        this.testUser = userRepository.findByEmail(userEmail).orElseThrow();
    }

    @AfterEach
    void cleanup() {
        // Clean up in reverse order to avoid foreign key constraints
        boatAvailabilityRepository.deleteAll();
        boatRepository.deleteAll();
        userRepository.deleteAll();
    }

    /**
     * Generates a valid CPF number for testing purposes
     */
    private String generateValidCpf() {
        int[] cpf = new int[11];
        for (int i = 0; i < 9; i++) cpf[i] = (int) (Math.random() * 10);
        cpf[9] = calculateCpfDigit(cpf, 9);
        cpf[10] = calculateCpfDigit(cpf, 10);
        StringBuilder sb = new StringBuilder();
        for (int d : cpf) sb.append(d);
        return sb.toString();
    }

    /**
     * Calculates a CPF digit for validation
     */
    private int calculateCpfDigit(int[] cpf, int length) {
        int sum = 0, weight = length + 1;
        for (int i = 0; i < length; i++) sum += cpf[i] * weight--;
        int mod = (sum * 10) % 11;
        return mod == 10 ? 0 : mod;
    }

    /**
     * Performs login and returns JWT token
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

    // ===========================
    // CREATE AVAILABILITY TESTS
    // ===========================

    @Test
    void shouldCreateAvailabilitySuccessfully() throws Exception {
        String requestJson = """
            {
                "startDate": "2024-12-01T10:00:00",
                "endDate": "2024-12-01T14:00:00",
                "pricePerHour": 150.50
            }
            """;

        MvcResult result = mockMvc.perform(post("/api/boats/{boatId}/availability", boatId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.boatId").value(boatId))
                .andExpect(jsonPath("$.startDate").value("2024-12-01T10:00:00"))
                .andExpect(jsonPath("$.endDate").value("2024-12-01T14:00:00"))
                .andExpect(jsonPath("$.pricePerHour").value(150.50))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        Long availabilityId = ((Number) JsonPath.read(response, "$.id")).longValue();
        assertNotNull(availabilityId);

        // Verify it was saved in database
        assertTrue(boatAvailabilityRepository.existsById(availabilityId));
    }

    @Test
    void shouldReturnNotFoundWhenCreateAvailabilityForNonExistentBoat() throws Exception {
        String requestJson = """
            {
                "startDate": "2024-12-01T10:00:00",
                "endDate": "2024-12-01T14:00:00",
                "pricePerHour": 150.50
            }
            """;

        mockMvc.perform(post("/api/boats/{boatId}/availability", 999999L)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnBadRequestWhenCreateAvailabilityWithInvalidDates() throws Exception {
        // End date before start date
        String requestJson = """
            {
                "startDate": "2024-12-01T14:00:00",
                "endDate": "2024-12-01T10:00:00",
                "pricePerHour": 150.50
            }
            """;

        mockMvc.perform(post("/api/boats/{boatId}/availability", boatId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenCreateAvailabilityWithMissingRequiredFields() throws Exception {
        // Missing pricePerHour
        String requestJson = """
            {
                "startDate": "2024-12-01T10:00:00",
                "endDate": "2024-12-01T14:00:00"
            }
            """;

        mockMvc.perform(post("/api/boats/{boatId}/availability", boatId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnInternalServerErrorWhenCreateAvailabilityWithInvalidJson() throws Exception {
        String invalidJson = "{ invalid json }";

        mockMvc.perform(post("/api/boats/{boatId}/availability", boatId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isInternalServerError());
    }

    // ===========================
    // GET AVAILABILITIES TESTS
    // ===========================

    @Test
    void shouldGetAllAvailabilitiesForBoat() throws Exception {
        // Create multiple availabilities
        BoatAvailability availability1 = new BoatAvailability();
        availability1.setBoat(testBoat);
        availability1.setStartDate(LocalDateTime.parse("2024-12-01T10:00:00"));
        availability1.setEndDate(LocalDateTime.parse("2024-12-01T14:00:00"));
        availability1.setPricePerHour(new BigDecimal(150));
        boatAvailabilityRepository.save(availability1);

        BoatAvailability availability2 = new BoatAvailability();
        availability2.setBoat(testBoat);
        availability2.setStartDate(LocalDateTime.parse("2024-12-02T09:00:00"));
        availability2.setEndDate(LocalDateTime.parse("2024-12-02T17:00:00"));
        availability2.setPricePerHour(new BigDecimal(200));
        boatAvailabilityRepository.save(availability2);

        mockMvc.perform(get("/api/boats/{boatId}/availability", boatId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].boatId").value(boatId))
                .andExpect(jsonPath("$[1].boatId").value(boatId));
    }

    @Test
    void shouldReturnEmptyListWhenNoAvailabilitiesForBoat() throws Exception {
        mockMvc.perform(get("/api/boats/{boatId}/availability", boatId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldGetAvailabilityById() throws Exception {
        // Create an availability first
        BoatAvailability availability = new BoatAvailability();
        availability.setBoat(testBoat);
        availability.setStartDate(LocalDateTime.parse("2024-12-01T10:00:00"));
        availability.setEndDate(LocalDateTime.parse("2024-12-01T14:00:00"));
        availability.setPricePerHour(new BigDecimal(150));
        BoatAvailability saved = boatAvailabilityRepository.save(availability);

        mockMvc.perform(get("/api/boats/{boatId}/availability/{id}", boatId, saved.getId())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.boatId").value(boatId))
                .andExpect(jsonPath("$.startDate").value("2024-12-01T10:00:00"))
                .andExpect(jsonPath("$.endDate").value("2024-12-01T14:00:00"))
                .andExpect(jsonPath("$.pricePerHour").value(150.0));
    }

    @Test
    void shouldReturnNotFoundWhenGetNonExistentAvailability() throws Exception {
        mockMvc.perform(get("/api/boats/{boatId}/availability/{id}", boatId, 99999L)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotFound());
    }

    // ===========================
    // UPDATE AVAILABILITY TESTS
    // ===========================

    @Test
    void shouldUpdateAvailabilitySuccessfully() throws Exception {
        // Create an availability first
        BoatAvailability availability = new BoatAvailability();
        availability.setBoat(testBoat);
        availability.setStartDate(LocalDateTime.parse("2024-12-01T10:00:00"));
        availability.setEndDate(LocalDateTime.parse("2024-12-01T14:00:00"));
        availability.setPricePerHour(new BigDecimal(150));
        BoatAvailability saved = boatAvailabilityRepository.save(availability);

        String updateJson = """
            {
                "startDate": "2024-12-02T09:00:00",
                "endDate": "2024-12-02T18:00:00",
                "pricePerHour": 180.75
            }
            """;

        mockMvc.perform(put("/api/boats/{boatId}/availability/{id}", boatId, saved.getId())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.boatId").value(boatId))
                .andExpect(jsonPath("$.startDate").value("2024-12-02T09:00:00"))
                .andExpect(jsonPath("$.endDate").value("2024-12-02T18:00:00"))
                .andExpect(jsonPath("$.pricePerHour").value(180.75));
    }

    @Test
    void shouldReturnNotFoundWhenUpdateNonExistentAvailability() throws Exception {
        String updateJson = """
            {
                "startDate": "2024-12-02T09:00:00",
                "endDate": "2024-12-02T18:00:00",
                "pricePerHour": 180.75
            }
            """;

        mockMvc.perform(put("/api/boats/{boatId}/availability/{id}", boatId, 99999L)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isNotFound());
    }

    // ===========================
    // DELETE AVAILABILITY TESTS
    // ===========================

    @Test
    void shouldDeleteAvailabilitySuccessfully() throws Exception {
        // Create an availability first
        BoatAvailability availability = new BoatAvailability();
        availability.setBoat(testBoat);
        availability.setStartDate(LocalDateTime.parse("2024-12-01T10:00:00"));
        availability.setEndDate(LocalDateTime.parse("2024-12-01T14:00:00"));
        availability.setPricePerHour(new BigDecimal(150));
        BoatAvailability saved = boatAvailabilityRepository.save(availability);

        mockMvc.perform(delete("/api/boats/{boatId}/availability/{id}", boatId, saved.getId())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());

        // Verify it was deleted
        assertFalse(boatAvailabilityRepository.existsById(saved.getId()));
    }

    @Test
    void shouldReturnNotFoundWhenDeleteNonExistentAvailability() throws Exception {
        mockMvc.perform(delete("/api/boats/{boatId}/availability/{id}", boatId, 99999L)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotFound());
    }

    // ===========================
    // CHECK AVAILABILITY TESTS
    // ===========================

    @Test
    void shouldCheckAvailabilityWhenBoatIsAvailable() throws Exception {
        // Create availability for a specific time
        BoatAvailability availability = new BoatAvailability();
        availability.setBoat(testBoat);
        availability.setStartDate(LocalDateTime.parse("2024-12-01T10:00:00"));
        availability.setEndDate(LocalDateTime.parse("2024-12-01T14:00:00"));
        availability.setPricePerHour(new BigDecimal(150));
        boatAvailabilityRepository.save(availability);

        // Check for a different time when boat should be available
        String startDate = "2024-12-02T10:00:00";
        String endDate = "2024-12-02T14:00:00";

        mockMvc.perform(get("/api/boats/{boatId}/availability/check-availability", boatId)
                        .param("startDate", startDate)
                        .param("endDate", endDate)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void shouldCheckAvailabilityWhenBoatIsNotAvailable() throws Exception {
        // Create availability for a specific time
        BoatAvailability availability = new BoatAvailability();
        availability.setBoat(testBoat);
        availability.setStartDate(LocalDateTime.parse("2024-12-01T10:00:00"));
        availability.setEndDate(LocalDateTime.parse("2024-12-01T14:00:00"));
        availability.setPricePerHour(new BigDecimal(300));
        boatAvailabilityRepository.save(availability);

        // Check for overlapping time when boat should not be available
        String startDate = "2024-12-01T12:00:00";  // Overlaps with existing
        String endDate = "2024-12-01T16:00:00";

        mockMvc.perform(get("/api/boats/{boatId}/availability/check-availability", boatId)
                        .param("startDate", startDate)
                        .param("endDate", endDate)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void shouldReturnBadRequestWhenCheckAvailabilityWithInvalidDates() throws Exception {
        // End date before start date
        String startDate = "2024-12-01T14:00:00";
        String endDate = "2024-12-01T10:00:00";

        mockMvc.perform(get("/api/boats/{boatId}/availability/check-availability", boatId)
                        .param("startDate", startDate)
                        .param("endDate", endDate)
                        .header("Authorization", "Bearer " + jwtToken))

                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnInternalServerErrorWhenCheckAvailabilityWithMissingParameters() throws Exception {
        // Missing endDate parameter
        String startDate = "2024-12-01T10:00:00";

        mockMvc.perform(get("/api/boats/{boatId}/availability/check-availability", boatId)
                        .param("startDate", startDate)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isInternalServerError());
    }

    // ===========================
    // AUTHENTICATION TESTS
    // ===========================

    @Test
    void shouldReturnUnauthorizedWhenAccessingWithoutToken() throws Exception {
        String requestJson = """
            {
                "startDate": "2024-12-01T10:00:00",
                "endDate": "2024-12-01T14:00:00",
                "pricePerHour": 150.50
            }
            """;

        mockMvc.perform(post("/api/boats/{boatId}/availability", boatId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnNotFoundWhenAccessingWithInvalidToken() throws Exception {
        String requestJson = """
            {
                "startDate": "2024-12-01T10:00:00",
                "endDate": "2024-12-01T14:00:00",
                "pricePerHour": 150.50
            }
            """;

        mockMvc.perform(post("/api/boats/{boatId}/availability", boatId)
                        .header("Authorization", "Bearer invalid_token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnauthorized());


    }

    // ===========================
    // CONCURRENCY AND EDGE CASES
    // ===========================

    @Test
    void shouldHandleConcurrentAvailabilityCreation() throws Exception {
        // Create two availabilities with non-overlapping times
        String requestJson1 = """
            {
                "startDate": "2024-12-01T10:00:00",
                "endDate": "2024-12-01T12:00:00",
                "pricePerHour": 150.00
            }
            """;

        String requestJson2 = """
            {
                "startDate": "2024-12-01T13:00:00",
                "endDate": "2024-12-01T15:00:00",
                "pricePerHour": 150.00
            }
            """;

        // Create first availability
        mockMvc.perform(post("/api/boats/{boatId}/availability", boatId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson1))
                .andExpect(status().isOk());

        // Create second availability
        mockMvc.perform(post("/api/boats/{boatId}/availability", boatId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson2))
                .andExpect(status().isOk());

        // Verify both were created
        mockMvc.perform(get("/api/boats/{boatId}/availability", boatId)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldReturnInternalServerErrorWhenCreatingOverlappingAvailability() throws Exception {
        // Create first availability
        String requestJson1 = """
            {
                "startDate": "2024-12-01T10:00:00",
                "endDate": "2024-12-01T14:00:00",
                "pricePerHour": 150.00
            }
            """;

        mockMvc.perform(post("/api/boats/{boatId}/availability", boatId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson1))
                .andExpect(status().isOk());

        // Try to create overlapping availability
        String requestJson2 = """
            {
                "startDate": "2024-12-01T12:00:00",  // Overlaps with first
                "endDate": "2024-12-01T16:00:00",
                "pricePerHour": 150.00
            }
            """;

        mockMvc.perform(post("/api/boats/{boatId}/availability", boatId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson2))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void shouldCreateAvailabilityWithValidDateTimeFormats() throws Exception {
        String requestJson = """
            {
                "startDate": "2024-12-25T08:30:00",
                "endDate": "2024-12-25T17:45:00",
                "pricePerHour": 250.00
            }
            """;

        mockMvc.perform(post("/api/boats/{boatId}/availability", boatId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startDate").value("2024-12-25T08:30:00"))
                .andExpect(jsonPath("$.endDate").value("2024-12-25T17:45:00"))
                .andExpect(jsonPath("$.pricePerHour").value(250.00));
    }

    @Test
    void shouldUpdateAvailabilityWithoutOverlap() throws Exception {
        // Create two availabilities
        BoatAvailability availability1 = new BoatAvailability();
        availability1.setBoat(testBoat);
        availability1.setStartDate(LocalDateTime.parse("2024-12-01T10:00:00"));
        availability1.setEndDate(LocalDateTime.parse("2024-12-01T12:00:00"));
        availability1.setPricePerHour(new BigDecimal(150));
        BoatAvailability saved1 = boatAvailabilityRepository.save(availability1);

        BoatAvailability availability2 = new BoatAvailability();
        availability2.setBoat(testBoat);
        availability2.setStartDate(LocalDateTime.parse("2024-12-01T14:00:00"));
        availability2.setEndDate(LocalDateTime.parse("2024-12-01T16:00:00"));
        availability2.setPricePerHour(new BigDecimal(200));
        boatAvailabilityRepository.save(availability2);

        // Update first availability to a non-overlapping time
        String updateJson = """
            {
                "startDate": "2024-12-01T17:00:00",
                "endDate": "2024-12-01T19:00:00",
                "pricePerHour": 200.00
            }
            """;

        mockMvc.perform(put("/api/boats/{boatId}/availability/{id}", boatId, saved1.getId())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnForbiddenWhenUserIsNotAdminOrOwner() throws Exception {

        // -------------- Creating a new user ---------------------
        // Create a unique user for each test
        String nonOwnerEmail = "nonOwner@boat.com";
        String validCpf = generateValidCpf();

        // User registration JSON
        String nonOwnerUserJson = String.format("""
        {
          "name": "Non Owner User",
          "email": "%s",
          "password": "asd@12345",
          "cpf": "%s",
          "phone": "11999999999"
        }
        """, nonOwnerEmail, validCpf);

        // Register user via API
        MvcResult registerResult = mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nonOwnerUserJson))
                .andExpect(status().isCreated())
                .andReturn();

        // Login to get JWT token
        String notOwnerloginJson = String.format("""
        {
          "email": "%s",
          "password": "asd@12345"
        }
        """, nonOwnerEmail);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notOwnerloginJson))
                .andExpect(status().isOk())
                .andReturn();

        String loginResponse = loginResult.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(loginResponse);
        String nonOwnerJwtToken = root.get("token").asText();


        // Configure a REGULAR user (not an admin, not the owner) for this test.
        TestSecurityConfig.setCurrentUser(nonOwnerEmail, "ROLE_USER");

        try {
            String requestJson = """
            {
                "startDate": "2024-12-01T10:00:00",
                "endDate": "2024-12-01T14:00:00",
                "pricePerHour": 150.50
            }
            """;

            // Regular user should not be able to create availability for a boat they don't own
            mockMvc.perform(post("/api/boats/{boatId}/availability", boatId)
                            .header("Authorization", "Bearer " + nonOwnerJwtToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isForbidden());
        } finally {
            TestSecurityConfig.clearCurrentUser();
        }
    }

    @Test
    void shouldReturnInternalServerErrorWhenUpdateAvailabilityWithMissingPrice() throws Exception {
        // Creates a valid availability
        BoatAvailability availability = new BoatAvailability();
        availability.setBoat(testBoat);
        availability.setStartDate(LocalDateTime.parse("2024-12-01T10:00:00"));
        availability.setEndDate(LocalDateTime.parse("2024-12-01T14:00:00"));
        availability.setPricePerHour(BigDecimal.valueOf(150));
        BoatAvailability saved = boatAvailabilityRepository.save(availability);

        // JSON without pricePerHour → viola @Column(nullable = false)
        String updateJson = """
        {
            "startDate": "2024-12-02T09:00:00",
            "endDate": "2024-12-02T18:00:00"
        }
        """;

        mockMvc.perform(put("/api/boats/{boatId}/availability/{id}", boatId, saved.getId())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void shouldReturnInternalServerErrorWhenGetBoatAvailabilitiesFails() throws Exception {
        // Configure the service to throw an exception for this boatId.
        THROW_FOR_BOAT_ID.set(boatId);
        try {
            mockMvc.perform(get("/api/boats/{boatId}/availability", boatId)
                            .header("Authorization", "Bearer " + jwtToken))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$").doesNotExist()); // null body
        } finally {
            THROW_FOR_BOAT_ID.remove();
        }
    }

    @Test
    void shouldReturnInternalServerErrorWhenGetAvailabilityByIdFails() throws Exception {
        // First, create a real availability.
        BoatAvailability availability = new BoatAvailability();
        availability.setBoat(testBoat);
        availability.setStartDate(LocalDateTime.parse("2024-12-01T10:00:00"));
        availability.setEndDate(LocalDateTime.parse("2024-12-01T14:00:00"));
        availability.setPricePerHour(new BigDecimal(150));
        BoatAvailability saved = boatAvailabilityRepository.save(availability);

        // Set an exception for this ID.
        THROW_FOR_AVAILABILITY_ID.set(saved.getId());
        try {
            mockMvc.perform(get("/api/boats/{boatId}/availability/{id}", boatId, saved.getId())
                            .header("Authorization", "Bearer " + jwtToken))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$").doesNotExist());
        } finally {
            THROW_FOR_AVAILABILITY_ID.remove();
        }
    }

    @Test
    void shouldReturnInternalServerErrorWhenUpdateAvailabilityFails() throws Exception {
        // Cria availability
        BoatAvailability availability = new BoatAvailability();
        availability.setBoat(testBoat);
        availability.setStartDate(LocalDateTime.parse("2024-12-01T10:00:00"));
        availability.setEndDate(LocalDateTime.parse("2024-12-01T14:00:00"));
        availability.setPricePerHour(new BigDecimal(150));
        BoatAvailability saved = boatAvailabilityRepository.save(availability);

        String updateJson = """
        {
            "startDate": "2024-12-02T09:00:00",
            "endDate": "2024-12-02T18:00:00",
            "pricePerHour": 180.75
        }
        """;

        THROW_FOR_AVAILABILITY_ID.set(saved.getId());
        try {
            mockMvc.perform(put("/api/boats/{boatId}/availability/{id}", boatId, saved.getId())
                            .header("Authorization", "Bearer " + jwtToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$").doesNotExist());
        } finally {
            THROW_FOR_AVAILABILITY_ID.remove();
        }
    }


    @TestConfiguration
    static class TestSecurityConfig {

        // ThreadLocal to store the specific security context of the test
        private static final ThreadLocal<SecurityContext> currentSecurityContext = new ThreadLocal<>();

        public static void setCurrentUser(String email, String role) {
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            List.of(new SimpleGrantedAuthority(role))
                    );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            currentSecurityContext.set(context);
        }

        public static void setCurrentUser(String email, List<String> roles) {
            List<GrantedAuthority> authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            authorities
                    );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            currentSecurityContext.set(context);
        }

        public static void clearCurrentUser() {
            currentSecurityContext.remove();
        }

        @Bean
        @Primary
        public JwtAuthenticationFilter testJwtAuthenticationFilter() {
            return new JwtAuthenticationFilter() {
                @Override
                protected void doFilterInternal(HttpServletRequest request,
                                                HttpServletResponse response,
                                                FilterChain filterChain)
                        throws ServletException, IOException {

                    // Check if we have a specific context for this test
                    SecurityContext context = currentSecurityContext.get();

                    if (context != null) {
                        // Use the specific context of the test
                        SecurityContextHolder.setContext(context);
                    } else {
                        // Default context (ADMIN for most tests)
                        String authHeader = request.getHeader("Authorization");

                        if (authHeader != null && authHeader.startsWith("Bearer ")) {
                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            "admin-test@boat.com",
                                            null,
                                            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    );

                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        }
                    }

                    filterChain.doFilter(request, response);
                }
            };
        }
    }

 // ==============================================
 // TEST CONFIGURATION FOR SIMULATING EXCEPTIONS
 // ==============================================

    private static final ThreadLocal<Long> THROW_FOR_BOAT_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> THROW_FOR_AVAILABILITY_ID = new ThreadLocal<>();

    @TestConfiguration
    static class TestServiceConfig {

        @Bean
        @Primary
        public BoatAvailabilityService testBoatAvailabilityService(
                BoatAvailabilityRepository boatAvailabilityRepository,
                BoatRepository boatRepository,
                UserRepository userRepository) {

            return new BoatAvailabilityService(boatAvailabilityRepository, boatRepository, userRepository) {

                @Override
                public List<BoatAvailabilityResponseDTO> findAvailabilityByBoatId(Long boatId) {
                    if (THROW_FOR_BOAT_ID.get() != null && THROW_FOR_BOAT_ID.get().equals(boatId)) {
                        throw new RuntimeException("Simulated exception in findAvailabilityByBoatId");
                    }
                    return super.findAvailabilityByBoatId(boatId);
                }

                @Override
                public BoatAvailabilityResponseDTO findById(Long id) {
                    if (THROW_FOR_AVAILABILITY_ID.get() != null && THROW_FOR_AVAILABILITY_ID.get().equals(id)) {
                        throw new RuntimeException("Simulated exception in findById");
                    }
                    return super.findById(id);
                }

                @Override
                @Transactional
                public BoatAvailabilityResponseDTO updateAvailability(Long id, BoatAvailabilityRequestDTO requestDTO) {
                    if (THROW_FOR_AVAILABILITY_ID.get() != null && THROW_FOR_AVAILABILITY_ID.get().equals(id)) {
                        throw new RuntimeException("Simulated exception in updateAvailability");
                    }
                    return super.updateAvailability(id, requestDTO);
                }

                @Override
                @Transactional
                public void deleteAvailability(Long id) {
                    if (THROW_FOR_AVAILABILITY_ID.get() != null && THROW_FOR_AVAILABILITY_ID.get().equals(id)) {
                        throw new RuntimeException("Simulated exception in deleteAvailability");
                    }
                    super.deleteAvailability(id);
                }
            };
        }
    }
}