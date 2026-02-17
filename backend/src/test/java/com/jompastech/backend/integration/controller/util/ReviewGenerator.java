package com.jompastech.backend.integration.controller.util;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ReviewGenerator {

    private final MockMvc mockMvc;

    public ReviewGenerator(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    public Long createReview(String bearerToken,
                             Long boatId,
                             int rating,
                             String comment) throws Exception {

        String reviewJson = String.format("""
            {
              "boatId": %d,
              "rating": %d,
              "comment": "%s"
            }
            """, boatId, rating, comment);

        MvcResult result = mockMvc.perform(post("/api/reviews")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewJson))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();

        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }
}
