package com.pranit.praya.model;

import lombok.Builder;

/**
 * Lightweight education summary representation for high-level credential overviews.
 */
@Builder
public record EducationSummary(
        String credentialName,
        String institution,
        String credentialType
) {}
