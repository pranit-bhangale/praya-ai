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
        if (!this.properties.enabled()) return chain.nextCall(request);
        final String input = request.prompt().getContents();
        if (this.properties.strictMode()) {
            this.guardrailService.validateInput(input);
        } else {
            final GuardrailResult inputResult = this.guardrailService.evaluate(input, ValidationPhase.INPUT);
            if (!inputResult.valid()) {
                log.warn("[GUARDRAIL] [INPUT-BLOCKED] Call phase violation: {}", inputResult.getFirstViolationMessage());
                return createFallbackResponse(request, inputResult.getFirstViolationMessage());
            }
        }
        final ChatClientResponse response = chain.nextCall(request);
        final String outputText = extractResponseText(response);
        if (this.properties.strictMode()) {
            this.guardrailService.validateOutput(outputText);
        } else {
            final GuardrailResult outputResult = this.guardrailService.evaluate(outputText, ValidationPhase.OUTPUT);
            if (!outputResult.valid()) {
                log.warn("[GUARDRAIL] [OUTPUT-BLOCKED] Call generation violation: {}", outputResult.getFirstViolationMessage());
                return createFallbackResponse(request, outputResult.getFirstViolationMessage());
            }
        }
        return response;
    }

    private ChatClientResponse createFallbackResponse(ChatClientRequest request, String message) {
        return ChatClientResponse.builder()
                .chatResponse(ChatResponse.builder()
                        .generations(List.of(new Generation(new AssistantMessage(message))))
                        .build())
                .context(Map.copyOf(request.context()))
                .build();
    }

    private String extractResponseText(ChatClientResponse response) {
        if (response == null || response.chatResponse() == null) return "";
        ChatResponse chatResponse = response.chatResponse();
        StringBuilder sb = new StringBuilder();
        for (Generation generation : chatResponse.getResults()) {
            if (generation.getOutput().getText() != null) sb.append(generation.getOutput().getText());
        }
        return sb.toString();
    }

    @Override
    public @NonNull Flux<ChatClientResponse> adviseStream(@NonNull ChatClientRequest request, StreamAdvisorChain chain) {
        if (!this.properties.enabled()) return chain.nextStream(request);
        final String input = request.prompt().getContents();
        if (this.properties.strictMode()) {
            try {
                this.guardrailService.validateInput(input);
            } catch (GuardrailViolationException ex) {
                log.warn("[GUARDRAIL] [INPUT-BLOCKED] Pre-stream violation: {}", ex.getMessage());
                return Flux.error(ex);
            }
        } else {
            final GuardrailResult inputResult = this.guardrailService.evaluate(input, ValidationPhase.INPUT);
            if (!inputResult.valid()) {
                log.warn("[GUARDRAIL] [INPUT-BLOCKED] Pre-stream violation: {}", inputResult.getFirstViolationMessage());
                return Flux.just(createFallbackResponse(request, inputResult.getFirstViolationMessage()));
            }
        }
        final StringBuilder buffer = new StringBuilder();
        return chain.nextStream(request).flatMap(chunk -> {
            String chunkText = extractResponseText(chunk);
            if (!chunkText.isEmpty()) {
                buffer.append(chunkText);
                if (this.properties.strictMode()) {
                    try {
                        this.guardrailService.validateOutput(buffer.toString());
                    } catch (GuardrailViolationException ex) {
                        log.warn("[GUARDRAIL] [OUTPUT-BLOCKED] Streaming buffer violation: {}", ex.getMessage());
                        return Flux.error(ex);
                    }
                } else {
                    GuardrailResult outputResult = this.guardrailService.evaluate(buffer.toString(), ValidationPhase.OUTPUT);
                    if (!outputResult.valid()) {
                        log.warn("[GUARDRAIL] [OUTPUT-BLOCKED] Streaming buffer violation: {}", outputResult.getFirstViolationMessage());
                        return Flux.just(createFallbackResponse(request, outputResult.getFirstViolationMessage()));
                    }
                }
            }
            return Flux.just(chunk);
        });
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
