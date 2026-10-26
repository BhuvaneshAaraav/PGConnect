package com.pgconnect.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/** A conservative single-process IP limiter for endpoints that are costly to abuse. */
final class SensitiveEndpointRateLimitFilter extends OncePerRequestFilter {
    private static final Map<String, Rule> RULES = Map.of(
            "login", new Rule(10, 5 * 60_000L),
            "google-login", new Rule(10, 5 * 60_000L),
            "register", new Rule(5, 60 * 60_000L),
            "account-link", new Rule(3, 15 * 60_000L),
            "account-token", new Rule(12, 15 * 60_000L),
            "booking", new Rule(10, 10 * 60_000L));
    private static final Pattern BOOKING_PATH = Pattern.compile("/api/resident/properties/[^/]+/beds/[^/]+/bookings");
    private static final long ENTRY_TTL_MS = 60 * 60_000L;

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final AtomicInteger callsSinceCleanup = new AtomicInteger();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod()) || ruleKey(request.getRequestURI()) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String ruleKey = ruleKey(request.getRequestURI());
        Rule rule = RULES.get(ruleKey);
        String address = request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
        String key = ruleKey + ":" + address;
        long now = System.currentTimeMillis();
        long retryAfter = windows.computeIfAbsent(key, ignored -> new Window(now)).consume(now, rule);
        if (retryAfter > 0) {
            response.setStatus(429);
            response.setHeader("Retry-After", Long.toString(retryAfter));
            response.setHeader("Cache-Control", "no-store");
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Too many requests. Please wait before trying again.\"}");
            return;
        }
        cleanupOccasionally(now);
        chain.doFilter(request, response);
    }

    private String ruleKey(String path) {
        if (path == null) return null;
        return switch (path) {
            case "/api/auth/login" -> "login";
            case "/api/auth/google" -> "google-login";
            case "/api/auth/register" -> "register";
            case "/api/auth/email-verification/request", "/api/auth/password-reset/request" -> "account-link";
            case "/api/auth/email-verification/confirm", "/api/auth/password-reset/complete" -> "account-token";
            default -> BOOKING_PATH.matcher(path).matches() ? "booking" : null;
        };
    }

    private void cleanupOccasionally(long now) {
        if (callsSinceCleanup.incrementAndGet() < 1024) return;
        callsSinceCleanup.set(0);
        windows.entrySet().removeIf(entry -> now - entry.getValue().lastSeen > ENTRY_TTL_MS);
    }

    private record Rule(int maximum, long windowMs) {}

    private static final class Window {
        private long start;
        private volatile long lastSeen;
        private int count;

        private Window(long now) {
            start = now;
            lastSeen = now;
        }

        private synchronized long consume(long now, Rule rule) {
            lastSeen = now;
            if (now - start >= rule.windowMs || now < start) {
                start = now;
                count = 0;
            }
            if (count >= rule.maximum) return Math.max(1, (start + rule.windowMs - now + 999) / 1000);
            count++;
            return 0;
        }
    }
}
