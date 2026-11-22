package com.pranit.praya.portfolio;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pranit.praya.cache.config.CacheConfig;
import com.pranit.praya.constant.ApiConstants;
import com.pranit.praya.constant.ProfileConstants;
import com.pranit.praya.model.BlogArticle;
import com.pranit.praya.model.DataSourceType;
import com.pranit.praya.model.KnowledgeItem;
import com.pranit.praya.model.ToolResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AI Tool specializing in Pranit's personal portfolio website.
 * Implements two-tier progressive disclosure: high-level summaries and on-demand deep dives.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PortfolioTools {

    private final PortfolioWebsiteSource portfolioWebsiteSource;
    private final ObjectMapper objectMapper;

    /**
     * Tier 1 (Summary): Returns high-level headline bio and official links.
     */
    @Tool(description = """
            [TIER 1 - HIGH-LEVEL SUMMARY]
            Retrieves Pranit Bhangale's high-level bio tagline, verified official profile links, and contact handles.
            
            Returns:
              A JSON object containing:
              - name: Full legal/professional name
              - bio: High-level engineering identity and summary
              - portfolioUrl: Official live portfolio website URL
              - githubUrl: Official GitHub profile link
              - linkedinUrl: Official LinkedIn profile link
            
            When to call:
              - The user asks general introductory questions such as "Who is Pranit?", "Give me his bio",
                "Where can I find his website?", or asks for links to his GitHub or LinkedIn profiles.
            
            When NOT to call:
              - Do NOT call when the user asks for deep personal story, comprehensive about-me sections,
                or detailed website narrative (use 'getPortfolioDeepDive' instead).
            """)
    @Cacheable(cacheNames = CacheConfig.CACHE_PORTFOLIO_KNOWLEDGE, key = "'portfolio-summary'")
    public String getPortfolioSummary() {
        Map<String, String> summary = Map.of(
                "name", ProfileConstants.NAME,
                "bio", ProfileConstants.DEFAULT_BIO,
                "portfolioUrl", ProfileConstants.PORTFOLIO_URL,
                "githubUrl", ProfileConstants.GITHUB_URL,
                "linkedinUrl", ProfileConstants.LINKEDIN_URL
        );

        return serializeToJson(ToolResponse.<Map<String, String>>builder()
                .status(ApiConstants.STATUS_SUCCESS)
                .source(DataSourceType.PORTFOLIO_WEBSITE)
                .count(1)
                .data(summary)
                .build());
    }

    /**
     * Tier 2 (Deep Dive): Returns complete website narrative and all scraped sections.
     */
    @Tool(description = """
            [TIER 2 - DEEP DIVE]
            Retrieves the full narrative text, comprehensive about-me sections, and detailed journey
            from Pranit Bhangale's live portfolio website.
            
            Returns:
              A JSON array of KnowledgeItem objects covering:
              - Detailed biographical story, motivations, and engineering passions
              - Full scraped narrative content from the portfolio sections
            
            When to call:
              - The user explicitly asks for deep personal background, complete about-me story, or extensive details
                from his portfolio website.
            
            When NOT to call:
              - Do NOT call for a quick bio or profile links (use 'getPortfolioSummary' instead).
            """)
    @Cacheable(cacheNames = CacheConfig.CACHE_PORTFOLIO_KNOWLEDGE, key = "'portfolio-deep-dive'")
    public String getPortfolioDeepDive() {
        List<KnowledgeItem> items = this.portfolioWebsiteSource.fetchKnowledge();
        return serializeToJson(ToolResponse.<List<KnowledgeItem>>builder()
                .status(ApiConstants.STATUS_SUCCESS)
                .source(DataSourceType.PORTFOLIO_WEBSITE)
                .count(items.size())
                .data(items)
                .build());
    }

    /**
     * Dedicated Tool: Returns technical blog articles, architecture write-ups, and engineering case studies.
     */
    @Tool(description = """
            [TECHNICAL BLOG ARTICLES]
            Retrieves Pranit Bhangale's published technical blog articles, architecture case studies, and engineering write-ups.
            
            Returns:
              A JSON array of BlogArticle objects containing:
              - id: Unique article identifier
              - title: Article headline
              - summary: Core technical premise and systems engineering insights
              - url: Direct article link or portfolio blog section
              - tags: Topic tags (e.g. STOMP, Clean Architecture, Reactive Streams, ESP32)
              - readingTime: Estimated reading duration (e.g. '6 min read')
            
            When to call:
              - The user asks about Pranit's blog posts, technical articles, engineering write-ups,
                system design tutorials, or publications.
            
            When NOT to call:
              - Do NOT call for resume/education queries (use 'listEducationSummary' or 'fetchCertifications').
            """)
    @Cacheable(cacheNames = CacheConfig.CACHE_PORTFOLIO_KNOWLEDGE, key = "'blog-articles'")
    public String fetchBlogArticles() {
        List<BlogArticle> articles = List.of(
                BlogArticle.builder()
                        .id("blog-realtime-stomp")
                        .title("Engineering High-Throughput Real-Time Chat with Spring Boot & STOMP WebSockets")
                        .summary("Architectural breakdown of bidirectional STOMP messaging, heartbeat connection leasing, and AES-256 payload encryption at rest.")
                        .url(ProfileConstants.PORTFOLIO_URL + "/blog/realtime-chat")
                        .tags(List.of("Spring Boot", "WebSockets", "STOMP", "Cryptography", "Java 21"))
                        .readingTime("7 min read")
                        .build(),
                BlogArticle.builder()
                        .id("blog-clean-architecture")
                        .title("Decoupling Business Logic with Clean Architecture in Modern Backend Systems")
                        .summary("How to isolate domain entities, use cases, and repository interfaces from Spring framework annotations and database drivers.")
                        .url(ProfileConstants.PORTFOLIO_URL + "/blog/clean-architecture")
                        .tags(List.of("Clean Architecture", "SOLID", "Domain-Driven Design", "Java"))
                        .readingTime("5 min read")
                        .build(),
                BlogArticle.builder()
                        .id("blog-iot-freertos")
                        .title("Multitasking and Obstacle Navigation on ESP32 Microcontrollers with FreeRTOS")
                        .summary("Hardware telemetry and concurrent dual-core task scheduling for autonomous robotics using C++ and FreeRTOS queues.")
                        .url(ProfileConstants.PORTFOLIO_URL + "/blog/esp32-freertos")
                        .tags(List.of("ESP32", "FreeRTOS", "C++", "IoT", "Robotics"))
                        .readingTime("6 min read")
                        .build()
        );

        return serializeToJson(ToolResponse.<List<BlogArticle>>builder()
                .status(ApiConstants.STATUS_SUCCESS)
                .source(DataSourceType.PORTFOLIO_WEBSITE)
                .count(articles.size())
                .data(articles)
                .build());
    }

    private <T> String serializeToJson(ToolResponse<T> response) {
        try {
            return this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
        } catch (JsonProcessingException e) {
            log.error("[TOOL] [PORTFOLIO-SERIALIZE-ERROR] Failed to serialize portfolio tool response: {}", e.getMessage(), e);
            return "{\"status\":\"" + ApiConstants.STATUS_ERROR + "\",\"message\":\"JSON serialization failed: " + e.getMessage() + "\"}";
        }
    }
}
