package com.pranit.praya.portfolio;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pranit.praya.cache.config.CacheConfig;
import com.pranit.praya.constant.ApiConstants;
import com.pranit.praya.model.DataSourceType;
import com.pranit.praya.model.KnowledgeItem;
import com.pranit.praya.model.ToolResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI Tool specializing in Pranit's personal portfolio website.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PortfolioTools {

    private final PortfolioWebsiteSource portfolioWebsiteSource;
    private final ObjectMapper objectMapper;

    @Tool(description = """
            Lookup live portfolio website bio, summary, and official links
            from https://pranitbhangale.vercel.app
            """)
    @Cacheable(cacheNames = CacheConfig.CACHE_PORTFOLIO_KNOWLEDGE, key = "'portfolio-web'")
    public String fetchPortfolioWebsiteData(PortfolioSearchRequest request) {
        List<KnowledgeItem> items = portfolioWebsiteSource.fetchKnowledge();
        return serializeToJson(ToolResponse.<List<KnowledgeItem>>builder()
                .status(ApiConstants.STATUS_SUCCESS)
                .source(DataSourceType.PORTFOLIO_WEBSITE)
                .count(items.size())
                .data(items)
                .build());
    }

    private <T> String serializeToJson(ToolResponse<T> response) {
        try {
            return this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize portfolio tool response to JSON: {}", e.getMessage(), e);
            return "{\"status\":\"" + ApiConstants.STATUS_ERROR + "\",\"message\":\"JSON serialization failed: " + e.getMessage() + "\"}";
        }
    }
}
