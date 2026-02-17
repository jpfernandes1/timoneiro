package com.jompastech.backend.integration.controller.util;

public record AuthenticatedUser(
        Long userId,
        String token,
        String email
) {}