package com.pranit.praya.constant;

import lombok.experimental.UtilityClass;

/**
 * Constants for external APIs, HTTP clients, and status responses.
 */
@UtilityClass
public class ApiConstants {

    /**
     * GitHub API
     */
    public static final String GITHUB_DEFAULT_BASE_URL = "https://api.github.com";
    public static final String GITHUB_API_VERSION_HEADER = "X-GitHub-Api-Version";
    public static final String GITHUB_API_VERSION = "2022-11-28";
    public static final String GITHUB_USER_AGENT = "Praya-AI-Agent";
    public static final int GITHUB_MAX_PAGE_SIZE = 100;

    /**
     * LinkedIn Web & Anti-Scraping
     */
    public static final String LINKEDIN_AUTHWALL_PATH = "/authwall";
    public static final String LINKEDIN_LOGIN_PATH = "/login";

    /**
     * General Web Scraping
     */
    public static final String BROWSER_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    /**
     * Rate Limiter Domain Keys
     */
    public static final String RATE_LIMIT_KEY_GITHUB = "github";
    public static final String RATE_LIMIT_KEY_LINKEDIN = "linkedin";
    public static final String RATE_LIMIT_KEY_PORTFOLIO = "portfolio";

    /**
     * Tool Response Statuses
     */
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_NOT_AVAILABLE = "NOT_AVAILABLE";
    public static final String STATUS_ERROR = "ERROR";
}
