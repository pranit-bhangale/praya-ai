package com.pranit.praya.ratelimit.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Resolves and sanitizes client identities (IP, SpEL evaluation values, PII hashes).
 * Uses secure remote address resolution respecting Spring's forward-headers-strategy.
 */
@Component
public class ClientIdentity {

    /**
     * Hashes identifiers (conversationId, secret values) to 16 hex chars using SHA-256.
     */
    public static String hash(String value) {
        if (value == null) {
            return "anonymous";
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm missing", e);
        }
    }

    /**
     * Resolves the real client IP.
     * Uses getRemoteAddr() which is populated by Spring Boot's ForwardedHeaderFilter
     * (configured via server.forward-headers-strategy=framework) to prevent header spoofing.
     * Normalizes IPv6 addresses to /64 prefixes so cycling subnet addresses cannot bypass limits.
     */
    public String ip(HttpServletRequest req) {
        if (req == null) {
            return "local";
        }
        String raw = req.getRemoteAddr();
        if (raw == null || raw.isBlank()) {
            return "unknown-ip";
        }

        try {
            InetAddress addr = InetAddress.getByName(raw);
            if (addr instanceof Inet6Address v6) {
                return HexFormat.of().formatHex(v6.getAddress(), 0, 8) + "::/64";
            }
        } catch (UnknownHostException ignored) {
        }
        return raw;
    }
}
