package com.jompastech.backend.security.filter;

import com.jompastech.backend.security.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter that intercepts each request, validates the JWT token,
 * loads the corresponding UserDetails, and sets the authentication in the security context.
 *
 * This filter ensures that the principal is a UserDetails object containing the user's ID
 * and authorities, enabling method-level security (@PreAuthorize) and role-based access control.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        logger.info("🔐 JWT Filter: {} {}", request.getMethod(), request.getRequestURI());

        // Skip processing for OPTIONS preflight requests
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            logger.info("🔧 Ignoring OPTIONS preflight request");
            filterChain.doFilter(request, response);
            return;
        }

        // Extract JWT token from Authorization header
        String token = extractToken(request);

        if (token != null) {
            logger.info("✅ Token found, length: {}", token.length());

            try {
                // Validate the token
                if (jwtUtil.isValidToken(token)) {
                    String email = jwtUtil.getEmail(token);
                    logger.info("✅ Valid JWT for user: {}", email);

                    // Load UserDetails to obtain authorities and ID
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                    // Create authentication token with UserDetails as principal
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,          // principal = UserDetails (contains ID and authorities)
                                    null,                 // credentials (already authenticated)
                                    userDetails.getAuthorities()
                            );

                    // Set authentication in security context
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.info("🔐 SecurityContext set for: {} with authorities: {}", email, userDetails.getAuthorities());
                } else {
                    logger.warn("❌ Invalid JWT token");

                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("""
                    {"error":"Unauthorized","message":"Invalid JWT token"}
                    """);
                    return;
                }
            } catch (UsernameNotFoundException e) {
                logger.error("❌ User not found for email extracted from token: {}", e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("""
        {"error":"Unauthorized","message":"User not found"}
        """);
                return;
            } catch (Exception e) {
            logger.error("💥 Error processing JWT: {}", e.getMessage());

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("""
            {"error":"Unauthorized","message":"Invalid JWT token"}
            """);
            return;
        }
        } else {
            logger.warn("❌ No JWT token found in Authorization header");
            // Optionally log headers for debugging
            if (logger.isDebugEnabled()) {
                logger.debug("Request headers:");
                request.getHeaderNames().asIterator().forEachRemaining(headerName ->
                        logger.debug("  {}: {}", headerName, request.getHeader(headerName))
                );
            }
        }

        // Continue the filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the JWT token from the Authorization header.
     * The header must be of the form "Bearer <token>".
     *
     * @param request the HTTP request
     * @return the token string, or null if not present or malformed
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    /**
     * Determines whether this filter should not be applied to the current request.
     * We skip OPTIONS requests to allow CORS preflight to pass without authentication.
     *
     * @param request the HTTP request
     * @return true if the request method is OPTIONS
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }
}