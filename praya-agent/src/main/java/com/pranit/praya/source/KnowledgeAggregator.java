package com.pranit.praya.source;

import com.pranit.praya.model.KnowledgeItem;
import com.pranit.praya.model.ProjectRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Composite Aggregator coordinating all registered KnowledgeSource strategies.
 * Demonstrates Open/Closed Principle (OCP): New sources are auto-discovered without
 * modifying this aggregator or downstream consumers.
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
        log.info("Aggregating knowledge from {} registered source(s)", sources.size());
        return sources.stream()
                .filter(KnowledgeSource::isAvailable)
                .flatMap(source -> source.fetchKnowledge().stream())
                .toList();
    }

    /**
     * Aggregates project catalogs across all active sources.
     */
    public List<ProjectRecord> aggregateProjects() {
        log.info("Aggregating projects from {} registered source(s)", sources.size());
        return sources.stream()
                .filter(KnowledgeSource::isAvailable)
                .flatMap(source -> source.fetchProjects().stream())
                .toList();
    }
}
