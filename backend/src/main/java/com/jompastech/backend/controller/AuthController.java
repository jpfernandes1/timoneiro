package com.jompastech.backend.controller;

import com.jompastech.backend.model.dto.UserRequestDTO;
import com.jompastech.backend.security.dto.AuthRequestDTO;
import com.jompastech.backend.security.dto.AuthResponseDTO;
import com.jompastech.backend.security.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody AuthRequestDTO authRequest) {
        AuthResponseDTO response = authService.authenticate(authRequest);
        return ResponseEntity.ok(response);
    }

}