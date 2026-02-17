package com.jompastech.backend.integration.controller.util;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static com.jompastech.backend.integration.controller.util.CpfGenerator.generateValidCpf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UserGenerator {

    private final MockMvc mockMvc;

    public UserGenerator(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    public AuthenticatedUser createAndAuthenticateUser() throws Exception {

        String uniqueEmail = "test_" + UUID.randomUUID() + "@boat.com";


        String registerJson = String.format("""
            {
              "name": "Test User",
              "email": "%s",
              "password": "asd@12345",
              "cpf": "%s",
              "phone": "11999999999"
            }
            """, uniqueEmail, generateValidCpf());

        MvcResult registerResult = mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isCreated())
                .andReturn();

        String registerResponse = registerResult.getResponse().getContentAsString();

        Long userId = ((Number) JsonPath.read(registerResponse, "$.userId")).longValue();

        String loginJson = String.format("""
            {
              "email": "%s",
              "password": "asd@12345"
            }
            """, uniqueEmail);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String loginResponse = loginResult.getResponse().getContentAsString();

        String token = JsonPath.read(loginResponse, "$.token");

        return new AuthenticatedUser(userId, token, uniqueEmail);
    }
}
