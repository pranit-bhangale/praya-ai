package com.pranit.praya.guardrail.advisor;

import com.pranit.praya.guardrail.exception.GuardrailViolationException;
import com.pranit.praya.guardrail.model.GuardrailResult;
import com.pranit.praya.guardrail.model.ValidationPhase;
import com.pranit.praya.guardrail.properties.GuardrailProperties;
import com.pranit.praya.guardrail.service.GuardrailService;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.core.Ordered;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * Spring AI Advisor adapter executing strict input and output guardrails.
 * Runs with {@link Ordered#HIGHEST_PRECEDENCE} so blocked requests never invoke the model
 * or contaminate chat memory.
 */
@Slf4j
public class StrictGuardrailAdvisor implements CallAdvisor, StreamAdvisor, Ordered {

    private final GuardrailService guardrailService;
    private final GuardrailProperties properties;

    public StrictGuardrailAdvisor(GuardrailService guardrailService, GuardrailProperties properties) {
        this.guardrailService = guardrailService;
        this.properties = properties;
    }

    @Override
    public @NonNull ChatClientResponse adviseCall(@NonNull ChatClientRequest request, CallAdvisorChain chain) {
        if (!this.properties.enabled()) {
            return chain.nextCall(request);
        }

        // 1. Strict Input Validation (Before Model Invocation)
        final String input = request.prompt().getContents();
        final GuardrailResult inputResult = this.guardrailService.evaluate(input, ValidationPhase.INPUT);
        if (!inputResult.valid()) {
            log.warn("Blocking input prompt due to guardrail violation: {}", inputResult.getFirstViolationMessage());
            if (this.properties.strictMode()) {
                throw new GuardrailViolationException(
                        ValidationPhase.INPUT,
                        inputResult.violations(),
                        inputResult.getFirstViolationMessage()
                );
            }
            return createFallbackResponse(request, inputResult.getFirstViolationMessage());
        }

        // 2. Invoke Next In Chain (LLM Call)
        final ChatClientResponse response = chain.nextCall(request);

        // 3. Strict Output Validation (After Model Generation)
        final String outputText = extractResponseText(response);
        final GuardrailResult outputResult = this.guardrailService.evaluate(outputText, ValidationPhase.OUTPUT);
        if (!outputResult.valid()) {
            log.warn("Blocking output response due to guardrail violation: {}", outputResult.getFirstViolationMessage());
            if (this.properties.strictMode()) {
                throw new GuardrailViolationException(
                        ValidationPhase.OUTPUT,
                        outputResult.violations(),
                        outputResult.getFirstViolationMessage()
                );
            }
            return createFallbackResponse(request, outputResult.getFirstViolationMessage());
        }

        return response;
    }

    @Override
    public @NonNull Flux<ChatClientResponse> adviseStream(@NonNull ChatClientRequest request, StreamAdvisorChain chain) {
        if (!this.properties.enabled()) {
            return chain.nextStream(request);
        }

        // 1. Strict Input Validation (Pre-stream)
        final String input = request.prompt().getContents();
        final GuardrailResult inputResult = this.guardrailService.evaluate(input, ValidationPhase.INPUT);
        if (!inputResult.valid()) {
            log.warn("Streaming blocked at input phase: {}", inputResult.getFirstViolationMessage());
            if (this.properties.strictMode()) {
                return Flux.error(new GuardrailViolationException(
                        ValidationPhase.INPUT,
                        inputResult.violations(),
                        inputResult.getFirstViolationMessage()
                ));
            }
            return Flux.just(createFallbackResponse(request, inputResult.getFirstViolationMessage()));
        }

        // 2. Stream chunks with output inspection
        final StringBuilder buffer = new StringBuilder();
        return chain.nextStream(request).flatMap(chunk -> {
            String chunkText = extractResponseText(chunk);
            if (!chunkText.isEmpty()) {
                buffer.append(chunkText);
                GuardrailResult outputResult = this.guardrailService.evaluate(buffer.toString(), ValidationPhase.OUTPUT);
                if (!outputResult.valid()) {
                    log.warn("Streaming output blocked due to guardrail violation: {}", outputResult.getFirstViolationMessage());
                    if (this.properties.strictMode()) {
                        return Flux.error(new GuardrailViolationException(
                                ValidationPhase.OUTPUT,
                                outputResult.violations(),
                                outputResult.getFirstViolationMessage()
                        ));
                    }
                    return Flux.just(createFallbackResponse(request, outputResult.getFirstViolationMessage()));
                }
            }
            return Flux.just(chunk);
        });
    }

    private String extractResponseText(ChatClientResponse response) {
        if (response == null || response.chatResponse() == null) {
            return "";
        }
        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse.getResults() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Generation generation : chatResponse.getResults()) {
            if (generation.getOutput() != null && generation.getOutput().getText() != null) {
                sb.append(generation.getOutput().getText());
            }
        }
        return sb.toString();
    }

    private ChatClientResponse createFallbackResponse(ChatClientRequest request, String message) {
        return ChatClientResponse.builder()
                .chatResponse(ChatResponse.builder()
                        .generations(List.of(new Generation(new AssistantMessage(message))))
                        .build())
                .context(Map.copyOf(request.context()))
                .build();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public @NonNull String getName() {
        return getClass().getSimpleName();
    }
}
