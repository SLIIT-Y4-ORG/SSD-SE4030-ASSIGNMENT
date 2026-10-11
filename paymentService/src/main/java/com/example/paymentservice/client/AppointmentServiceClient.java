package com.example.paymentservice.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class AppointmentServiceClient {

    private final RestTemplate restTemplate;
    private final String appointmentServiceUrl;
    private final String internalApiKey;

    public AppointmentServiceClient(RestTemplate restTemplate,
            @Value("${services.appointment.base-url}") String appointmentServiceUrl,
            @Value("${payment.security.internal-api-key}") String internalApiKey) {
        this.restTemplate = restTemplate;
        this.appointmentServiceUrl = appointmentServiceUrl;
        this.internalApiKey = internalApiKey;
    }

    public void notifyPaymentCompleted(UUID appointmentId, UUID transactionId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Api-Key", internalApiKey);

        Map<String, Object> payload = new HashMap<>();
        payload.put("appointmentId", appointmentId);
        payload.put("paymentStatus", "success");
        payload.put("transactionId", transactionId);

        restTemplate.postForEntity(
                appointmentServiceUrl + "/api/appointments/payment-callback",
                new HttpEntity<>(payload, headers),
                Void.class);
    }
}
