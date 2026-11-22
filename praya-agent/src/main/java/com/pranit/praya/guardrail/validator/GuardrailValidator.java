package com.pranit.praya.guardrail.validator;

import com.pranit.praya.guardrail.model.GuardrailResult;
import com.pranit.praya.guardrail.model.ValidationPhase;
import org.springframework.core.Ordered;

/**
 * Strategy interface for guardrail validators.
 * Implementations focus on specific validation domains (e.g. keywords, regexes, semantic safety).
 */
public interface GuardrailValidator extends Ordered {

    /**
     * Determines whether this validator applies to the given phase.
     */
    boolean supports(ValidationPhase phase);

    /**
     * Evaluates the provided text and returns a {@link GuardrailResult}.
     */
    GuardrailResult validate(String text, ValidationPhase phase);

    @Override
    default int getOrder() {
        return 0;
    }
}
