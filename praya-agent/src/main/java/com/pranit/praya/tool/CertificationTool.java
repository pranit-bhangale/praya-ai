package com.pranit.praya.tool;

import com.pranit.praya.cache.config.CacheConfig;
import com.pranit.praya.constant.ProfileConstant;
import com.pranit.praya.linkedin.LinkedInSource;
import com.pranit.praya.model.CertificationRecord;
import com.pranit.praya.model.DataSourceType;
import com.pranit.praya.model.KnowledgeItem;
import com.pranit.praya.model.ToolResponse;
import com.pranit.praya.parser.ResponseSerializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI Tool retrieving verified professional certifications dynamically from the LinkedIn data source.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CertificationTool {

    private final LinkedInSource linkedInSource;
    private final ResponseSerializer responseSerializer;

    /**
     * Retrieves verified professional certifications with credential name, issuer, and details from the LinkedIn datasource.
     */
    @Tool(name = "certifications", description = """
            Retrieves Pranit Bhangale's verified certifications, diplomas, and credentials, including
            credential name, issuing authority/issuer, credential ID, and verification link from LinkedIn.
            Call when the user asks about certifications, diplomas, accredited courses, or verified certificates.
            """)
    @Cacheable(cacheNames = CacheConfig.CACHE_PORTFOLIO_KNOWLEDGE, key = "'certifications'")
    public String getCertifications() {
        try {
            List<KnowledgeItem> items = this.linkedInSource.fetchKnowledge()
                    .stream()
                    .filter(item -> ProfileConstant.CATEGORY_CERTIFICATION.equalsIgnoreCase(item.category()))
                    .toList();

            if (items.isEmpty()) {
                return this.responseSerializer.serializeToJson(
                        ToolResponse.notAvailable(DataSourceType.LINKEDIN, "Certifications are currently not available from the live profile.")
                );
            }

            List<CertificationRecord> certs = items.stream()
                    .map(k -> CertificationRecord.builder()
                            .name(k.title())
                            .issuingAuthority(k.content())
                            .credentialType("Accredited Credential")
                            .credentialId(k.id())
                            .verificationUrl(ProfileConstant.LINKEDIN_URL)
                            .details(k.content())
                            .build())
                    .toList();

            return this.responseSerializer.serializeToJson(
                    ToolResponse.success(DataSourceType.LINKEDIN, certs.size(), certs)
            );
        } catch (Exception e) {
            log.error("[CERTIFICATION-TOOL] [FETCH-ERROR] Failed to fetch certifications from LinkedIn datasource: {}", e.getMessage(), e);
            return this.responseSerializer.serializeToJson(
                    ToolResponse.error(DataSourceType.LINKEDIN, "Something went wrong while retrieving certifications from LinkedIn.")
            );
        }
    }
}
