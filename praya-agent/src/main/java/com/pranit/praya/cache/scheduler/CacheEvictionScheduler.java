package com.pranit.praya.cache.scheduler;

import com.pranit.praya.cache.service.AgentCacheService;
import com.pranit.praya.repository.KnowledgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled background tasks for automated cache eviction and data collection refresh.
 * Keeps memory optimal and prevents stale cached responses without exposing manual HTTP eviction endpoints.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheEvictionScheduler {

    private final AgentCacheService agentCacheService;
    private final KnowledgeRepository knowledgeRepository;

    /**
     * Periodically evicts all cached AI answers every 6 hours (or configurable via cron).
     */
    @Scheduled(cron = "0 0 */6 * * *")
    public void scheduleAnswerCacheEviction() {
        log.info("Executing scheduled eviction of AI agent response cache");
        this.agentCacheService.evictAllAnswers();
        log.info("AI agent response cache successfully evicted");
    }

    /**
     * Refreshes portfolio and project data collections and clears knowledge caches every 24 hours at midnight.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void scheduleKnowledgeRefreshAndEviction() {
        log.info("Executing scheduled refresh and cache eviction for portfolio knowledge collections");
        this.knowledgeRepository.refreshAndEvictAll();
        log.info("Portfolio collections refreshed and associated caches evicted");
    }
}
