package com.pranit.praya.model;

import lombok.Builder;

/**
 * Immutable model representing a public GitHub repository with repository statistics and URLs.
 *
 * @param name        Repository name
 * @param description Brief summary of the repository's purpose
 * @param language    Primary programming language
 * @param stars       Stargazers count
 * @param forks       Fork count
 * @param htmlUrl     Direct GitHub repository URL
 * @param liveUrl     Live demo or project deployment URL
 */
@Builder
public record GitHubRepoRecord(
        String name,
        String description,
        String language,
        int stars,
        int forks,
        String htmlUrl,
        String liveUrl
) {}
