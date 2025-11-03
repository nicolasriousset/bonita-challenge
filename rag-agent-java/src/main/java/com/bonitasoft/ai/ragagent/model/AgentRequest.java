package com.bonitasoft.ai.ragagent.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * RAG Agent Request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRequest {

    @NotBlank(message = "Task is required")
    private String task;

    @JsonProperty("input_data")
    @NotNull(message = "Input data cannot be null")
    @NotEmpty(message = "Input data cannot be empty")
    private Map<String, Object> inputData;

    private Map<String, Object> params;
}
