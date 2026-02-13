package com.jompastech.backend.integration.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jompastech.backend.model.entity.User;
import com.jompastech.backend.repository.UserRepository;
import com.jompastech.backend.security.dto.AuthRequestDTO;
import com.jompastech.backend.security.service.AuthService;
import com.jompastech.backend.security.util.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static com.jompastech.backend.integration.controller.util.CpfGenerator.generateValidCpf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthService authService;

    private String validEmail;
    private String validPassword;
    private String validCpf;
    private String jwtToken;
    private Long userId;
    private User testUser;

    @BeforeEach
    void setup() throws Exception {
        validEmail = "auth_test_" + System.nanoTime() + "@boat.com";
        validPassword = "asd@12345";
        validCpf = generateValidCpf();

        // 1. Creates a user
        String userJson = String.format("""
            {
                "name": "Auth Test User",
                "email": "%s",
                "password": "%s",
                "cpf": "%s",
                "phone": "11999999999"
            }
            """, validEmail, validPassword, validCpf);

        MvcResult registerResult = mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isCreated())
                .andReturn();

        String registerResponse = registerResult.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(registerResponse);
        this.jwtToken = root.get("token").asText();
        this.userId = root.get("userId").asLong();
        this.testUser = userRepository.findById(userId).orElseThrow();
    }

    @AfterEach
    void cleanup() {
        userRepository.deleteAll();
    }

    // ----------------------------------------------------------------
    //  POST /api/auth/login
    // ----------------------------------------------------------------

    @Test
    @DisplayName("Login - must return 200 with valid credentials")
    void login_ValidCredentials_ReturnsToken() throws Exception {
        AuthRequestDTO request = new AuthRequestDTO();
        request.setEmail(validEmail);
        request.setPassword(validPassword);

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.email").value(validEmail))
                .andExpect(jsonPath("$.name").value("Auth Test User"));
    }

    @Test
    @DisplayName("Login - must return 401 with invalid password")
    void login_InvalidPassword_ReturnsUnauthorized() throws Exception {
        AuthRequestDTO request = new AuthRequestDTO();
        request.setEmail(validEmail);
        request.setPassword("wrongPassword");

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Login - must return 401 com inexistent email")
    void login_NonExistentEmail_ReturnsUnauthorized() throws Exception {
        AuthRequestDTO request = new AuthRequestDTO();
        request.setEmail("naoexiste@boat.com");
        request.setPassword(validPassword);

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Login - must return 400 for invalid request (ex: blank email)")
    void login_InvalidRequest_ReturnsBadRequest() throws Exception {
        String invalidJson = """
            {
                "email": "",
                "password": "123"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    // ----------------------------------------------------------------
    //  GET /api/auth/validate
    // ----------------------------------------------------------------

    @Test
    @DisplayName("Validate - must return 200 and token information when valid")
    void validateToken_ValidToken_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/auth/validate")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.email").value(validEmail))
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Validate - must return 400 when Authorization header is missing")
    void validateToken_MissingHeader_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/auth/validate"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Authorization header is required"));
    }

    @Test
    @DisplayName("Validate - must return 400 when header doesn't starts with Bearer")
    void validateToken_InvalidHeaderFormat_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/auth/validate")
                        .header("Authorization", "Basic abc123"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Authorization header is required"));
    }

    @Test
    @DisplayName("Validate - must return 401 when token is invalid")
    void validateToken_InvalidToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/validate")
                        .header("Authorization", "Bearer token.invalido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$").value(org.hamcrest.Matchers.containsString("Token is invalid or expired")));
    }

    @Test
    @DisplayName("Validate - must return 401 when the token is valid but user doesn't exist")
    void validateToken_ValidTokenButUserDeleted_ReturnsUnauthorized() throws Exception {
        // 1. Delete user
        userRepository.delete(testUser);
        userRepository.flush();

        // 2. Calls validate with token (stills valid, but the user doesn't exist anymore)
        mockMvc.perform(get("/api/auth/validate")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$").value(org.hamcrest.Matchers.containsString("User no longer exists")));
    }

    // ----------------------------------------------------------------
    //  GET /api/auth/me
    // ----------------------------------------------------------------

    @Test
    @DisplayName("Me - must return 200 and the authenticated user's data")
    void getCurrentUser_Authenticated_ReturnsUserInfo() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.name").value("Auth Test User"))
                .andExpect(jsonPath("$.email").value(validEmail));
    }

    @Test
    @DisplayName("Me - must return 401 when not authenticated")
    void getCurrentUser_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("AuthService - must return true to correct password")
    void validateUserCredentials_ValidPassword_ReturnsTrue() {
        boolean isValid = authService.validateUserCredentials(validEmail, validPassword);
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("AuthService - must return false to incorrect password")
    void validateUserCredentials_InvalidPassword_ReturnsFalse() {
        boolean isValid = authService.validateUserCredentials(validEmail, "wrong");
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("AuthService - must throw an exception for a non-existent email address.")
    void validateUserCredentials_NonExistentEmail_ThrowsException() {
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> authService.validateUserCredentials("naoexiste@boat.com", validPassword));
    }

    @Test
    @DisplayName("Must return a 409 error when the email is already in use.")
    void register_DuplicateEmail_ReturnsConflict() throws Exception {
        // Try registering with the same email already used in the setup.
        String duplicateEmailJson = String.format("""
            {
                "name": "Another User",
                "email": "%s",
                "password": "asd@12345",
                "cpf": "%s",
                "phone": "11999999999"
            }
            """, validEmail, generateValidCpf());

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateEmailJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already registered")));
    }

    @Test
    @DisplayName("Must return error 409 when CPF already exists.")
    void register_DuplicateCpf_ReturnsConflict() throws Exception {
        // Try to register a CPF already in use
        String duplicateCpfJson = String.format("""
            {
                "name": "Another User",
                "email": "outro_%s@boat.com",
                "password": "asd@12345",
                "cpf": "%s",
                "phone": "11999999999"
            }
            """, System.nanoTime(), validCpf);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateCpfJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already in use")));
    }

}