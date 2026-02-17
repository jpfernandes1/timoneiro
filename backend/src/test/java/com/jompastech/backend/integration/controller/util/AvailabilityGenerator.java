package com.jompastech.backend.integration.controller.util;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AvailabilityGenerator {

    private final MockMvc mockMvc;

    public AvailabilityGenerator(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    public Long createAvailability(
            String bearerToken,
            Long boatId,
            LocalDateTime start,
            LocalDateTime end,
            BigDecimal pricePerHour
    ) throws Exception {

        String json = """
            {
              "startDate": "%s",
              "endDate": "%s",
              "pricePerHour": %s
            }
            """.formatted(start, end, pricePerHour);

        MvcResult result = mockMvc.perform(
                        post("/api/boats/{boatId}/availability", boatId)
                                .header("Authorization", bearerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    public void updateAvailability(
            String bearerToken,
            Long boatId,
            Long availabilityId,
            LocalDateTime start,
            LocalDateTime end,
            BigDecimal pricePerHour
    ) throws Exception {

        String json = """
            {
              "startDate": "%s",
              "endDate": "%s",
              "pricePerHour": %s
            }
            """.formatted(start, end, pricePerHour);

        mockMvc.perform(
                        put("/api/boats/{boatId}/availability/{id}", boatId, availabilityId)
                                .header("Authorization", bearerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk());
    }

    public void deleteAvailability(
            String bearerToken,
            Long boatId,
            Long availabilityId
    ) throws Exception {

        mockMvc.perform(
                        delete("/api/boats/{boatId}/availability/{id}", boatId, availabilityId)
                                .header("Authorization", bearerToken)
                )
                .andExpect(status().isNoContent());
    }
}
