package com.pranit.praya.tool;

import com.pranit.praya.cache.config.CacheConfig;
import com.pranit.praya.github.GitHubSource;
import com.pranit.praya.model.DataSourceType;
import com.pranit.praya.model.KnowledgeItem;
import com.pranit.praya.model.ProjectRecord;
import com.pranit.praya.model.ToolResponse;
import com.pranit.praya.parser.ResponseSerializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AI Tool focusing exclusively on Pranit Bhangale's technical skills, dynamically fetched from the GitHub datasource.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SkillsTool {

    private final GitHubSource gitHubSource;
    private final ResponseSerializer responseSerializer;

    /**
     * Fetches primary programming languages and core technology stack dynamically from the GitHub datasource.
     */
    @Tool(name = "skills-summary", description = """
            Retrieves a concise summary of Pranit Bhangale's primary programming languages, backend frameworks,
            and foundational technology stack fetched from his live GitHub data source. Call when the user asks general
            questions about his skills, languages known, or core tech stack.
            """)
    @Cacheable(cacheNames = CacheConfig.CACHE_PORTFOLIO_KNOWLEDGE, key = "'skills-summary'")
    public String getSkillsSummary() {
        try {
            List<ProjectRecord> projects = this.gitHubSource.fetchProjects();
            if (projects.isEmpty()) {
                return this.responseSerializer.serializeToJson(
                        ToolResponse.notAvailable(DataSourceType.GITHUB, "Skills are currently not available from GitHub.")
                );
            }

            Set<String> detectedLanguages = new LinkedHashSet<>();
            Set<String> detectedTech = new LinkedHashSet<>();

            for (ProjectRecord p : projects) {
                if (p.techStack() != null) {
                    for (String tech : p.techStack()) {
                        if (isLanguage(tech)) {
                            detectedLanguages.add(tech);
                        } else {
                            detectedTech.add(tech);
                        }
                    }
                }
            }

            if (detectedLanguages.isEmpty() && detectedTech.isEmpty()) {
                return this.responseSerializer.serializeToJson(
                        ToolResponse.notAvailable(DataSourceType.GITHUB, "No public skill records found on GitHub.")
                );
            }

            Map<String, Object> liveSummary = new LinkedHashMap<>();
            liveSummary.put("primaryLanguages", new ArrayList<>(detectedLanguages));
            liveSummary.put("technologiesAndTopics", new ArrayList<>(detectedTech));
            liveSummary.put("totalPublicRepositories", projects.size());

            return this.responseSerializer.serializeToJson(
                    ToolResponse.success(DataSourceType.GITHUB, projects.size(), liveSummary)
            );
        } catch (Exception e) {
            log.error("[SKILLS-TOOL] [FETCH-ERROR] Failed to fetch skills from GitHub datasource: {}", e.getMessage(), e);
            return this.responseSerializer.serializeToJson(
                    ToolResponse.error(DataSourceType.GITHUB, "Something went wrong while retrieving skills from GitHub.")
            );
        }
    }

    /**
     * Fetches a full categorized breakdown of technical proficiencies dynamically from the GitHub datasource.
     */
    @Tool(name = "skills-categorized", description = """
            Retrieves a comprehensive categorized breakdown of Pranit Bhangale's technical skills across
            languages, backend frameworks, databases, cloud DevOps, and engineering practices from the GitHub datasource.
            Call when the user asks for a detailed, categorized, or in-depth technical skill inventory.
            """)
    @Cacheable(cacheNames = CacheConfig.CACHE_PORTFOLIO_KNOWLEDGE, key = "'skills-categorized'")
    public String getSkillsCategorized() {
        try {
            List<ProjectRecord> projects = this.gitHubSource.fetchProjects();
            if (projects.isEmpty()) {
                return this.responseSerializer.serializeToJson(
                        ToolResponse.notAvailable(DataSourceType.GITHUB, "Categorized skills are currently not available from GitHub.")
                );
            }

            Set<String> languages = new LinkedHashSet<>();
            Set<String> frameworks = new LinkedHashSet<>();
            Set<String> devops = new LinkedHashSet<>();

            for (ProjectRecord p : projects) {
                if (p.techStack() != null) {
                    for (String item : p.techStack()) {
                        String lower = item.toLowerCase();
                        if (isLanguage(item)) {
                            languages.add(item);
                        } else if (lower.contains("docker") || lower.contains("aws") || lower.contains("ci") || lower.contains("git")) {
                            devops.add(item);
                        } else {
                            frameworks.add(item);
                        }
                    }
                }
            }

            List<KnowledgeItem> liveCategorized = new ArrayList<>();
            if (!languages.isEmpty()) {
                liveCategorized.add(KnowledgeItem.builder()
                        .id("live-skills-languages")
                        .source(DataSourceType.GITHUB)
                        .category("skills")
                        .title("Programming Languages & Core Runtimes (Live from GitHub)")
                        .content(String.join(", ", languages))
                        .tags(new ArrayList<>(languages))
                        .build());
            }
            if (!frameworks.isEmpty()) {
                liveCategorized.add(KnowledgeItem.builder()
                        .id("live-skills-frameworks")
                        .source(DataSourceType.GITHUB)
                        .category("skills")
                        .title("Frameworks & Technologies (Live from GitHub Repositories)")
                        .content(String.join(", ", frameworks))
                        .tags(new ArrayList<>(frameworks))
                        .build());
            }
            if (!devops.isEmpty()) {
                liveCategorized.add(KnowledgeItem.builder()
                        .id("live-skills-devops")
                        .source(DataSourceType.GITHUB)
                        .category("skills")
                        .title("DevOps & Infrastructure (Live from GitHub)")
                        .content(String.join(", ", devops))
                        .tags(new ArrayList<>(devops))
                        .build());
            }

            if (liveCategorized.isEmpty()) {
                return this.responseSerializer.serializeToJson(
                        ToolResponse.notAvailable(DataSourceType.GITHUB, "No categorized skills detected from GitHub repositories.")
                );
            }

            return this.responseSerializer.serializeToJson(
                    ToolResponse.success(DataSourceType.GITHUB, liveCategorized.size(), liveCategorized)
            );
        } catch (Exception e) {
            log.error("[SKILLS-TOOL] [CATEGORIZED-ERROR] Failed to fetch categorized skills from GitHub: {}", e.getMessage(), e);
            return this.responseSerializer.serializeToJson(
                    ToolResponse.error(DataSourceType.GITHUB, "Something went wrong while retrieving categorized skills from GitHub.")
            );
        }
    }

    private boolean isLanguage(String tech) {
        String lower = tech.toLowerCase();
        return lower.contains("java") || lower.contains("python") || lower.contains("typescript")
                || lower.contains("javascript") || lower.contains("c++") || lower.contains("sql")
                || lower.contains("html") || lower.contains("css");
    }
}
