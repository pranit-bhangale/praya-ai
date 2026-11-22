package com.pranit.praya.github;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pranit.praya.cache.config.CacheConfig;
import com.pranit.praya.constant.ApiConstants;
import com.pranit.praya.model.DataSourceType;
import com.pranit.praya.model.KnowledgeItem;
import com.pranit.praya.model.ProjectRecord;
import com.pranit.praya.model.ToolResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI Tool specializing in Pranit's GitHub ecosystem.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GitHubTools {

    private final GitHubSource gitHubSource;
    private final ObjectMapper objectMapper;

    @Tool(description = """
            Lookup software projects, real-time chat engines,
            habit trackers, and GitHub repositories by keyword or technology name
            """)
    @Cacheable(
            cacheNames = CacheConfig.CACHE_PORTFOLIO_PROJECTS,
            key = "#request != null && #request.keyword() != null ? #request.keyword().toLowerCase().trim() : 'all'"
    )
    public String fetchGitHubProjects(ProjectSearchRequest request) {
        String k = request != null && request.keyword() != null ? request.keyword().toLowerCase().trim() : "";
        List<ProjectRecord> projects = gitHubSource.fetchProjects().stream()
                .filter(p -> k.isBlank()
                        || p.name().toLowerCase().contains(k)
                        || p.description().toLowerCase().contains(k)
                        || p.techStack().stream().anyMatch(t -> t.toLowerCase().contains(k)))
                .toList();
        return serializeToJson(ToolResponse.<List<ProjectRecord>>builder()
                .status(ApiConstants.STATUS_SUCCESS)
                .source(DataSourceType.GITHUB)
                .count(projects.size())
                .data(projects)
                .build());
    }

    private <T> String serializeToJson(ToolResponse<T> response) {
        try {
            return this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize GitHub tool response to JSON: {}", e.getMessage(), e);
            return "{\"status\":\"" + ApiConstants.STATUS_ERROR + "\",\"message\":\"JSON serialization failed: " + e.getMessage() + "\"}";
        }
    }

    @Tool(description = """
            Lookup Pranit's technical stack,
            programming languages (Java 21/25, Python, TypeScript),
            frameworks, and database skills from GitHub
            """)
    public String fetchGitHubSkills() {
        List<KnowledgeItem> skills = gitHubSource.fetchKnowledge();
        return serializeToJson(ToolResponse.<List<KnowledgeItem>>builder()
                .status(ApiConstants.STATUS_SUCCESS)
                .source(DataSourceType.GITHUB)
                .count(skills.size())
                .data(skills)
                .build());
    }
}
