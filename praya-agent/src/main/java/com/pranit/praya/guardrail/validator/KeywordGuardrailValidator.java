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

/**
 * Concrete validator strategy for case-insensitive blocked word detection.
 */
@Component
public class KeywordGuardrailValidator implements GuardrailValidator {

    private final GuardrailProperties properties;
    private final List<String> lowerBlockedWords;

    public KeywordGuardrailValidator(GuardrailProperties properties) {
        this.properties = properties;
        this.lowerBlockedWords = properties.blockedWords() != null
                ? properties.blockedWords()
                .stream()
                .filter(StringUtils::hasText)
                .map(w -> w.toLowerCase(Locale.ROOT))
                .toList()
                : List.of();
    }

    @Override
    public boolean supports(ValidationPhase phase) {
        return !this.lowerBlockedWords.isEmpty();
    }

    @Override
    public GuardrailResult validate(String text, ValidationPhase phase) {
        if (!StringUtils.hasText(text) || this.lowerBlockedWords.isEmpty()) return GuardrailResult.success(text);
        final String lowerText = text.toLowerCase(Locale.ROOT);
        final List<GuardrailViolation> violations = new ArrayList<>();
        final String fallbackMessage = phase == ValidationPhase.INPUT
                ? this.properties.inputViolationMessage() : this.properties.outputViolationMessage();
        for (String blockedWord : this.lowerBlockedWords) {
            if (lowerText.contains(blockedWord)) {
                violations.add(new GuardrailViolation(
                        phase,
                        "KEYWORD_BLOCKED",
                        blockedWord,
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
