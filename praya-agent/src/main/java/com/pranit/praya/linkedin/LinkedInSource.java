package com.pranit.praya.linkedin;

import com.pranit.praya.constant.ApiConstant;
import com.pranit.praya.constant.ProfileConstant;
import com.pranit.praya.jsoup.JsoupApiClient;
import com.pranit.praya.model.DataSourceType;
import com.pranit.praya.model.KnowledgeItem;
import com.pranit.praya.source.KnowledgeSource;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data source implementation for Pranit's LinkedIn profile operating strictly on public information
 * using {@link JsoupApiClient}.
 */
@Slf4j
@Component
public class LinkedInSource implements KnowledgeSource {

    private final String profileUrl;
    private final JsoupApiClient jsoupApiClient;

    public LinkedInSource(
            JsoupApiClient jsoupApiClient,
            @Value("${praya.linkedin.url:" + ProfileConstant.LINKEDIN_URL + "}") String profileUrl
    ) {
        this.jsoupApiClient = jsoupApiClient;
        this.profileUrl = profileUrl;
    }

    @Override
    public DataSourceType getType() {
        return DataSourceType.LINKEDIN;
    }

    @Override
    public List<KnowledgeItem> fetchKnowledge() {
        Optional<Document> docOpt = this.jsoupApiClient.fetchDocument(this.profileUrl, ApiConstant.RATE_LIMIT_KEY_LINKEDIN);
        if (docOpt.isEmpty()) return List.of();
        Document doc = docOpt.get();
        String pageTitle = doc.title();
        String location = doc.location();
        if (location.contains(ApiConstant.LINKEDIN_AUTHWALL_PATH)
                || location.contains(ApiConstant.LINKEDIN_LOGIN_PATH)
                || pageTitle.toLowerCase().contains("authwall")) {
            log.warn("[DATA-SOURCE] [LINKEDIN-AUTHWALL] LinkedIn AuthWall gate detected (location: {}). Live profile data blocked by LinkedIn.", location);
            return List.of();
        }
        String ogTitle = doc.select("meta[property=og:title]").attr("content");
        String ogDesc = doc.select("meta[property=og:description]").attr("content");
        if (StringUtils.hasText(ogTitle) || StringUtils.hasText(ogDesc)) {
            List<KnowledgeItem> items = new ArrayList<>();
            items.add(KnowledgeItem.builder()
                    .id("linkedin-live-opengraph")
                    .source(DataSourceType.LINKEDIN)
                    .category(ProfileConstant.CATEGORY_PROFILE)
                    .title("Live LinkedIn Profile: " + (ogTitle.isBlank() ? pageTitle : ogTitle))
                    .content("Headline & Summary: " + ogDesc + " | Profile Link: " + this.profileUrl)
                    .tags(List.of("linkedin", "live", "opengraph", "headline", "profile"))
                    .build());
            Elements certElements = doc.select("section[id*=cert], section[class*=cert], div[class*=certification], li[class*=certification]");
            int certIndex = 1;
            for (Element c : certElements) {
                String certTitle = c.select("h3, h4, .title").text().trim();
                String issuer = c.select(".subtitle, .issuer, p").text().trim();
                if (!certTitle.isBlank()) {
                    items.add(KnowledgeItem.builder()
                            .id("linkedin-live-cert-" + (certIndex++))
                            .source(DataSourceType.LINKEDIN)
                            .category(ProfileConstant.CATEGORY_CERTIFICATION)
                            .title(certTitle)
                            .content(issuer.isBlank() ? certTitle : issuer)
                            .tags(List.of("certification", "credential", "linkedin", "live"))
                            .build());
                }
            }
            return items;
        }
        return List.of();
    }
}
