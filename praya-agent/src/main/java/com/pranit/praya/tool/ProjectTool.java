package com.pranit.praya.tool;

import com.pranit.praya.cache.config.CacheConfig;
import com.pranit.praya.github.GitHubSource;
import com.pranit.praya.model.DataSourceType;
import com.pranit.praya.model.GitHubRepoRecord;
import com.pranit.praya.model.ProjectRecord;
import com.pranit.praya.model.ProjectSummary;
import com.pranit.praya.model.ToolResponse;
import com.pranit.praya.parser.ResponseSerializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * AI Tool retrieving Pranit Bhangale's software engineering projects and public GitHub repositories.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectTool {

    private final GitHubSource gitHubSource;
    private final ResponseSerializer responseSerializer;

    /**
     * Returns lightweight summaries of all featured projects.
     */
    @Tool(name = "projects-summary", description = """
            Retrieves a concise catalog of Pranit Bhangale's featured software engineering projects.
            Call when the user asks what projects Pranit has built, asks for his project list,
            or wants an overview of his portfolio work.
            """)
    @Cacheable(cacheNames = CacheConfig.CACHE_PORTFOLIO_PROJECTS, key = "'project-summaries'")
    public String listProjects() {
        try {
            List<ProjectRecord> allProjects = this.gitHubSource.fetchProjects();
            if (allProjects.isEmpty()) {
                return this.responseSerializer.serializeToJson(
                        ToolResponse.notAvailable(DataSourceType.GITHUB, "Project summaries are currently not available.")
                );
            }
            List<ProjectSummary> summaries = allProjects.stream()
                    .map(ProjectSummary::from)
                    .toList();
            return this.responseSerializer.serializeToJson(
                    ToolResponse.success(DataSourceType.GITHUB, summaries.size(), summaries)
            );
        } catch (Exception e) {
            log.error("[PROJECT-TOOL] [SUMMARIES-ERROR] Failed to fetch project summaries: {}", e.getMessage(), e);
            return this.responseSerializer.serializeToJson(
                    ToolResponse.error(DataSourceType.GITHUB, "Something went wrong while retrieving projects. Please try again later.")
            );
        }
    }

    /**
     * Returns comprehensive architectural specification for a specific named project.
     */
    @Tool(name = "project-detail", description = """
            Retrieves detailed architecture specs, technical highlights, tech stack, and GitHub repository URL
            for a specific project. Call when the user asks for deep architectural details, highlights,
            or repository links for a specific named project (e.g. 'open-connect', 'todo-app', 'praya-ai').
            """)
    @Cacheable(
            cacheNames = CacheConfig.CACHE_PORTFOLIO_PROJECTS,
            key = "'project-detail-' + (#projectName != null ? #projectName.toLowerCase().trim() : 'none')"
    )
    public String getProjectDetails(
            @ToolParam(description = "The name or identifier of the project to inspect (e.g. 'open-connect', 'todo-app', 'praya-ai').")
            String projectName
    ) {
        if (projectName == null || projectName.isBlank()) {
            return this.responseSerializer.serializeToJson(
                    ToolResponse.notAvailable(DataSourceType.GITHUB, "Please specify a project name to view details.")
            );
        }
        try {
            String target = projectName.toLowerCase().replaceAll("[^a-z0-9]", "");
            List<ProjectRecord> allProjects = this.gitHubSource.fetchProjects();
            if (allProjects.isEmpty()) {
                return this.responseSerializer.serializeToJson(
                        ToolResponse.notAvailable(DataSourceType.GITHUB, "Project details are currently not available.")
                );
            }
            Optional<ProjectRecord> match = allProjects.stream()
                    .filter(p -> {
                        String cleanId = p.id().replaceAll("[^a-z0-9]", "");
                        String cleanName = p.name().toLowerCase().replaceAll("[^a-z0-9]", "");
                        return cleanId.contains(target) || cleanName.contains(target) || target.contains(cleanId);
                    })
                    .findFirst();

            if (match.isPresent()) {
                return this.responseSerializer.serializeToJson(
                        ToolResponse.success(DataSourceType.GITHUB, 1, match.get())
                );
            }
            return this.responseSerializer.serializeToJson(
                    ToolResponse.notAvailable(DataSourceType.GITHUB, "No project found matching '" + projectName + "'.")
            );
        } catch (Exception e) {
            log.error("[PROJECT-TOOL] [DETAIL-ERROR] Failed to fetch project details for [{}]: {}", projectName, e.getMessage(), e);
            return this.responseSerializer.serializeToJson(
                    ToolResponse.error(DataSourceType.GITHUB, "Something went wrong while retrieving project details. Please try again later.")
            );
        }
    }

    /**
     * Returns public repositories and source links from GitHub.
     */
    @Tool(name = "public-repositories", description = """
            Retrieves Pranit Bhangale's public GitHub repositories with descriptions, primary languages,
            and repository links. Call when the user specifically asks for his GitHub repositories, source code links,
            or public code repositories.
            """)
    @Cacheable(cacheNames = CacheConfig.CACHE_PORTFOLIO_PROJECTS, key = "'github-repos'")
    public String getPublicRepositories() {
        try {
            List<ProjectRecord> allProjects = this.gitHubSource.fetchProjects();
            if (allProjects.isEmpty()) {
                return this.responseSerializer.serializeToJson(
                        ToolResponse.notAvailable(DataSourceType.GITHUB, "Public GitHub repository data is currently not available.")
                );
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
            return this.responseSerializer.serializeToJson(
                    ToolResponse.success(DataSourceType.GITHUB, repos.size(), repos)
            );
        } catch (Exception e) {
            log.error("[PROJECT-TOOL] [REPOS-ERROR] Failed to retrieve GitHub repositories: {}", e.getMessage(), e);
            return this.responseSerializer.serializeToJson(
                    ToolResponse.error(DataSourceType.GITHUB, "Something went wrong while retrieving repositories. Please try again later.")
            );
        }
    }
}
