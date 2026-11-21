package com.pranit.praya.service;

import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

public interface AiService {

    Flux<ServerSentEvent<String>> askAiAgent(String prompt, String conversationId);
}
