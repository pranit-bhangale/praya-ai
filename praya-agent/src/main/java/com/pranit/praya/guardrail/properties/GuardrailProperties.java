package com.pranit.praya.guardrail.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Strongly typed configuration for AI input and output guardrails.
 *
 * @param enabled                Whether guardrails are active
 * @param strictMode             If true, throws GuardrailViolationException; if false, returns fallback text
 * @param blockedWords           Prohibited keywords (checked in both input and output)
 * @param inputPatterns          Regex patterns for prompt injections, jailbreaks, and sensitive input
 * @param outputPatterns         Regex patterns for secrets, API tokens, JWTs, and internal leaks
 * @param inputViolationMessage  User-facing failure message for input rejections
 * @param outputViolationMessage User-facing failure message for output rejections
 */
@ConfigurationProperties(prefix = "praya.ai.guardrail")
public record GuardrailProperties(
        boolean enabled,
        boolean strictMode,
        List<String> blockedWords,
        List<String> inputPatterns,
        List<String> outputPatterns,
        String inputViolationMessage,
        String outputViolationMessage
) {

    public GuardrailProperties {
        if (blockedWords == null) {
            blockedWords = List.of();
        }
        if (inputPatterns == null) {
            inputPatterns = List.of();
        }
        if (outputPatterns == null) {
            outputPatterns = List.of();
        }
        if (inputViolationMessage == null || inputViolationMessage.isBlank()) {
            inputViolationMessage = "Input security validation failed: Request violates safety policies.";
        }
        if (outputViolationMessage == null || outputViolationMessage.isBlank()) {
            outputViolationMessage = "Output security validation failed: Response withheld due to safety policies.";
        }
    }
}
