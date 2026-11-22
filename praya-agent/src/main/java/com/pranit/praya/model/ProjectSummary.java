package com.pranit.praya.model;

import lombok.Builder;

import java.util.List;

/**
 * Lightweight project summary representation for high-level overviews.
 * Intentionally excludes heavy multi-bullet architecture highlights to keep initial responses crisp.
 */
@Builder
public record ProjectSummary(
        String id,
        String name,
        String summary,
        List<String> primaryTech
) {
    public static ProjectSummary from(ProjectRecord p) {
        return ProjectSummary.builder()
                .id(p.id())
                .name(p.name())
                .summary(p.description())
                .primaryTech(p.techStack() != null ? p.techStack() : List.of())
                .build();
    }
}
