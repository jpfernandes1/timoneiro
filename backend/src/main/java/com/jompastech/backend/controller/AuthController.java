package com.jompastech.backend.controller;

import com.jompastech.backend.model.dto.UserRequestDTO;
import com.jompastech.backend.security.dto.AuthRequestDTO;
import com.jompastech.backend.security.dto.AuthResponseDTO;
import com.jompastech.backend.security.service.AuthService;
import com.jompastech.backend.security.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    @Autowired
    private AuthService authService;
    private final JwtUtil jwtUtil;

    public AuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody AuthRequestDTO authRequest) {
        AuthResponseDTO response = authService.authenticate(authRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Validates the current JWT token and returns user information.
     * This endpoint is used by the frontend to check if the token is still valid
     * and to get basic user information without requiring a full login.
     *
     * @param request HTTP request to extract the Authorization header
     * @return ResponseEntity with user information if token is valid
     */
    @GetMapping("/validate")
    @Operation(
            summary = "Validate JWT token",
            description = "Validates the current JWT token and returns user information if valid"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token is valid"),
            @ApiResponse(responseCode = "401", description = "Token is invalid or expired"),
            @ApiResponse(responseCode = "400", description = "Authorization header is missing or malformed")
    })
    public ResponseEntity<?> validateToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Authorization header missing or invalid");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Authorization header is required");
        }

        String token = authHeader.substring(7);

        try {
            // Validate token
            if (!jwtUtil.isValidToken(token)) {
                log.warn("Invalid token provided");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Token is invalid or expired");
            }

            // Extract user information from token
            String email = jwtUtil.getEmail(token);

            // Get authentication from context (if available)
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // Return basic user info
            Map<String, Object> response = new HashMap<>();
            response.put("valid", true);
            response.put("email", email);
            response.put("authenticated", authentication != null && authentication.isAuthenticated());
            response.put("timestamp", java.time.Instant.now().toString());

            log.debug("Token validated successfully for user: {}", email);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error validating token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Token validation failed: " + e.getMessage());
        }
    }

}