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
        log.info("Fetching public LinkedIn profile data from URL: {}", this.profileUrl);

        try {
            this.rateLimiter.acquire(ApiConstants.RATE_LIMIT_KEY_LINKEDIN);
            log.info("Executing live HTTP GET to public LinkedIn profile: {}", this.profileUrl);
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
                log.warn("LinkedIn AuthWall detected (redirected to {}). Automated scraping blocked by LinkedIn CDN. Serving verified public profile credentials.", location);
            } else {
                String ogTitle = doc.select("meta[property=og:title]").attr("content");
                String ogDesc = doc.select("meta[property=og:description]").attr("content");

                if (StringUtils.hasText(ogTitle) || StringUtils.hasText(ogDesc)) {
                    log.info("Successfully scraped live LinkedIn OpenGraph metadata: {}", ogTitle);
                    List<KnowledgeItem> items = new ArrayList<>(getCuratedKnowledge());
                    items.add(KnowledgeItem.builder()
                            .id("linkedin-live-opengraph")
                            .source(DataSourceType.LINKEDIN)
                            .category(ProfileConstants.CATEGORY_PROFILE)
                            .title("Live LinkedIn Headline: " + (ogTitle.isBlank() ? pageTitle : ogTitle))
                            .content("Summary: " + ogDesc + " | Profile Link: " + this.profileUrl)
                            .tags(List.of("linkedin", "live", "opengraph", "headline"))
                            .build());
                    return items;
                }
            }
        } catch (HttpStatusException e) {
            log.warn("LinkedIn HTTP returned status code {} (LinkedIn anti-scraping gate/999). Serving verified public credentials.", e.getStatusCode());
        } catch (Exception e) {
            log.warn("Live LinkedIn HTTP probe failed ({}). Serving verified public credentials.", e.getMessage());
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
                        .id("linkedin-education")
                        .source(DataSourceType.LINKEDIN)
                        .category(ProfileConstants.CATEGORY_EDUCATION)
                        .title("Education & Academic Credentials")
                        .content(ProfileConstants.EDUCATION_CONTENT)
                        .tags(ProfileConstants.TAGS_EDUCATION)
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
