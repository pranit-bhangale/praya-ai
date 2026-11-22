package com.pranit.praya.linkedin;

import lombok.Builder;

/**
 * Filter request for LinkedIn knowledge searches.
 */
@Builder
public record LinkedInSearchRequest(String query) {}
