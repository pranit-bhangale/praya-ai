package com.pranit.praya.github;

import com.pranit.praya.constant.ApiConstants;
import com.pranit.praya.ratelimit.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Production implementation of the GitHub REST API client operating strictly on public data.
 */
@Slf4j
@Service
public final class GitHubApiClientImpl implements GitHubApiClient {

    private static final ParameterizedTypeReference<List<Map<String, Object>>> REPOSITORY_LIST_RESPONSE =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Map<String, Object>> GITHUB_OBJECT_RESPONSE =
            new ParameterizedTypeReference<>() {};

    private final RestClient.Builder restClientBuilder;
    private final RateLimiter rateLimiter;
    private final String baseUrl;

    public GitHubApiClientImpl(
            RestClient.Builder restClientBuilder,
            RateLimiter rateLimiter,
            @Value("${praya.github.api-url:https://api.github.com}") String baseUrl
    ) {
        this.restClientBuilder = restClientBuilder;
        this.rateLimiter = rateLimiter;
        this.baseUrl = StringUtils.hasText(baseUrl) ? baseUrl : GitHubConstants.DEFAULT_API_BASE_URL;
    }

    @Override
    public List<Map<String, Object>> listPublicRepos(final String username) {
        final List<Map<String, Object>> allRepositories = new ArrayList<>();
        int page = 1;
        while (true) {
            final int currentPage = page;
            this.rateLimiter.acquire(ApiConstants.RATE_LIMIT_KEY_GITHUB);
            List<Map<String, Object>> repositories = client()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/users/{username}/repos")
                            .queryParam("type", "public")
                            .queryParam("sort", "updated")
                            .queryParam("per_page", GitHubConstants.MAX_PAGE_SIZE)
                            .queryParam("page", currentPage)
                            .build(username))
                    .retrieve()
                    .body(REPOSITORY_LIST_RESPONSE);

            if (repositories == null || repositories.isEmpty()) break;
            allRepositories.addAll(repositories);
            if (repositories.size() < GitHubConstants.MAX_PAGE_SIZE) break;
            page++;
        }
        return allRepositories;
    }

    @Override
    public Map<String, Object> getRepoTree(String owner, String repo, String branch) {
        this.rateLimiter.acquire(ApiConstants.RATE_LIMIT_KEY_GITHUB);
        return client()
                .get()
                .uri("/repos/{owner}/{repo}/git/trees/{branch}?recursive=1", owner, repo, branch)
                .retrieve()
                .body(GITHUB_OBJECT_RESPONSE);
    }

    @Override
    public String getFileContent(String owner, String repo, String path) {
        this.rateLimiter.acquire(ApiConstants.RATE_LIMIT_KEY_GITHUB);
        final Map<String, Object> response = client()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repo}/contents/{+path}")
                        .build(owner, repo, path))
                .retrieve()
                .body(GITHUB_OBJECT_RESPONSE);

        if (response == null) return null;
        Object encoding = response.get("encoding");
        Object content = response.get("content");
        if (content == null) return null;

        if ("base64".equalsIgnoreCase(String.valueOf(encoding))) {
            final String encodedContent = String.valueOf(content).replaceAll("\\s", "");
            final byte[] decodedContent = Base64.getDecoder().decode(encodedContent);
            return new String(decodedContent, StandardCharsets.UTF_8);
        }
        return String.valueOf(content);
    }

    @Override
    public Map<String, Object> getUserProfile(String username) {
        this.rateLimiter.acquire(ApiConstants.RATE_LIMIT_KEY_GITHUB);
        return client()
                .get()
                .uri("/users/{username}", username)
                .retrieve()
                .body(GITHUB_OBJECT_RESPONSE);
    }

    private RestClient client() {
        return restClientBuilder
                .baseUrl(this.baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader(GitHubConstants.API_VERSION_HEADER, GitHubConstants.API_VERSION)
                .defaultHeader(HttpHeaders.USER_AGENT, GitHubConstants.USER_AGENT)
                .build();
    }
}
