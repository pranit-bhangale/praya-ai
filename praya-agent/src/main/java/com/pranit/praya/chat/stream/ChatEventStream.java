package com.pranit.praya.chat.stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pranit.praya.chat.advisor.ToolCallObservingAdvisor;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Per-request bundle of the SSE plumbing for one chat turn: the side channel
 * (sink) tool events are pushed onto, the advisor that feeds it, and the
 * assembly of the final merged stream.
 * <p>
 * Event vocabulary: token | tool | done | error. Token and tool payloads are
 * JSON-encoded.
 */
public class ChatEventStream {

    private final ObjectMapper json;
    private final Sinks.Many<ServerSentEvent<String>> toolEvents =
            Sinks.many().unicast().onBackpressureBuffer();
    private final ToolCallObservingAdvisor observer;

    public ChatEventStream(ObjectMapper json) {
        this.json = json;
        this.observer = new ToolCallObservingAdvisor(event -> {
            try {
                this.toolEvents.tryEmitNext(sse("tool", this.json.writeValueAsString(event)));
            } catch (JsonProcessingException e) {
                this.toolEvents.tryEmitNext(sse("error", "Failed to serialize tool event: " + e.getMessage()));
            }
        });
    }

    public static ServerSentEvent<String> sse(String event, String data) {
        return ServerSentEvent.<String>builder().event(event).data(data).build();
    }

    public ToolCallObservingAdvisor observer() {
        return this.observer;
    }

    public ServerSentEvent<String> token(String token) {
        try {
            return sse("token", this.json.writeValueAsString(token));
        } catch (JsonProcessingException e) {
            return sse("token", "\"" + token.replace("\"", "\\\"") + "\"");
        }
    }

    public Flux<ServerSentEvent<String>> mergeWith(Flux<ServerSentEvent<String>> tokens) {
        return Flux.merge(tokens.doFinally(_ ->
                        this.toolEvents.tryEmitComplete()), this.toolEvents.asFlux())
                .onErrorResume(e -> Flux.just(sse("error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())))
                .concatWith(Flux.just(sse("done", "")));
    }
}
