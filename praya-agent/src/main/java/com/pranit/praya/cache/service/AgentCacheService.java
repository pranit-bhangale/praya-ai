package com.pranit.praya.cache.service;

import com.pranit.praya.cache.config.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Service providing Spring annotation-based caching for AI question responses.
 * Uses @Cacheable, @CachePut, and @CacheEvict backed by the AtomicReference cache.
 */
@Service
public class AgentCacheService {

    /**
     * Retrieves cached answer if available.
     * If not found, returns null so the caller can trigger live LLM execution.
     */
    @Cacheable(
            cacheNames = CacheConfig.CACHE_AGENT_RESPONSES,
            key = "T(com.pranit.praya.cache.model.QueryNormalizer).normalize(#prompt)",
            unless = "#result == null"
    )
    public String getCachedAnswer(String prompt) {
        return null;
    }

    /**
     * Stores or updates the computed answer in the cache using @CachePut.
     * Returns the cached answer so Spring's AOP interceptor can store it.
     */
    @CachePut(
            cacheNames = CacheConfig.CACHE_AGENT_RESPONSES,
            key = "T(com.pranit.praya.cache.model.QueryNormalizer).normalize(#prompt)"
    )
    public String putAnswer(String prompt, String answer) {
        return answer;
    }

    /**
     * Evicts a single question and its cached answer using @CacheEvict.
     */
    @CacheEvict(
            cacheNames = CacheConfig.CACHE_AGENT_RESPONSES,
            key = "T(com.pranit.praya.cache.model.QueryNormalizer).normalize(#prompt)"
    )
    public void evictPrompt(String prompt) {
    }

    /**
     * Clears all cached AI answers using @CacheEvict(allEntries = true).
     */
    @CacheEvict(
            cacheNames = CacheConfig.CACHE_AGENT_RESPONSES,
            allEntries = true
    )
    public void evictAllAnswers() {
    }
}
