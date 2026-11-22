package com.pranit.praya.github;

import java.util.List;
import java.util.Map;

/**
 * Public Contract for interacting with the GitHub REST API.
 */
public interface GitHubApiClient {

    List<Map<String, Object>> listPublicRepos(String username);

    Map<String, Object> getRepoTree(String owner, String repo, String branch);

    String getFileContent(String owner, String repo, String path);

    Map<String, Object> getUserProfile(String username);

    default String getReadmeContent(String owner, String repo) {
        return getFileContent(owner, repo, "README.md");
    }
}
