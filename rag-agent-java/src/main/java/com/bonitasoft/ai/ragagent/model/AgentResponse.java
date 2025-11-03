package com.bonitasoft.ai.ragagent.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * RAG Agent Response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentResponse {

    private String status;

    private Map<String, Object> output;

    private Map<String, Object> usage;

    private String error;

    @JsonProperty("conflict_info")
    private ConflictInfo conflictInfo;

    /**
     * Conflict information when multiple sources provide different answers
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConflictInfo {
        private boolean detected;
        private List<String> conflictingSources;
        private String resolutionStrategy;
        private String reasoning;
    }
}
