package com.pranit.praya.model;

import lombok.Builder;

/**
 * Strict JSON payload envelope returned by all AI Agent tools.
 */
@Builder
public record ToolResponse<T>(
        String status,
        DataSourceType source,
        int count,
        T data
) {}
