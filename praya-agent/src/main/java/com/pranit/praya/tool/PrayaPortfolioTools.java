package com.pranit.praya.tool;

import com.pranit.praya.model.KnowledgeItem;
import com.pranit.praya.model.ProjectRecord;
import com.pranit.praya.repository.KnowledgeRepository;
import lombok.Builder;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Spring AI Tools providing grounded access to Pranit's verified background,
 * skills, projects, and portfolio data fetched from the in-memory collection.
 */
@Component
public class PrayaPortfolioTools {

    private final KnowledgeRepository knowledgeRepository;

    public PrayaPortfolioTools(KnowledgeRepository knowledgeRepository) {
        this.knowledgeRepository = knowledgeRepository;
    }

    @Tool(description = "Lookup Pranit Bhangale's biography, tech stack, skills, education, and portfolio links")
    public List<KnowledgeItem> lookupKnowledge(KnowledgeSearchRequest request) {
        return knowledgeRepository.search(request.query());
    }

    @Tool(description = "Lookup Pranit's projects including Open-Connect chat engine, Habit Tracker, IoT Cleaning Bot, and To-Do app")
    public List<ProjectRecord> lookupProjects(ProjectSearchRequest request) {
        return knowledgeRepository.findProjects(request.keyword());
    }

    @Builder
    public record KnowledgeSearchRequest(String query) {
    }

    @Builder
    public record ProjectSearchRequest(String keyword) {
    }
}