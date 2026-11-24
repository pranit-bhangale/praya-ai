package com.pranit.praya.tool;

import com.pranit.praya.cache.config.CacheConfig;
import com.pranit.praya.constant.ProfileConstant;
import com.pranit.praya.model.DataSourceType;
import com.pranit.praya.model.QualificationRecord;
import com.pranit.praya.model.ToolResponse;
import com.pranit.praya.parser.ResponseSerializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AI Tool providing static, verified personal identity, contact details, and academic qualifications
 * for Pranit Bhangale. These core profile attributes are maintained as static server records.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileTool {

    private final ResponseSerializer responseSerializer;

    private static final Map<String, Object> STATIC_ABOUT_ME = Map.of(
            "name", ProfileConstant.NAME,
            "title", ProfileConstant.NAME + " — Systems & Backend Engineer",
            "bio", ProfileConstant.DEFAULT_BIO,
            "philosophy", ProfileConstant.PHILOSOPHY_CONTENT.trim(),
            "portfolioUrl", ProfileConstant.PORTFOLIO_URL,
            "githubUrl", ProfileConstant.GITHUB_URL,
            "linkedinUrl", ProfileConstant.LINKEDIN_URL
    );

    private static final Map<String, String> STATIC_CONTACT_INFO = Map.of(
            "name", ProfileConstant.NAME,
            "email", ProfileConstant.EMAIL,
            "linkedinUrl", ProfileConstant.LINKEDIN_URL,
            "portfolioUrl", ProfileConstant.PORTFOLIO_URL,
            "githubUrl", ProfileConstant.GITHUB_URL,
            "preferredChannel", "Email (" + ProfileConstant.EMAIL + ") or LinkedIn for direct engineering and collaboration opportunities."
    );

    private static final List<QualificationRecord> STATIC_QUALIFICATIONS = List.of(
            QualificationRecord.builder()
                    .degree("Post Graduate Diploma in Advanced Computing (PG-DAC)")
                    .institution("Institute of Emerging Technologies (IET), CDAC ACTS")
                    .period("2025 - 2026")
                    .details("Specialization in Java, Spring Boot, Microservices, Systems Engineering, Algorithms, and Distributed Computing.")
                    .build(),
            QualificationRecord.builder()
                    .degree("Bachelor of Engineering (BE) in Electronics & Telecommunication")
                    .institution("Savitribai Phule Pune University (SPPU)")
                    .period("2021 - 2025")
                    .details("Specialization in Computer Networks, Embedded Systems, Microprocessors, and Software Engineering.")
                    .build(),
            QualificationRecord.builder()
                    .degree("Higher Secondary Certificate (12th - Science)")
                    .institution("Maharashtra State Board")
                    .period("2019 - 2021")
                    .details("Major in Physics, Chemistry, and Mathematics.")
                    .build(),
            QualificationRecord.builder()
                    .degree("Secondary School Certificate (10th)")
                    .institution("Maharashtra State Board")
                    .period("2018 - 2019")
                    .details("Foundational secondary education.")
                    .build()
    );

    /**
     * Returns static verified bio, identity, and engineering focus.
     */
    @Tool(name = "about-me", description = """
            Retrieves Pranit Bhangale's verified professional bio, engineering background, and core focus
            as a systems and backend engineer. Call when the user asks who Pranit is, for his bio, introductory
            overview, or architectural background.
            """)
    @Cacheable(cacheNames = CacheConfig.CACHE_PORTFOLIO_KNOWLEDGE, key = "'profile-about'")
    public String getAboutMe() {
        return this.responseSerializer.serializeToJson(
                ToolResponse.success(DataSourceType.PORTFOLIO_WEBSITE, 1, STATIC_ABOUT_ME)
        );
    }

    /**
     * Returns static verified direct reach-out channels and official handles.
     */
    @Tool(name = "contact-details", description = """
            Retrieves Pranit Bhangale's verified direct contact information, including official email,
            LinkedIn profile, portfolio URL, GitHub profile, and preferred communication channels.
            Call when someone asks how to reach, contact, email, or hire Pranit.
            """)
    @Cacheable(cacheNames = CacheConfig.CACHE_PORTFOLIO_KNOWLEDGE, key = "'profile-contact'")
    public String getContactDetails() {
        return this.responseSerializer.serializeToJson(
                ToolResponse.success(DataSourceType.PORTFOLIO_WEBSITE, 1, STATIC_CONTACT_INFO)
        );
    }

    /**
     * Returns static verified degrees, institutions, and academic history.
     */
    @Tool(name = "qualifications", description = """
            Retrieves Pranit Bhangale's verified academic qualifications, degrees, educational institutions,
            and core coursework (including PG-DAC and Bachelor of Engineering). Call when the user asks about
            education, college, university, degrees, or schooling history.
            """)
    @Cacheable(cacheNames = CacheConfig.CACHE_PORTFOLIO_KNOWLEDGE, key = "'profile-qualifications'")
    public String getQualifications() {
        return this.responseSerializer.serializeToJson(
                ToolResponse.success(DataSourceType.LINKEDIN, STATIC_QUALIFICATIONS.size(), STATIC_QUALIFICATIONS)
        );
    }
}
