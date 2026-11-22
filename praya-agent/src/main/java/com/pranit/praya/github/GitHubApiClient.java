package com.pranit.praya.github;

import java.util.List;
import java.util.Map;

/**
 * Public Contract for interacting with the GitHub REST API.
 */
public interface GitHubApiClient {

    List<Map<String, Object>> listPublicRepos(String username);

    Map<String, Object> getUserProfile(String username);
}
