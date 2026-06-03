package com.quickdeliver.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter implements Filter {

    private final Map<String, Bucket> loginBuckets    = new ConcurrentHashMap<>();
    private final Map<String, Bucket> registerBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> paymentBuckets  = new ConcurrentHashMap<>();
    private final Map<String, Bucket> locationBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> orderBuckets    = new ConcurrentHashMap<>();

    // 5 attempts per 15 minutes
    private Bucket loginBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(15))))
                .build();
    }

    // 5 registrations per hour
    private Bucket registerBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(5, Refill.intervally(5, Duration.ofHours(1))))
                .build();
    }

    // 10 payment initiations per hour
    private Bucket paymentBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(10, Refill.intervally(10, Duration.ofHours(1))))
                .build();
    }

    // 30 location resolves per minute
    private Bucket locationBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(30, Refill.intervally(30, Duration.ofMinutes(1))))
                .build();
    }

    // 20 orders per hour
    private Bucket orderBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(20, Refill.intervally(20, Duration.ofHours(1))))
                .build();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  req  = (HttpServletRequest)  request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String path   = req.getRequestURI();
        String method = req.getMethod();
        String ip     = getClientIp(req);

        Bucket bucket = null;

        if (path.contains("/auth/login")) {
            bucket = loginBuckets.computeIfAbsent(ip, k -> loginBucket());

        } else if (path.contains("/auth/register")) {
            bucket = registerBuckets.computeIfAbsent(ip, k -> registerBucket());

        } else if (path.contains("/payments/phonepe/initiate")) {
            bucket = paymentBuckets.computeIfAbsent(ip, k -> paymentBucket());

        } else if (path.contains("/location/resolve")) {
            bucket = locationBuckets.computeIfAbsent(ip, k -> locationBucket());

        } else if (path.contains("/orders") && method.equals("POST")) {
            bucket = orderBuckets.computeIfAbsent(ip, k -> orderBucket());
        }

        // No rate limit applies to this endpoint
        if (bucket == null) {
            chain.doFilter(request, response);
            return;
        }

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            resp.setStatus(429);
            resp.setContentType("application/json");
            resp.getWriter().write(
                    "{\"message\":\"Too many requests. Please slow down.\",\"status\":429}"
            );
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}