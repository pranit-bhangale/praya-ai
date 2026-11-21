package com.pranit.praya.repository;

import com.pranit.praya.cache.config.CacheConfig;
import com.pranit.praya.data.Data;
import com.pranit.praya.model.KnowledgeItem;
import com.pranit.praya.model.ProjectRecord;
import jakarta.annotation.PostConstruct;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe centralized collection store.
 * Initialized with static seed data and enhanced with Spring annotation-based caching:
 *
 * @Cacheable and @CacheEvict.
 */
@org.springframework.stereotype.Repository
public class KnowledgeRepository {

    private final CopyOnWriteArrayList<KnowledgeItem> knowledgeCollection = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<ProjectRecord> projectCollection = new CopyOnWriteArrayList<>();

    @PostConstruct
    public void init() {
        populateSeedData();
    }

    private void populateSeedData() {
        knowledgeCollection.clear();
        projectCollection.clear();
        knowledgeCollection.addAll(Data.SEED_KNOWLEDGE);
        projectCollection.addAll(Data.SEED_PROJECTS);
    }

    /**
     * Searches knowledge documents. Cached via @Cacheable backed by AtomicReference cache.
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
     * Looks up projects. Cached via @Cacheable backed by AtomicReference cache.
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
     * Refreshes the collection from static defaults and evicts all cached entries via @CacheEvict.
     */
    @CacheEvict(cacheNames = {CacheConfig.CACHE_PORTFOLIO_KNOWLEDGE, CacheConfig.CACHE_PORTFOLIO_PROJECTS}, allEntries = true)
    public void refreshAndEvictAll() {
        populateSeedData();
    }
}
