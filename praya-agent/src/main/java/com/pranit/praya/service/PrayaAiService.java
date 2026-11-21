package com.pranit.praya.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pranit.praya.cache.service.AgentCacheService;
import com.pranit.praya.chat.stream.ChatEventStream;
import com.pranit.praya.tool.PrayaPortfolioTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * Main AI Agent service for Praya (Pranit's Personal Assistant).
 * Integrates Spring AI streaming, tool calling advisors, and annotation-based caching:
 * - Checks @Cacheable on request
 * - On cache miss, streams via ChatClient and triggers @CachePut upon stream completion,
 * actively utilizing the returned cached value.
 */
@Slf4j
@Service
public class PrayaAiService implements AiService {

    private final ChatClient chatClient;
    private final PrayaPortfolioTools portfolioTools;
    private final ObjectMapper objectMapper;
    private final AgentCacheService agentCacheService;

    public PrayaAiService(
            ChatClient.Builder chatClientBuilder,
            PrayaPortfolioTools portfolioTools,
            ObjectMapper objectMapper,
            AgentCacheService agentCacheService
    ) {
        this.portfolioTools = portfolioTools;
        this.objectMapper = objectMapper;
        this.agentCacheService = agentCacheService;
        this.chatClient = chatClientBuilder.defaultSystem("""
                        You are Praya, the dedicated and intelligent personal AI assistant to Pranit Bhangale.
                        Pranit's verified portfolio: https://pranitbhangale.vercel.app
                        GitHub: https://github.com/pranit-bhangale
                        LinkedIn: https://www.linkedin.com/in/pranit-bhangale-1745542a7
                        
                        Use the provided tools to lookup Pranit's background, education, tech stack, projects, and contact info.
                        Keep answers concise, professional, articulate, and accurate. Highlight his strong background in Java,
                        Spring Boot, microservices, and system engineering. Never fabricate facts.
                        """)
                .build();
    }

    @Override
    public Flux<ServerSentEvent<String>> askAiAgent(final String prompt, final String conversationId) {
        final ChatEventStream events = new ChatEventStream(this.objectMapper);
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
                .tools(this.portfolioTools)
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