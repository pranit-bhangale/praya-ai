package com.pranit.praya.constant;

import java.util.List;

/**
 * Centralized CORS configuration constants.
 */
public final class CorsConstants {

    private CorsConstants() {}

    public static final String API_PATH_PATTERN = "/api/**";

    public static final String[] ALLOWED_ORIGINS = {
            ProfileConstants.PORTFOLIO_URL,
            "http://localhost:3000",
            "http://localhost:5173"
    };

    public static final String[] ALLOWED_METHODS = {
            "GET", "POST", "PUT", "DELETE", "OPTIONS"
    };

    public static final String ALLOWED_HEADERS = "*";

    public static final String[] EXPOSED_HEADERS = {
            "Content-Type", "Cache-Control", "Connection", "X-API-Version"
    };

    public static final long MAX_AGE_SECONDS = 3600L;
}
