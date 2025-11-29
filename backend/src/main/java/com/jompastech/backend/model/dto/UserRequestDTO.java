package com.jompastech.backend.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.validator.constraints.br.CPF;

@Data
@AllArgsConstructor
public class UserRequestDTO {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank @Email
    @Size(max = 100)
    private String email;

    @NotBlank @Size(min = 8, max = 72)
    private String password;

    @NotBlank(message = "CPF is required")
    @Pattern(regexp = "\\d{11}", message = "CPF must have exactly 11 digits")
    @CPF(message = "Invalid CPF")
    private String cpf;


    @Size(max = 20)
    private String phone;

}
