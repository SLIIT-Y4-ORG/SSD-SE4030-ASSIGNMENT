package com.example.appointmentservice.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.payment.base-url}")
    private String paymentServiceUrl;

    @Value("${services.payment.internal-api-key}")
    private String internalApiKey;

    public PaymentSessionResponse createPaymentSession(PaymentSessionRequest request) {
        try {
            String url = paymentServiceUrl + "/api/internal/payments/sessions";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Api-Key", internalApiKey);
            
            HttpEntity<PaymentSessionRequest> entity = new HttpEntity<>(request, headers);
            
            log.info("Calling paymentService at: {} with appointment: {}", url, request.appointmentId);
            
            PaymentSessionResponse response = restTemplate.postForObject(url, entity, PaymentSessionResponse.class);
            
            if (response != null) {
                log.info("Payment session created successfully: {}", response.sessionId);
            }
            return response;
        } catch (Exception e) {
            log.error("Error creating payment session: ", e);
            throw new RuntimeException("Failed to create payment session: " + e.getMessage(), e);
        }
    }

    public static class PaymentSessionRequest {
        @JsonProperty("appointmentId")
        public UUID appointmentId;
        
        @JsonProperty("patientId")
        public UUID patientId;
        
        @JsonProperty("amount")
        public String amount;
        
        @JsonProperty("currency")
        public String currency;
        
        @JsonProperty("description")
        public String description;
        
        @JsonProperty("appointmentDate")
        public String appointmentDate;

        public PaymentSessionRequest(UUID appointmentId, UUID patientId, String amount, 
                                   String currency, String description, String appointmentDate) {
            this.appointmentId = appointmentId;
            this.patientId = patientId;
            this.amount = amount;
            this.currency = currency;
            this.description = description;
            this.appointmentDate = appointmentDate;
        }
    }

    public static class PaymentSessionResponse {
        @JsonProperty("sessionId")
        public String sessionId;
        
        @JsonProperty("checkoutUrl")
        public String checkoutUrl;
        
        @JsonProperty("status")
        public String status;

        public String getSessionId() {
            return sessionId;
        }

        public String getCheckoutUrl() {
            return checkoutUrl;
        }

        public String getStatus() {
            return status;
        }
    }
}