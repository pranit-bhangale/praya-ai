package com.pranit.praya.repository;

import com.pranit.praya.cache.config.CacheConfig;
import com.pranit.praya.model.DataSourceType;
import com.pranit.praya.model.KnowledgeItem;
import com.pranit.praya.model.ProjectRecord;
import com.pranit.praya.source.KnowledgeAggregator;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe centralized collection store adhering to the Repository Pattern.
 * Ingests data through the KnowledgeAggregator composite strategy across all 3 sources
 * (Portfolio Website, GitHub, and LinkedIn).
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class KnowledgeRepository {

    private final KnowledgeAggregator knowledgeAggregator;
    private final CopyOnWriteArrayList<KnowledgeItem> knowledgeCollection = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<ProjectRecord> projectCollection = new CopyOnWriteArrayList<>();

    @PostConstruct
    public void init() {
        populateData();
    }

    private void populateData() {
        knowledgeCollection.clear();
        projectCollection.clear();

        // Dynamically aggregate from all registered source strategies
        knowledgeCollection.addAll(this.knowledgeAggregator.aggregateKnowledge());
        projectCollection.addAll(this.knowledgeAggregator.aggregateProjects());

        log.info("KnowledgeRepository loaded {} knowledge items and {} projects across all sources",
                knowledgeCollection.size(), projectCollection.size());
    }

    /**
     * Searches knowledge documents across all sources. Cached via @Cacheable.
     */
    @Cacheable(cacheNames = CacheConfig.CACHE_PORTFOLIO_KNOWLEDGE, key = "#query.toLowerCase().trim()")
    public List<KnowledgeItem> search(String query) {
        String q = query == null ? "" : query.toLowerCase().trim();
        return knowledgeCollection.stream()
                .filter(item -> item.title().toLowerCase().contains(q)
                        || item.content().toLowerCase().contains(q)
                        || item.tags().stream().anyMatch(t -> t.toLowerCase().contains(q)))
                .toList();
    }

    /**
     * Retrieves knowledge documents filtered by originating data source strategy.
     */
    public List<KnowledgeItem> findBySource(DataSourceType source) {
        return knowledgeCollection.stream()
                .filter(item -> item.source() == source)
                .toList();
    }

    /**
     * Looks up projects by name, description, or tech stack keyword. Cached via @Cacheable.
     */
    @Cacheable(cacheNames = CacheConfig.CACHE_PORTFOLIO_PROJECTS, key = "(#keyword != null ? #keyword.toLowerCase().trim() : '')")
    public List<ProjectRecord> findProjects(String keyword) {
        String k = keyword == null ? "" : keyword.toLowerCase().trim();
        return projectCollection.stream()
                .filter(p -> p.name().toLowerCase().contains(k)
                        || p.description().toLowerCase().contains(k)
                        || p.techStack().stream().anyMatch(t -> t.toLowerCase().contains(k)))
                .toList();
    }

    /**
     * Refreshes the collection from all sources and evicts all cached entries via @CacheEvict.
     */
    @CacheEvict(cacheNames = {CacheConfig.CACHE_PORTFOLIO_KNOWLEDGE, CacheConfig.CACHE_PORTFOLIO_PROJECTS}, allEntries = true)
    public void refreshAndEvictAll() {
        populateData();
    }
}
