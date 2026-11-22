package com.pranit.praya.guardrail.service;

import com.pranit.praya.guardrail.exception.GuardrailViolationException;
import com.pranit.praya.guardrail.model.GuardrailResult;
import com.pranit.praya.guardrail.model.GuardrailViolation;
import com.pranit.praya.guardrail.model.ValidationPhase;
import com.pranit.praya.guardrail.properties.GuardrailProperties;
import com.pranit.praya.guardrail.validator.GuardrailValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Composite orchestrator executing all registered {@link GuardrailValidator} strategies.
 */
@Slf4j
@Service
public class GuardrailServiceImpl implements GuardrailService {

    private final GuardrailProperties properties;
    private final List<GuardrailValidator> validators;

    public GuardrailServiceImpl(GuardrailProperties properties, List<GuardrailValidator> validators) {
        this.properties = properties;
        this.validators = new ArrayList<>(validators);
        AnnotationAwareOrderComparator.sort(this.validators);
    }

    @Override
    public GuardrailResult evaluate(String text, ValidationPhase phase) {
        if (!this.properties.enabled()) {
            return GuardrailResult.success(text);
        }

        final List<GuardrailViolation> aggregatedViolations = new ArrayList<>();

        for (GuardrailValidator validator : this.validators) {
            if (validator.supports(phase)) {
                GuardrailResult result = validator.validate(text, phase);
                if (!result.valid()) {
                    aggregatedViolations.addAll(result.violations());
                    if (this.properties.strictMode()) {
                        log.warn("[GUARDRAIL] [CHECK-FAILED] phase: {}, validator: {}, ruleType: {}",
                                phase,
                                validator.getClass().getSimpleName(),
                                result.violations().getFirst().ruleType());
                        return result;
                    }
                }
            }
        }

        if (!aggregatedViolations.isEmpty()) {
            String fallback = phase == ValidationPhase.INPUT
                    ? this.properties.inputViolationMessage()
                    : this.properties.outputViolationMessage();
            return GuardrailResult.failure(aggregatedViolations, fallback);
        }

        return GuardrailResult.success(text);
    }

    @Override
    public void validateInput(String prompt) {
        GuardrailResult result = evaluate(prompt, ValidationPhase.INPUT);
        if (!result.valid() && this.properties.strictMode()) {
            throw new GuardrailViolationException(
                    ValidationPhase.INPUT,
                    result.violations(),
                    result.getFirstViolationMessage()
            );
        }
    }

    @Override
    public void validateOutput(String response) {
        GuardrailResult result = evaluate(response, ValidationPhase.OUTPUT);
        if (!result.valid() && this.properties.strictMode()) {
            throw new GuardrailViolationException(
                    ValidationPhase.OUTPUT,
                    result.violations(),
                    result.getFirstViolationMessage()
            );
        }
    }
}
