package com.pranit.praya.guardrail.model;

import java.util.Collections;
import java.util.List;

/**
 * Result representation of a guardrail validation cycle.
 *
 * @param valid         True if all checks passed, false otherwise
 * @param violations    List of violations detected (empty on success)
 * @param sanitizedText Sanitized or fallback content
 */
public record GuardrailResult(
        boolean valid,
        List<GuardrailViolation> violations,
        String sanitizedText
) {

    public static GuardrailResult success(String text) {
        return new GuardrailResult(true, Collections.emptyList(), text);
    }

    public static GuardrailResult failure(List<GuardrailViolation> violations, String fallbackText) {
        return new GuardrailResult(false, List.copyOf(violations), fallbackText);
    }

    public static GuardrailResult failure(GuardrailViolation violation, String fallbackText) {
        return new GuardrailResult(false, List.of(violation), fallbackText);
    }

    public String getFirstViolationMessage() {
        return violations.isEmpty() ? "" : violations.getFirst().message();
    }
}
