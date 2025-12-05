package com.jompastech.backend.model.dto;
import lombok.AllArgsConstructor;
import lombok.Data;


import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class UserResponseDTO {

    private Long id;
    private String name;
    private String phone;
    private String email;
    private LocalDateTime createdAt;
}
