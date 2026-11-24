package com.pranit.praya.github;

import com.pranit.praya.constant.ApiConstant;
import lombok.experimental.UtilityClass;

/**
 * Constants for the GitHub integration module.
 */
@UtilityClass
public class GitHubConstants {

    public static final String DEFAULT_API_BASE_URL = ApiConstant.GITHUB_DEFAULT_BASE_URL;
    public static final String API_VERSION_HEADER = ApiConstant.GITHUB_API_VERSION_HEADER;
    public static final String API_VERSION = ApiConstant.GITHUB_API_VERSION;
    public static final String USER_AGENT = ApiConstant.GITHUB_USER_AGENT;
    public static final int MAX_PAGE_SIZE = ApiConstant.GITHUB_MAX_PAGE_SIZE;
}
