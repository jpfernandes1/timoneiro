package com.jompastech.backend.Unit.security;

import com.jompastech.backend.security.filter.JwtAuthenticationFilter;
import com.jompastech.backend.security.util.JwtUtil;
import com.jompastech.backend.security.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    void doFilter_whenValidToken_shouldSetAuthentication() throws ServletException, IOException {
        // Arrange
        String token = "valid.jwt.token";
        String email = "user@example.com";

        request.addHeader("Authorization", "Bearer " + token);
        when(jwtUtil.isValidToken(token)).thenReturn(true);
        when(jwtUtil.getEmail(token)).thenReturn(email);

        // Act
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Assert - Apenas verifica o que realmente acontece
        verify(jwtUtil).isValidToken(token);
        verify(jwtUtil).getEmail(token);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(email, authentication.getPrincipal());

        // NÃO verifica userDetailsService porque não é usado!
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_withInvalidToken_shouldNotSetAuthentication() throws ServletException, IOException {
        // Arrange
        String token = "invalid.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);
        when(jwtUtil.isValidToken(token)).thenReturn(false);

        // Act
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Assert
        verify(jwtUtil).isValidToken(token);
        verify(jwtUtil, never()).getEmail(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_withoutAuthorizationHeader_shouldNotProcessToken() throws ServletException, IOException {
        // Act
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Assert
        verify(jwtUtil, never()).isValidToken(anyString());
        verify(jwtUtil, never()).getEmail(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Basic token",
            "Bearer",
            "Token without-bearer"
    })
    void doFilter_withInvalidAuthorizationHeader_shouldNotProcessToken(String authHeader)
            throws ServletException, IOException {
        // Arrange
        request.addHeader("Authorization", authHeader);

        // Act
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Assert
        verify(jwtUtil, never()).isValidToken(anyString());
        verify(jwtUtil, never()).getEmail(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void extractToken_withBearerToken_shouldReturnTokenWithoutPrefix() {
        // Arrange
        request.addHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...");

        String result = ReflectionTestUtils.invokeMethod(
                jwtAuthenticationFilter,
                "extractToken",
                request
        );

        // Assert
        assertThat(result).isEqualTo("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...");
    }

    @Test
    void extractToken_withoutBearerPrefix_shouldReturnNull() {
        // Arrange
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        // Act
        String result = ReflectionTestUtils.invokeMethod(
                jwtAuthenticationFilter,
                "extractToken",
                request
        );

        // Assert
        assertThat(result).isNull();
    }

    @Test
    void extractToken_withNullHeader_shouldReturnNull() {
        // Arrange - Não setamos header, então será null

        // Act
        String result = ReflectionTestUtils.invokeMethod(
                jwtAuthenticationFilter,
                "extractToken",
                request
        );

        // Assert
        assertThat(result).isNull();
    }

    @Test
    void extractToken_withEmptyHeader_shouldReturnNull() {
        // Arrange
        request.addHeader("Authorization", "");

        // Act
        String result = ReflectionTestUtils.invokeMethod(
                jwtAuthenticationFilter,
                "extractToken",
                request
        );

        // Assert
        assertThat(result).isNull();
    }

    @Test
    void extractToken_withOnlyBearerKeyword_shouldReturnNull() {
        // Arrange
        request.addHeader("Authorization", "Bearer");

        // Act
        String result = ReflectionTestUtils.invokeMethod(
                jwtAuthenticationFilter,
                "extractToken",
                request
        );

        // Assert
        assertThat(result).isNull();
    }

    @Test
    void extractToken_withBearerAndSpaceOnly_shouldReturnEmptyString() {
        // Arrange
        request.addHeader("Authorization", "Bearer ");

        // Act
        String result = ReflectionTestUtils.invokeMethod(
                jwtAuthenticationFilter,
                "extractToken",
                request
        );

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void doFilter_whenAuthenticationAlreadyExistsAndValidToken_shouldOverrideAuthentication()
            throws ServletException, IOException {
        // Arrange
        Authentication existingAuth = mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        String token = "valid.jwt.token";
        String email = "user@example.com";

        request.addHeader("Authorization", "Bearer " + token);
        when(jwtUtil.isValidToken(token)).thenReturn(true);
        when(jwtUtil.getEmail(token)).thenReturn(email);

        // Act
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Assert
        verify(jwtUtil).isValidToken(token);
        verify(jwtUtil).getEmail(token);

        Authentication newAuth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(newAuth);
        assertNotSame(existingAuth, newAuth);
        assertEquals(email, newAuth.getPrincipal());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_whenAuthenticationAlreadyExistsAndInvalidToken_shouldNotOverride()
            throws ServletException, IOException {
        // Arrange - Já existe uma autenticação no contexto
        Authentication existingAuth = mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        String token = "invalid.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);
        when(jwtUtil.isValidToken(token)).thenReturn(false);

        // Act
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Assert - Token inválido, não deve sobrescrever
        verify(jwtUtil).isValidToken(token);
        verify(jwtUtil, never()).getEmail(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());

        // A autenticação deve permanecer a mesma
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        assertSame(existingAuth, currentAuth); // Deve ser a mesma

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_whenAuthenticationAlreadyExistsAndNoToken_shouldNotOverride()
            throws ServletException, IOException {
        // Arrange - Já existe uma autenticação no contexto
        Authentication existingAuth = mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        // Não adiciona header Authorization

        // Act
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Assert - Sem token, não deve sobrescrever
        verify(jwtUtil, never()).isValidToken(anyString());
        verify(jwtUtil, never()).getEmail(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());

        // A autenticação deve permanecer a mesma
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        assertSame(existingAuth, currentAuth);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldHandleMalformedTokenGracefully() throws ServletException, IOException {
        // Arrange
        String token = "malformed.token";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtil.isValidToken(token)).thenReturn(false);

        // Act
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Assert
        verify(jwtUtil).isValidToken(token);
        verify(jwtUtil, never()).getEmail(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldSetAuthenticationWhenValidToken() throws ServletException, IOException {
        // Arrange
        String token = "valid.jwt.token";
        String email = "user@example.com";

        request.addHeader("Authorization", "Bearer " + token);
        when(jwtUtil.isValidToken(token)).thenReturn(true);
        when(jwtUtil.getEmail(token)).thenReturn(email);

        // Act
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // Assert -
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication, "Authentication should be set");
        assertEquals(email, authentication.getPrincipal(), "Principal should be the email");

        verify(filterChain).doFilter(request, response);
    }
}