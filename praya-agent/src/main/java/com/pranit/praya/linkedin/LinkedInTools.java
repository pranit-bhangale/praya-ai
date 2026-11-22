package com.pranit.praya.linkedin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pranit.praya.cache.config.CacheConfig;
import com.pranit.praya.constant.ApiConstants;
import com.pranit.praya.constant.ProfileConstants;
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
 * AI Tool specializing in Pranit's LinkedIn profile and professional background.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LinkedInTools {

    private final LinkedInSource linkedInSource;
    private final ObjectMapper objectMapper;

    @Tool(description = """
            Lookup Pranit's educational degrees (Savitribai Phule Pune University BE, CDAC PG-DAC),
            academic background, and certifications from LinkedIn
            """)
    @Cacheable(
            cacheNames = CacheConfig.CACHE_PORTFOLIO_KNOWLEDGE,
            key = "'linkedin-education-' + (#request != null && #request.query() != null ? #request.query().toLowerCase().trim() : 'all')"
    )
    public String fetchEducationAndDegrees(LinkedInSearchRequest request) {
        List<KnowledgeItem> items = linkedInSource.fetchKnowledge().stream()
                .filter(item -> ProfileConstants.CATEGORY_EDUCATION.equalsIgnoreCase(item.category()))
                .toList();
        return serializeToJson(ToolResponse.<List<KnowledgeItem>>builder()
                .status(ApiConstants.STATUS_SUCCESS)
                .source(DataSourceType.LINKEDIN)
                .count(items.size())
                .data(items)
                .build());
    }

    private <T> String serializeToJson(ToolResponse<T> response) {
        try {
            return this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize LinkedIn tool response to JSON: {}", e.getMessage(), e);
            return "{\"status\":\"" + ApiConstants.STATUS_ERROR + "\",\"message\":\"JSON serialization failed: " + e.getMessage() + "\"}";
        }
    }

    @Tool(description = """
            Lookup Pranit's engineering philosophy,
            clean architecture focus, and professional career profile from LinkedIn
            """)
    public String fetchCareerAndPhilosophy(LinkedInSearchRequest request) {
        List<KnowledgeItem> items = linkedInSource.fetchKnowledge()
                .stream()
                .filter(item -> !ProfileConstants.CATEGORY_EDUCATION.equalsIgnoreCase(item.category()))
                .toList();
        return serializeToJson(ToolResponse.<List<KnowledgeItem>>builder()
                .status(ApiConstants.STATUS_SUCCESS)
                .source(DataSourceType.LINKEDIN)
                .count(items.size())
                .data(items)
                .build());
    }
}
