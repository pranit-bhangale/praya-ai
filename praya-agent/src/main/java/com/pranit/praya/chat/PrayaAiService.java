package com.pranit.praya.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pranit.praya.cache.service.AgentCacheService;
import com.pranit.praya.constant.StreamEventConstants;
import com.pranit.praya.github.GitHubTools;
import com.pranit.praya.guardrail.exception.GuardrailViolationException;
import com.pranit.praya.guardrail.service.GuardrailService;
import com.pranit.praya.linkedin.LinkedInTools;
import com.pranit.praya.portfolio.PortfolioTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * Main AI Agent service for Praya (Pranit's Personal Assistant).
 * Integrates strict input/output guardrails, caching, and multi-tool orchestration.
 */
@Slf4j
@Service
public class PrayaAiService implements AiService {

    private final ChatClient chatClient;
    private final GitHubTools gitHubTools;
    private final LinkedInTools linkedInTools;
    private final PortfolioTools portfolioTools;
    private final ObjectMapper objectMapper;
    private final AgentCacheService agentCacheService;
    private final GuardrailService guardrailService;

    public PrayaAiService(
            ChatClient chatClient,
            GitHubTools gitHubTools,
            LinkedInTools linkedInTools,
            PortfolioTools portfolioTools,
            ObjectMapper objectMapper,
            AgentCacheService agentCacheService,
            GuardrailService guardrailService
    ) {
        this.chatClient = chatClient;
        this.gitHubTools = gitHubTools;
        this.linkedInTools = linkedInTools;
        this.portfolioTools = portfolioTools;
        this.objectMapper = objectMapper;
        this.agentCacheService = agentCacheService;
        this.guardrailService = guardrailService;
    }

    @Override
    public Flux<ServerSentEvent<String>> askAiAgent(final String prompt, final String conversationId) {
        final ChatEventStream events = new ChatEventStream(this.objectMapper);

        // Pre-cache strict input validation to avoid cache poisoning or leaking
        try {
            this.guardrailService.validateInput(prompt);
        } catch (GuardrailViolationException ex) {
            log.warn("Blocked prompt prior to processing: {}", ex.getMessage());
            return Flux.just(
                    ChatEventStream.sse(StreamEventConstants.EVENT_ERROR, ex.getMessage()),
                    ChatEventStream.sse(StreamEventConstants.EVENT_DONE, "")
            );
        }

        final String cachedAnswer = this.agentCacheService.getCachedAnswer(prompt);
        if (cachedAnswer != null && !cachedAnswer.isBlank()) {
            Flux<ServerSentEvent<String>> cachedStream =
                    Flux.fromArray(cachedAnswer.split("(?<=\\s)|(?<=\\n)")).map(events::token);
            return events.mergeWith(cachedStream);
        }

        final StringBuilder answerAccumulator = new StringBuilder();
        final Flux<ServerSentEvent<String>> answerTokens = this.chatClient
                .prompt()
                .advisors(events.observer())
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .tools(this.gitHubTools, this.linkedInTools, this.portfolioTools)
                .user(prompt)
                .stream()
                .content()
                .doOnNext(answerAccumulator::append)
                .doOnComplete(() -> {
                    String fullResponse = answerAccumulator.toString();
                    if (!fullResponse.isBlank()) {
                        String cachedResponse = this.agentCacheService.putAnswer(prompt, fullResponse);
                        log.info("Cached AI response ({} characters) via @CachePut for prompt: {}",
                                cachedResponse.length(), prompt);
                    }
                })
                .map(events::token);

        return events.mergeWith(answerTokens);
    }
}
