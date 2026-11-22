package com.pranit.praya.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

/**
 * Incoming chat request payload from clients.
 */
@Builder
public record ChatRequest(
        @NotBlank(message = "Prompt cannot be blank")
        String prompt,
        String conversationId
) {}
