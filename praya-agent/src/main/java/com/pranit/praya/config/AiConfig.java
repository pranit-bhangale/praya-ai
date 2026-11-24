package com.pranit.praya.config;

import com.pranit.praya.constant.AiConstant;
import com.pranit.praya.github.GitHubTools;
import com.pranit.praya.guardrail.advisor.StrictGuardrailAdvisor;
import com.pranit.praya.guardrail.advisor.StrictGuardrailAdvisor.Stage;
import com.pranit.praya.guardrail.properties.GuardrailProperties;
import com.pranit.praya.guardrail.service.GuardrailService;
import com.pranit.praya.linkedin.LinkedInTools;
import com.pranit.praya.portfolio.PortfolioTools;
import com.pranit.praya.properties.CorsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Spring AI configuration registering strict input/output guardrails, conversation memory,
 * and the pre-configured {@link ChatClient} bean.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({GuardrailProperties.class, CorsProperties.class})
public class AiConfig {

    public static final int DEFAULT_MAX_MEMORY_MESSAGES = 20;

    /**
     * Outermost: runs before chat memory, RAG and tools.
     */
    @Bean
    public StrictGuardrailAdvisor inputGuardrailAdvisor(GuardrailService service, GuardrailProperties properties) {
        return new StrictGuardrailAdvisor(service, properties, Stage.INPUT, Ordered.HIGHEST_PRECEDENCE);
    }

    /**
     * Innermost: runs right after the model, BEFORE the memory advisor persists the answer.
     * Its order number must be larger than every other advisor's (memory, RAG, tool-call).
     */
    @Bean
    public StrictGuardrailAdvisor outputGuardrailAdvisor(GuardrailService service, GuardrailProperties properties) {
        return new StrictGuardrailAdvisor(service, properties, Stage.OUTPUT, Ordered.LOWEST_PRECEDENCE - 100);
    }

    /**
     * Makes silent misconfiguration (wrong prefix, missing yml) visible at startup.
     */
    @Bean
    public InitializingBean guardrailSelfCheck(GuardrailProperties p) {
        return () -> {
            if (!p.enabled()) {
                log.warn("[GUARDRAIL] [DISABLED] Guardrails are OFF (praya.ai.guardrail.enabled=false). Do not run like this in production.");
                return;
            }
            log.info("[GUARDRAIL] [READY] strict={}, maxInputLength={}, keywords={}, inputPatterns={}, outputPatterns={}, scope={}",
                    p.strictMode(), p.maxInputLength(), p.blockedWords().size(),
                    p.inputPatterns().size(), p.outputPatterns().size(), p.scope().enabled());
            if (p.blockedWords().isEmpty() && p.inputPatterns().isEmpty() && p.outputPatterns().isEmpty()) {
                log.warn("[GUARDRAIL] [NO-RULES] Enabled but no keywords or patterns are configured. Check the YAML prefix and indentation.");
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(DEFAULT_MAX_MEMORY_MESSAGES)
                .build();
    }

    @Bean
    public MessageChatMemoryAdvisor memoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }

    @Bean("chatClient")
    public ChatClient chatClient(
            ChatClient.Builder builder,
            StrictGuardrailAdvisor inputGuardrailAdvisor,
            StrictGuardrailAdvisor outputGuardrailAdvisor,
            MessageChatMemoryAdvisor memoryAdvisor,
            GitHubTools gitHubTools,
            LinkedInTools linkedInTools,
            PortfolioTools portfolioTools
    ) {
        return builder
                .defaultSystem(AiConstant.SYSTEM_PROMPT)
                .defaultTools(gitHubTools, linkedInTools, portfolioTools)
                .defaultAdvisors(inputGuardrailAdvisor, memoryAdvisor, outputGuardrailAdvisor)
                .build();
    }
}