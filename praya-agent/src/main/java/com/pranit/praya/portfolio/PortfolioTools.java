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
import java.util.Optional;

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
     * Dedicated Tool: Returns verified direct contact information (Email, LinkedIn, Portfolio).
     */
    @Tool(description = """
            [CONTACT INFORMATION]
            Retrieves verified contact details and direct reach-out links for Pranit Bhangale.
            
            Returns:
              A JSON object containing:
              - name: Full name
              - email: Official direct email address (pranitbhangale14@gmail.com)
              - linkedinUrl: Official LinkedIn profile URL
              - portfolioUrl: Official live portfolio URL
              - githubUrl: Official GitHub profile URL
              - preferredChannel: Recommended communication channels for engineering opportunities
            
            When to call:
              - The user or client asks "How can I contact Pranit?", "What is his email?", "How do I reach out?",
                "Provide his contact info", "How to get in touch?", or asks for ways to connect with him.
            
            When NOT to call:
              - Do NOT call for technical skills inquiries (use 'getSkillsSummary') or project details (use 'getProjectDetails').
            """)
    @Cacheable(cacheNames = CacheConfig.CACHE_PORTFOLIO_KNOWLEDGE, key = "'contact-info'")
    public String getContactInformation() {
        Map<String, String> contactInfo = Map.of(
                "name", ProfileConstants.NAME,
                "email", ProfileConstants.EMAIL,
                "linkedinUrl", ProfileConstants.LINKEDIN_URL,
                "portfolioUrl", ProfileConstants.PORTFOLIO_URL,
                "githubUrl", ProfileConstants.GITHUB_URL,
                "preferredChannel", "Email (" + ProfileConstants.EMAIL + ") or LinkedIn for direct engineering and collaboration inquiries."
        );
        return serializeToJson(ToolResponse.success(DataSourceType.PORTFOLIO_WEBSITE, 1, contactInfo));
    }

    private <T> String serializeToJson(ToolResponse<T> response) {
        try {
            return this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
        } catch (JsonProcessingException e) {
            log.error("[TOOL] [PORTFOLIO-SERIALIZE-ERROR] Failed to serialize portfolio tool response: {}", e.getMessage(), e);
            return "{\"status\":\"" + ApiConstants.STATUS_ERROR + "\",\"message\":\"JSON serialization failed: " + e.getMessage() + "\"}";
        }
    }

    /**
     * Returns high-level headline bio and official links.
     */
    @Tool(description = """
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
        try {
            List<KnowledgeItem> items = this.portfolioWebsiteSource.fetchKnowledge();
            String bio = ProfileConstants.DEFAULT_BIO;
            String title = ProfileConstants.NAME + " — Systems & Backend Engineer";
            if (!items.isEmpty()) {
                Optional<KnowledgeItem> bioItem = items.stream()
                        .filter(k -> ProfileConstants.CATEGORY_BIO.equalsIgnoreCase(k.category()))
                        .findFirst();
                if (bioItem.isPresent()) {
                    title = bioItem.get().title();
                    bio = bioItem.get().content();
                }
            }

            Map<String, String> summary = Map.of(
                    "name", ProfileConstants.NAME,
                    "title", title,
                    "bio", bio,
                    "portfolioUrl", ProfileConstants.PORTFOLIO_URL,
                    "githubUrl", ProfileConstants.GITHUB_URL,
                    "linkedinUrl", ProfileConstants.LINKEDIN_URL
            );
            return serializeToJson(ToolResponse.success(DataSourceType.PORTFOLIO_WEBSITE, 1, summary));
        } catch (Exception e) {
            log.error("[TOOL] [PORTFOLIO-SUMMARY-ERROR] Live fetch error: {}", e.getMessage(), e);
            Map<String, String> summary = Map.of(
                    "name", ProfileConstants.NAME,
                    "bio", ProfileConstants.DEFAULT_BIO,
                    "portfolioUrl", ProfileConstants.PORTFOLIO_URL,
                    "githubUrl", ProfileConstants.GITHUB_URL,
                    "linkedinUrl", ProfileConstants.LINKEDIN_URL
            );
            return serializeToJson(ToolResponse.success(DataSourceType.PORTFOLIO_WEBSITE, 1, summary));
        }
    }

    /**
     * Returns complete website narrative and all scraped sections.
     */
    @Tool(description = """
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
        try {
            List<KnowledgeItem> items = this.portfolioWebsiteSource.fetchKnowledge();
            if (items.isEmpty()) {
                return serializeToJson(ToolResponse.notAvailable(DataSourceType.PORTFOLIO_WEBSITE, "Detailed portfolio content is currently not available from the live website."));
            }
            return serializeToJson(ToolResponse.success(DataSourceType.PORTFOLIO_WEBSITE, items.size(), items));
        } catch (Exception e) {
            log.error("[TOOL] [PORTFOLIO-DEEPDIVE-ERROR] Failed to retrieve live portfolio content: {}", e.getMessage(), e);
            return serializeToJson(ToolResponse.error(DataSourceType.PORTFOLIO_WEBSITE, "Something went wrong while retrieving live portfolio content. Please try again later."));
        }
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
        try {
            List<KnowledgeItem> items = this.portfolioWebsiteSource.fetchKnowledge()
                    .stream()
                    .filter(k -> ProfileConstants.CATEGORY_BLOG.equalsIgnoreCase(k.category()))
                    .toList();
            if (items.isEmpty()) {
                return serializeToJson(ToolResponse.notAvailable(DataSourceType.PORTFOLIO_WEBSITE, "Blog articles are currently not available from the live website."));
            }
            List<BlogArticle> articles = items.stream()
                    .map(k -> BlogArticle.builder()
                            .id(k.id())
                            .title(k.title())
                            .summary(k.content())
                            .url(ProfileConstants.PORTFOLIO_URL)
                            .tags(k.tags())
                            .readingTime("N/A")
                            .build())
                    .toList();
            return serializeToJson(ToolResponse.success(DataSourceType.PORTFOLIO_WEBSITE, articles.size(), articles));
        } catch (Exception e) {
            log.error("[TOOL] [PORTFOLIO-BLOG-ERROR] Failed to retrieve live blog articles: {}", e.getMessage(), e);
            return serializeToJson(ToolResponse.error(DataSourceType.PORTFOLIO_WEBSITE, "Something went wrong while retrieving blog articles from the live website. Please try again later."));
        }
    }
}
