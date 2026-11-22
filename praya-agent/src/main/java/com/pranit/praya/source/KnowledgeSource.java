package com.pranit.praya.source;

import com.pranit.praya.model.DataSourceType;
import com.pranit.praya.model.KnowledgeItem;
import com.pranit.praya.model.ProjectRecord;

import java.util.List;

/**
 * Strategy contract for all knowledge and project data sources.
 * Adheres to Strategy Pattern, Single Responsibility, and Open/Closed Principle (OCP).
 */
public interface KnowledgeSource {

    DataSourceType getType();

    List<KnowledgeItem> fetchKnowledge();

    List<ProjectRecord> fetchProjects();

    default boolean isAvailable() {
        return true;
    }
}
