package com.pranit.praya.github;

import lombok.Builder;

/**
 * Filter request for GitHub project searches.
 */
@Builder
public record ProjectSearchRequest(String keyword) {}
