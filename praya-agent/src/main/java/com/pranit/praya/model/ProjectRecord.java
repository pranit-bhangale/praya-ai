package com.pranit.praya.model;

import lombok.Builder;

import java.util.List;

@Builder
public record ProjectRecord(
        String id,
        String name,
        String description,
        List<String> techStack,
        String liveUrl,
        String repoUrl,
        List<String> highlights
) {
}
