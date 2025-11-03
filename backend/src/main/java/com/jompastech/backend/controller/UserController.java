package com.jompastech.backend.controller;

import com.jompastech.backend.model.dto.UserRequestDTO;
import com.jompastech.backend.model.dto.UserResponseDTO;
import com.jompastech.backend.security.dto.AuthResponseDTO;
import com.jompastech.backend.security.service.UserDetailsImpl;
import com.jompastech.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // PUBLIC REGISTER - everyone can access
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody UserRequestDTO dto) {
        AuthResponseDTO response = userService.register(dto);
        return ResponseEntity.created(URI.create("/api/users/" + response.getUserId()))
                .body(response);
    }

    // CREATE ADMIN - apenas administradores
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')") // Only ADMINS can create new users
    public ResponseEntity<AuthResponseDTO> createUser(@Valid @RequestBody UserRequestDTO dto) {
        AuthResponseDTO createdUser = userService.register(dto);
        return ResponseEntity.created(URI.create("/api/users/" + createdUser.getUserId()))
                .body(createdUser);
    }

    // READ ALL - Only admins
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.findAllUsers());
    }

    // READ BY NAME - Only admins
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponseDTO>> getUsersByName(@RequestParam String name) {
        List<UserResponseDTO> users = userService.findUsersByName(name);
        return ResponseEntity.ok(users);
    }

    // Search for emailemail (útil para admin)
    @GetMapping("/email/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> getUserByEmail(@PathVariable String email) {
        UserResponseDTO user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }

    // UPDATE - User can update his own profile or an admin
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<UserResponseDTO> updateUser(@PathVariable Long id, @Valid @RequestBody UserRequestDTO dto) {
        UserResponseDTO updatedUser = userService.updateUser(id, dto);
        return ResponseEntity.ok(updatedUser);
    }

    // DELETE - Only admins
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // GET PROFILE - logged-in user can see own profile
    @GetMapping("/profile")
    public ResponseEntity<UserResponseDTO> getProfile() {
        UserResponseDTO profile = userService.getCurrentUserProfile();
        return ResponseEntity.ok(profile);
    }

    // UPDATE PROFILE - logged-in user can update own profile
    @PutMapping("/profile")
    public ResponseEntity<UserResponseDTO> updateProfile(@Valid @RequestBody UserRequestDTO dto) {
        UserResponseDTO updatedProfile = userService.updateCurrentUserProfile(dto);
        return ResponseEntity.ok(updatedProfile);
    }
}