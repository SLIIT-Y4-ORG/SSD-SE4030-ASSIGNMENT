package com.example.paymentservice.controller;

import com.example.paymentservice.config.PaymentSecurityProperties;
import com.example.paymentservice.dto.CreateCheckoutSessionResponse;
import com.example.paymentservice.dto.InternalCreatePaymentSessionRequest;
import com.example.paymentservice.dto.PaymentProfileResponse;
import com.example.paymentservice.dto.ProvisionCustomerRequest;
import com.example.paymentservice.exception.GlobalExceptionHandler;
import com.example.paymentservice.service.CheckoutService;
import com.example.paymentservice.service.InternalAuthService;
import com.example.paymentservice.service.PaymentProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * V-04 (CWE-798): InternalPaymentController security tests.
 *
 * Verifies that:
 * - Missing X-Internal-Api-Key header → HTTP 401.
 * - Blank X-Internal-Api-Key header → HTTP 401.
 * - Incorrect key → HTTP 401.
 * - Correct key → controller proceeds and calls the downstream service.
 * - /customers endpoint also requires the key (was unprotected).
 * - HTTP 401 response body contains only the static "Unauthorized" message.
 * - Neither the supplied key nor the configured key appears in the response.
 */
@WebMvcTest(controllers = InternalPaymentController.class)
@Import({InternalPaymentControllerSecurityTest.TestConfig.class, GlobalExceptionHandler.class})
@DisplayName("V-04: InternalPaymentController authentication tests")
class InternalPaymentControllerSecurityTest {

    private static final String CORRECT_KEY = "test-correct-key-xyz789";
    private static final String SESSION_URL = "/api/internal/payments/sessions";
    private static final String CUSTOMERS_URL = "/api/internal/payments/customers";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private PaymentProfileService paymentProfileService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public PaymentProfileService paymentProfileService() {
            return mock(PaymentProfileService.class);
        }

        @Bean
        public CheckoutService checkoutService() {
            return mock(CheckoutService.class);
        }

        @Bean
        public InternalAuthService internalAuthService() {
            PaymentSecurityProperties props = new PaymentSecurityProperties(CORRECT_KEY, true);
            return new InternalAuthService(props);
        }
    }

    // ═══════════════════════ /sessions endpoint ═══════════════════════════════

    @Test
    @DisplayName("/sessions: missing header returns HTTP 401")
    void sessionsMissingHeaderReturns401() throws Exception {
        mockMvc.perform(post(SESSION_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSessionBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    @DisplayName("/sessions: blank header returns HTTP 401")
    void sessionsBlankHeaderReturns401() throws Exception {
        mockMvc.perform(post(SESSION_URL)
                        .header("X-Internal-Api-Key", "   ")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSessionBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    @DisplayName("/sessions: incorrect key returns HTTP 401")
    void sessionsIncorrectKeyReturns401() throws Exception {
        mockMvc.perform(post(SESSION_URL)
                        .header("X-Internal-Api-Key", "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSessionBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    @DisplayName("/sessions: 401 response body does not contain the supplied or configured key")
    void sessionsResponseDoesNotLeakKeys() throws Exception {
        String response = mockMvc.perform(post(SESSION_URL)
                        .header("X-Internal-Api-Key", "leaked-wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSessionBody()))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        // Neither the supplied key nor the correct key must appear in the response
        org.assertj.core.api.Assertions.assertThat(response).doesNotContain("leaked-wrong-key");
        org.assertj.core.api.Assertions.assertThat(response).doesNotContain(CORRECT_KEY);
    }

    @Test
    @DisplayName("/sessions: correct key → authentication passes and downstream is called")
    void sessionsCorrectKeyCallsService() throws Exception {
        UUID sessionId = UUID.randomUUID();
        when(checkoutService.createCheckoutSession(any()))
                .thenReturn(new CreateCheckoutSessionResponse(
                        UUID.randomUUID().toString(),
                        "https://checkout.stripe.com/fake",
                        "open"
                ));
        when(paymentProfileService.getEntityByUserId(any()))
                .thenThrow(new com.example.paymentservice.exception.ResourceNotFoundException("not found"));
        when(paymentProfileService.provisionStripeCustomer(any()))
                .thenReturn(new PaymentProfileResponse(
                        UUID.randomUUID(), "p@p.local", "Patient", null, "cus_test", true, null));

        mockMvc.perform(post(SESSION_URL)
                        .header("X-Internal-Api-Key", CORRECT_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSessionBody()))
                .andExpect(status().isCreated());

        verify(checkoutService).createCheckoutSession(any());
    }

    // ═══════════════════════ /customers endpoint ══════════════════════════════

    @Test
    @DisplayName("/customers: missing header returns HTTP 401 (was previously unprotected)")
    void customersMissingHeaderReturns401() throws Exception {
        mockMvc.perform(post(CUSTOMERS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCustomerBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    @DisplayName("/customers: incorrect key returns HTTP 401")
    void customersIncorrectKeyReturns401() throws Exception {
        mockMvc.perform(post(CUSTOMERS_URL)
                        .header("X-Internal-Api-Key", "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCustomerBody()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("/customers: correct key → authentication passes and downstream is called")
    void customersCorrectKeyCallsService() throws Exception {
        when(paymentProfileService.provisionStripeCustomer(any()))
                .thenReturn(new PaymentProfileResponse(UUID.randomUUID(), "p@p.local", "Patient", null, "cus_test", true, null));


        mockMvc.perform(post(CUSTOMERS_URL)
                        .header("X-Internal-Api-Key", CORRECT_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCustomerBody()))
                .andExpect(status().isCreated());

        verify(paymentProfileService).provisionStripeCustomer(any());
    }

    // ═══════════════════════ Helpers ══════════════════════════════════════════

    private String validSessionBody() throws Exception {
        var request = new InternalCreatePaymentSessionRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                "Appointment fee",
                "USD",
                "2026-10-01"
        );
        return objectMapper.writeValueAsString(request);
    }

    private String validCustomerBody() throws Exception {
        var request = new ProvisionCustomerRequest(
                UUID.randomUUID(),
                "patient@example.com",
                "Test Patient",
                null,
                "PATIENT"
        );
        return objectMapper.writeValueAsString(request);
    }
}
