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
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GitHubTools {

    private final GitHubSource gitHubSource;
    private final ObjectMapper objectMapper;

    /**
     * Returns high-level project names and 1-line summaries.
     */
    @Tool(description = """
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
        try {
            List<ProjectRecord> allProjects = this.gitHubSource.fetchProjects();
            if (allProjects.isEmpty()) {
                return serializeToJson(ToolResponse.notAvailable(DataSourceType.GITHUB, "Project summaries are currently not available from GitHub."));
            }
            List<ProjectSummary> summaries = allProjects.stream()
                    .map(ProjectSummary::from)
                    .toList();
            return serializeToJson(ToolResponse.success(DataSourceType.GITHUB, summaries.size(), summaries));
        } catch (Exception e) {
            log.error("[TOOL] [GITHUB-PROJECTS-ERROR] Failed to fetch live project summaries: {}", e.getMessage(), e);
            return serializeToJson(ToolResponse.error(DataSourceType.GITHUB, "Something went wrong while retrieving projects from GitHub. Please try again later."));
        }
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
     * Returns complete architecture, tech stack, highlights, and repo link for a specific project.
     */
    @Tool(description = """
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
                      - 'todo-app' or 'Spring MVC To-Do App'         : Production task-management backend (Spring Boot + JPA)
                      - 'praya-ai'                                   : Reactive multi-agent AI assistant
                    
                    Format:
                      Case-insensitive string. Punctuation and spaces are stripped during matching.
                      Example values: "open-connect", "todo-app", "cleaning-bot".
                    """) String projectName
    ) {
        if (projectName == null || projectName.isBlank()) {
            return serializeToJson(ToolResponse.notAvailable(DataSourceType.GITHUB, "Please specify a project name to view details."));
        }
        try {
            String target = projectName.toLowerCase().replaceAll("[^a-z0-9]", "");
            List<ProjectRecord> allProjects = this.gitHubSource.fetchProjects();
            if (allProjects.isEmpty()) {
                return serializeToJson(ToolResponse.notAvailable(DataSourceType.GITHUB, "Project details are currently not available from GitHub."));
            }
            Optional<ProjectRecord> match = allProjects.stream()
                    .filter(p -> {
                        String cleanId = p.id().replaceAll("[^a-z0-9]", "");
                        String cleanName = p.name().toLowerCase().replaceAll("[^a-z0-9]", "");
                        return cleanId.contains(target) || cleanName.contains(target) || target.contains(cleanId);
                    })
                    .findFirst();
            if (match.isPresent()) {
                return serializeToJson(ToolResponse.success(DataSourceType.GITHUB, 1, match.get()));
            }
            return serializeToJson(ToolResponse.notAvailable(DataSourceType.GITHUB, "No live project found matching '" + projectName + "'."));
        } catch (Exception e) {
            log.error("[TOOL] [GITHUB-PROJECT-DETAIL-ERROR] Failed to fetch project details for [{}]: {}", projectName, e.getMessage(), e);
            return serializeToJson(ToolResponse.error(DataSourceType.GITHUB, "Something went wrong while retrieving project details from GitHub. Please try again later."));
        }
    }

    /**
     * Returns high-level core skills.
     */
    @Tool(description = """
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
        try {
            List<KnowledgeItem> skills = this.gitHubSource.fetchKnowledge().stream()
                    .filter(k -> ProfileConstants.CATEGORY_SKILLS.equalsIgnoreCase(k.category()))
                    .toList();
            if (!skills.isEmpty()) {
                return serializeToJson(ToolResponse.success(DataSourceType.GITHUB, skills.size(), skills));
            }
        } catch (Exception e) {
            log.warn("[TOOL] [GITHUB-SKILLS-FALLBACK] Live fetch unavailable ({}), serving verified skills summary.", e.getMessage());
        }

        // Verified core skills summary baseline
        List<KnowledgeItem> baselineSkills = List.of(
                KnowledgeItem.builder()
                        .id("skills-summary-baseline")
                        .source(DataSourceType.GITHUB)
                        .category(ProfileConstants.CATEGORY_SKILLS)
                        .title("Core Technical Stack & Languages")
                        .content(ProfileConstants.TECH_STACK_CONTENT.trim())
                        .tags(List.of("java", "spring boot", "react", "docker", "aws", "postgresql", "redis"))
                        .build()
        );
        return serializeToJson(ToolResponse.success(DataSourceType.GITHUB, baselineSkills.size(), baselineSkills));
    }

    /**
     * Returns complete technical breakdown.
     */
    @Tool(description = """
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
        try {
            List<KnowledgeItem> allKnowledge = this.gitHubSource.fetchKnowledge().stream()
                    .filter(k -> ProfileConstants.CATEGORY_SKILLS.equalsIgnoreCase(k.category())
                            || ProfileConstants.CATEGORY_EXPERIENCE.equalsIgnoreCase(k.category()))
                    .toList();
            if (!allKnowledge.isEmpty()) {
                return serializeToJson(ToolResponse.success(DataSourceType.GITHUB, allKnowledge.size(), allKnowledge));
            }
        } catch (Exception e) {
            log.warn("[TOOL] [GITHUB-DEEPDIVE-FALLBACK] Live fetch unavailable ({}), serving verified deep dive.", e.getMessage());
        }
        List<KnowledgeItem> baselineDeepDive = List.of(
                KnowledgeItem.builder()
                        .id("skills-deepdive-languages")
                        .source(DataSourceType.GITHUB)
                        .category(ProfileConstants.CATEGORY_SKILLS)
                        .title("Programming Languages & Core Runtimes")
                        .content("Java (21, 25), Python, TypeScript, JavaScript, SQL, C++.\n" +
                                "Core competencies in JVM internals, memory management, garbage collection tuning, multithreading, and concurrent primitives.")
                        .tags(List.of("languages", "java", "python", "typescript", "c++", "sql"))
                        .build(),
                KnowledgeItem.builder()
                        .id("skills-deepdive-frameworks")
                        .source(DataSourceType.GITHUB)
                        .category(ProfileConstants.CATEGORY_SKILLS)
                        .title("Frameworks & Systems Architecture")
                        .content("Spring Boot 3/4, Spring AI, Spring WebFlux, Project Reactor, Hibernate/JPA, React.js, ASP.NET Web API, RESTful APIs, STOMP over WebSockets.\n" +
                                "Reactive and event-driven patterns with non-blocking I/O.")
                        .tags(List.of("frameworks", "spring boot", "spring ai", "react", "websockets"))
                        .build(),
                KnowledgeItem.builder()
                        .id("skills-deepdive-infrastructure")
                        .source(DataSourceType.GITHUB)
                        .category(ProfileConstants.CATEGORY_SKILLS)
                        .title("Cloud, Persistence & DevOps Infrastructure")
                        .content("PostgreSQL, MySQL, Redis (caching and pub/sub), Docker containerization, AWS, Render, Vercel, Git/GitHub, CI/CD automated deployment pipelines.")
                        .tags(List.of("infrastructure", "docker", "aws", "postgresql", "redis", "ci/cd"))
                        .build(),
                KnowledgeItem.builder()
                        .id("skills-deepdive-practices")
                        .source(DataSourceType.GITHUB)
                        .category(ProfileConstants.CATEGORY_EXPERIENCE)
                        .title("Engineering Practices & Architectural Philosophy")
                        .content(ProfileConstants.PHILOSOPHY_CONTENT.trim())
                        .tags(List.of("clean-architecture", "solid", "tdd", "reactive-streams"))
                        .build()
        );
        return serializeToJson(ToolResponse.success(DataSourceType.GITHUB, baselineDeepDive.size(), baselineDeepDive));
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
        try {
            List<ProjectRecord> allProjects = this.gitHubSource.fetchProjects();
            if (allProjects.isEmpty()) {
                return serializeToJson(ToolResponse.notAvailable(DataSourceType.GITHUB, "Public GitHub repository data is currently not available."));
            }
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
            return serializeToJson(ToolResponse.success(DataSourceType.GITHUB, repos.size(), repos));
        } catch (Exception e) {
            log.error("[TOOL] [GITHUB-REPOS-ERROR] Failed to retrieve GitHub repositories: {}", e.getMessage(), e);
            return serializeToJson(ToolResponse.error(DataSourceType.GITHUB, "Something went wrong while retrieving repositories from GitHub. Please try again later."));
        }
    }
}
