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
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Concrete validator strategy for case-insensitive blocked word detection.
 */
@Component
public class KeywordGuardrailValidator implements GuardrailValidator {

    private final GuardrailProperties properties;
    private final List<Pattern> blockedPatterns;

    public KeywordGuardrailValidator(GuardrailProperties properties) {
        this.properties = properties;
        this.blockedPatterns = properties.blockedWords() != null
                ? properties.blockedWords()
                .stream()
                .filter(StringUtils::hasText)
                .map(w -> Pattern.compile(
                        "(?<![\\p{L}\\p{N}_])" + Pattern.quote(w.trim()) + "(?![\\p{L}\\p{N}_])",
                        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE))
                .toList()
                : List.of();
    }

    @Override
    public boolean supports(ValidationPhase phase) {
        return phase == ValidationPhase.INPUT && !this.blockedPatterns.isEmpty();
    }

    @Override
    public GuardrailResult validate(String text, ValidationPhase phase) {
        if (!StringUtils.hasText(text) || this.blockedPatterns.isEmpty()) return GuardrailResult.success(text);
        final String lowerText = text.toLowerCase(Locale.ROOT);
        final List<GuardrailViolation> violations = new ArrayList<>();
        final String fallbackMessage = phase == ValidationPhase.INPUT
                ? this.properties.inputViolationMessage() : this.properties.outputViolationMessage();
        for (Pattern  blockedWord : this.blockedPatterns) {
            if (blockedWord.matcher(text).find()) {
                violations.add(new GuardrailViolation(
                        phase,
                        "KEYWORD_BLOCKED",
                        blockedWord.pattern(),
                        fallbackMessage
                ));
                if (this.properties.strictMode()) return GuardrailResult.failure(violations, fallbackMessage);
            }
        }
        if (!violations.isEmpty()) return GuardrailResult.failure(violations, fallbackMessage);
        return GuardrailResult.success(text);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
