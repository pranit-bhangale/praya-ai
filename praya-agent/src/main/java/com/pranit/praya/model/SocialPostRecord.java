package com.pranit.praya.model;

import lombok.Builder;

import java.util.List;

/**
 * Immutable model representing a LinkedIn thought leadership post, engineering article, or professional update.
 *
 * @param id       Unique identifier for the post
 * @param title    Headline or title of the post
 * @param platform Platform where published (e.g. "LinkedIn")
 * @param summary  Executive summary of the discussion or technical insight
 * @param url      Direct link to the post or profile
 * @param topics   Key engineering topics discussed
 */
@Builder
public record SocialPostRecord(
        String id,
        String title,
        String platform,
        String summary,
        String url,
        List<String> topics
) {}
