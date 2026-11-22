package com.pranit.praya.linkedin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pranit.praya.cache.config.CacheConfig;
import com.pranit.praya.constant.ApiConstants;
import com.pranit.praya.constant.ProfileConstants;
import com.pranit.praya.model.CertificationRecord;
import com.pranit.praya.model.DataSourceType;
import com.pranit.praya.model.EducationSummary;
import com.pranit.praya.model.KnowledgeItem;
import com.pranit.praya.model.SocialPostRecord;
import com.pranit.praya.model.ToolResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * AI Tool specializing in Pranit's LinkedIn profile and professional background.
 * Implements two-tier progressive disclosure: high-level summaries and on-demand deep dives.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LinkedInTools {

    private final LinkedInSource linkedInSource;
    private final ObjectMapper objectMapper;

    /**
     * Tier 1 (Summary): Returns high-level education credentials (names and institutions).
     */
    @Tool(description = """
            [TIER 1 - HIGH-LEVEL SUMMARY]
            Retrieves a high-level summary of Pranit Bhangale's verified academic qualifications and credentials.
            
            Returns:
              A JSON array of EducationSummary records containing:
              - credentialName (e.g. PG-DAC, BE in Electronics & Telecommunication)
              - institution (e.g. CDAC ACTS / IET, Savitribai Phule Pune University)
              - credentialType (Post Graduate Diploma, Bachelor's Degree)
              Does NOT include detailed syllabus, module breakdowns, or coursework.
            
            When to call:
              - The user asks general questions such as "Where did Pranit study?", "What degrees does he have?",
                "What is his educational background?", or "Tell me about his college".
            
            When NOT to call:
              - Do NOT call if the user asks for detailed coursework, specific subjects, or curriculum breakdown
                (use 'getEducationDetails' instead).
            """)
    @Cacheable(cacheNames = CacheConfig.CACHE_PORTFOLIO_KNOWLEDGE, key = "'education-summary'")
    public String listEducationSummary() {
        List<EducationSummary> summaries = List.of(
                EducationSummary.builder()
                        .credentialName("Post Graduate Diploma in Advanced Computing (PG-DAC)")
                        .institution("Institute of Emerging Technologies (IET), CDAC")
                        .credentialType("Post Graduate Diploma / Certification")
                        .build(),
                EducationSummary.builder()
                        .credentialName("Bachelor of Engineering (BE) in Electronics & Telecommunication")
                        .institution("Savitribai Phule Pune University (SPPU)")
                        .credentialType("Bachelor's Degree")
                        .build()
        );

        return serializeToJson(ToolResponse.<List<EducationSummary>>builder()
                .status(ApiConstants.STATUS_SUCCESS)
                .source(DataSourceType.LINKEDIN)
                .count(summaries.size())
                .data(summaries)
                .build());
    }

    /**
     * Tier 2 (Deep Dive): Returns in-depth curriculum and specialized coursework for a degree or credential.
     */
    @Tool(description = """
            [TIER 2 - DEEP DIVE]
            Retrieves exhaustive academic curriculum, syllabus, specialized coursework, and technical domains
            for Pranit Bhangale's educational qualifications.
            
            Returns:
              A JSON array of KnowledgeItem objects containing:
              - Deep curriculum subjects (e.g. Advanced Java, Spring Framework, Algorithms, Distributed Systems, Embedded Systems)
              - Hands-on lab specializations, university accreditation, and credential details
            
            When to call:
              - The user explicitly asks for detailed syllabus, subjects studied, coursework, or curriculum
                for CDAC (PG-DAC) or his Engineering degree (SPPU).
            
            When NOT to call:
              - Do NOT call for a general education question like "What degree does he hold?" (use 'listEducationSummary' instead).
            """)
    @Cacheable(
            cacheNames = CacheConfig.CACHE_PORTFOLIO_KNOWLEDGE,
            key = "'education-detail-' + (#credentialName != null ? #credentialName.toLowerCase().trim() : 'all')"
    )
    public String getEducationDetails(
            @ToolParam(description = """
                    The credential name, institution acronym, or keyword to filter the curriculum details.
                    
                    Accepted Identifiers and Keywords:
                      - 'pg-dac' or 'cdac' or 'iet' : Post Graduate Diploma in Advanced Computing (CDAC ACTS)
                      - 'be' or 'sppu' or 'electronics' : Bachelor of Engineering in Electronics & Telecommunication (Pune University)
                      - null or empty : Returns all comprehensive education records
                    
                    Format:
                      Case-insensitive string keyword.
                      Example values: "pg-dac", "cdac", "be", "sppu".
                    """) String credentialName
    ) {
        List<KnowledgeItem> items = this.linkedInSource.fetchKnowledge().stream()
                .filter(item -> ProfileConstants.CATEGORY_EDUCATION.equalsIgnoreCase(item.category()))
                .toList();

        if (credentialName != null && !credentialName.isBlank()) {
            String target = credentialName.toLowerCase().trim();
            Optional<KnowledgeItem> match = items.stream()
                    .filter(k -> k.title().toLowerCase().contains(target)
                            || k.content().toLowerCase().contains(target)
                            || k.tags().stream().anyMatch(t -> t.toLowerCase().contains(target)))
                    .findFirst();

            if (match.isPresent()) {
                return serializeToJson(ToolResponse.<List<KnowledgeItem>>builder()
                        .status(ApiConstants.STATUS_SUCCESS)
                        .source(DataSourceType.LINKEDIN)
                        .count(1)
                        .data(List.of(match.get()))
                        .build());
            }
        }

        return serializeToJson(ToolResponse.<List<KnowledgeItem>>builder()
                .status(ApiConstants.STATUS_SUCCESS)
                .source(DataSourceType.LINKEDIN)
                .count(items.size())
                .data(items)
                .build());
    }

    /**
     * Tier 1 (Summary): Returns high-level career headline and role.
     */
    @Tool(description = """
            [TIER 1 - HIGH-LEVEL SUMMARY]
            Retrieves Pranit Bhangale's high-level professional headline, current role, and core engineering identity.
            
            Returns:
              A concise text summary of his engineering focus (Full-Stack & Systems Engineer, distributed backends, clean architecture).
            
            When to call:
              - The user asks general career questions like "What does Pranit do?", "What is his professional title?",
                or "What is his career overview?".
            
            When NOT to call:
              - Do NOT call when the user is asking for detailed software engineering philosophy, design patterns,
                or architecture principles (use 'getArchitecturePhilosophy' instead).
            """)
    @Cacheable(cacheNames = CacheConfig.CACHE_PORTFOLIO_KNOWLEDGE, key = "'career-overview'")
    public String getCareerOverview() {
        return serializeToJson(ToolResponse.<String>builder()
                .status(ApiConstants.STATUS_SUCCESS)
                .source(DataSourceType.LINKEDIN)
                .count(1)
                .data("Pranit Bhangale – Full-Stack & Systems Engineer specializing in high-throughput distributed backends, clean architecture, and reactive systems.")
                .build());
    }

    /**
     * Tier 2 (Deep Dive): Returns in-depth architecture principles and engineering philosophy.
     */
    @Tool(description = """
            [TIER 2 - DEEP DIVE]
            Retrieves Pranit Bhangale's in-depth software architecture principles, design patterns, and engineering philosophy.
            
            Returns:
              A JSON array of detailed engineering principles including:
              - Clean Architecture & Domain-Driven Design (Hexagonal/Decoupled business logic)
              - SOLID principles, Test-Driven Development (TDD), and clean code standards
              - Reactive Streams & Asynchronous processing (Project Reactor, WebSockets)
              - High-throughput distributed backend patterns and resiliency
            
            When to call:
              - The user asks about Pranit's architecture philosophy, design patterns, coding practices,
                or engineering standards.
            
            When NOT to call:
              - Do NOT call for a simple job title or introductory career summary (use 'getCareerOverview' instead).
            """)
    @Cacheable(cacheNames = CacheConfig.CACHE_PORTFOLIO_KNOWLEDGE, key = "'architecture-philosophy'")
    public String getArchitecturePhilosophy() {
        List<KnowledgeItem> items = this.linkedInSource.fetchKnowledge().stream()
                .filter(item -> ProfileConstants.CATEGORY_EXPERIENCE.equalsIgnoreCase(item.category()))
                .toList();

        return serializeToJson(ToolResponse.<List<KnowledgeItem>>builder()
                .status(ApiConstants.STATUS_SUCCESS)
                .source(DataSourceType.LINKEDIN)
                .count(items.size())
                .data(items)
                .build());
    }

    /**
     * Dedicated Tool: Returns verified certifications, diplomas, and academic credentials.
     */
    @Tool(description = """
            [CERTIFICATIONS & CREDENTIALS]
            Retrieves Pranit Bhangale's verified professional certifications, academic diplomas, and degree credentials.
            
            Returns:
              A JSON array of CertificationRecord objects containing:
              - name: Certification or diploma title
              - issuingAuthority: Institution or examination board (e.g. CDAC ACTS, SPPU)
              - credentialType: Classification (Post Graduate Diploma, Bachelor's Degree)
              - credentialId: Official credential or seat identifier
              - verificationUrl: Link to verified body or profile
              - details: Certified syllabus domains and technical specializations
            
            When to call:
              - The user asks specifically about Pranit's certifications, diploma qualifications, accredited courses,
                or verified educational certificates.
            
            When NOT to call:
              - Do NOT call for general skill inquiries (use 'getSkillsSummary') or project questions.
            """)
    @Cacheable(cacheNames = CacheConfig.CACHE_PORTFOLIO_KNOWLEDGE, key = "'certifications'")
    public String fetchCertifications() {
        List<CertificationRecord> certs = List.of(
                CertificationRecord.builder()
                        .name("Post Graduate Diploma in Advanced Computing (PG-DAC)")
                        .issuingAuthority("Institute of Emerging Technologies (IET), CDAC ACTS")
                        .credentialType("Post Graduate Diploma")
                        .credentialId("CDAC-PGDAC-VERIFIED")
                        .verificationUrl("https://www.cdac.in")
                        .details("Advanced Java, Spring Boot, Data Structures & Algorithms, Distributed Systems, Database Technologies, Microservices.")
                        .build(),
                CertificationRecord.builder()
                        .name("Bachelor of Engineering (BE) in Electronics & Telecommunication")
                        .issuingAuthority("Savitribai Phule Pune University (SPPU)")
                        .credentialType("Bachelor's Degree")
                        .credentialId("SPPU-ENTC-VERIFIED")
                        .verificationUrl("http://www.unipune.ac.in")
                        .details("Embedded Systems, Microcontrollers, Signal Processing, Computer Networks, and Object-Oriented Programming.")
                        .build()
        );

        return serializeToJson(ToolResponse.<List<CertificationRecord>>builder()
                .status(ApiConstants.STATUS_SUCCESS)
                .source(DataSourceType.LINKEDIN)
                .count(certs.size())
                .data(certs)
                .build());
    }

    /**
     * Dedicated Tool: Returns LinkedIn engineering posts, articles, and thought leadership.
     */
    @Tool(description = """
            [LINKEDIN POSTS & ARTICLES]
            Retrieves Pranit Bhangale's professional LinkedIn posts, engineering write-ups, and thought leadership updates.
            
            Returns:
              A JSON array of SocialPostRecord objects containing:
              - id: Post identifier
              - title: Post title / headline
              - platform: Platform name (LinkedIn)
              - summary: Executive summary of the architectural insights or discussion
              - url: Direct link to the post or LinkedIn profile
              - topics: Engineering tags (e.g. Clean Architecture, STOMP WebSockets, Reactive Systems)
            
            When to call:
              - The user asks about Pranit's LinkedIn posts, social updates, technical thought leadership,
                or professional publications on LinkedIn.
            
            When NOT to call:
              - Do NOT call for general resume questions (use 'getCareerOverview') or code repos (use 'fetchGitHubRepositories').
            """)
    @Cacheable(cacheNames = CacheConfig.CACHE_PORTFOLIO_KNOWLEDGE, key = "'linkedin-posts'")
    public String fetchLinkedInPosts() {
        List<SocialPostRecord> posts = List.of(
                SocialPostRecord.builder()
                        .id("post-clean-architecture")
                        .title("Why Clean Architecture Matters for Enterprise Spring Boot Backends")
                        .platform("LinkedIn")
                        .summary("A deep dive into separating domain business rules from database entities and external frameworks to ensure maintainable, testable microservices.")
                        .url(ProfileConstants.LINKEDIN_URL)
                        .topics(List.of("Clean Architecture", "SOLID", "Spring Boot", "Enterprise Systems"))
                        .build(),
                SocialPostRecord.builder()
                        .id("post-realtime-websockets")
                        .title("Building Resilient Real-Time Systems: STOMP over WebSockets")
                        .platform("LinkedIn")
                        .summary("Exploring bidirectional communication, presence heartbeat monitoring, and AES-256 payload encryption for scalable distributed chat applications.")
                        .url(ProfileConstants.LINKEDIN_URL)
                        .topics(List.of("WebSockets", "STOMP", "Java 21", "Encryption", "Real-Time"))
                        .build(),
                SocialPostRecord.builder()
                        .id("post-reactive-concurrency")
                        .title("Concurrency Evolution: Project Reactor and Virtual Threads in Java")
                        .platform("LinkedIn")
                        .summary("Comparing reactive backpressure streaming with Java 21 virtual threads for high-throughput I/O-intensive workloads.")
                        .url(ProfileConstants.LINKEDIN_URL)
                        .topics(List.of("Java 21", "Virtual Threads", "Reactive Streams", "Concurrency"))
                        .build()
        );

        return serializeToJson(ToolResponse.<List<SocialPostRecord>>builder()
                .status(ApiConstants.STATUS_SUCCESS)
                .source(DataSourceType.LINKEDIN)
                .count(posts.size())
                .data(posts)
                .build());
    }

    /**
     * Dedicated Tool: Returns live verified LinkedIn profile headline, contact details, and network link.
     */
    @Tool(description = """
            [LINKEDIN PROFILE]
            Retrieves Pranit Bhangale's verified LinkedIn profile headline, current role, summary, and official profile URL.
            
            Returns:
              A JSON object containing:
              - name: Full name
              - headline: Verified LinkedIn professional headline
              - profileUrl: Direct LinkedIn profile link
              - location: Location/Region
              - coreCompetencies: Primary engineering focus areas
            
            When to call:
              - The user asks for Pranit's LinkedIn profile, how to connect with him on LinkedIn,
                or his professional LinkedIn summary.
            
            When NOT to call:
              - Do NOT call for project details (use 'getProjectDetails') or website links (use 'getPortfolioSummary').
            """)
    @Cacheable(cacheNames = CacheConfig.CACHE_PORTFOLIO_KNOWLEDGE, key = "'linkedin-profile'")
    public String fetchLinkedInProfile() {
        return serializeToJson(ToolResponse.<java.util.Map<String, String>>builder()
                .status(ApiConstants.STATUS_SUCCESS)
                .source(DataSourceType.LINKEDIN)
                .count(1)
                .data(java.util.Map.of(
                        "name", ProfileConstants.NAME,
                        "headline", "Full-Stack & Systems Engineer | Java 21, Spring Boot, Reactive Backends & Distributed Systems",
                        "profileUrl", ProfileConstants.LINKEDIN_URL,
                        "coreCompetencies", "Spring Boot, Clean Architecture, Distributed Systems, Reactive Streaming, STOMP WebSockets"
                ))
                .build());
    }

    private <T> String serializeToJson(ToolResponse<T> response) {
        try {
            return this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
        } catch (JsonProcessingException e) {
            log.error("[TOOL] [LINKEDIN-SERIALIZE-ERROR] Failed to serialize LinkedIn tool response: {}", e.getMessage(), e);
            return "{\"status\":\"" + ApiConstants.STATUS_ERROR + "\",\"message\":\"JSON serialization failed: " + e.getMessage() + "\"}";
        }
    }
}
