package com.pranit.praya.github;

import com.pranit.praya.constant.ProfileConstants;
import com.pranit.praya.constant.ProjectConstants;
import com.pranit.praya.model.DataSourceType;
import com.pranit.praya.model.KnowledgeItem;
import com.pranit.praya.model.ProjectRecord;
import com.pranit.praya.source.KnowledgeSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data source implementation for Pranit's GitHub ecosystem.
 */
@Slf4j
@Component
public class GitHubSource implements KnowledgeSource {

    private final GitHubApiClient gitHubApiClient;
    private final String username;

    public GitHubSource(
            GitHubApiClient gitHubApiClient,
            @Value("${praya.github.username:" + ProfileConstants.DEFAULT_GITHUB_USERNAME + "}") String username
    ) {
        this.gitHubApiClient = gitHubApiClient;
        this.username = username;
    }

    @Override
    public DataSourceType getType() {
        return DataSourceType.GITHUB;
    }

    @Override
    public List<KnowledgeItem> fetchKnowledge() {
        final long startTime = System.currentTimeMillis();
        log.info("[DATA-SOURCE] [GITHUB-FETCH-START] Fetching profile knowledge for user: {}", this.username);
        try {
            Map<String, Object> profile = this.gitHubApiClient.getUserProfile(this.username);
            if (profile != null && !profile.isEmpty()) {
                String name = String.valueOf(profile.getOrDefault("name", this.username));
                String bio = String.valueOf(profile.getOrDefault("bio", ProfileConstants.DEFAULT_BIO));
                int publicRepos = profile.get("public_repos") instanceof Number n ? n.intValue() : 0;
                int followers = profile.get("followers") instanceof Number n ? n.intValue() : 0;
                String blog = String.valueOf(profile.getOrDefault("blog", ProfileConstants.PORTFOLIO_URL));
                String htmlUrl = String.valueOf(profile.getOrDefault("html_url", "https://github.com/" + this.username));

                List<KnowledgeItem> items = List.of(
                        KnowledgeItem.builder()
                                .id("github-live-profile")
                                .source(DataSourceType.GITHUB)
                                .category(ProfileConstants.CATEGORY_PROFILE)
                                .title("Live GitHub Profile: " + name)
                                .content(String.format(
                                        "GitHub Profile: %s | Bio: %s | Public Repositories: %d | Followers: %d | Website: %s",
                                        htmlUrl, bio, publicRepos, followers, blog))
                                .tags(List.of("github", "live", "profile", "stats", "repositories"))
                                .build(),
                        KnowledgeItem.builder()
                                .id("github-tech-stack")
                                .source(DataSourceType.GITHUB)
                                .category(ProfileConstants.CATEGORY_SKILLS)
                                .title("GitHub Technical Stack & Languages")
                                .content(ProfileConstants.TECH_STACK_CONTENT)
                                .tags(ProfileConstants.TAGS_SKILLS)
                                .build()
                );
                final long durationMs = System.currentTimeMillis() - startTime;
                log.info("[DATA-SOURCE] [GITHUB-FETCH-SUCCESS] Retrieved profile items: {}, publicRepos: {}, durationMs: {}",
                        items.size(), publicRepos, durationMs);
                return items;
            }
        } catch (Exception e) {
            log.warn("[DATA-SOURCE] [GITHUB-FETCH-FALLBACK] Profile retrieval failed (reason: {}): falling back to curated baseline", e.getMessage());
        }

        return getBaselineKnowledge();
    }

    @Override
    public List<ProjectRecord> fetchProjects() {
        final long startTime = System.currentTimeMillis();
        log.info("[DATA-SOURCE] [GITHUB-FETCH-START] Fetching public repository list for user: {}", this.username);
        try {
            List<Map<String, Object>> repos = this.gitHubApiClient.listPublicRepos(this.username);
            if (repos != null && !repos.isEmpty()) {
                List<ProjectRecord> liveProjects = new ArrayList<>();
                for (Map<String, Object> repo : repos) {
                    String name = String.valueOf(repo.getOrDefault("name", ""));

                    // Security Boundary: Only expose public repositories to the portfolio agent
                    Object isPrivate = repo.get("private");
                    if (Boolean.TRUE.equals(isPrivate) || "private".equalsIgnoreCase(String.valueOf(repo.get("visibility")))) {
                        log.debug("[DATA-SOURCE] [GITHUB-REPO-SKIP] Skipping private repository [{}]", name);
                        continue;
                    }

                    String description = String.valueOf(repo.getOrDefault("description", "Repository by " + this.username));
                    if ("null".equals(description) || description.isBlank()) {
                        description = "Open-source repository by " + this.username;
                    }
                    String htmlUrl = String.valueOf(repo.getOrDefault("html_url", "https://github.com/" + this.username));
                    String homepage = String.valueOf(repo.getOrDefault("homepage", ""));
                    String language = String.valueOf(repo.getOrDefault("language", ""));
                    if ("null".equals(language)) language = "";

                    int stars = repo.get("stargazers_count") instanceof Number n ? n.intValue() : 0;
                    int forks = repo.get("forks_count") instanceof Number n ? n.intValue() : 0;
                    String updatedAt = String.valueOf(repo.getOrDefault("updated_at", ""));

                    List<String> techStack = new ArrayList<>();
                    if (StringUtils.hasText(language)) {
                        techStack.add(language);
                    }
                    Object topicsObj = repo.get("topics");
                    if (topicsObj instanceof Collection<?> collection) {
                        for (Object topic : collection) {
                            if (topic != null && StringUtils.hasText(topic.toString())) {
                                techStack.add(topic.toString());
                            }
                        }
                    }

                    List<String> highlights = resolveHighlights(name, language, stars, forks, updatedAt);

                    liveProjects.add(ProjectRecord.builder()
                            .id(name.toLowerCase().replaceAll("[^a-z0-9-]", "-"))
                            .name(formatProjectTitle(name))
                            .description(description)
                            .techStack(techStack.isEmpty() ? List.of("Software Engineering") : techStack)
                            .liveUrl(StringUtils.hasText(homepage) && !"null".equals(homepage) ? homepage : ProfileConstants.PORTFOLIO_URL)
                            .repoUrl(htmlUrl)
                            .highlights(highlights)
                            .build());
                }

                if (!liveProjects.isEmpty()) {
                    // Merge with flagship baseline projects so comprehensive architecture highlights are always present
                    Map<String, ProjectRecord> projectMap = new LinkedHashMap<>();
                    for (ProjectRecord baseline : ProjectConstants.getBaselineProjects(this.username)) {
                        projectMap.put(baseline.id(), baseline);
                    }
                    for (ProjectRecord live : liveProjects) {
                        projectMap.putIfAbsent(live.id(), live);
                    }
                    List<ProjectRecord> merged = new ArrayList<>(projectMap.values());
                    final long durationMs = System.currentTimeMillis() - startTime;
                    log.info("[DATA-SOURCE] [GITHUB-FETCH-SUCCESS] Retrieved repositories count: {} (merged with flagship catalog) durationMs: {}",
                            merged.size(), durationMs);
                    return merged;
                }
            }
        } catch (Exception e) {
            log.warn("[DATA-SOURCE] [GITHUB-FETCH-FALLBACK] Repository retrieval failed (reason: {}): falling back to curated catalog", e.getMessage());
        }

        return ProjectConstants.getBaselineProjects(this.username);
    }

    private List<String> resolveHighlights(String repoName, String language, int stars, int forks, String updatedAt) {
        String lower = repoName.toLowerCase();
        if (lower.contains("open-connect")) {
            return ProjectConstants.HIGHLIGHTS_OPEN_CONNECT;
        } else if (lower.contains("habit")) {
            return ProjectConstants.HIGHLIGHTS_HABIT_TRACKER;
        } else if (lower.contains("cleaning") || lower.contains("bot")) {
            return ProjectConstants.HIGHLIGHTS_CLEANING_BOT;
        } else if (lower.contains("todo")) {
            return ProjectConstants.HIGHLIGHTS_TODO_APP;
        }

        List<String> highlights = new ArrayList<>();
        if (StringUtils.hasText(language)) {
            highlights.add("Primary language: " + language);
        }
        highlights.add(String.format("Public GitHub repository (Stars: %d, Forks: %d)", stars, forks));
        if (StringUtils.hasText(updatedAt) && !"null".equals(updatedAt)) {
            highlights.add("Recently active: " + updatedAt);
        }
        return highlights;
    }

    private String formatProjectTitle(String name) {
        if (ProjectConstants.ID_OPEN_CONNECT.equalsIgnoreCase(name)) return ProjectConstants.NAME_OPEN_CONNECT;
        if (ProjectConstants.ID_HABIT_TRACKER.equalsIgnoreCase(name)) return ProjectConstants.NAME_HABIT_TRACKER;
        if (ProjectConstants.ID_CLEANING_BOT.equalsIgnoreCase(name)) return ProjectConstants.NAME_CLEANING_BOT;
        if (ProjectConstants.ID_TODO_APP.equalsIgnoreCase(name)) return ProjectConstants.NAME_TODO_APP;
        return name.replace("-", " ").replace("_", " ");
    }

    private List<KnowledgeItem> getBaselineKnowledge() {
        return List.of(
                KnowledgeItem.builder()
                        .id("github-tech-stack")
                        .source(DataSourceType.GITHUB)
                        .category(ProfileConstants.CATEGORY_SKILLS)
                        .title("GitHub Technical Stack & Languages")
                        .content(ProfileConstants.TECH_STACK_CONTENT)
                        .tags(ProfileConstants.TAGS_SKILLS)
                        .build(),
                KnowledgeItem.builder()
                        .id("github-profile-link")
                        .source(DataSourceType.GITHUB)
                        .category(ProfileConstants.CATEGORY_CONTACT)
                        .title("GitHub Profile")
                        .content("GitHub Profile: https://github.com/" + this.username + " | Public repositories, open-source chat engine, habit tracker, and IoT projects.")
                        .tags(ProfileConstants.TAGS_CONTACT)
                        .build()
        );
    }
}
