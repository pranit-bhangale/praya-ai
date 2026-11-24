package com.pranit.praya.linkedin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pranit.praya.cache.config.CacheConfig;
import com.pranit.praya.constant.ApiConstant;
import com.pranit.praya.constant.ProfileConstant;
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
import java.util.Map;
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
     * Returns high-level education credentials (names and institutions).
     */
    @Tool(description = """
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
        try {
            List<KnowledgeItem> items = this.linkedInSource.fetchKnowledge().stream()
                    .filter(item -> ProfileConstant.CATEGORY_EDUCATION.equalsIgnoreCase(item.category())
                            || ProfileConstant.CATEGORY_CERTIFICATION.equalsIgnoreCase(item.category()))
                    .toList();
            if (!items.isEmpty()) {
                List<EducationSummary> summaries = items.stream()
                        .map(item -> EducationSummary.builder()
                                .credentialName(item.title())
                                .institution(item.content())
                                .credentialType(item.category())
                                .build())
                        .toList();
                return serializeToJson(ToolResponse.success(DataSourceType.LINKEDIN, summaries.size(), summaries));
            }
        } catch (Exception e) {
            log.warn("[TOOL] [LINKEDIN-EDUCATION-LIVE-FALLBACK] Live fetch unvailable ({}), serving verified baseline qualifications.", e.getMessage());
        }

        // Verified academic baseline qualifications
        List<EducationSummary> baselineSummaries = List.of(
                EducationSummary.builder()
                        .credentialName("Post Graduate Diploma in Advanced Computing (PG-DAC)")
                        .institution("Institute of Emerging Technologies (IET), CDAC ACTS")
                        .credentialType("Post Graduate Diploma / Accredited Credential")
                        .build(),
                EducationSummary.builder()
                        .credentialName("Bachelor of Engineering (BE) in Electronics & Telecommunication")
                        .institution("Savitribai Phule Pune University (SPPU)")
                        .credentialType("Bachelor's Degree")
                        .build()
        );
        return serializeToJson(ToolResponse.success(DataSourceType.LINKEDIN, baselineSummaries.size(), baselineSummaries));
    }

    private <T> String serializeToJson(ToolResponse<T> response) {
        try {
            return this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
        } catch (JsonProcessingException e) {
            log.error("[TOOL] [LINKEDIN-SERIALIZE-ERROR] Failed to serialize LinkedIn tool response: {}", e.getMessage(), e);
            return "{\"status\":\"" + ApiConstant.STATUS_ERROR + "\",\"message\":\"JSON serialization failed: " + e.getMessage() + "\"}";
        }
    }

    /**
     * Returns in-depth curriculum and specialized coursework for a degree or credential.
     */
    @Tool(description = """
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
        try {
            List<KnowledgeItem> items = this.linkedInSource.fetchKnowledge().stream()
                    .filter(item -> ProfileConstant.CATEGORY_EDUCATION.equalsIgnoreCase(item.category())
                            || ProfileConstant.CATEGORY_CERTIFICATION.equalsIgnoreCase(item.category()))
                    .toList();
            if (!items.isEmpty()) {
                if (credentialName != null && !credentialName.isBlank()) {
                    String target = credentialName.toLowerCase().trim();
                    Optional<KnowledgeItem> match = items.stream()
                            .filter(k -> k.title().toLowerCase().contains(target)
                                    || k.content().toLowerCase().contains(target)
                                    || k.tags().stream().anyMatch(t -> t.toLowerCase().contains(target)))
                            .findFirst();
                    if (match.isPresent()) {
                        return serializeToJson(ToolResponse.success(DataSourceType.LINKEDIN, 1, List.of(match.get())));
                    }
                }
                return serializeToJson(ToolResponse.success(DataSourceType.LINKEDIN, items.size(), items));
            }
        } catch (Exception e) {
            log.warn("[TOOL] [LINKEDIN-EDUCATION-DETAIL-FALLBACK] Live fetch unavailable ({}), serving verified curriculum baseline.", e.getMessage());
        }

        // Verified academic curriculum baseline
        List<KnowledgeItem> baselineDetails = List.of(
                KnowledgeItem.builder()
                        .id("education-overview")
                        .source(DataSourceType.LINKEDIN)
                        .category(ProfileConstant.CATEGORY_EDUCATION)
                        .title("Academic Degrees & Qualifications Overview")
                        .content(ProfileConstant.EDUCATION_CONTENT.trim())
                        .tags(List.of("education", "degrees", "overview", "sppu", "cdac", "iet", "qualifications"))
                        .build(),
                KnowledgeItem.builder()
                        .id("education-pgdac-cdac")
                        .source(DataSourceType.LINKEDIN)
                        .category(ProfileConstant.CATEGORY_EDUCATION)
                        .title("Post Graduate Diploma in Advanced Computing (PG-DAC) - CDAC ACTS")
                        .content("Post Graduate Diploma in Advanced Computing (PG-DAC) from Institute of Emerging Technologies (IET), CDAC ACTS.\n" +
                                "Specialized Curriculum: Object-Oriented Programming (Java, C++), Spring Framework & Spring Boot, Microservices, " +
                                "Data Structures & Algorithms, Database Technologies (PostgreSQL, MySQL, SQL), Operating Systems, and Distributed Computing.")
                        .tags(List.of("education", "pg-dac", "cdac", "iet", "java", "spring boot", "microservices"))
                        .build(),
                KnowledgeItem.builder()
                        .id("education-be-sppu")
                        .source(DataSourceType.LINKEDIN)
                        .category(ProfileConstant.CATEGORY_EDUCATION)
                        .title("Bachelor of Engineering (BE) in Electronics & Telecommunication - Pune University")
                        .content("Bachelor of Engineering in Electronics & Telecommunication from Savitribai Phule Pune University (SPPU).\n" +
                                "Core Domains: Object-Oriented Programming, Computer Networks, Embedded Systems, Microprocessors & Microcontrollers, and Software Engineering.")
                        .tags(List.of("education", "be", "sppu", "pune university", "electronics", "telecommunication"))
                        .build()
        );

        if (credentialName != null && !credentialName.isBlank()) {
            String target = credentialName.toLowerCase().trim();
            Optional<KnowledgeItem> match = baselineDetails.stream()
                    .filter(k -> k.title().toLowerCase().contains(target)
                            || k.content().toLowerCase().contains(target)
                            || k.tags().stream().anyMatch(t -> t.toLowerCase().contains(target)))
                    .findFirst();
            if (match.isPresent()) {
                return serializeToJson(ToolResponse.success(DataSourceType.LINKEDIN, 1, List.of(match.get())));
            }
        }
        return serializeToJson(ToolResponse.success(DataSourceType.LINKEDIN, baselineDetails.size(), baselineDetails));
    }

    /**
     * Returns high-level career headline and role.
     */
    @Tool(description = """
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
        try {
            List<KnowledgeItem> items = this.linkedInSource.fetchKnowledge();
            if (!items.isEmpty()) {
                String headline = items.stream()
                        .filter(k -> ProfileConstant.CATEGORY_PROFILE.equalsIgnoreCase(k.category()))
                        .map(KnowledgeItem::content)
                        .findFirst()
                        .orElse(items.getFirst().content());
                return serializeToJson(ToolResponse.success(DataSourceType.LINKEDIN, 1, headline));
            }
        } catch (Exception e) {
            log.warn("[TOOL] [LINKEDIN-CAREER-FALLBACK] Live fetch unavailable ({}), serving verified career overview.", e.getMessage());
        }

        String headline = ProfileConstant.NAME + " — " + ProfileConstant.DEFAULT_BIO;
        return serializeToJson(ToolResponse.success(DataSourceType.LINKEDIN, 1, headline));
    }

    /**
     * Returns in-depth architecture principles and engineering philosophy.
     */
    @Tool(description = """
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
        try {
            List<KnowledgeItem> items = this.linkedInSource.fetchKnowledge().stream()
                    .filter(item -> ProfileConstant.CATEGORY_EXPERIENCE.equalsIgnoreCase(item.category()))
                    .toList();
            if (!items.isEmpty()) {
                return serializeToJson(ToolResponse.success(DataSourceType.LINKEDIN, items.size(), items));
            }
        } catch (Exception e) {
            log.warn("[TOOL] [LINKEDIN-PHILOSOPHY-FALLBACK] Live fetch unavailable ({}), serving verified philosophy baseline.", e.getMessage());
        }

        List<KnowledgeItem> baselinePhilosophy = List.of(
                KnowledgeItem.builder()
                        .id("career-philosophy-baseline")
                        .source(DataSourceType.LINKEDIN)
                        .category(ProfileConstant.CATEGORY_EXPERIENCE)
                        .title("Core Engineering Philosophy & Architecture")
                        .content(ProfileConstant.PHILOSOPHY_CONTENT.trim())
                        .tags(List.of("clean-architecture", "solid", "tdd", "reactive-streams", "distributed-systems"))
                        .build()
        );
        return serializeToJson(ToolResponse.success(DataSourceType.LINKEDIN, baselinePhilosophy.size(), baselinePhilosophy));
    }

    /**
     * Returns verified certifications, diplomas, and academic credentials.
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
        try {
            List<KnowledgeItem> items = this.linkedInSource.fetchKnowledge().stream()
                    .filter(item -> ProfileConstant.CATEGORY_CERTIFICATION.equalsIgnoreCase(item.category()))
                    .toList();
            if (!items.isEmpty()) {
                List<CertificationRecord> certs = items.stream()
                        .map(k -> CertificationRecord.builder()
                                .name(k.title())
                                .issuingAuthority("LinkedIn Verified Body")
                                .credentialType("Certification / Credential")
                                .credentialId(k.id())
                                .verificationUrl(ProfileConstant.LINKEDIN_URL)
                                .details(k.content())
                                .build())
                        .toList();
                return serializeToJson(ToolResponse.success(DataSourceType.LINKEDIN, certs.size(), certs));
            }
        } catch (Exception e) {
            log.warn("[TOOL] [LINKEDIN-CERTIFICATIONS-FALLBACK] Live fetch unavailable ({}), serving verified certification baseline.", e.getMessage());
        }

        List<CertificationRecord> baselineCerts = List.of(
                CertificationRecord.builder()
                        .name("Post Graduate Diploma in Advanced Computing (PG-DAC)")
                        .issuingAuthority("Institute of Emerging Technologies (IET), CDAC ACTS")
                        .credentialType("Post Graduate Diploma / Accredited Credential")
                        .credentialId("CDAC-PGDAC-VERIFIED")
                        .verificationUrl(ProfileConstant.LINKEDIN_URL)
                        .details("Specialized post-graduate diploma covering Java, Spring Boot, Microservices, Systems Engineering, and Algorithms.")
                        .build()
        );
        return serializeToJson(ToolResponse.success(DataSourceType.LINKEDIN, baselineCerts.size(), baselineCerts));
    }

    /**
     * Returns LinkedIn engineering posts, articles, and thought leadership.
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
        try {
            List<KnowledgeItem> items = this.linkedInSource.fetchKnowledge().stream()
                    .filter(item -> ProfileConstant.CATEGORY_POSTS.equalsIgnoreCase(item.category()))
                    .toList();
            if (items.isEmpty()) {
                return serializeToJson(ToolResponse.notAvailable(DataSourceType.LINKEDIN, "LinkedIn posts and publications are currently not available."));
            }
            List<SocialPostRecord> posts = items.stream()
                    .map(k -> SocialPostRecord.builder()
                            .id(k.id())
                            .title(k.title())
                            .platform("LinkedIn")
                            .summary(k.content())
                            .url(ProfileConstant.LINKEDIN_URL)
                            .topics(k.tags())
                            .build())
                    .toList();
            return serializeToJson(ToolResponse.success(DataSourceType.LINKEDIN, posts.size(), posts));
        } catch (Exception e) {
            log.error("[TOOL] [LINKEDIN-POSTS-ERROR] Failed to retrieve LinkedIn posts: {}", e.getMessage(), e);
            return serializeToJson(ToolResponse.error(DataSourceType.LINKEDIN, "Something went wrong while retrieving posts from LinkedIn. Please try again later."));
        }
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
        try {
            List<KnowledgeItem> items = this.linkedInSource.fetchKnowledge();
            if (!items.isEmpty()) {
                KnowledgeItem profileItem = items.getFirst();
                Map<String, String> data = Map.of(
                        "title", profileItem.title(),
                        "summary", profileItem.content(),
                        "profileUrl", ProfileConstant.LINKEDIN_URL
                );
                return serializeToJson(ToolResponse.success(DataSourceType.LINKEDIN, 1, data));
            }
        } catch (Exception e) {
            log.warn("[TOOL] [LINKEDIN-PROFILE-FALLBACK] Live fetch unavailable ({}), serving verified profile baseline.", e.getMessage());
        }

        Map<String, String> data = Map.of(
                "title", ProfileConstant.NAME + " — " + ProfileConstant.DEFAULT_BIO,
                "summary", ProfileConstant.PHILOSOPHY_CONTENT.trim(),
                "profileUrl", ProfileConstant.LINKEDIN_URL
        );
        return serializeToJson(ToolResponse.success(DataSourceType.LINKEDIN, 1, data));
    }
}
