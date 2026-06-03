package com.quickdeliver.controller;

import com.quickdeliver.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Step 1 — Frontend calls this after placing order.
     * Returns { redirectUrl, merchantTransactionId }
     * Frontend does: window.location.href = redirectUrl
     */
    @PostMapping("/phonepe/initiate/{orderId}")
    public ResponseEntity<Map<String, String>> initiatePayment(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.initiatePayment(orderId));
    }

    /**
     * Step 2 — Frontend polls this after PhonePe redirects browser back.
     * Returns { state, success, orderId }
     */
    @GetMapping("/phonepe/status/{merchantTransactionId}")
    public ResponseEntity<Map<String, String>> checkStatus(
            @PathVariable String merchantTransactionId) {
        return ResponseEntity.ok(paymentService.checkStatus(merchantTransactionId));
    }

    /**
     * Webhook — PhonePe calls this server-to-server.
     * Must be a publicly reachable URL — use ngrok for local testing:
     *   ngrok http 8080
     *   then set phonepe.callback.url=https://xxxx.ngrok.io/api/payments/phonepe/webhook
     */
    @PostMapping("/phonepe/webhook")
    public ResponseEntity<Map<String, String>> webhook(
            @RequestHeader("Authorization")  String authHeader,
            @RequestBody Map<String, Object> body) {

        log.info("PhonePe webhook received");
        paymentService.handleWebhook(authHeader, body);
        return ResponseEntity.ok(Map.of("message", "OK"));
    }

    /**
     * COD — delivery agent marks cash collected on delivery.
     */
    @PostMapping("/cod/collected/{orderId}")
    public ResponseEntity<Map<String, String>> markCodCollected(@PathVariable Long orderId) {
        paymentService.markCodCollected(orderId);
        return ResponseEntity.ok(Map.of("message", "COD collected. Order marked delivered."));
    }
}
