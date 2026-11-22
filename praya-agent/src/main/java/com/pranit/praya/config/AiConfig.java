package com.pranit.praya.config;

import com.pranit.praya.constant.AiConstants;
import com.pranit.praya.github.GitHubTools;
import com.pranit.praya.guardrail.advisor.StrictGuardrailAdvisor;
import com.pranit.praya.guardrail.properties.GuardrailProperties;
import com.pranit.praya.guardrail.service.GuardrailService;
import com.pranit.praya.linkedin.LinkedInTools;
import com.pranit.praya.portfolio.PortfolioTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Spring AI configuration registering strict input/output guardrails, conversation memory,
 * and the pre-configured {@link ChatClient} bean.
 */
@Configuration
@EnableConfigurationProperties({GuardrailProperties.class, CorsProperties.class})
public class AiConfig {

    public static final int DEFAULT_MAX_MEMORY_MESSAGES = 20;

    @Bean
    public StrictGuardrailAdvisor strictGuardrailAdvisor(
            GuardrailService guardrailService,
            GuardrailProperties properties
    ) {
        return new StrictGuardrailAdvisor(guardrailService, properties);
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

    @Bean
    public List<Advisor> defaultAdvisors(StrictGuardrailAdvisor strictGuardrailAdvisor, MessageChatMemoryAdvisor memoryAdvisor) {
        return List.of(strictGuardrailAdvisor, memoryAdvisor);
    }

    @Bean("chatClient")
    public ChatClient chatClient(
            ChatClient.Builder builder,
            List<Advisor> advisors,
            GitHubTools gitHubTools,
            LinkedInTools linkedInTools,
            PortfolioTools portfolioTools
    ) {
        return builder
                .defaultSystem(AiConstants.SYSTEM_PROMPT)
                .defaultTools(gitHubTools, linkedInTools, portfolioTools)
                .defaultAdvisors(advisors)
                .build();
    }
}
