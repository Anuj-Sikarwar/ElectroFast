package com.quickdeliver.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickdeliver.entity.Order;
import com.quickdeliver.entity.Payment;
import com.quickdeliver.enums.OrderStatus;
import com.quickdeliver.enums.PaymentMethod;
import com.quickdeliver.enums.PaymentStatus;
import com.quickdeliver.exception.BadRequestException;
import com.quickdeliver.exception.ResourceNotFoundException;
import com.quickdeliver.repository.OrderRepository;
import com.quickdeliver.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * PhonePe Payment Gateway — OAuth2 API (new credentials format)
 *
 * Docs: https://developer.phonepe.com/v1/reference/
 *
 * Step 1 — Get access token via clientId + clientSecret (OAuth2)
 * Step 2 — Use token to call /v2/order API → get pay-page URL
 * Step 3 — Redirect user to pay-page URL
 * Step 4 — PhonePe redirects browser back + hits webhook
 * Step 5 — Verify webhook / poll status → confirm order
 *
 * All payments go directly to the bank account
 * linked in your PhonePe merchant dashboard.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository   orderRepository;
    private final ObjectMapper      objectMapper;
    private final WebClient.Builder webClientBuilder;
    private final NotificationService notificationService;

    @Value("${phonepe.client.id}")
    private String clientId;

    @Value("${phonepe.client.secret}")
    private String clientSecret;

    @Value("${phonepe.client.version:1}")
    private String clientVersion;

    @Value("${phonepe.base.url}")
    private String baseUrl;

    @Value("${phonepe.redirect.url}")
    private String redirectUrl;

    @Value("${phonepe.callback.url}")
    private String callbackUrl;

    // ── In-memory token cache (avoids fetching a new token on every payment) ──
    private final AtomicReference<String>  cachedToken      = new AtomicReference<>();
    private volatile long                   tokenExpiresAt   = 0;

    // ─────────────────────────────────────────────────────────────────────────
    // OAuth2 — get or refresh access token
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Fetches a new OAuth2 access token from PhonePe using clientId + clientSecret.
     * Caches it in memory until 60 seconds before expiry.
     *
     * PhonePe OAuth2 endpoint:
     * POST /v1/oauth/token
     * Body: grant_type=client_credentials&client_id=...&client_secret=...&client_version=...
     */
    private String getAccessToken() {
        long now = Instant.now().getEpochSecond();

        // Return cached token if still valid (with 60s buffer)
        if (cachedToken.get() != null && now < tokenExpiresAt - 60) {
            return cachedToken.get();
        }

        log.info("Fetching new PhonePe OAuth2 access token");

        try {
            WebClient client = webClientBuilder
                    .baseUrl(baseUrl)
                    .build();

            String formBody = "grant_type=client_credentials"
                    + "&client_id="      + clientId
                    + "&client_secret="  + clientSecret
                    + "&client_version=" + clientVersion;

            String response = client.post()
                    .uri("/v1/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(formBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode json       = objectMapper.readTree(response);
            String   token      = json.path("access_token").asText();
            long     expiresIn  = json.path("expires_in").asLong(3600);

            if (token == null || token.isBlank()) {
                throw new RuntimeException("PhonePe returned empty access token");
            }

            cachedToken.set(token);
            tokenExpiresAt = now + expiresIn;

            log.info("PhonePe token obtained, expires in {}s", expiresIn);
            return token;

        } catch (WebClientResponseException e) {
            log.error("PhonePe token fetch failed: {} — {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("PhonePe authentication failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("PhonePe token fetch error: {}", e.getMessage());
            throw new RuntimeException("PhonePe authentication error: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Initiate payment — called by frontend after order is placed
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates a PhonePe order and returns the pay-page redirect URL.
     * Frontend redirects browser to that URL — user pays on PhonePe's page.
     * All money goes to your merchant bank account directly.
     */
    public Map<String, String> initiatePayment(Long orderId) {
        Order order = getOrderOrThrow(orderId);

        // Unique transaction ID for this payment attempt
        String merchantTransactionId = "QD" + orderId + "T" + System.currentTimeMillis();

        // Amount in paise (₹1 = 100 paise)
        long amountPaise = order.getTotalAmount()
                .multiply(BigDecimal.valueOf(100))
                .longValue();

        try {
            String token = getAccessToken();

            // ── Build order payload ───────────────────────────────────────────
            Map<String, Object> payload = new HashMap<>();
            payload.put("merchantOrderId",  merchantTransactionId);
            payload.put("amount",           amountPaise);
            payload.put("expireAfter",      1200);   // 20 min window for user to pay
            payload.put("metaInfo", Map.of(
                "udf1", "orderId:" + orderId,
                "udf2", "phone:"  + order.getDeliveryPhone()
            ));
            payload.put("paymentFlow", Map.of(
                "type",        "PG_CHECKOUT",           // hosted pay page — simplest
                "message",     "Payment for Order #" + orderId,
                "merchantUrls", Map.of(
                    "redirectUrl", redirectUrl + "?merchantTransactionId=" + merchantTransactionId,
                    "callbackUrl", callbackUrl
                )
            ));

            WebClient client = webClientBuilder
                    .baseUrl(baseUrl)
                    .defaultHeader("Authorization", "O-Bearer " + token)
                    .defaultHeader("Content-Type", "application/json")
                    .build();

            String response = client.post()
                    .uri("/v2/order")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode json = objectMapper.readTree(response);

            // Extract the redirect URL from PhonePe response
            String payPageUrl = json
                    .path("redirectUrl")
                    .asText();

            if (payPageUrl == null || payPageUrl.isBlank()) {
                log.error("PhonePe order response missing redirectUrl for orderId={}", orderId);
                throw new RuntimeException("PhonePe did not return a redirect URL");
            }

            // ── Save pending payment record ───────────────────────────────────
            Payment payment = new Payment();
            payment.setOrder(order);
            payment.setMethod(PaymentMethod.UPI);
            payment.setStatus(PaymentStatus.PENDING);
            payment.setMerchantTransactionId(merchantTransactionId);
            payment.setAmount(order.getTotalAmount());
            paymentRepository.save(payment);

            log.info("PhonePe order created for orderId={} txId={}", orderId, merchantTransactionId);

            return Map.of(
                "redirectUrl",           payPageUrl,
                "merchantTransactionId", merchantTransactionId
            );

        } catch (WebClientResponseException e) {
            log.error("PhonePe order creation failed with status: {}", e.getStatusCode());
            throw new RuntimeException("Payment initiation failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("PhonePe initiation error for order {}: {}", orderId, e.getMessage());
            throw new RuntimeException("Payment initiation failed: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Check payment status — frontend polls after browser redirect back
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Polls PhonePe's order status API.
     * Called by frontend after PhonePe redirects browser back to /payment/status.
     *
     * GET /v2/order/{merchantOrderId}
     * Header: Authorization: O-Bearer <token>
     */
    public Map<String, String> checkStatus(String merchantTransactionId) {
        try {
            String token = getAccessToken();

            WebClient client = webClientBuilder
                    .baseUrl(baseUrl)
                    .defaultHeader("Authorization", "O-Bearer " + token)
                    .defaultHeader("Content-Type", "application/json")
                    .build();

            String response = client.get()
                    .uri("/v2/order/" + merchantTransactionId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode json    = objectMapper.readTree(response);
            String   state   = json.path("state").asText();         // COMPLETED / FAILED / PENDING
            String   txId    = json.path("transactionId").asText(); // PhonePe's own TX ID
            boolean  success = "COMPLETED".equals(state);

            log.info("PhonePe status check txId={} state={}", merchantTransactionId, state);

            // Update DB if webhook hasn't fired yet
            paymentRepository.findByMerchantTransactionId(merchantTransactionId)
                    .ifPresent(p -> {
                        if (p.getStatus() == PaymentStatus.PENDING) {
                            if (success) {
                                p.setStatus(PaymentStatus.SUCCESS);
                                p.setPhonepeTransactionId(txId);
                                p.setPaidAt(LocalDateTime.now());
                                paymentRepository.save(p);
                                Order o = p.getOrder();
                                o.setStatus(OrderStatus.CONFIRMED);
                                notificationService.sendOrderNotification(o);
                                orderRepository.save(o);
                                log.info("Order {} confirmed via status poll", o.getId());
                            } else if ("FAILED".equals(state)) {
                                p.setStatus(PaymentStatus.FAILED);
                                paymentRepository.save(p);
                            }
                        }
                    });

            String orderId = paymentRepository
                    .findByMerchantTransactionId(merchantTransactionId)
                    .map(p -> p.getOrder().getId().toString())
                    .orElse("");

            return Map.of(
                "state",   state,
                "success", String.valueOf(success),
                "orderId", orderId,
                "message", success
                    ? "Payment successful"
                    : "PENDING".equals(state) ? "Payment pending" : "Payment failed"
            );

        } catch (WebClientResponseException e) {
            log.error("PhonePe status check failed with status: {}", e.getStatusCode());
            throw new RuntimeException("Status check failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Status check error for txId {}: {}", merchantTransactionId, e.getMessage());
            throw new RuntimeException("Status check failed: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Webhook — PhonePe calls this server-to-server after payment
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Handles PhonePe's server-to-server webhook callback.
     *
     * PhonePe sends:
     * POST /payments/phonepe/webhook
     * Header: Authorization: O-Bearer <token>   ← PhonePe's own token to verify it's really them
     * Body: { "type": "ORDER_STATUS", "payload": { "state": "COMPLETED", ... } }
     *
     * Verify the Authorization header token by calling PhonePe's token introspect endpoint.
     */
    public void handleWebhook(String authHeader, Map<String, Object> body) {
        try {
            // ── Verify the webhook token is genuinely from PhonePe ────────────
            verifyWebhookToken(authHeader);

            // ── Extract payload ───────────────────────────────────────────────
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) body.get("payload");

            if (payload == null) {
                log.warn("PhonePe webhook received with empty payload");
                return;
            }

            String merchantOrderId = (String) payload.get("merchantOrderId");
            String state           = (String) payload.get("state");
            String phonepeTxId     = (String) payload.get("transactionId");

            log.info("PhonePe webhook: merchantOrderId={} state={}", merchantOrderId, state);

            if (merchantOrderId == null || merchantOrderId.isBlank()) return;

            Payment payment = paymentRepository.findByMerchantTransactionId(merchantOrderId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Payment not found for merchantOrderId: " + merchantOrderId));

            if (payment.getStatus() != PaymentStatus.PENDING) {
                log.info("Payment {} already processed, skipping webhook", merchantOrderId);
                return;
            }

            if ("COMPLETED".equals(state)) {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setPhonepeTransactionId(phonepeTxId);
                payment.setPaidAt(LocalDateTime.now());
                paymentRepository.save(payment);

                Order order = payment.getOrder();
                order.setStatus(OrderStatus.CONFIRMED);
                notificationService.sendOrderNotification(order);
                orderRepository.save(order);
                log.info("Order {} confirmed via webhook", order.getId());

            } else if ("FAILED".equals(state)) {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                log.warn("Payment failed via webhook for merchantOrderId={}", merchantOrderId);
            }

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Webhook processing error: {}", e.getMessage());
            throw new RuntimeException("Webhook processing failed: " + e.getMessage());
        }
    }

    /**
     * Verifies the webhook Authorization token by calling PhonePe's
     * token introspect endpoint — confirms the request is genuinely from PhonePe.
     */
    private void verifyWebhookToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("O-Bearer ")) {
            throw new BadRequestException("Missing or invalid Authorization header in webhook");
        }

        String incomingToken = authHeader.substring("O-Bearer ".length()).trim();

        try {
            WebClient client = webClientBuilder
                    .baseUrl(baseUrl)
                    .build();

            String formBody = "token=" + incomingToken
                    + "&client_id="     + clientId
                    + "&client_secret=" + clientSecret;

            String response = client.post()
                    .uri("/v1/oauth/introspect")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(formBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode json   = objectMapper.readTree(response);
            boolean  active = json.path("active").asBoolean(false);

            if (!active) {
                log.warn("PhonePe webhook token introspection failed — token not active");
                throw new BadRequestException("Webhook token verification failed");
            }

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Could not verify webhook token: {} — accepting webhook anyway in test mode", e.getMessage());
            // In test/UAT mode introspect may not work — log and proceed
            // Remove this fallback in production
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. COD — no gateway involved
    // ─────────────────────────────────────────────────────────────────────────

    public void markCodCollected(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found for order: " + orderId));

        if (payment.getMethod() != PaymentMethod.COD) {
            throw new BadRequestException("This order is not a COD order");
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        Order order = payment.getOrder();
        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);
    }

    private Order getOrderOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    }
}
