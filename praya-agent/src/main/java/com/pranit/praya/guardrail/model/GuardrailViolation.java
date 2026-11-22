package com.pranit.praya.guardrail.model;

/**
 * Details of an individual guardrail rule violation.
 *
 * @param phase         Input or Output phase
 * @param ruleName      Identifier of the violated rule
 * @param matchedTarget Keyword or pattern that triggered the violation
 * @param message       Human-readable error or warning message
 */
public record GuardrailViolation(
        ValidationPhase phase,
        String ruleName,
        String matchedTarget,
        String message
) {
}
