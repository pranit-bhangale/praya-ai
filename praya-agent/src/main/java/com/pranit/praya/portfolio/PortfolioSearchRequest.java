package com.pranit.praya.portfolio;

import lombok.Builder;

/**
 * Filter request for Portfolio website searches.
 */
@Builder
public record PortfolioSearchRequest(String query) {}
