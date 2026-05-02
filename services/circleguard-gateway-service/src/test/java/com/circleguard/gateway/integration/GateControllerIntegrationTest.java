package com.circleguard.gateway.integration;

import com.circleguard.gateway.controller.GateController;
import com.circleguard.gateway.service.QrValidationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GateController.class)
@ActiveProfiles("test")
class GateControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private QrValidationService qrValidationService;

    @Test
    void shouldReturn200WithGreenStatusForValidToken() throws Exception {
        when(qrValidationService.validateToken(anyString()))
                .thenReturn(new QrValidationService.ValidationResult(true, "GREEN", "Welcome to Campus"));

        mockMvc.perform(post("/api/v1/gate/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", "valid.jwt.token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.status").value("GREEN"))
                .andExpect(jsonPath("$.message").value("Welcome to Campus"));
    }

    @Test
    void shouldReturn200WithRedStatusForDeniedToken() throws Exception {
        when(qrValidationService.validateToken(anyString()))
                .thenReturn(new QrValidationService.ValidationResult(false, "RED", "Access Denied: Health Risk Detected"));

        mockMvc.perform(post("/api/v1/gate/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", "infected.jwt.token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.status").value("RED"));
    }

    @Test
    void shouldReturn200WithRedStatusForInvalidToken() throws Exception {
        when(qrValidationService.validateToken(anyString()))
                .thenReturn(new QrValidationService.ValidationResult(false, "RED", "Invalid or Expired Token"));

        mockMvc.perform(post("/api/v1/gate/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", "bad-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.message").value("Invalid or Expired Token"));
    }

    @Test
    void shouldDelegateTokenToValidationServiceExactly() throws Exception {
        String expectedToken = "precise.token.value";
        when(qrValidationService.validateToken(expectedToken))
                .thenReturn(new QrValidationService.ValidationResult(true, "GREEN", "OK"));

        mockMvc.perform(post("/api/v1/gate/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", expectedToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));

        org.mockito.Mockito.verify(qrValidationService).validateToken(expectedToken);
    }
}
