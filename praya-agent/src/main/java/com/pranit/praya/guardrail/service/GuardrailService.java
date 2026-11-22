package com.pranit.praya.guardrail.service;

import com.pranit.praya.guardrail.model.GuardrailResult;
import com.pranit.praya.guardrail.model.ValidationPhase;

/**
 * Service contract for orchestrating guardrail evaluation across input and output lifecycle phases.
 */
public interface GuardrailService {

    /**
     * Evaluates text through all registered validators for the specified phase.
     */
    GuardrailResult evaluate(String text, ValidationPhase phase);

    /**
     * Convenience method to validate input; throws {@link com.pranit.praya.guardrail.exception.GuardrailViolationException} if strict and invalid.
     */
    void validateInput(String prompt);

    /**
     * Convenience method to validate output; throws {@link com.pranit.praya.guardrail.exception.GuardrailViolationException} if strict and invalid.
     */
    void validateOutput(String response);
}
