package com.bonitasoft.ai.ragagent.controller;

import com.bonitasoft.ai.ragagent.model.AgentRequest;
import com.bonitasoft.ai.ragagent.model.AgentResponse;
import com.bonitasoft.ai.ragagent.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

/**
 * RAG Agent REST Controller
 */
@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
@Validated
public class AgentController {

    private final RagService ragService;

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "ok",
            "service", "rag-agent"
        ));
    }

    /**
     * Main RAG endpoint
     */
    @PostMapping("/run")
    public ResponseEntity<AgentResponse> runAgent(@Valid @RequestBody AgentRequest request) {
        try {
            log.info("Received request for task: {}", request.getTask());

            // Extract question from input data
            String question = (String) request.getInputData().get("question");
            if (question == null || question.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    AgentResponse.builder()
                        .status("error")
                        .error("Question is required in input_data")
                        .build()
                );
            }

            // Process query
            AgentResponse response = ragService.processQuery(question);
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                AgentResponse.builder()
                    .status("error")
                    .error("Internal server error: " + e.getMessage())
                    .build()
            );
        }
    }

    /**
     * Exception handler
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<AgentResponse> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            AgentResponse.builder()
                .status("error")
                .error(e.getMessage())
                .build()
        );
    }
}
