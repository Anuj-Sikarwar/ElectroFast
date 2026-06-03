package com.quickdeliver.util;

import org.springframework.stereotype.Component;

@Component
public class InputSanitizer {

    // Strip characters that could be used in injection attempts
    public String sanitize(String input) {
        if (input == null) return null;
        return input
                .trim()
                .replaceAll("[<>\"'%;()&+\\\\]", "")  // strip dangerous chars
                .replaceAll("\\s+", " ")               // collapse multiple spaces
                .substring(0, Math.min(input.length(), 500)); // max length
    }

    // For search queries specifically
    public String sanitizeSearch(String input) {
        if (input == null) return null;
        return input
                .trim()
                .replaceAll("[<>\"'%;()&+\\\\]", "")
                .replaceAll("\\s+", " ")
                .substring(0, Math.min(input.length(), 100));
    }

    // For phone numbers — digits only
    public String sanitizePhone(String phone) {
        if (phone == null) return null;
        return phone.replaceAll("[^0-9]", "");
    }

    // For email — lowercase and trim
    public String sanitizeEmail(String email) {
        if (email == null) return null;
        return email.trim().toLowerCase();
    }
}