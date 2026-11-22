package com.pranit.praya.guardrail.validator;

import com.pranit.praya.guardrail.model.GuardrailResult;
import com.pranit.praya.guardrail.model.GuardrailViolation;
import com.pranit.praya.guardrail.model.ValidationPhase;
import com.pranit.praya.guardrail.properties.GuardrailProperties;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Concrete validator strategy for pre-compiled regex evaluation (prompt injection, PII, secret leaks).
 */
@Component
public class RegexPatternGuardrailValidator implements GuardrailValidator {

    private final GuardrailProperties properties;
    private final List<Pattern> compiledInputPatterns;
    private final List<Pattern> compiledOutputPatterns;

    public RegexPatternGuardrailValidator(GuardrailProperties properties) {
        this.properties = properties;
        this.compiledInputPatterns = properties.inputPatterns() != null
                ? properties.inputPatterns().stream()
                .filter(StringUtils::hasText)
                .map(p -> Pattern.compile(p, Pattern.CASE_INSENSITIVE | Pattern.DOTALL))
                .toList()
                : List.of();
        this.compiledOutputPatterns = properties.outputPatterns() != null
                ? properties.outputPatterns().stream()
                .filter(StringUtils::hasText)
                .map(p -> Pattern.compile(p, Pattern.CASE_INSENSITIVE | Pattern.DOTALL))
                .toList()
                : List.of();
    }

    @Override
    public boolean supports(ValidationPhase phase) {
        return phase == ValidationPhase.INPUT
                ? !this.compiledInputPatterns.isEmpty()
                : !this.compiledOutputPatterns.isEmpty();
    }

    @Override
    public GuardrailResult validate(String text, ValidationPhase phase) {
        if (!StringUtils.hasText(text)) {
            return GuardrailResult.success(text);
        }

        final List<Pattern> targetPatterns = (phase == ValidationPhase.INPUT)
                ? this.compiledInputPatterns
                : this.compiledOutputPatterns;

        if (targetPatterns.isEmpty()) {
            return GuardrailResult.success(text);
        }

        final List<GuardrailViolation> violations = new ArrayList<>();
        final String fallbackMessage = (phase == ValidationPhase.INPUT)
                ? this.properties.inputViolationMessage()
                : this.properties.outputViolationMessage();

        for (Pattern pattern : targetPatterns) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                violations.add(new GuardrailViolation(
                        phase,
                        "REGEX_PATTERN_MATCH",
                        pattern.pattern(),
                        fallbackMessage
                ));

                // Fail-fast in strict mode
                if (this.properties.strictMode()) {
                    return GuardrailResult.failure(violations, fallbackMessage);
                }
            }
        }

        if (!violations.isEmpty()) {
            return GuardrailResult.failure(violations, fallbackMessage);
        }

        return GuardrailResult.success(text);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
