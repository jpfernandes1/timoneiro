package com.jompastech.backend.integration.controller.util;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class BookingGenerator {

    private final MockMvc mockMvc;

    public BookingGenerator(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    public Long createBooking(
            String bearerToken,
            Long boatId,
            LocalDateTime start,
            LocalDateTime end
    ) throws Exception {

        String startStr = start.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String endStr = end.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        String json = """
            {
              "boatId": %d,
              "startDate": "%s",
              "endDate": "%s",
              "paymentMethod": "CREDIT_CARD",
              "mockCardData": {
                "cardNumber": "4111111111111111",
                "holderName": "John Doe",
                "expirationDate": "12/2030",
                "cvv": "123"
              }
            }
            """.formatted(boatId, startStr, endStr);

        MvcResult result = mockMvc.perform(
                        post("/api/bookings")
                                .header("Authorization", bearerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }
}
