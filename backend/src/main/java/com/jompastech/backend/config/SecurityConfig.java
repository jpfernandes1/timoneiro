package com.jompastech.backend.config;

import com.jompastech.backend.security.filter.JwtAuthenticationFilter;
import com.jompastech.backend.security.service.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // Public endpoints - no authentication required
                        .requestMatchers(
                                "/api/auth/**",           // Authentication endpoints
                                "/api/public/**",         // Public API endpoints
                                "/swagger-ui/**",         // Swagger UI
                                "/v3/api-docs/**",        // API documentation
                                "/swagger-resources/**",  // Swagger resources
                                "/api/users/register",    // User registration
                                "/webjars/**"             // Web jars
                        ).permitAll()

                        // Protected endpoints with role-based authorization
                        .requestMatchers("/api/boats/**").hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasAuthority("ROLE_ADMIN") // Only admins can delete users
                        .requestMatchers("/api/users/**").hasAnyAuthority("ROLE_USER", "ROLE_ADMIN") // Both users and admins can access

                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )
                // Add JWT filter before Spring Security's authentication processing
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    /**
     * Configures CORS settings for the application
     * @return CorsConfigurationSource with allowed origins, methods, and headers
     */
    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*")); // Allow all origins in development
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")); // Allowed HTTP methods
        configuration.setAllowedHeaders(List.of("*")); // Allow all headers
        configuration.setAllowCredentials(true); // Allow credentials

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Apply to all endpoints
        return source;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}