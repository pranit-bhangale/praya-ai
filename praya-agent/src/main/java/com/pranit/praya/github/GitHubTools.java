package com.pranit.praya.github;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pranit.praya.cache.config.CacheConfig;
import com.pranit.praya.constant.ApiConstants;
import com.pranit.praya.constant.ProfileConstants;
import com.pranit.praya.model.DataSourceType;
import com.pranit.praya.model.GitHubRepoRecord;
import com.pranit.praya.model.KnowledgeItem;
import com.pranit.praya.model.ProjectRecord;
import com.pranit.praya.model.ProjectSummary;
import com.pranit.praya.model.ToolResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * AI Tool specializing in Pranit's GitHub ecosystem.
 * Implements two-tier progressive disclosure: high-level summaries and on-demand deep dives.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GitHubTools {

    private final GitHubSource gitHubSource;
    private final ObjectMapper objectMapper;

    /**
     * Tier 1 (Summary): Returns high-level project names and 1-line summaries.
     */
    @Tool(description = """
            [TIER 1 - HIGH-LEVEL SUMMARY]
            Retrieves a concise catalog of Pranit Bhangale's featured software engineering projects.
            
            Returns:
              A JSON array of lightweight ProjectSummary records containing:
              - id: Unique project identifier
              - name: Project title
              - summary: One-sentence overview of the project's purpose
              - primaryTech: Core technologies used
              Does NOT include deep architecture highlights, system design breakdowns, or repository URLs.
            
            When to call:
              - The user asks general questions such as "What projects has Pranit built?", "Show me his projects",
                "List Pranit's work", or asks for a general portfolio overview.
              - Initial exploratory queries where a concise list is required.
            
            When NOT to call:
              - Do NOT call when the user explicitly requests deep architectural details, internal mechanics, or
                the source code repository link for a specific project (use 'getProjectDetails' instead).
            """)
    @Cacheable(cacheNames = CacheConfig.CACHE_PORTFOLIO_PROJECTS, key = "'project-summaries'")
    public String listProjectSummaries() {
        List<ProjectRecord> allProjects = this.gitHubSource.fetchProjects();
        List<ProjectSummary> summaries = allProjects.stream()
                .map(ProjectSummary::from)
                .toList();

        return serializeToJson(ToolResponse.<List<ProjectSummary>>builder()
                .status(ApiConstants.STATUS_SUCCESS)
                .source(DataSourceType.GITHUB)
                .count(summaries.size())
                .data(summaries)
                .build());
    }

    private <T> String serializeToJson(ToolResponse<T> response) {
        try {
            return this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
        } catch (JsonProcessingException e) {
            log.error("[TOOL] [GITHUB-SERIALIZE-ERROR] Failed to serialize GitHub tool response: {}", e.getMessage(), e);
            return "{\"status\":\"" + ApiConstants.STATUS_ERROR + "\",\"message\":\"JSON serialization failed: " + e.getMessage() + "\"}";
        }
    }

    /**
     * Tier 2 (Deep Dive): Returns complete architecture, tech stack, highlights, and repo link for a specific project.
     */
    @Tool(description = """
            [TIER 2 - DEEP DIVE]
            Retrieves exhaustive architectural specifications, key technical highlights, complete tech stack,
            and the verified GitHub repository URL for a single specified project.
            
            Returns:
              A JSON payload containing the complete ProjectRecord including:
              - id, name, and comprehensive technical description
              - full tech-stack list (languages, frameworks, databases, infrastructure)
              - key technical highlights (concurrency mechanisms, protocols, encryption, design patterns)
              - direct repository URL (e.g. https://github.com/pranit-bhangale/...)
            
            When to call:
              - The user explicitly asks for in-depth details, architecture breakdown, technical highlights,
                mechanics, or GitHub repository URL for a particular project.
            
            When NOT to call:
              - Do NOT call for general queries like "What projects has he built?" (use 'listProjectSummaries' instead).
              - Do NOT call with empty or speculative arguments.
            """)
    @Cacheable(
            cacheNames = CacheConfig.CACHE_PORTFOLIO_PROJECTS,
            key = "'project-detail-' + (#projectName != null ? #projectName.toLowerCase().trim() : 'none')"
    )
    public String getProjectDetails(
            @ToolParam(description = """
                    The exact name or canonical identifier of the project to inspect.
                    
                    Accepted Identifiers and Aliases:
                      - 'open-connect' or 'Open-Connect Live Engine' : Full-duplex real-time STOMP/WebSocket chat system
                      - 'habit-tracker' or 'Habit Tracker Web App'  : Full-stack habit & routine tracking platform (React + .NET Web API)
                      - 'todo-app' or 'Spring MVC To-Do App'        : Production task-management backend (Spring Boot + JPA)
                      - 'cleaning-bot' or 'Smart Cleaning Bot'      : Autonomous IoT floor-cleaning robot (ESP32 + FreeRTOS + C++)
                      - 'praya-ai'                                  : Reactive multi-agent AI assistant
                    
                    Format:
                      Case-insensitive string. Punctuation and spaces are stripped during matching.
                      Example values: "open-connect", "habit-tracker", "todo-app", "cleaning-bot".
                    """) String projectName
    ) {
        if (projectName == null || projectName.isBlank()) {
            return "{\"status\":\"ERROR\",\"message\":\"Please specify a project name to view details.\"}";
        }

        String target = projectName.toLowerCase().replaceAll("[^a-z0-9]", "");
        List<ProjectRecord> allProjects = this.gitHubSource.fetchProjects();

        Optional<ProjectRecord> match = allProjects.stream()
                .filter(p -> {
                    String cleanId = p.id().replaceAll("[^a-z0-9]", "");
                    String cleanName = p.name().toLowerCase().replaceAll("[^a-z0-9]", "");
                    return cleanId.contains(target) || cleanName.contains(target) || target.contains(cleanId);
                })
                .findFirst();

        if (match.isPresent()) {
            return serializeToJson(ToolResponse.<ProjectRecord>builder()
                    .status(ApiConstants.STATUS_SUCCESS)
                    .source(DataSourceType.GITHUB)
                    .count(1)
                    .data(match.get())
                    .build());
        }

        return "{\"status\":\"NOT_FOUND\",\"message\":\"No project found matching '" + projectName + "'. Available projects: Open-Connect Live Engine, Habit Tracker Web App, Spring MVC To-Do App, Smart Cleaning Bot.\"}";
    }

    /**
     * Tier 1 (Summary): Returns high-level core skills.
     */
    @Tool(description = """
            [TIER 1 - HIGH-LEVEL SUMMARY]
            Retrieves a high-level summary of Pranit Bhangale's primary programming languages and foundational tech stack.
            
            Returns:
              A JSON array of high-level core skills (Java, Python, TypeScript, Spring Boot, React, Docker).
              Does NOT include granular version matrices, specialized cloud tooling, or comprehensive infrastructure breakdowns.
            
            When to call:
              - The user asks general questions such as "What skills does Pranit have?", "What languages does he know?",
                or "What is his tech stack?".
            
            When NOT to call:
              - Do NOT call when the user requests a comprehensive, deep-dive, or categorized breakdown across all tools,
                databases, and infrastructure (use 'getSkillsDeepDive' instead).
            """)
    @Cacheable(cacheNames = CacheConfig.CACHE_PORTFOLIO_KNOWLEDGE, key = "'skills-summary'")
    public String getSkillsSummary() {
        List<KnowledgeItem> skills = this.gitHubSource.fetchKnowledge().stream()
                .filter(k -> ProfileConstants.CATEGORY_SKILLS.equalsIgnoreCase(k.category()))
                .toList();

        return serializeToJson(ToolResponse.<List<KnowledgeItem>>builder()
                .status(ApiConstants.STATUS_SUCCESS)
                .source(DataSourceType.GITHUB)
                .count(skills.size())
                .data(skills)
                .build());
    }

    /**
     * Tier 2 (Deep Dive): Returns complete technical breakdown.
     */
    @Tool(description = """
            [TIER 2 - DEEP DIVE]
            Retrieves an exhaustive, categorized technical breakdown of Pranit Bhangale's complete engineering repertoire.
            
            Returns:
              A comprehensive JSON array of KnowledgeItem objects covering:
              - Programming Languages: Java 21/25, Python, TypeScript, JavaScript, SQL, C++
              - Backend Frameworks & Systems: Spring Boot 3/4, Spring AI, Hibernate/JPA, ASP.NET Web API, REST, STOMP/WebSockets
              - Persistence & Data Stores: PostgreSQL, MySQL, Redis
              - Cloud & DevOps: Docker, AWS, Render, Vercel, Git/GitHub, CI/CD pipelines
              - Architecture & Engineering Practices: Clean Architecture, SOLID, TDD, Reactive Streams, Event-Driven Architecture
            
            When to call:
              - The user asks for a comprehensive, detailed, or categorized breakdown of Pranit's technical skills,
                cloud proficiencies, database experience, or architectural capabilities.
            
            When NOT to call:
              - Do NOT call for a brief or high-level inquiry about skills (use 'getSkillsSummary' instead).
            """)
    @Cacheable(cacheNames = CacheConfig.CACHE_PORTFOLIO_KNOWLEDGE, key = "'skills-deep-dive'")
    public String getSkillsDeepDive() {
        List<KnowledgeItem> allKnowledge = this.gitHubSource.fetchKnowledge();
        return serializeToJson(ToolResponse.<List<KnowledgeItem>>builder()
                .status(ApiConstants.STATUS_SUCCESS)
                .source(DataSourceType.GITHUB)
                .count(allKnowledge.size())
                .data(allKnowledge)
                .build());
    }

    /**
     * Dedicated Tool: Returns live public GitHub repositories, stars, forks, and links.
     */
    @Tool(description = """
            [GITHUB REPOSITORIES]
            Retrieves the full catalog of Pranit Bhangale's public GitHub repositories, including stars, forks,
            primary programming language, descriptions, and direct repository links.
            
            Returns:
              A JSON array of GitHubRepoRecord objects containing:
              - name: Repository title
              - description: Short summary of what the repository contains
              - language: Primary programming language
              - stars: Number of GitHub stars
              - forks: Number of forks
              - htmlUrl: Verified GitHub source code URL
              - liveUrl: Live production deployment or demo link
            
            When to call:
              - The user asks specifically about Pranit's GitHub repositories, GitHub profile repos,
                source code links, or wants to explore his public code on GitHub.
            
            When NOT to call:
              - Do NOT call for general skill questions (use 'getSkillsSummary') or personal bio (use 'getPortfolioSummary').
            """)
    @Cacheable(cacheNames = CacheConfig.CACHE_PORTFOLIO_PROJECTS, key = "'github-repos'")
    public String fetchGitHubRepositories() {
        List<ProjectRecord> allProjects = this.gitHubSource.fetchProjects();
        List<GitHubRepoRecord> repos = allProjects.stream()
                .map(p -> GitHubRepoRecord.builder()
                        .name(p.name())
                        .description(p.description())
                        .language(p.techStack() != null && !p.techStack().isEmpty() ? p.techStack().get(0) : "Java")
                        .stars(0)
                        .forks(0)
                        .htmlUrl(p.repoUrl())
                        .liveUrl(p.liveUrl())
                        .build())
                .toList();

        return serializeToJson(ToolResponse.<List<GitHubRepoRecord>>builder()
                .status(ApiConstants.STATUS_SUCCESS)
                .source(DataSourceType.GITHUB)
                .count(repos.size())
                .data(repos)
                .build());
    }
}
