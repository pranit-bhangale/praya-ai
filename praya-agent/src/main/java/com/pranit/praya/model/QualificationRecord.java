package com.pranit.praya.model;

import lombok.Builder;

/**
 * Immutable record representing an academic qualification or formal degree.
 */
@Builder
public record QualificationRecord(
        String degree,
        String institution,
        String period,
        String details
) {}
