package com.pranit.praya.cache.scheduler;

import com.pranit.praya.cache.service.AgentCacheService;
import com.pranit.praya.repository.KnowledgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled background tasks for automated cache eviction and data collection refresh.
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
        final long startTime = System.currentTimeMillis();
        log.info("[SCHEDULER] [CACHE-EVICT] Starting scheduled eviction of AI agent response cache");
        this.agentCacheService.evictAllAnswers();
        final long durationMs = System.currentTimeMillis() - startTime;
        log.info("[SCHEDULER] [CACHE-EVICT] AI agent response cache successfully cleared (durationMs: {})", durationMs);
    }

    /**
     * Refreshes portfolio and project data collections and clears knowledge caches every 24 hours at midnight.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void scheduleKnowledgeRefreshAndEviction() {
        final long startTime = System.currentTimeMillis();
        log.info("[SCHEDULER] [DATA-REFRESH] Starting scheduled refresh of portfolio knowledge and project collections");
        this.knowledgeRepository.refreshAndEvictAll();
        final long durationMs = System.currentTimeMillis() - startTime;
        log.info("[SCHEDULER] [DATA-REFRESH] Portfolio collections refreshed and associated caches evicted (durationMs: {})", durationMs);
    }
}
