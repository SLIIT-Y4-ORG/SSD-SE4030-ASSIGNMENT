package com.example.paymentservice.controller;

import com.example.paymentservice.dto.CreateCheckoutSessionRequest;
import com.example.paymentservice.dto.CreateCheckoutSessionResponse;
import com.example.paymentservice.dto.PaymentTransactionResponse;
import com.example.paymentservice.dto.PaymentProfileResponse;
import com.example.paymentservice.security.AuthHelper;
import com.example.paymentservice.service.CheckoutService;
import com.example.paymentservice.service.PaymentProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * C-7 fix (CWE-306 — Missing Authentication for Critical Function):
 *
 * All user-facing endpoints now require a valid JWT via
 * "Authorization: Bearer <token>".  AuthHelper.requireBearer() calls
 * userService GET /api/auth/validate and throws UnauthorizedException (→ 401)
 * if the token is absent, malformed, or expired.
 *
 * The Stripe webhook endpoint (POST /webhooks/stripe) is intentionally excluded:
 * it is called by Stripe's servers (not by users), is already protected by
 * Stripe-Signature verification inside CheckoutService.handleStripeWebhook(),
 * and must remain publicly reachable for Stripe's webhook delivery.
 *
 * No changes were made to business logic, service calls, or response shapes.
 * GlobalExceptionHandler (Dev 3) is not touched.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentProfileService paymentProfileService;
    private final CheckoutService checkoutService;
    private final AuthHelper authHelper;

    public PaymentController(PaymentProfileService paymentProfileService,
                             CheckoutService checkoutService,
                             AuthHelper authHelper) {
        this.paymentProfileService = paymentProfileService;
        this.checkoutService       = checkoutService;
        this.authHelper            = authHelper;
    }

    /**
     * Get the Stripe payment profile (customer ID) for a user.
     * Requires: valid user JWT.
     */
    @GetMapping("/customers/{userId}")
    public ResponseEntity<PaymentProfileResponse> getByUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable UUID userId) {

        authHelper.requireBearer(authHeader);  // C-7 fix: must be authenticated
        return ResponseEntity.ok(paymentProfileService.getByUserId(userId));
    }

    /**
     * Create a Stripe checkout session (initiates payment).
     * Requires: valid user JWT.
     */
    @PostMapping("/checkout-session")
    public ResponseEntity<CreateCheckoutSessionResponse> createCheckoutSession(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody CreateCheckoutSessionRequest request) {

        authHelper.requireBearer(authHeader);  // C-7 fix
        return ResponseEntity.status(HttpStatus.CREATED).body(checkoutService.createCheckoutSession(request));
    }

    /**
     * Get transaction history for a user.
     * Requires: valid user JWT.
     */
    @GetMapping("/users/{userId}/transactions")
    public ResponseEntity<List<PaymentTransactionResponse>> getUserTransactions(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable UUID userId) {

        authHelper.requireBearer(authHeader);  // C-7 fix
        return ResponseEntity.ok(checkoutService.getTransactionsForUser(userId));
    }

    /**
     * Confirm a Stripe checkout session after payment redirect.
     * Requires: valid user JWT.
     */
    @PostMapping("/checkout-session/{sessionId}/confirm")
    public ResponseEntity<PaymentTransactionResponse> confirmCheckoutSession(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String sessionId) {

        authHelper.requireBearer(authHeader);  // C-7 fix
        return ResponseEntity.ok(checkoutService.confirmCheckoutSession(sessionId));
    }

    /**
     * Stripe webhook — intentionally NOT protected by user auth.
     *
     * This endpoint is called by Stripe's servers, not by users.
     * It is already secured by Stripe-Signature verification inside
     * CheckoutService.handleStripeWebhook() (HMAC-SHA256 of the payload against
     * the configured webhook secret).  Adding Bearer-token auth here would
     * break Stripe's webhook delivery.
     */
    @PostMapping("/webhooks/stripe")
    public ResponseEntity<Void> receiveStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {

        checkoutService.handleStripeWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }
}
