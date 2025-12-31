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
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        logger.info("🔐 JWT Filter: {} {}", request.getMethod(), request.getRequestURI());

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            logger.info("🔧 Ignoring OPTIONS preflight request");
            filterChain.doFilter(request, response);
            return;
        }

        // Extract Token
        String token = extractToken(request);

        if (token != null) {
            logger.info("✅ Token found, length: {}", token.length());

            try {
                if (jwtUtil.isValidToken(token)) {
                    String email = jwtUtil.getEmail(token);
                    logger.info("✅ Valid JWT for user: {}", email);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    email,
                                    null,
                                    Collections.emptyList()
                            );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.info("🔐 SecurityContext set for: {}", email);
                } else {
                    logger.warn("❌ Invalid JWT token");
                }
            } catch (Exception e) {
                logger.error("💥 Error processing JWT: {}", e.getMessage());
            }
        } else {
            logger.warn("❌ No JWT token found in Authorization header");
            // Log all headers for debugging.
            logger.debug("Request headers:");
            request.getHeaderNames().asIterator().forEachRemaining(headerName ->
                    logger.debug("  {}: {}", headerName, request.getHeader(headerName))
            );
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    // ⚠️ IMPORTANTE: Não filtrar requisições OPTIONS
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }
}