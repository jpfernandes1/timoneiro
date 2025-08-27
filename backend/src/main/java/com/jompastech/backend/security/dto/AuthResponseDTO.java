package com.jompastech.backend.security.dto;

import lombok.Data;

@Data
public class AuthResponseDTO {
    private String token;
    private String tokenType = "Bearer";
    private Long userId;
    private String email;
    private String name;
}