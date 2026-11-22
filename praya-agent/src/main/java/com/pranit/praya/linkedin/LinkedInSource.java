package com.pranit.praya.linkedin;

import com.pranit.praya.constant.ApiConstants;
import com.pranit.praya.constant.ProfileConstants;
import com.pranit.praya.model.DataSourceType;
import com.pranit.praya.model.KnowledgeItem;
import com.pranit.praya.model.ProjectRecord;
import com.pranit.praya.ratelimit.RateLimiter;
import com.pranit.praya.source.KnowledgeSource;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Data source implementation for Pranit's LinkedIn profile operating strictly on public information.
 */
@Slf4j
@Component
public class LinkedInSource implements KnowledgeSource {

    private final RateLimiter rateLimiter;
    private final String profileUrl;

    public LinkedInSource(
            RateLimiter rateLimiter,
            @Value("${praya.linkedin.url:" + ProfileConstants.LINKEDIN_URL + "}") String profileUrl
    ) {
        this.rateLimiter = rateLimiter;
        this.profileUrl = profileUrl;
    }

    @Override
    public DataSourceType getType() {
        return DataSourceType.LINKEDIN;
    }

    @Override
    public List<KnowledgeItem> fetchKnowledge() {
        final long startTime = System.currentTimeMillis();
        log.info("[DATA-SOURCE] [LINKEDIN-FETCH-START] Initiating fetch for LinkedIn profile: {}", this.profileUrl);

        try {
            this.rateLimiter.acquire(ApiConstants.RATE_LIMIT_KEY_LINKEDIN);
            Document doc = Jsoup.connect(this.profileUrl)
                    .userAgent(ApiConstants.BROWSER_USER_AGENT)
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .timeout(6000)
                    .followRedirects(true)
                    .get();

            String pageTitle = doc.title();
            String location = doc.location();

            // Detect LinkedIn AuthWall gate
            if (location.contains(ApiConstants.LINKEDIN_AUTHWALL_PATH)
                    || location.contains(ApiConstants.LINKEDIN_LOGIN_PATH)
                    || pageTitle.toLowerCase().contains("authwall")) {
                log.warn("[DATA-SOURCE] [LINKEDIN-AUTHWALL] LinkedIn AuthWall gate detected (location: {}). Serving verified public baseline credentials.", location);
            } else {
                String ogTitle = doc.select("meta[property=og:title]").attr("content");
                String ogDesc = doc.select("meta[property=og:description]").attr("content");

                if (StringUtils.hasText(ogTitle) || StringUtils.hasText(ogDesc)) {
                    List<KnowledgeItem> items = new ArrayList<>(getCuratedKnowledge());
                    items.add(KnowledgeItem.builder()
                            .id("linkedin-live-opengraph")
                            .source(DataSourceType.LINKEDIN)
                            .category(ProfileConstants.CATEGORY_PROFILE)
                            .title("Live LinkedIn Headline: " + (ogTitle.isBlank() ? pageTitle : ogTitle))
                            .content("Summary: " + ogDesc + " | Profile Link: " + this.profileUrl)
                            .tags(List.of("linkedin", "live", "opengraph", "headline"))
                            .build());
                    final long durationMs = System.currentTimeMillis() - startTime;
                    log.info("[DATA-SOURCE] [LINKEDIN-FETCH-SUCCESS] Scraped live metadata successfully (items: {}, durationMs: {})",
                            items.size(), durationMs);
                    return items;
                }
            }
        } catch (HttpStatusException e) {
            log.warn("[DATA-SOURCE] [LINKEDIN-SCRAPE-GATED] HTTP status {} (LinkedIn anti-scraping CDN/999). Serving verified public credentials.", e.getStatusCode());
        } catch (Exception e) {
            log.warn("[DATA-SOURCE] [LINKEDIN-FETCH-FALLBACK] Live probe failed (reason: {}). Serving verified public credentials.", e.getMessage());
        }

        return getCuratedKnowledge();
    }

    @Override
    public List<ProjectRecord> fetchProjects() {
        return List.of();
    }

    private List<KnowledgeItem> getCuratedKnowledge() {
        return List.of(
                KnowledgeItem.builder()
                        .id("linkedin-pgdac")
                        .source(DataSourceType.LINKEDIN)
                        .category(ProfileConstants.CATEGORY_EDUCATION)
                        .title("Post Graduate Diploma in Advanced Computing (PG-DAC)")
                        .content("Credential / Diploma: Post Graduate Diploma in Advanced Computing (PG-DAC) | Institution: Institute of Emerging Technologies (IET), CDAC | Core Curriculum: Object-Oriented Programming, Data Structures & Algorithms, Operating Systems, Computer Networks, Microservices, and Distributed Systems.")
                        .tags(List.of("education", "pg-dac", "cdac", "iet", "diploma", "certification", "credentials"))
                        .build(),

                KnowledgeItem.builder()
                        .id("linkedin-be")
                        .source(DataSourceType.LINKEDIN)
                        .category(ProfileConstants.CATEGORY_EDUCATION)
                        .title("Bachelor of Engineering (BE) in Electronics & Telecommunication")
                        .content("Degree: Bachelor of Engineering (BE) in Electronics & Telecommunication | Institution: Savitribai Phule Pune University (SPPU) | Focus: Electronics, Embedded Systems, Telecommunication, and Core Engineering Concepts.")
                        .tags(List.of("education", "degree", "be", "sppu", "pune university", "engineering"))
                        .build(),

                KnowledgeItem.builder()
                        .id("linkedin-experience")
                        .source(DataSourceType.LINKEDIN)
                        .category(ProfileConstants.CATEGORY_EXPERIENCE)
                        .title("Professional Focus & Architecture Philosophy")
                        .content(ProfileConstants.PHILOSOPHY_CONTENT)
                        .tags(ProfileConstants.TAGS_PHILOSOPHY)
                        .build(),

                KnowledgeItem.builder()
                        .id("linkedin-profile-link")
                        .source(DataSourceType.LINKEDIN)
                        .category(ProfileConstants.CATEGORY_CONTACT)
                        .title("LinkedIn Profile Link")
                        .content("LinkedIn Profile: " + this.profileUrl + " | Connect with Pranit for engineering collaborations and backend roles.")
                        .tags(ProfileConstants.TAGS_CONTACT)
                        .build()
        );
    }
}
