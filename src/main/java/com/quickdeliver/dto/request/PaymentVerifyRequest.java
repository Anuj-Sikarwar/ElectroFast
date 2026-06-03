package com.quickdeliver.dto.request;

import lombok.Data;

/**
 * Payload received from PhonePe's server-to-server webhook callback.
 *
 * PhonePe POST body:
 * {
 *   "response": "<base64-encoded JSON>",    ← the actual payload
 * }
 *
 * The X-VERIFY header from PhonePe = SHA256(response) + "###" + saltIndex
 * We verify that header before trusting the body.
 */
@Data
public class PaymentVerifyRequest {
    // base64-encoded response string sent by PhonePe
    private String response;
}
