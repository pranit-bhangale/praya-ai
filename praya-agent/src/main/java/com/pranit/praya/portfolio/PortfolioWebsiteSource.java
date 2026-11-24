package com.pranit.praya.portfolio;

import com.pranit.praya.constant.ApiConstant;
import com.pranit.praya.constant.ProfileConstant;
import com.pranit.praya.model.DataSourceType;
import com.pranit.praya.model.KnowledgeItem;
import com.pranit.praya.model.ProjectRecord;
import com.pranit.praya.ratelimit.RateLimiter;
import com.pranit.praya.source.KnowledgeSource;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data source implementation for Pranit's personal portfolio website.
 */
@Slf4j
@Component
public class PortfolioWebsiteSource implements KnowledgeSource {

    private final String portfolioUrl;
    private final RateLimiter rateLimiter;

    public PortfolioWebsiteSource(
            RateLimiter rateLimiter,
            @Value("${praya.portfolio.url:" + ProfileConstant.PORTFOLIO_URL + "}") String portfolioUrl
    ) {
        this.rateLimiter = rateLimiter;
        this.portfolioUrl = portfolioUrl;
    }

    @Override
    public DataSourceType getType() {
        return DataSourceType.PORTFOLIO_WEBSITE;
    }

    @Override
    public List<KnowledgeItem> fetchKnowledge() {
        this.rateLimiter.acquire(ApiConstant.RATE_LIMIT_KEY_PORTFOLIO);
        final long startTime = System.currentTimeMillis();
        log.info("[DATA-SOURCE] [PORTFOLIO-FETCH-START] Initiating scrape for portfolio website: {}", this.portfolioUrl);
        List<KnowledgeItem> items = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(this.portfolioUrl)
                    .userAgent(ApiConstant.BROWSER_USER_AGENT)
                    .timeout(6000)
                    .followRedirects(true)
                    .get();
            String title = doc.title();
            String metaDesc = Optional.ofNullable(doc.selectFirst("meta[name=description]"))
                    .map(e -> e.attr("content"))
                    .orElse("");
            Elements paragraphs = doc.select("p");
            StringBuilder bodyText = new StringBuilder();
            for (Element p : paragraphs) {
                String text = p.text().trim();
                if (!text.isBlank() && text.length() > 20) {
                    bodyText.append(text).append("\n\n");
                }
            }
            if (!bodyText.isEmpty() || !metaDesc.isBlank()) {
                String content = (metaDesc.isBlank() ? "" : metaDesc + "\n\n") + bodyText.toString().trim();
                items.add(KnowledgeItem.builder()
                        .id("portfolio-web-bio")
                        .source(DataSourceType.PORTFOLIO_WEBSITE)
                        .category(ProfileConstant.CATEGORY_BIO)
                        .title(title.isBlank() ? "Portfolio Summary" : title)
                        .content(content)
                        .tags(List.of("portfolio", "bio", "vercel", "live", "about"))
                        .build());
            }
            Elements links = doc.select("a[href]");
            List<String> portfolioLinks = new ArrayList<>();
            for (Element link : links) {
                String href = link.absUrl("href");
                String anchorText = link.text().trim();
                if (href.contains("github.com") || href.contains("linkedin.com") || href.startsWith("mailto:")) {
                    portfolioLinks.add(anchorText.isBlank() ? href : anchorText + ": " + href);
                }
            }
            if (!portfolioLinks.isEmpty()) {
                items.add(KnowledgeItem.builder()
                        .id("portfolio-web-links")
                        .source(DataSourceType.PORTFOLIO_WEBSITE)
                        .category(ProfileConstant.CATEGORY_CONTACT)
                        .title("Portfolio Links & Contact")
                        .content(String.join("\n", portfolioLinks))
                        .tags(List.of("portfolio", "links", "contact"))
                        .build());
            }

            final long durationMs = System.currentTimeMillis() - startTime;
            log.info("[DATA-SOURCE] [PORTFOLIO-FETCH-SUCCESS] Scraped live portfolio sections successfully (items: {}, durationMs: {})",
                    items.size(), durationMs);
        } catch (Exception e) {
            log.warn("[DATA-SOURCE] [PORTFOLIO-FETCH-ERROR] Scrape failed for {} (reason: {})", this.portfolioUrl, e.getMessage());
        }
        return items;
    }

    @Override
    public List<ProjectRecord> fetchProjects() {
        return List.of();
    }
}
