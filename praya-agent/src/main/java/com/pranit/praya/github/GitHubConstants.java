package com.pranit.praya.github;

import com.pranit.praya.constant.ApiConstants;
import lombok.experimental.UtilityClass;

/**
 * Constants for the GitHub integration module.
 */
@UtilityClass
public class GitHubConstants {

    public static final String DEFAULT_API_BASE_URL = ApiConstants.GITHUB_DEFAULT_BASE_URL;
    public static final String API_VERSION_HEADER = ApiConstants.GITHUB_API_VERSION_HEADER;
    public static final String API_VERSION = ApiConstants.GITHUB_API_VERSION;
    public static final String USER_AGENT = ApiConstants.GITHUB_USER_AGENT;
    public static final int MAX_PAGE_SIZE = ApiConstants.GITHUB_MAX_PAGE_SIZE;
}
