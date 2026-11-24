package com.pranit.praya.source;

import com.pranit.praya.model.DataSourceType;
import com.pranit.praya.model.KnowledgeItem;
import com.pranit.praya.model.ProjectRecord;

import java.util.List;

/**
 * Strategy contract for knowledge data sources.
 */
public interface KnowledgeSource {

    DataSourceType getType();

    List<KnowledgeItem> fetchKnowledge();

    default List<ProjectRecord> fetchProjects() {
        return List.of();
    }

    default boolean isAvailable() {
        return true;
    }
}
