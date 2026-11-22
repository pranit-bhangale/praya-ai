package com.pranit.praya.model;

import lombok.Builder;

/**
 * General knowledge search filter request.
 */
@Builder
public record KnowledgeSearchRequest(String query) {}
