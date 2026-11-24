package com.pranit.praya.tool;

import com.pranit.praya.cache.config.CacheConfig;
import com.pranit.praya.constant.ProfileConstant;
import com.pranit.praya.model.BlogArticle;
import com.pranit.praya.model.DataSourceType;
import com.pranit.praya.model.KnowledgeItem;
import com.pranit.praya.model.ToolResponse;
import com.pranit.praya.parser.ResponseSerializer;
import com.pranit.praya.portfolio.PortfolioWebsiteSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI Tool retrieving published technical blog articles dynamically from the portfolio website data source.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlogsTool {

    private final PortfolioWebsiteSource portfolioWebsiteSource;
    private final ResponseSerializer responseSerializer;

    /**
     * Retrieves technical blog articles dynamically fetched from the live portfolio data source.
     */
    @Tool(name = "blog-articles", description = """
            Retrieves Pranit Bhangale's published technical blog articles, architecture write-ups,
            and engineering case studies from his portfolio website. Call when the user asks about blog
            articles, technical posts, system design tutorials, or engineering publications.
            """)
    @Cacheable(cacheNames = CacheConfig.CACHE_PORTFOLIO_KNOWLEDGE, key = "'blog-articles'")
    public String getBlogArticles() {
        try {
            List<KnowledgeItem> items = this.portfolioWebsiteSource.fetchKnowledge()
                    .stream()
                    .filter(k -> ProfileConstant.CATEGORY_BLOG.equalsIgnoreCase(k.category()))
                    .toList();

            if (items.isEmpty()) {
                return this.responseSerializer.serializeToJson(
                        ToolResponse.notAvailable(DataSourceType.PORTFOLIO_WEBSITE, "No blog articles found on the live portfolio website.")
                );
            }

            List<BlogArticle> articles = items.stream()
                    .map(k -> BlogArticle.builder()
                            .id(k.id())
                            .title(k.title())
                            .summary(k.content())
                            .url(ProfileConstant.PORTFOLIO_URL)
                            .tags(k.tags())
                            .readingTime("N/A")
                            .build())
                    .toList();

            return this.responseSerializer.serializeToJson(
                    ToolResponse.success(DataSourceType.PORTFOLIO_WEBSITE, articles.size(), articles)
            );
        } catch (Exception e) {
            log.error("[BLOGS-TOOL] [FETCH-ERROR] Failed to fetch blog articles from portfolio datasource: {}", e.getMessage(), e);
            return this.responseSerializer.serializeToJson(
                    ToolResponse.error(DataSourceType.PORTFOLIO_WEBSITE, "Something went wrong while retrieving blog articles from the live website.")
            );
        }
    }
}
