package com.jompastech.backend.integration.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jompastech.backend.model.entity.User;
import com.jompastech.backend.repository.UserRepository;
import com.jompastech.backend.security.filter.JwtAuthenticationFilter;
import com.jompastech.backend.security.service.UserDetailsImpl;
import com.jompastech.backend.security.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.jompastech.backend.integration.controller.util.CpfGenerator.generateValidCpf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private String adminEmail;
    private String adminPassword;
    private String adminCpf;
    private String adminToken;
    private Long adminId;

    private String userEmail;
    private String userPassword;
    private String userCpf;
    private String userToken;
    private Long userId;

    // -----------------------------------------------------------------
    //  Helper class to serve as Principal with an 'id' property
    // -----------------------------------------------------------------
    static class PrincipalWithId {
        private final Long id;
        public PrincipalWithId(Long id) { this.id = id; }
        public Long getId() { return id; }
    }

    // -----------------------------------------------------------------
    //  Test-specific Security Configuration
    // -----------------------------------------------------------------
    @TestConfiguration
    @Slf4j
    static class TestSecurityConfig {

        @Autowired
        private UserDetailsService userDetailsService;

        @Autowired
        private JwtUtil jwtUtil;

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
                        try {
                            String email = jwtUtil.getEmail(token);
                            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                            if (userDetails instanceof UserDetailsImpl) {
                                UserDetailsImpl userDetailsImpl = (UserDetailsImpl) userDetails;
                                Long userId = userDetailsImpl.getId();
                                List<GrantedAuthority> authorities = new ArrayList<>(userDetails.getAuthorities());
                                UsernamePasswordAuthenticationToken authentication =
                                        new UsernamePasswordAuthenticationToken(userDetailsImpl, null, authorities);
                                SecurityContextHolder.getContext().setAuthentication(authentication);
                            }
                        } catch (Exception e) {
                            // ignore invalid token
                        }
                    }
                    filterChain.doFilter(request, response);
                }
            };
        }
    }


    // -----------------------------------------------------------------
    //  Setup and Cleanup
    // -----------------------------------------------------------------
    @BeforeEach
    void setup() throws Exception {
        // Create admin user via registration (role USER initially)
        adminEmail = "admin_" + System.nanoTime() + "@boat.com";
        adminPassword = "asd@12345";
        adminCpf = generateValidCpf();

        String adminJson = String.format("""
            {
                "name": "Admin User",
                "email": "%s",
                "password": "%s",
                "cpf": "%s",
                "phone": "11999999999"
            }
            """, adminEmail, adminPassword, adminCpf);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adminJson))
                .andExpect(status().isCreated());

        // Promote to ADMIN in database
        User admin = userRepository.findByEmail(adminEmail).orElseThrow();
        admin.setRole("ROLE_ADMIN");
        userRepository.save(admin);
        adminId = admin.getId();

        // Login to obtain a new token with ADMIN role
        String loginJson = String.format("""
            {
                "email": "%s",
                "password": "%s"
            }
            """, adminEmail, adminPassword);
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();
        String loginResponse = loginResult.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(loginResponse);
        adminToken = root.get("token").asText();

        // Create regular user via registration (role USER)
        userEmail = "user_" + System.nanoTime() + "@boat.com";
        userPassword = "User@123";
        userCpf = generateValidCpf();

        String regularUserJson = String.format("""
            {
                "name": "Common User",
                "email": "%s",
                "password": "%s",
                "cpf": "%s",
                "phone": "11999999999"
            }
            """, userEmail, userPassword, userCpf);

        MvcResult registerResult = mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(regularUserJson))
                .andExpect(status().isCreated())
                .andReturn();

        String registerResponse = registerResult.getResponse().getContentAsString();
        root = objectMapper.readTree(registerResponse);
        userToken = root.get("token").asText();
        userId = root.get("userId").asLong();
    }

    @AfterEach
    void cleanup() {
        userRepository.deleteAll();
    }

    // ----------------------------------------------------------------
    //  POST /api/users/register (public)
    // ----------------------------------------------------------------

    @Test
    @DisplayName("Public registration - should create user and return token")
    void register_Public_Success() throws Exception {
        String newEmail = "new_" + System.nanoTime() + "@boat.com";
        String newCpf = generateValidCpf();
        String requestJson = String.format("""
            {
                "name": "New User",
                "email": "%s",
                "password": "Senha@123",
                "cpf": "%s",
                "phone": "11988887777"
            }
            """, newEmail, newCpf);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.email").value(newEmail))
                .andExpect(jsonPath("$.name").value("New User"));
    }

    @Test
    @DisplayName("Registration - should return 400 for invalid input")
    void register_InvalidData_ReturnsBadRequest() throws Exception {
        String invalidJson = """
            {
                "name": "",
                "email": "not-an-email",
                "password": "123",
                "cpf": "123",
                "phone": ""
            }
            """;

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Registration - should return 409 when email already exists")
    void register_DuplicateEmail_ReturnsConflict() throws Exception {
        String duplicateEmailJson = String.format("""
            {
                "name": "Another User",
                "email": "%s",
                "password": "Senha@123",
                "cpf": "%s",
                "phone": "11999999999"
            }
            """, userEmail, generateValidCpf());

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateEmailJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already registered")));
    }

    @Test
    @DisplayName("Registration - should return 409 when CPF already exists")
    void register_DuplicateCpf_ReturnsConflict() throws Exception {
        String duplicateCpfJson = String.format("""
            {
                "name": "Another User",
                "email": "another_%s@boat.com",
                "password": "Senha@123",
                "cpf": "%s",
                "phone": "11999999999"
            }
            """, System.nanoTime(), userCpf);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateCpfJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already in use")));
    }

    // ----------------------------------------------------------------
    //  POST /api/users (admin only)
    // ----------------------------------------------------------------

    @Test
    @DisplayName("Create user (admin) - should return 201 with token")
    void createUser_AsAdmin_Success() throws Exception {
        String newEmail = "created_" + System.nanoTime() + "@boat.com";
        String newCpf = generateValidCpf();
        String requestJson = String.format("""
            {
                "name": "Created by Admin",
                "email": "%s",
                "password": "Senha@123",
                "cpf": "%s",
                "phone": "11988887777"
            }
            """, newEmail, newCpf);

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.email").value(newEmail));
    }

    @Test
    @DisplayName("Create user (no token) - should return 401")
    void createUser_NoToken_ReturnsUnauthorized() throws Exception {
        String requestJson = "{}";
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Create user (regular user token) - should return 403")
    void createUser_AsCommonUser_ReturnsForbidden() throws Exception {
        String newEmail = "created_" + System.nanoTime() + "@boat.com";
        String newCpf = generateValidCpf();
        String requestJson = String.format("""
            {
                "name": "Created by Admin",
                "email": "%s",
                "password": "Senha@123",
                "cpf": "%s",
                "phone": "11988887777"
            }
            """, newEmail, newCpf);
        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isForbidden());
    }

    // ----------------------------------------------------------------
    //  GET /api/users (admin only)
    // ----------------------------------------------------------------

    @Test
    @DisplayName("Get all users (admin) - should return list")
    void getAllUsers_AsAdmin_Success() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].email", containsInAnyOrder(adminEmail, userEmail)));
    }

    @Test
    @DisplayName("Get all users (regular user) - should return 403")
    void getAllUsers_AsCommonUser_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // ----------------------------------------------------------------
    //  GET /api/users/search?name= (admin only)
    // ----------------------------------------------------------------

    @Test
    @DisplayName("Search users by name (admin) - should return filtered list")
    void getUsersByName_AsAdmin_Success() throws Exception {
        mockMvc.perform(get("/api/users/search")
                        .param("name", "Common User")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", containsString("Common User")));
    }

    @Test
    @DisplayName("Search users by name (admin) - name not found returns empty list")
    void getUsersByName_NotFound_ReturnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/users/search")
                        .param("name", "NonExistentName")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ----------------------------------------------------------------
    //  GET /api/users/email/{email} (admin only)
    // ----------------------------------------------------------------

    @Test
    @DisplayName("Get user by email (admin) - success")
    void getUserByEmail_AsAdmin_Success() throws Exception {
        mockMvc.perform(get("/api/users/email/{email}", userEmail)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(userEmail))
                .andExpect(jsonPath("$.name").value("Common User"));
    }

    @Test
    @DisplayName("Get user by email (admin) - email not found returns 404")
    void getUserByEmail_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/users/email/{email}", "notfound@boat.com")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    // ----------------------------------------------------------------
    //  PUT /api/users/{id} (admin or own user)
    // ----------------------------------------------------------------

    @Test
    @DisplayName("Update user as self - success")
    void updateUser_AsSelf_Success() throws Exception {
        String updateJson = String.format("""
            {
                "name": "Updated Name",
                "email": "%s",
                "password": "NewPass@123",
                "cpf": "%s",
                "phone": "11911112222"
            }
            """, userEmail, userCpf);

        mockMvc.perform(put("/api/users/{id}", userId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.phone").value("11911112222"));
    }

    @Test
    @DisplayName("Update user as admin - success")
    void updateUser_AsAdmin_Success() throws Exception {
        String updateJson = String.format("""
            {
                "name": "Updated By Admin",
                "password": "asd@12345",
                "email": "%s",
                "cpf": "%s",
                "phone": "11933334444"
            }
            """, userEmail, userCpf);

        mockMvc.perform(put("/api/users/{id}", userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated By Admin"));
    }

    @Test
    @DisplayName("Update another user as regular user - should return 403")
    void updateUser_AsOtherUser_ReturnsForbidden() throws Exception {
        // Use a complete valid payload to pass validation
        String updateJson = String.format("""
            {
                "name": "Dummy Name",
                "email": "%s",
                "password": "SomePass@123",
                "cpf": "%s",
                "phone": "11999999999"
            }
            """, "dummy_" + System.nanoTime() + "@boat.com", generateValidCpf());

        mockMvc.perform(put("/api/users/{id}", adminId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Update user with email already used by another - should return 409")
    void updateUser_DuplicateEmail_ReturnsConflict() throws Exception {
        String updateJson = String.format("""
            {
                 "name": "Dummy Name",
                "email": "%s",
                "password": "SomePass@123",
                "cpf": "%s",
                "phone": "11999999999"
            }
            """, adminEmail, generateValidCpf());

        mockMvc.perform(put("/api/users/{id}", userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already registered")));
    }

    @Test
    @DisplayName("Update non-existent user - should return 404")
    void updateUser_NotFound_Returns404() throws Exception {
        String updateJson = String.format("""
            {
                "name": "Updated By Admin",
                "password": "asd@12345",
                "email": "%s",
                "cpf": "%s",
                "phone": "11933334444"
            }
            """, userEmail, userCpf);
        mockMvc.perform(put("/api/users/{id}", 99999L)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isNotFound());
    }

    // ----------------------------------------------------------------
    //  DELETE /api/users/{id} (admin only)
    // ----------------------------------------------------------------

    @Test
    @DisplayName("Delete user as admin - success (204)")
    void deleteUser_AsAdmin_Success() throws Exception {
        mockMvc.perform(delete("/api/users/{id}", userId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findById(userId)).isEmpty();
    }

    @Test
    @DisplayName("Delete user as regular user - should return 403")
    void deleteUser_AsCommonUser_ReturnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/users/{id}", userId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Delete non-existent user - should return 404")
    void deleteUser_NotFound_Returns404() throws Exception {
        mockMvc.perform(delete("/api/users/{id}", 99999L)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    // ----------------------------------------------------------------
    //  GET /api/users/profile (own user)
    // ----------------------------------------------------------------

    @Test
    @DisplayName("Get own profile - success")
    void getProfile_Authenticated_Success() throws Exception {
        mockMvc.perform(get("/api/users/profile")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(userEmail))
                .andExpect(jsonPath("$.name").value("Common User"));
    }

    @Test
    @DisplayName("Get profile without token - should return 401")
    void getProfile_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Get profile with valid token but user deleted - should return 401")
    void getProfile_UserDeleted_Returns404() throws Exception {
        userRepository.deleteById(userId);
        userRepository.flush();

        mockMvc.perform(get("/api/users/profile")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isUnauthorized());
    }

    // ----------------------------------------------------------------
    //  PUT /api/users/profile (own user)
    // ----------------------------------------------------------------

    @Test
    @DisplayName("Update own profile - success")
    void updateProfile_Authenticated_Success() throws Exception {
        String updateJson = String.format("""
            {
                "name": "Updated Profile Name",
                "password": "asd@12345",
                "email": "%s",
                "cpf": "%s",
                "phone": "11955556666"
            }
            """, userEmail, userCpf);

        mockMvc.perform(put("/api/users/profile")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Profile Name"))
                .andExpect(jsonPath("$.phone").value("11955556666"));
    }

    @Test
    @DisplayName("Update profile without token - returns 401")
    void updateProfile_Unauthenticated_Returns401() throws Exception {
        String updateJson = "{}";
        mockMvc.perform(put("/api/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Update profile with valid token but user deleted - returns 401")
    void updateProfile_UserDeleted_Returns401() throws Exception {
        userRepository.deleteById(userId);
        userRepository.flush();

        String updateJson = "{}";
        mockMvc.perform(put("/api/users/profile")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isUnauthorized());
    }
}