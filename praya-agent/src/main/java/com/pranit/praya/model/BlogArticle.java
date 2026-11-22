package com.pranit.praya.model;

import lombok.Builder;

import java.util.List;

/**
 * Immutable model representing a published technical blog article or engineering write-up.
 *
 * @param id          Unique identifier for the blog article
 * @param title       Title of the article
 * @param summary     Brief technical summary of the article's contents
 * @param url         Direct URL to the article
 * @param tags        Categorical and technology tags
 * @param readingTime Estimated reading time (e.g. "5 min read")
 */
@Builder
public record BlogArticle(
        String id,
        String title,
        String summary,
        String url,
        List<String> tags,
        String readingTime
) {}
