package com.pranit.praya.model;

import lombok.Builder;

/**
 * Immutable model representing a verified academic credential, professional diploma, or certification.
 *
 * @param name               Name of the certification or credential
 * @param issuingAuthority   Organization or university that issued the credential
 * @param credentialType     Classification (e.g. "Post Graduate Diploma", "Bachelor's Degree")
 * @param credentialId       Unique credential identifier or registration number if applicable
 * @param verificationUrl    URL where the credential or issuing body can be verified
 * @param details            Key subject domains, syllabus focus, and competencies certified
 */
@Builder
public record CertificationRecord(
        String name,
        String issuingAuthority,
        String credentialType,
        String credentialId,
        String verificationUrl,
        String details
) {}
