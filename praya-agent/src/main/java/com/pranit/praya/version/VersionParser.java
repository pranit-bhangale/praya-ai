package com.pranit.praya.version;

import org.jspecify.annotations.NullMarked;
import org.springframework.web.accept.ApiVersionParser;

/**
 * Custom API Version Parser for versioning.
 */
@NullMarked
public final class VersionParser implements ApiVersionParser<Integer> {

    @Override
    public Integer parseVersion(String version) {
        if (!version.trim().matches("v\\d+")) {
            throw new IllegalArgumentException("Invalid API version format: '" + version + "'. Expected format 'v{number}' (e.g., 'v1').");
        }
        return Integer.parseInt(version.trim().substring(1));
    }
}
