package com.example.paymentservice.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.paymentservice.dto.CreateCheckoutSessionRequest;
import com.example.paymentservice.dto.CreateCheckoutSessionResponse;
import com.example.paymentservice.dto.PaymentProfileResponse;
import com.example.paymentservice.dto.PaymentTransactionResponse;
import com.example.paymentservice.service.AuthService;
import com.example.paymentservice.service.CheckoutService;
import com.example.paymentservice.service.PaymentProfileService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentProfileService paymentProfileService;
    private final CheckoutService checkoutService;
    private final AuthService authService;

    public PaymentController(PaymentProfileService paymentProfileService,
            CheckoutService checkoutService,
            AuthService authService) {
        this.paymentProfileService = paymentProfileService;
        this.checkoutService = checkoutService;
        this.authService = authService;
    }

    @GetMapping("/customers/{userId}")
    public ResponseEntity<PaymentProfileResponse> getByUser(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID userId) {
        authService.requirePatientAccess(userId, authHeader);
        return ResponseEntity.ok(paymentProfileService.getByUserId(userId));
    }

    @PostMapping("/checkout-session")
    public ResponseEntity<CreateCheckoutSessionResponse> createCheckoutSession(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody CreateCheckoutSessionRequest request) {
        authService.requirePatientAccess(request.userId(), authHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(checkoutService.createCheckoutSession(request));
    }

    @GetMapping("/users/{userId}/transactions")
    public ResponseEntity<List<PaymentTransactionResponse>> getUserTransactions(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID userId) {
        authService.requirePatientAccess(userId, authHeader);
        return ResponseEntity.ok(checkoutService.getTransactionsForUser(userId));
    }

    @PostMapping("/checkout-session/{sessionId}/confirm")
    public ResponseEntity<PaymentTransactionResponse> confirmCheckoutSession(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String sessionId) {
        authService.requirePatientAccess(checkoutService.getTransactionUserId(sessionId), authHeader);
        return ResponseEntity.ok(checkoutService.confirmCheckoutSession(sessionId));
    }

    @PostMapping("/webhooks/stripe")
    public ResponseEntity<Void> receiveStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        checkoutService.handleStripeWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }
}
