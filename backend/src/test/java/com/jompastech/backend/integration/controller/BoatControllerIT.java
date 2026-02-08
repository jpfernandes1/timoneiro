package com.jompastech.backend.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jompastech.backend.security.filter.JwtAuthenticationFilter;
import com.jompastech.backend.service.CloudinaryService;
import com.jompastech.backend.service.PhotoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BoatControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CloudinaryService cloudinaryService;

    @MockBean
    private PhotoService photoService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private final String boatJson = """
        {
          "name": "Boat Get",
          "description": "Lancha rápida",
          "type": "LANCHA",
          "capacity": 10,
          "pricePerHour": 500,
          "cep": "12345-000",
          "number": "10",
          "street": "Rua A",
          "neighborhood": "Centro",
          "city": "São Paulo",
          "state": "SP",
          "marina": "Marina Teste"
        }
        """;

    @BeforeEach
    void setup() throws Exception {
        Mockito.doNothing()
                .when(jwtAuthenticationFilter)
                .doFilter(any(), any(), any());
    }

    private void createBoat() throws Exception {
        MockMultipartFile boatPart = new MockMultipartFile(
                "boat",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                boatJson.getBytes()
        );

        mockMvc.perform(
                multipart("/api/boats")
                        .file(boatPart)
        ).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = "USER")
    void shouldCreateBoatWithoutImages() throws Exception {
        createBoat();
    }

    @Test
    @WithMockUser
    void shouldGetAllBoats() throws Exception {

        createBoat();

        mockMvc.perform(get("/api/boats"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void shouldGetBoatById() throws Exception {

        createBoat();

        mockMvc.perform(get("/api/boats/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void shouldGetMyBoatsPaginated() throws Exception {

        createBoat();

        mockMvc.perform(get("/api/boats/my-boats")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void shouldUpdateBoat() throws Exception {

        createBoat();

        mockMvc.perform(put("/api/boats/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "name": "Boat Updated",
                              "capacity": 4,
                              "pricePerHour": 350
                            }
                        """))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void shouldGetBoatPhotos() throws Exception {

        when(photoService.getBoatPhotos(anyLong()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/boats/1/photos"))
                .andExpect(status().isOk());
    }
}
