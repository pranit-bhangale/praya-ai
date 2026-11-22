package com.pranit.praya.github;

import com.pranit.praya.constant.ProfileConstants;
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
        log.info("[DATA-SOURCE] [GITHUB-FETCH-START] Fetching live profile knowledge for user: {}", this.username);
        try {
            Map<String, Object> profile = this.gitHubApiClient.getUserProfile(this.username);
            if (profile != null && !profile.isEmpty()) {
                String name = String.valueOf(profile.getOrDefault("name", this.username));
                String bio = String.valueOf(profile.getOrDefault("bio", ""));
                int publicRepos = profile.get("public_repos") instanceof Number n ? n.intValue() : 0;
                int followers = profile.get("followers") instanceof Number n ? n.intValue() : 0;
                String blog = String.valueOf(profile.getOrDefault("blog", ""));
                String htmlUrl = String.valueOf(profile.getOrDefault("html_url", "https://github.com/" + this.username));
                String location = String.valueOf(profile.getOrDefault("location", ""));
                String company = String.valueOf(profile.getOrDefault("company", ""));
                List<KnowledgeItem> items = List.of(
                        KnowledgeItem.builder()
                                .id("github-live-profile")
                                .source(DataSourceType.GITHUB)
                                .category(ProfileConstants.CATEGORY_PROFILE)
                                .title("Live GitHub Profile: " + name)
                                .content(String.format(
                                        "GitHub Profile: %s | Name: %s | Bio: %s | Public Repositories: %d | Followers: %d | Location: %s | Company: %s | Website: %s",
                                        htmlUrl, name, bio.isBlank() ? "N/A" : bio, publicRepos, followers,
                                        location.isBlank() ? "N/A" : location,
                                        company.isBlank() ? "N/A" : company,
                                        blog.isBlank() ? "N/A" : blog))
                                .tags(List.of("github", "live", "profile", "stats", "repositories"))
                                .build()
                );
                final long durationMs = System.currentTimeMillis() - startTime;
                log.info("[DATA-SOURCE] [GITHUB-FETCH-SUCCESS] Retrieved live profile items: {}, publicRepos: {}, durationMs: {}",
                        items.size(), publicRepos, durationMs);
                return items;
            }
        } catch (Exception e) {
            log.warn("[DATA-SOURCE] [GITHUB-FETCH-ERROR] Profile retrieval failed for user: {} (reason: {})", this.username, e.getMessage());
        }
        return List.of();
    }

    @Override
    public List<ProjectRecord> fetchProjects() {
        final long startTime = System.currentTimeMillis();
        log.info("[DATA-SOURCE] [GITHUB-FETCH-START] Fetching live public repository list for user: {}", this.username);
        try {
            List<Map<String, Object>> repos = this.gitHubApiClient.listPublicRepos(this.username);
            if (repos != null && !repos.isEmpty()) {
                List<ProjectRecord> liveProjects = new ArrayList<>();
                for (Map<String, Object> repo : repos) {
                    String name = String.valueOf(repo.getOrDefault("name", ""));
                    Object isPrivate = repo.get("private");
                    if (Boolean.TRUE.equals(isPrivate) || "private".equalsIgnoreCase(String.valueOf(repo.get("visibility")))) {
                        log.debug("[DATA-SOURCE] [GITHUB-REPO-SKIP] Skipping private repository [{}]", name);
                        continue;
                    }
                    String description = String.valueOf(repo.getOrDefault("description", ""));
                    if ("null".equals(description) || description.isBlank()) {
                        description = "Open-source repository on GitHub";
                    }
                    String htmlUrl = String.valueOf(repo.getOrDefault("html_url", "https://github.com/" + this.username + "/" + name));
                    String homepage = String.valueOf(repo.getOrDefault("homepage", ""));
                    String language = String.valueOf(repo.getOrDefault("language", ""));
                    if ("null".equals(language)) language = "";
                    int stars = repo.get("stargazers_count") instanceof Number n ? n.intValue() : 0;
                    int forks = repo.get("forks_count") instanceof Number n ? n.intValue() : 0;
                    String updatedAt = String.valueOf(repo.getOrDefault("updated_at", ""));
                    List<String> techStack = new ArrayList<>();
                    if (StringUtils.hasText(language)) techStack.add(language);
                    Object topicsObj = repo.get("topics");
                    if (topicsObj instanceof Collection<?> collection) {
                        for (Object topic : collection) {
                            if (topic != null && StringUtils.hasText(topic.toString())) techStack.add(topic.toString());
                        }
                    }
                    List<String> highlights = new ArrayList<>();
                    if (stars > 0) highlights.add(stars + " GitHub stars");
                    if (forks > 0) highlights.add(forks + " forks");
                    if (StringUtils.hasText(language)) highlights.add("Primary language: " + language);
                    if (StringUtils.hasText(updatedAt) && !"null".equals(updatedAt))
                        highlights.add("Recently updated: " + updatedAt);
                    if (highlights.isEmpty()) highlights.add("Live GitHub repository by " + this.username);
                    liveProjects.add(ProjectRecord.builder()
                            .id(name.toLowerCase().replaceAll("[^a-z0-9-]", "-"))
                            .name(formatProjectTitle(name))
                            .description(description)
                            .techStack(techStack.isEmpty() ? List.of("Software Engineering") : techStack)
                            .liveUrl(StringUtils.hasText(homepage) && !"null".equals(homepage) ? homepage : htmlUrl)
                            .repoUrl(htmlUrl)
                            .highlights(highlights)
                            .build());
                }

                final long durationMs = System.currentTimeMillis() - startTime;
                log.info("[DATA-SOURCE] [GITHUB-FETCH-SUCCESS] Retrieved live repositories count: {}, durationMs: {}",
                        liveProjects.size(), durationMs);
                return liveProjects;
            }
        } catch (Exception e) {
            log.warn("[DATA-SOURCE] [GITHUB-FETCH-ERROR] Live repository retrieval failed for user: {} (reason: {})", this.username, e.getMessage());
        }
        return List.of();
    }

    private String formatProjectTitle(String name) {
        return name.replace("-", " ").replace("_", " ");
    }
}
