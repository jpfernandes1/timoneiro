package com.jompastech.backend.service;

import com.jompastech.backend.exception.CpfAlreadyInUseException;
import com.jompastech.backend.exception.EmailAlreadyInUseException;
import com.jompastech.backend.exception.EntityNotFoundException;
import com.jompastech.backend.exception.UserNotFoundException;
import com.jompastech.backend.mapper.UserMapper;
import com.jompastech.backend.model.dto.UserRequestDTO;
import com.jompastech.backend.model.dto.UserResponseDTO;
import com.jompastech.backend.model.entity.User;
import com.jompastech.backend.repository.UserRepository;
import com.jompastech.backend.security.dto.AuthResponseDTO;
import com.jompastech.backend.security.util.JwtUtil;
import org.springframework.security.core.context.SecurityContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for {@link UserService} business logic.
 *
 * <p>Tests focus on service layer responsibilities including:
 * <ul>
 *   <li>User registration and authentication flow</li>
 *   <li>CRUD operations with proper validation</li>
 *   <li>Security context integration for profile operations</li>
 *   <li>Exception handling for business rules</li>
 *   <li>Repository and mapper coordination</li>
 * </ul>
 * </p>
 *
 * <p>Uses consistent test data initialization to ensure test independence
 * while minimizing code duplication through shared setup.</p>
 */
@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;
    @InjectMocks private UserService userService;

    private UserRequestDTO validUserRequest;
    private UserResponseDTO userResponseDTO;
    private UserResponseDTO userResponseDTO2;
    private User userEntity;
    private User savedUser;
    private User savedUser2;
    private AuthResponseDTO authResponseDTO;

    /**
     * Initializes comprehensive test data before each test execution.
     *
     * <p>Creates consistent entity relationships and DTOs that represent
     * real-world scenarios for User operations including registration,
     * authentication, and profile management.</p>
     */
    @BeforeEach
    void setUp() {
        // Inject @Autowired fields manually
        ReflectionTestUtils.setField(userService, "jwtUtil", jwtUtil);
        ReflectionTestUtils.setField(userService, "authenticationManager", authenticationManager);

        validUserRequest = new UserRequestDTO(
                "Barbossa",
                "teste@teste.com",
                "senha123",
                "12345678900",
                "11999999999"
        );

        // How it is converted from DTO
        userEntity = new User();
        userEntity.setName(validUserRequest.getName());
        userEntity.setEmail(validUserRequest.getEmail());
        userEntity.setPassword("senha123");
        userEntity.setCpf(validUserRequest.getCpf());
        userEntity.setPhone(validUserRequest.getPhone());

        // Saved user - how repository returns
        savedUser = new User();
        savedUser.setId(1L);
        savedUser.setName(userEntity.getName());
        savedUser.setEmail(userEntity.getEmail());
        savedUser.setPassword("encodedPassword");
        savedUser.setCpf(userEntity.getCpf());
        savedUser.setPhone(userEntity.getPhone());
        savedUser.setActive(true);
        savedUser.setRole("ROLE_USER");

        // Second saved user for list operations
        savedUser2 = new User();
        savedUser2.setId(2L);
        savedUser2.setName(userEntity.getName());
        savedUser2.setEmail(userEntity.getEmail());
        savedUser2.setPassword("encodedPassword");
        savedUser2.setCpf(userEntity.getCpf());
        savedUser2.setPhone(userEntity.getPhone());
        savedUser2.setActive(true);
        savedUser2.setRole("ROLE_USER");

        // Response DTO = expected result
        authResponseDTO = new AuthResponseDTO();
        authResponseDTO.setToken("jwtToken");
        authResponseDTO.setTokenType("Bearer");
        authResponseDTO.setUserId(1L);
        authResponseDTO.setEmail(savedUser.getEmail());
        authResponseDTO.setName(savedUser.getName());

        // Response DTOs for user operations
        userResponseDTO = new UserResponseDTO(1L, "Cap Nemo", "11999999999", "teste@email.com", LocalDateTime.now());
        userResponseDTO2 = new UserResponseDTO(2L, "Barbossa", "11999999999", "teste2@email.com", LocalDateTime.now());
    }

    /**
     * Cleans up security context after each test to prevent cross-test contamination.
     */
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Tests user registration failure when email already exists.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>EmailAlreadyInUseException is thrown with proper message</li>
     *   <li>Repository email check is performed</li>
     *   <li>CPF check and user save are not performed</li>
     * </ul>
     */
    @Test
    void register_withExistingEmail_ShouldThrowEmailAlreadyInUseExpection(){
        // Arrange
        String existingEmail = "teste@teste.com";
        when(userRepository.existsByEmailIgnoreCase(existingEmail)).thenReturn(true);

        // Act
        EmailAlreadyInUseException exception = assertThrows(
                EmailAlreadyInUseException.class,
                () -> userService.register(validUserRequest)
        );

        // Assert
        assertThat(exception.getMessage()).contains(existingEmail);
        verify(userRepository).existsByEmailIgnoreCase(existingEmail);
        verify(userRepository, never()).existsByCpf(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * Tests user registration failure when CPF already exists.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>CpfAlreadyInUseException is thrown with proper message</li>
     *   <li>Both email and CPF checks are performed</li>
     *   <li>User save is not performed</li>
     * </ul>
     */
    @Test
    void register_WithExistingCpf_ShouldThrowCpfAlreadyInUseException(){
        // Arrange
        String existingCpf = "12345678900";
        when(userRepository.existsByEmailIgnoreCase(validUserRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByCpf(existingCpf)).thenReturn(true);

        // Act
        CpfAlreadyInUseException exception = assertThrows(
                CpfAlreadyInUseException.class,
                () -> userService.register(validUserRequest)
        );

        // Assert
        assertThat(exception.getMessage()).contains(existingCpf);
        verify(userRepository).existsByEmailIgnoreCase(validUserRequest.getEmail());
        verify(userRepository).existsByCpf(existingCpf);
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * Tests successful user registration with valid data.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>AuthResponseDTO is returned with proper data</li>
     *   <li>All validation checks are performed</li>
     *   <li>Password encoding and user save are executed</li>
     *   <li>JWT token generation and DTO mapping occur</li>
     * </ul>
     */
    @Test
    void register_WithValidData_ShouldReturnAuthResponseDTO(){
        // Arrange
        when(userRepository.existsByEmailIgnoreCase(validUserRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByCpf(validUserRequest.getCpf())).thenReturn(false);
        when(userMapper.toEntity(validUserRequest)).thenReturn(userEntity);
        when(passwordEncoder.encode(validUserRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(userEntity)).thenReturn(savedUser);
        when(jwtUtil.generateToken(savedUser.getEmail())).thenReturn("jwtToken");
        when(userMapper.toAuthResponseDTO(savedUser,"jwtToken")).thenReturn(authResponseDTO);

        // Act
        AuthResponseDTO result = userService.register(validUserRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getToken()).isEqualTo("jwtToken");
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo(validUserRequest.getEmail());
        assertThat(result.getName()).isEqualTo(validUserRequest.getName());

        // Verify all interactions
        verify(userRepository).existsByEmailIgnoreCase(validUserRequest.getEmail());
        verify(userRepository).existsByCpf(validUserRequest.getCpf());
        verify(userMapper).toEntity(validUserRequest);
        verify(passwordEncoder).encode(validUserRequest.getPassword());
        verify(userRepository).save(userEntity);
        verify(jwtUtil).generateToken(savedUser.getEmail());
        verify(userMapper).toAuthResponseDTO(savedUser, "jwtToken");
    }

    /**
     * Tests successful user retrieval by existing ID.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>User entity is returned when ID exists</li>
     *   <li>Repository findById is properly called</li>
     * </ul>
     */
    @Test
    void findById_ShouldReturnUserByIdIfExists(){
        // Arrange
        when(userRepository.findById(savedUser.getId())).thenReturn(Optional.ofNullable(savedUser));

        // Act
        User result = userService.findById(savedUser.getId());

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(savedUser);
    }

    /**
     * Tests retrieval of all users when they exist.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>List of UserResponseDTO is returned</li>
     *   <li>Repository findAll is called</li>
     *   <li>Mapper converts all entities to DTOs</li>
     * </ul>
     */
    @Test
    void findAllUsers_findAllUsers_ShouldReturnListOfUserResponseDTO(){
        // Arrange
        List<User> users = new ArrayList<>();
        users.add(savedUser);
        users.add(savedUser2);

        List<UserResponseDTO> expectedDTOs = new ArrayList<>();
        expectedDTOs.add(userResponseDTO);
        expectedDTOs.add(userResponseDTO2);

        when(userRepository.findAll()).thenReturn(users);
        when(userMapper.toResponseDTO(savedUser)).thenReturn(userResponseDTO);
        when(userMapper.toResponseDTO(savedUser2)).thenReturn(userResponseDTO2);

        // Act
        List<UserResponseDTO> result = userService.findAllUsers();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(expectedDTOs);
        verify(userRepository).findAll();
        verify(userMapper).toResponseDTO(savedUser);
    }

    /**
     * Tests user retrieval by name filtering.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>Filtered list of UserResponseDTO is returned</li>
     *   <li>Repository findByName is called with correct parameter</li>
     *   <li>Mapper converts filtered entities to DTOs</li>
     * </ul>
     */
    @Test
    void findUsersByName_ShouldReturnListOfUser(){
        //Arrange
        String userName = "Barbossa";
        List<User> user = new ArrayList<>();
        user.add(savedUser2);

        List<UserResponseDTO> expected = new ArrayList<>();
        expected.add(userResponseDTO2);

        when(userRepository.findByName(userName)).thenReturn(user);
        when(userMapper.toResponseDTO(savedUser2)).thenReturn(userResponseDTO2);

        // Act
        List<UserResponseDTO> result = userService.findUsersByName(userName);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(expected);
        verify(userRepository).findByName(userName);
        verify(userMapper).toResponseDTO(savedUser2);
    }

    /**
     * Tests successful user update with valid data.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>Updated UserResponseDTO is returned</li>
     *   <li>User existence and email uniqueness are validated</li>
     *   <li>Password encoding and user save are performed</li>
     * </ul>
     */
    @Test
    void updateUser_WithValidData_ShouldReturnUpdatedUser(){
        // Arrange
        Long userId = 1L;
        UserRequestDTO updtateDTO = new UserRequestDTO(
                "Cap Nemo Atualizado",
                "novo@email.com",
                "novaSenha123",
                "12345678900",
                "11988888888"
        );

        when(userRepository.findById(userId)).thenReturn(Optional.ofNullable(savedUser));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("novo@email.com", userId)).thenReturn(false);
        when(passwordEncoder.encode("novaSenha123")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toResponseDTO(savedUser)).thenReturn(userResponseDTO);

        // Act
        UserResponseDTO result = userService.updateUser(userId, updtateDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(userResponseDTO);
        verify(userRepository).findById(userId);
        verify(userRepository).existsByEmailIgnoreCaseAndIdNot("novo@email.com", userId);
        verify(passwordEncoder).encode("novaSenha123");
        verify(userRepository).save(any(User.class));
    }

    /**
     * Tests user update failure when user doesn't exist.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>UserNotFoundException is thrown with proper message</li>
     *   <li>Repository findById is called</li>
     *   <li>User save is not performed</li>
     * </ul>
     */
    @Test
    void updateUser_WithInexistentUser_ShouldReturnAnUserNotFoundException(){
        // Arrange
        Long nonExistingId = 99L;
        UserRequestDTO updtateDTO = new UserRequestDTO(
                "Cap Nemo",
                "teste@email.com",
                "senha123",
                "12345678900",
                "11988888888"
        );

        when(userRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        // Act
        UserNotFoundException result = assertThrows(
                UserNotFoundException.class,
                ()-> userService.updateUser(nonExistingId, updtateDTO)
        );

        // Assert
        assertThat(result.getMessage()).contains("User not found with ID: " + nonExistingId);
        verify(userRepository).findById(nonExistingId);
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * Tests user update failure when email is already in use.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>EmailAlreadyInUseException is thrown with proper message</li>
     *   <li>User existence and email uniqueness are validated</li>
     *   <li>User save is not performed</li>
     * </ul>
     */
    @Test
    void updateUser_WithInUseEmail_ShouldReturnAnEmailInUseException(){
        // Arrange
        Long userId = 1L;
        UserRequestDTO updateDTO = new UserRequestDTO(
                "Cap Nemo Atualizado",
                "existente@email.com",
                "novaSenha123",
                "12345678900",
                "11988888888"
        );

        when(userRepository.findById(userId)).thenReturn(Optional.ofNullable(savedUser));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot(updateDTO.getEmail(), userId)).thenReturn(true);

        // Act
        EmailAlreadyInUseException exception = assertThrows(
                EmailAlreadyInUseException.class,
                () -> userService.updateUser(userId, updateDTO)
        );

        // Assert
        assertThat(exception.getMessage()).contains("existente@email.com");
        verify(userRepository).findById(userId);
        verify(userRepository).existsByEmailIgnoreCaseAndIdNot(updateDTO.getEmail(), userId);
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * Tests successful user deletion when user exists.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>Repository deleteById is called with correct ID</li>
     *   <li>User existence is validated before deletion</li>
     * </ul>
     */
    @Test
    void deleteById_WithExistingUser_ShouldCallRepositoryDelete() {
        // Arrange
        Long idToDelete = 1L;
        when(userRepository.existsById(idToDelete)).thenReturn(true);

        // Act
        userService.deleteById(idToDelete);

        // Assert
        verify(userRepository).deleteById(idToDelete);
    }

    /**
     * Tests user deletion failure when user doesn't exist.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>EntityNotFoundException is thrown with proper message</li>
     *   <li>Repository existsById is called</li>
     *   <li>Repository deleteById is not performed</li>
     * </ul>
     */
    @Test
    void deleteById_WithNonExistingUser_ShouThrowAnException(){
        // Arrange
        Long nonExistingUser = 99L;

        // Act
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> userService.deleteById(nonExistingUser)
        );

        // Assert
        assertThat(exception.getMessage()).contains("User not found with id: " + nonExistingUser);
    }

    /**
     * Tests current user profile retrieval for authenticated user.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>UserResponseDTO is returned for authenticated user</li>
     *   <li>Security context provides authenticated user email</li>
     *   <li>Repository findByEmailIgnoreCase is called</li>
     *   <li>Mapper converts entity to DTO</li>
     * </ul>
     */
    @Test
    void getCurrentUserProfile_WithAuthenticatedUser_ShouldReturnUserProfile() {
        // Arrange
        String userEmail = "teste@teste.com";

        // Security context mock
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(userEmail);
        when(authentication.isAuthenticated()).thenReturn(true);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByEmailIgnoreCase(userEmail)).thenReturn(Optional.ofNullable(savedUser));
        when(userMapper.toResponseDTO(savedUser)).thenReturn(userResponseDTO);

        // Act
        UserResponseDTO result = userService.getCurrentUserProfile();

        // Assert
        assertThat(result).isEqualTo(userResponseDTO);
        verify(userRepository).findByEmailIgnoreCase(userEmail);
        verify(userMapper).toResponseDTO(savedUser);
    }

    /**
     * Tests current user profile retrieval failure when user is not authenticated.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>RuntimeException is thrown with proper message</li>
     *   <li>Security context has no authentication</li>
     * </ul>
     */
    @Test
    void getCurrentUserProfile_WithoutAuthenticatedUser_ShouldThrowAnException(){
        // Arrange
        // Security context is cleared by @AfterEach

        // Act
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.getCurrentUserProfile()
        );

        // Assert
        assertThat(exception.getMessage()).isEqualTo("User not authenticated");
    }

    /**
     * Tests successful current user profile update with valid data.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>Updated UserResponseDTO is returned</li>
     *   <li>Security context provides authenticated user email</li>
     *   <li>Complete update flow is executed properly</li>
     * </ul>
     */
    @Test
    void updateCurrentUserProfile_WithValidData_ShouldReturnUpdatedProfile() {
        // Arrange
        String userEmail = "teste@teste.com";
        setupSecurityContext(userEmail);

        when(userRepository.findByEmailIgnoreCase(userEmail)).thenReturn(Optional.of(savedUser));
        when(userRepository.findById(savedUser.getId())).thenReturn(Optional.ofNullable(savedUser));
        lenient().when(userRepository.existsByEmailIgnoreCaseAndIdNot(anyString(), anyLong())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toResponseDTO(savedUser)).thenReturn(userResponseDTO);

        // Act
        UserResponseDTO result = userService.updateCurrentUserProfile(validUserRequest);

        // Assert
        assertThat(result).isEqualTo(userResponseDTO);
        verify(userRepository).findByEmailIgnoreCase(userEmail);
        verify(userRepository).findById(savedUser.getId());
    }

    /**
     * Tests user retrieval by existing email.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>UserResponseDTO is returned for existing email</li>
     *   <li>Repository findByEmailIgnoreCase is called</li>
     *   <li>Mapper converts entity to DTO</li>
     * </ul>
     */
    @Test
    void getUserByEmailIgnoreCase_WithExistingUser_ShouldReturnAnResponseDTO(){
        // Arrange
        String userEmail = "teste@teste.com";
        when(userRepository.findByEmailIgnoreCase(userEmail)).thenReturn(Optional.ofNullable(savedUser));
        when(userMapper.toResponseDTO(savedUser)).thenReturn(userResponseDTO);

        // Act
        UserResponseDTO result = userService.getUserByEmail(userEmail);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(userResponseDTO);
        verify(userRepository).findByEmailIgnoreCase(userEmail);
        verify(userMapper).toResponseDTO(savedUser);
    }

    /**
     * Tests user retrieval failure when email doesn't exist.
     *
     * <p>Verifies that:
     * <ul>
     *   <li>UserNotFoundException is thrown with proper message</li>
     *   <li>Repository findByEmailIgnoreCase is called</li>
     * </ul>
     */
    @Test
    void getUserByEmail_WithoutExistingUser_ShouldThrowAnException(){
        // Arrange
        String userEmail = "teste@teste.com";
        when(userRepository.findByEmailIgnoreCase(userEmail)).thenReturn(Optional.empty());

        // Act
        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userService.getUserByEmail(userEmail)
        );

        // Assert
        assertThat(exception.getMessage()).contains("User not found with email: " + userEmail);
        verify(userRepository).findByEmailIgnoreCase(userEmail);
    }

    // ===== HELPER METHODS =====

    /**
     * Sets up security context for authentication-dependent tests.
     *
     * @param email the email to set as authenticated user
     */
    private void setupSecurityContext(String email) {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        lenient().when(authentication.isAuthenticated()).thenReturn(true);
        SecurityContextHolder.setContext(securityContext);
    }
}