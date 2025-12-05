package com.jompastech.backend.security.service;

import com.jompastech.backend.exception.CpfAlreadyInUseException;
import com.jompastech.backend.exception.EmailAlreadyInUseException;
import com.jompastech.backend.mapper.UserMapper;
import com.jompastech.backend.model.dto.UserRequestDTO;
import com.jompastech.backend.model.entity.User;
import com.jompastech.backend.repository.UserRepository;
import com.jompastech.backend.security.dto.AuthRequestDTO;
import com.jompastech.backend.security.dto.AuthResponseDTO;
import com.jompastech.backend.security.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserMapper userMapper;

    /**
     * Authenticates a user and returns JWT token
     */
    public AuthResponseDTO authenticate(AuthRequestDTO authRequestDTO) {
        // Authenticate user using Spring Security
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authRequestDTO.getEmail(),
                        authRequestDTO.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Get authenticated user details
        String username = authentication.getName();
        User user = userRepository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new RuntimeException("User not found after authentication"));

        // Generate JWT token
        String jwtToken = jwtUtil.generateToken(user.getEmail());

        // Build response with user details and token
        return buildAuthResponse(user, jwtToken);
    }

    /**
     * Registers a new user and returns JWT token
     */
    public AuthResponseDTO register(UserRequestDTO userRequestDTO) {
        // Check if user already exists
        if (userRepository.existsByEmailIgnoreCase(userRequestDTO.getEmail())) {
            throw new EmailAlreadyInUseException(userRequestDTO.getEmail());
        }

        // Check if CPF already exists
        if (userRepository.existsByCpf(userRequestDTO.getCpf())) {
            throw new CpfAlreadyInUseException(userRequestDTO.getCpf());
        }

        // Create new user entity
        User user = userMapper.toEntity(userRequestDTO);
        user.setPassword(passwordEncoder.encode(userRequestDTO.getPassword()));
        user.setActive(true);
        user.setRole("ROLE_USER");

        // Save user to database
        User savedUser = userRepository.save(user);

        // Generate JWT token
        String jwtToken = jwtUtil.generateToken(savedUser.getEmail());

        // Build response with user details and token
        return buildAuthResponse(savedUser, jwtToken);
    }

    /**
     * Builds AuthResponseDTO from User entity and JWT token
     */
    private AuthResponseDTO buildAuthResponse(User user, String jwtToken) {
        AuthResponseDTO response = new AuthResponseDTO();
        response.setToken(jwtToken);
        response.setTokenType("Bearer");
        response.setUserId(user.getId());
        response.setEmail(user.getEmail());
        response.setName(user.getName());
        // ✅ NÃO inclua roles - segurança!

        return response;
    }

    /**
     * Validates user credentials
     */
    public boolean validateUserCredentials(String email, String password) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return passwordEncoder.matches(password, user.getPassword());
    }
}