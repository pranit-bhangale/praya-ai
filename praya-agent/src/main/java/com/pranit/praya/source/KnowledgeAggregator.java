package com.pranit.praya.source;

import com.pranit.praya.model.KnowledgeItem;
import com.pranit.praya.model.ProjectRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Composite Aggregator coordinating all registered KnowledgeSource strategies.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeAggregator {

    private final List<KnowledgeSource> sources;

    /**
     * Aggregates knowledge documents across all active sources.
     */
    public List<KnowledgeItem> aggregateKnowledge() {
        final long startTime = System.currentTimeMillis();
        List<KnowledgeItem> items = sources.stream()
                .filter(KnowledgeSource::isAvailable)
                .flatMap(source -> source.fetchKnowledge().stream())
                .toList();
        final long durationMs = System.currentTimeMillis() - startTime;
        log.info("[DATA-SOURCE] [KNOWLEDGE-AGGREGATED] Registered sources: {}, totalItems: {}, durationMs: {}",
                sources.size(), items.size(), durationMs);
        return items;
    }

    /**
     * Aggregates project catalogs across all active sources.
     */
    public List<ProjectRecord> aggregateProjects() {
        final long startTime = System.currentTimeMillis();
        List<ProjectRecord> projects = sources.stream()
                .filter(KnowledgeSource::isAvailable)
                .flatMap(source -> source.fetchProjects().stream())
                .toList();
        final long durationMs = System.currentTimeMillis() - startTime;
        log.info("[DATA-SOURCE] [PROJECTS-AGGREGATED] Registered sources: {}, totalProjects: {}, durationMs: {}",
                sources.size(), projects.size(), durationMs);
        return projects;
    }
}
