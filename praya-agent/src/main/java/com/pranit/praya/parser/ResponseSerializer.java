package com.pranit.praya.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pranit.praya.model.ToolResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResponseSerializer {

    private final ObjectMapper objectMapper;

    public <T> String serializeToJson(ToolResponse<T> response) {
        try {
            return objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(response);

        } catch (JsonProcessingException e) {
            log.error("[TOOL] [RESPONSE-SERIALIZE-ERROR] {}", e.getMessage(), e);
            return """
                    {"status":"ERROR","message":"JSON serialization failed"}
                    """;
        }
    }
}