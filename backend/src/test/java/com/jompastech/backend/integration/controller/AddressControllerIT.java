package com.jompastech.backend.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jompastech.backend.model.dto.AddressRequestDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AddressControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    void shouldCreateAddress() throws Exception {

        AddressRequestDTO request = new AddressRequestDTO();
        request.setCep("12345-000");
        request.setNumber("10");
        request.setStreet("Rua Teste");
        request.setNeighborhood("Centro");
        request.setCity("São Paulo");
        request.setState("SP");

        mockMvc.perform(post("/api/address")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void shouldGetAddressById() throws Exception {

        String response = mockMvc.perform(post("/api/address")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                  "cep": "12345-000",
                  "number": "20",
                  "street": "Rua A",
                  "neighborhood": "Centro",
                  "city": "Rio",
                  "state": "RJ",
                  "marina": "Marina X"
                }
            """))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = ((Number) JsonPath.read(response, "$.id")).longValue();

        mockMvc.perform(get("/api/address/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Rio"));
    }

    @Test
    @WithMockUser
    void shouldDeleteAddress() throws Exception {

        String response = mockMvc.perform(post("/api/address")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                  "cep": "00000-000",
                  "number": "1",
                  "street": "Rua Delete",
                  "neighborhood": "Centro",
                  "city": "BH",
                  "state": "MG",
                  "marina": "Marina D"
                }
            """))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = ((Number) JsonPath.read(response, "$.id")).longValue();

        mockMvc.perform(delete("/api/address/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void shouldGetAllAddresses() throws Exception {

        mockMvc.perform(get("/api/address"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser
    void shouldGetFullAddress() throws Exception {

        String response = mockMvc.perform(post("/api/address")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                  "cep": "11111-000",
                  "number": "99",
                  "street": "Rua Full",
                  "neighborhood": "Centro",
                  "city": "Curitiba",
                  "state": "PR",
                  "marina": "Marina Full"
                }
            """))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = ((Number) JsonPath.read(response, "$.id")).longValue();

        mockMvc.perform(get("/api/address/private/{id}", id))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void shouldUpdateAddress() throws Exception {

        String response = mockMvc.perform(post("/api/address")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                  "cep": "22222-000",
                  "number": "5",
                  "street": "Rua Original",
                  "neighborhood": "Centro",
                  "city": "Porto Alegre",
                  "state": "RS"
                }
            """))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = ((Number) JsonPath.read(response, "$.id")).longValue();

        AddressRequestDTO update = new AddressRequestDTO();
        update.setCep("22222-000");
        update.setNumber("50");
        update.setStreet("Rua Atualizada");
        update.setNeighborhood("Centro");
        update.setCity("Porto Alegre");
        update.setState("RS");

        mockMvc.perform(put("/api/address/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk());
    }
}
