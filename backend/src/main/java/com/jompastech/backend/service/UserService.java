package com.jompastech.backend.service;

import com.jompastech.backend.exception.CpfAlreadyInUseException;
import com.jompastech.backend.exception.EmailAlreadyInUseException;
import com.jompastech.backend.exception.UserNotFoundException;
import com.jompastech.backend.exception.EntityNotFoundException;
import com.jompastech.backend.mapper.UserMapper;
import com.jompastech.backend.model.dto.UserRequestDTO;
import com.jompastech.backend.model.dto.UserResponseDTO;
import com.jompastech.backend.model.entity.User;
import com.jompastech.backend.repository.UserRepository;
import com.jompastech.backend.security.dto.AuthResponseDTO;
import com.jompastech.backend.security.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;


@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    public UserService(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder){
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Finds a user by ID without throwing exceptions for query operations.
     * Used by other services that need to validate user existence.
     *
     * @param userId the user identifier
     * @return User if found, Exception otherwise
     */
    @Transactional(readOnly = true)
    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
    }

    public AuthResponseDTO register(UserRequestDTO dto) {
        if (userRepository.existsByEmailIgnoreCase(dto.getEmail())) {
            throw new EmailAlreadyInUseException(dto.getEmail());
        }

        if (userRepository.existsByCpf(dto.getCpf())) {
            throw new CpfAlreadyInUseException(dto.getCpf());
        }

        User entity = userMapper.toEntity(dto);
        entity.setPassword(passwordEncoder.encode(dto.getPassword()));
        entity.setActive(true);
        entity.setRole("ROLE_USER");

        User savedUser = userRepository.save(entity);

        // Generates JWT token
        String jwtToken = jwtUtil.generateToken(savedUser.getEmail());

        // Converts to AuthResponseDTO with token
        return userMapper.toAuthResponseDTO(savedUser, jwtToken);
    }

    @Transactional(readOnly = true)
    public List<UserResponseDTO> findAllUsers(){
        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<UserResponseDTO> findUsersByName(String name){
        return userRepository.findByName(name)
                .stream()
                .map(userMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponseDTO updateUser(Long id, UserRequestDTO dto) {
        User existing = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));

        // If the email has changed,  guarantee uniqueness
        if (dto.getEmail() != null && !dto.getEmail().equalsIgnoreCase(existing.getEmail())) {
            if (userRepository.existsByEmailIgnoreCaseAndIdNot(dto.getEmail(), id)) {
                throw new EmailAlreadyInUseException(dto.getEmail());
            }
        }

        // merge  without killing null fields
        userMapper.updateEntityFromRequest(dto, existing);

        // if you sent a new password, re-hash it
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            existing.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        User saved = userRepository.save(existing);
        return userMapper.toResponseDTO(saved);
    }

    public void deleteById(Long id) {
        if (!userRepository.existsById(id)) {
            throw new EntityNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    /**
     * It gets the current user (via SecurityContext)
     */
    public UserResponseDTO getCurrentUserProfile() {
        // Implementation with SecurityContextHolder
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        return userMapper.toResponseDTO(user);
    }

    /**
     * Updates profile to current user
     */
    @Transactional
    public UserResponseDTO updateCurrentUserProfile(UserRequestDTO dto) {
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        return updateUser(user.getId(), dto);
    }

    /**
     * Get user by email
     */
    @Transactional(readOnly = true)
    public UserResponseDTO getUserByEmail(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
        return userMapper.toResponseDTO(user);
    }

    /**
     * Find user by email (returns entity)
     */
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
    }


}
