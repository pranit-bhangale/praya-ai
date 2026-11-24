package com.pranit.praya.jsoup;

import com.pranit.praya.constant.ApiConstant;
import com.pranit.praya.ratelimit.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

/**
 * Production implementation of {@link JsoupApiClient} providing resilient, rate-limited,
 * and pre-configured web scraping via Jsoup.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JsoupApiClientImpl implements JsoupApiClient {

    public static final int DEFAULT_TIMEOUT_MS = 6000;

    private final RateLimiter rateLimiter;

    @Override
    public Optional<Document> fetchDocument(String url) {
        return fetchDocument(url, null);
    }

    @Override
    public Optional<Document> fetchDocument(String url, String rateLimitKey) {
        if (url == null || url.isBlank()) return Optional.empty();
        if (rateLimitKey != null && !rateLimitKey.isBlank()) this.rateLimiter.acquire(rateLimitKey);
        final long startTime = System.currentTimeMillis();
        log.info("[JSOUP-CLIENT] [FETCH-START] Connecting to URL: {}", url);
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(ApiConstant.BROWSER_USER_AGENT)
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .timeout(DEFAULT_TIMEOUT_MS)
                    .followRedirects(true)
                    .get();
            final long durationMs = System.currentTimeMillis() - startTime;
            log.info("[JSOUP-CLIENT] [FETCH-SUCCESS] Retrieved document from: {} (title: '{}', durationMs: {})",
                    url, doc.title(), durationMs);
            return Optional.of(doc);
        } catch (HttpStatusException e) {
            log.warn("[JSOUP-CLIENT] [HTTP-ERROR] HTTP {} when fetching {} (reason: {})",
                    e.getStatusCode(), url, e.getMessage());
        } catch (IOException e) {
            log.warn("[JSOUP-CLIENT] [IO-ERROR] Connection error fetching {}: {}", url, e.getMessage());
        } catch (Exception e) {
            log.error("[JSOUP-CLIENT] [UNEXPECTED-ERROR] Unexpected error fetching {}: {}", url, e.getMessage(), e);
        }
        return Optional.empty();
    }
}
