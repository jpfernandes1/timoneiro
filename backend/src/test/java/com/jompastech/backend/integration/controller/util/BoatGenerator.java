package com.jompastech.backend.integration.controller.util;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class BoatGenerator {

    private final MockMvc mockMvc;

    public BoatGenerator(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    public Long createBoat(String bearerToken) throws Exception {

        String boatJson = """
            {
              "name": "Boat_%s",
              "description": "Integration test boat",
              "type": "LANCHA",
              "capacity": 6,
              "pricePerHour": 500,
              "cep": "12345-000",
              "number": "10",
              "street": "Rua A",
              "neighborhood": "Centro",
              "city": "São Paulo",
              "state": "SP",
              "marina": "Marina Teste"
            }
            """.formatted(UUID.randomUUID());

        MockMultipartFile boatPart = new MockMultipartFile(
                "boat",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                boatJson.getBytes()
        );

        MockMultipartFile image = new MockMultipartFile(
                "images",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake-image-content".getBytes()
        );


        MvcResult response = mockMvc.perform(
                        multipart("/api/boats")
                                .file(boatPart)
                                .file(image)
                                .header("Authorization", bearerToken)
                )
                .andExpect(status().isCreated())
                .andReturn();


        String json = response.getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }
}
