package com.jompastech.backend.config;

import com.jompastech.backend.security.filter.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
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

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // ========== PUBLIC ENDPOINTS (no authentication required) ==========
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/users/register").permitAll()
                        .requestMatchers("/api/payments/webhook/**").permitAll()
                        .requestMatchers(
                                "/swagger-ui/**", // Swagger UI
                                "/v3/api-docs/**", // API documentation
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()

                        // Public GET endpoints for boats and search
                        .requestMatchers(HttpMethod.GET, "/api/boats").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/boats/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/boats/{id}/availability/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/search/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()

                        // ========== BOAT MANAGEMENT (role-based) ==========
                        .requestMatchers(HttpMethod.POST, "/api/boats").hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/boats/**").hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/boats/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/boats/my-boats/**").hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")

                        // ========== USER MANAGEMENT (specific rules) ==========
                        // Admin-only endpoints
                        .requestMatchers(HttpMethod.GET, "/api/users").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/users/search").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/users/email/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/users").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasAuthority("ROLE_ADMIN")
                        // Any authenticated user can access profile and update own profile (further authorization in service)
                        .requestMatchers("/api/users/profile").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/users/{id}").authenticated()
                        // Catch-all for any other /api/users/** endpoints (e.g., GET /api/users/{id}) – requires authentication
                        .requestMatchers("/api/users/**").authenticated()

                        // ========== BOOKINGS ==========
                        .requestMatchers("/api/bookings/**").authenticated()

                        // ========== DEFAULT ==========
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"Authentication required\"}");
                        })
                )
                // Add JWT filter before Spring Security's authentication processing
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configures CORS settings for the application.
     * In development profile, allows all origins; in production, restricts to specific origins.
     *
     * @return CorsConfigurationSource with the appropriate CORS configuration
     */
    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        String profile = System.getenv("SPRING_PROFILES_ACTIVE");
        if ("dev".equals(profile) || "local".equals(profile)) {
            configuration.setAllowedOriginPatterns(List.of("*"));
        } else {
            configuration.setAllowedOriginPatterns(Arrays.asList(
                    "http://localhost:*",          // Frontend on port 80
                    "http://localhost:3000",     // React dev server
                    "https://p01--timoneirobackend--*.code.run", // Northflank (deploy-prod)
                    "https://*.code.run",        // wildcard access to everything coming from the northflank
                    "https://seusite.com"        // Future: production domain
            ));
        }

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
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