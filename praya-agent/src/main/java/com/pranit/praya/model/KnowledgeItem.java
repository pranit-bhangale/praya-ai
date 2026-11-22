package com.pranit.praya.model;

import lombok.Builder;

import java.util.List;

@Builder
public record KnowledgeItem(
        String id,
        DataSourceType source,
        String category,
        String title,
        String content,
        List<String> tags
) {}
