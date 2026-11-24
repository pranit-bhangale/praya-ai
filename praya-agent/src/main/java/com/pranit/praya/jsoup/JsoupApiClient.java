package com.pranit.praya.jsoup;

import org.jsoup.nodes.Document;

import java.util.Optional;

/**
 * Reusable client contract for executing safe, rate-limited, and pre-configured Jsoup web scraping requests.
 */
public interface JsoupApiClient {

    /**
     * Fetches and parses an HTML document from the given URL using default browser headers and timeouts.
     *
     * @param url the web URL to connect and fetch
     * @return an Optional containing the parsed Jsoup Document, or empty if the fetch failed
     */
    Optional<Document> fetchDocument(String url);

    /**
     * Fetches and parses an HTML document from the given URL applying rate limiting under the specified key.
     *
     * @param url the web URL to connect and fetch
     * @param rateLimitKey the key used to acquire rate limit permits
     * @return an Optional containing the parsed Jsoup Document, or empty if the fetch failed
     */
    Optional<Document> fetchDocument(String url, String rateLimitKey);
}
