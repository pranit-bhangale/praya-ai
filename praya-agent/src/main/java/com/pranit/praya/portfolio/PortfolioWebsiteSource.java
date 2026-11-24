package com.pranit.praya.portfolio;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data source implementation for Pranit's personal portfolio website using {@link JsoupApiClient}.
 */
@Slf4j
@Component
public class PortfolioWebsiteSource implements KnowledgeSource {

    private final String portfolioUrl;
    private final JsoupApiClient jsoupApiClient;

    public PortfolioWebsiteSource(
            JsoupApiClient jsoupApiClient,
            @Value("${praya.portfolio.url:" + ProfileConstant.PORTFOLIO_URL + "}") String portfolioUrl
    ) {
        this.jsoupApiClient = jsoupApiClient;
        this.portfolioUrl = portfolioUrl;
    }

    @Override
    public DataSourceType getType() {
        return DataSourceType.PORTFOLIO_WEBSITE;
    }

    @Override
    public List<KnowledgeItem> fetchKnowledge() {
        Optional<Document> docOpt = this.jsoupApiClient.fetchDocument(this.portfolioUrl, ApiConstant.RATE_LIMIT_KEY_PORTFOLIO);
        if (docOpt.isEmpty()) return List.of();
        Document doc = docOpt.get();
        List<KnowledgeItem> items = new ArrayList<>();
        String title = doc.title();
        String metaDesc = Optional.ofNullable(doc.selectFirst("meta[name=description]"))
                .map(e -> e.attr("content"))
                .orElse("");
        Elements paragraphs = doc.select("p");
        StringBuilder bodyText = new StringBuilder();
        for (Element p : paragraphs) {
            String text = p.text().trim();
            if (!text.isBlank() && text.length() > 20) bodyText.append(text).append("\n\n");
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
        Elements blogNodes = doc.select("article, section[id*=blog], section[class*=blog], div[class*=blog], div[class*=post], a[href*=/blog]");
        int blogCounter = 1;
        for (Element node : blogNodes) {
            String blogTitle = node.select("h1, h2, h3, h4, .title").text().trim();
            String blogSummary = node.select("p, .description, .summary").text().trim();
            if (blogTitle.isBlank() && node.is("a")) blogTitle = node.text().trim();
            if (!blogTitle.isBlank() && blogTitle.length() < 150) {
                items.add(KnowledgeItem.builder()
                        .id("portfolio-live-blog-" + (blogCounter++))
                        .source(DataSourceType.PORTFOLIO_WEBSITE)
                        .category(ProfileConstant.CATEGORY_BLOG)
                        .title(blogTitle)
                        .content(blogSummary.isBlank() ? blogTitle : blogSummary)
                        .tags(List.of("blog", "article", "engineering", "live"))
                        .build());
            }
        }
        return items;
    }
}
