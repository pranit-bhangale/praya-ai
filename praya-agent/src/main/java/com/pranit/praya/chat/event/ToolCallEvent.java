package com.pranit.praya.chat.event;

import lombok.Builder;

/**
 * A tool lifecycle event observed inside the tool-calling loop.
 * Serialized to the client as the SSE "tool" event.
 */
@Builder
public record ToolCallEvent(String id, String name, Phase phase, String responseData) {

    public enum Phase {
        STARTED, COMPLETED
    }
}
