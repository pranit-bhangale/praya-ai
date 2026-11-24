package com.pranit.praya.guardrail.validator;

import com.pranit.praya.guardrail.model.GuardrailResult;
import com.pranit.praya.guardrail.model.GuardrailViolation;
import com.pranit.praya.guardrail.model.ValidationPhase;
import com.pranit.praya.guardrail.properties.GuardrailProperties;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

/**
 * INPUT-only topic scope check for a portfolio assistant.
 * <ol>
 *   <li>greetings / thanks always pass</li>
 *   <li>clearly off-topic requests are rejected</li>
 *   <li>optionally, longer messages must mention an allowed topic (career, skills, projects, ...)</li>
 * </ol>
 * This is a cost/UX guard, NOT a security boundary: a keyword can be stuffed into an off-topic message.
 * The real boundary is that the model only ever receives public data (see system-prompt.txt).
 */
@Component
public class TopicScopeGuardrailValidator implements GuardrailValidator {

    private final GuardrailProperties properties;
    private final List<Pattern> smalltalk;
    private final List<Pattern> offTopic;
    private final List<Pattern> allowedTopics;

    public TopicScopeGuardrailValidator(GuardrailProperties properties) {
        this.properties = properties;
        GuardrailProperties.Scope scope = properties.scope();
        this.smalltalk = scope.smalltalkPatterns().stream()
                .filter(StringUtils::hasText).map(p ->
                        Pattern.compile(p, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)).toList();
        this.offTopic = scope.offTopicPatterns().stream()
                .filter(StringUtils::hasText).map(p ->
                        Pattern.compile(p, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)).toList();
        this.allowedTopics = scope.allowedTopics()
                .stream()
                .filter(StringUtils::hasText)
                .map(t -> Pattern.compile("(?<![\\p{L}\\p{N}_])" + Pattern.quote(t.trim()),
                        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE))
                .toList();
    }

    @Override
    public boolean supports(ValidationPhase phase) {
        return phase == ValidationPhase.INPUT && properties.scope().enabled();
    }

    @Override
    public GuardrailResult validate(String text, ValidationPhase phase) {
        if (!StringUtils.hasText(text)) return GuardrailResult.success(text);
        GuardrailProperties.Scope scope = properties.scope();
        for (Pattern p : smalltalk) {
            if (p.matcher(text).find()) return GuardrailResult.success(text);
        }
        for (Pattern p : offTopic) {
            if (p.matcher(text).find()) return fail(phase, "OFF_TOPIC", p.pattern(), scope.message());
        }
        if (scope.requireTopicKeyword() && wordCount(text) > scope.followUpMaxWords()) {
            boolean onTopic = allowedTopics.stream().anyMatch(p -> p.matcher(text).find());
            if (!onTopic) return fail(phase, "NO_ALLOWED_TOPIC", "allowed-topics", scope.message());
        }
        return GuardrailResult.success(text);
    }

    private static int wordCount(String text) {
        return text.trim().split("\\s+").length;
    }

    private static GuardrailResult fail(ValidationPhase phase, String rule, String target, String message) {
        return GuardrailResult.failure(List.of(new GuardrailViolation(phase, rule, target, message)), message);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
}