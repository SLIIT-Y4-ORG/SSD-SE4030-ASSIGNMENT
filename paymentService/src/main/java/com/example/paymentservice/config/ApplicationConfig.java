package com.example.paymentservice.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(StripeProperties.class)
public class ApplicationConfig {

    private final StripeProperties stripeProperties;

    public ApplicationConfig(StripeProperties stripeProperties) {
        this.stripeProperties = stripeProperties;
    }

    @PostConstruct
    void initializeStripe() {
        if (stripeProperties.apiKey() != null && !stripeProperties.apiKey().isBlank()) {
            Stripe.apiKey = stripeProperties.apiKey();
            // Keep Stripe calls bounded so internal payment endpoint doesn't hang indefinitely.
            Stripe.setConnectTimeout(10_000);
            Stripe.setReadTimeout(20_000);
            Stripe.setMaxNetworkRetries(1);
        }
    }

    /**
     * C-7 fix (CWE-306): RestTemplate used by UserServiceClient to call userService
     * GET /api/auth/validate for Bearer token validation on public PaymentController endpoints.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
