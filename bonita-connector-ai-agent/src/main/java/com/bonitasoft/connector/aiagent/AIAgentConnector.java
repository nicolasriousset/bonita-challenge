package com.bonitasoft.connector.aiagent;

import org.bonitasoft.engine.connector.AbstractConnector;
import org.bonitasoft.engine.connector.ConnectorException;
import org.bonitasoft.engine.connector.ConnectorValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI Agent Connector for Bonita
 * 
 * Allows Bonita processes to communicate with external AI agents via HTTP.
 * Supports various agent tasks including RAG-based Q&A, summarization, and more.
 */
public class AIAgentConnector extends AbstractConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIAgentConnector.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // Store outputs for testing access
    private final Map<String, Object> outputs = new HashMap<>();

    // Input parameters
    protected static final String AGENT_URL = "agentUrl";
    protected static final String AUTH_HEADER = "authHeader";
    protected static final String TASK = "task";
    protected static final String INPUT_DATA = "inputData";
    protected static final String PARAMS = "params";
    protected static final String TIMEOUT_MS = "timeoutMs";

    // Output parameters
    protected static final String STATUS = "status";
    protected static final String OUTPUT = "output";
    protected static final String USAGE = "usage";
    protected static final String ERROR = "error";

    // Default values
    private static final int DEFAULT_TIMEOUT_MS = 30000;
    private static final String DEFAULT_TASK = "rag_qa";

    @Override
    public void validateInputParameters() throws ConnectorValidationException {
        List<String> errors = new java.util.ArrayList<>();

        // Validate required parameters
        String agentUrl = (String) getInputParameter(AGENT_URL);
        if (agentUrl == null || agentUrl.trim().isEmpty()) {
            errors.add("Agent URL is required");
        } else if (!agentUrl.startsWith("http://") && !agentUrl.startsWith("https://")) {
            errors.add("Agent URL must start with http:// or https://");
        }

        String inputData = (String) getInputParameter(INPUT_DATA);
        if (inputData == null || inputData.trim().isEmpty()) {
            errors.add("Input data is required");
        } else {
            // Validate JSON format
            try {
                OBJECT_MAPPER.readTree(inputData);
            } catch (IOException e) {
                errors.add("Input data must be valid JSON: " + e.getMessage());
            }
        }

        // Validate optional parameters
        String params = (String) getInputParameter(PARAMS);
        if (params != null && !params.trim().isEmpty()) {
            try {
                OBJECT_MAPPER.readTree(params);
            } catch (IOException e) {
                errors.add("Params must be valid JSON: " + e.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            throw new ConnectorValidationException(this, errors);
        }
    }

    @Override
    protected void executeBusinessLogic() throws ConnectorException {
        String agentUrl = (String) getInputParameter(AGENT_URL);
        String authHeader = (String) getInputParameter(AUTH_HEADER);
        String task = getInputParameter(TASK) != null ? (String) getInputParameter(TASK) : DEFAULT_TASK;
        String inputData = (String) getInputParameter(INPUT_DATA);
        String params = (String) getInputParameter(PARAMS);
        Integer timeoutMs = getInputParameter(TIMEOUT_MS) != null ? 
            (Integer) getInputParameter(TIMEOUT_MS) : DEFAULT_TIMEOUT_MS;

        LOGGER.info("Calling AI Agent at: {} with task: {}", agentUrl, task);

        try {
            // Build request payload
            Map<String, Object> requestPayload = new HashMap<>();
            requestPayload.put("task", task);
            requestPayload.put("input", OBJECT_MAPPER.readTree(inputData));
            
            if (params != null && !params.trim().isEmpty()) {
                requestPayload.put("params", OBJECT_MAPPER.readTree(params));
            }

            String jsonPayload = OBJECT_MAPPER.writeValueAsString(requestPayload);
            LOGGER.debug("Request payload: {}", jsonPayload);

            // Execute HTTP request
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpPost httpPost = new HttpPost(agentUrl);
                httpPost.setHeader("Content-Type", "application/json");
                
                if (authHeader != null && !authHeader.trim().isEmpty()) {
                    httpPost.setHeader("Authorization", authHeader);
                }

                httpPost.setEntity(new StringEntity(jsonPayload, StandardCharsets.UTF_8));

                try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                    int statusCode = response.getCode();
                    String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    
                    LOGGER.debug("Response status: {}, body: {}", statusCode, responseBody);

                    if (statusCode >= 200 && statusCode < 300) {
                        // Parse successful response
                        JsonNode responseJson = OBJECT_MAPPER.readTree(responseBody);
                        parseAndSetOutputs(responseJson);
                    } else {
                        // Handle error response
                        setAndStoreOutputParameter(STATUS, "error");
                        setAndStoreOutputParameter(ERROR, "HTTP " + statusCode + ": " + responseBody);
                        setAndStoreOutputParameter(OUTPUT, "{}");
                        setAndStoreOutputParameter(USAGE, "{}");
                        throw new ConnectorException("Agent returned error status: " + statusCode);
                    }
                }
            }

        } catch (IOException | ParseException e) {
            LOGGER.error("Error communicating with AI Agent", e);
            setAndStoreOutputParameter(STATUS, "error");
            setAndStoreOutputParameter(ERROR, "Communication error: " + e.getMessage());
            setAndStoreOutputParameter(OUTPUT, "{}");
            setAndStoreOutputParameter(USAGE, "{}");
            throw new ConnectorException("Failed to communicate with AI Agent: " + e.getMessage(), e);
        }
    }

    private void parseAndSetOutputs(JsonNode responseJson) {
        try {
            // Extract status
            String status = responseJson.has("status") ? 
                responseJson.get("status").asText() : "ok";
            setAndStoreOutputParameter(STATUS, status);

            // Extract output
            String output = responseJson.has("output") ? 
                responseJson.get("output").toString() : "{}";
            setAndStoreOutputParameter(OUTPUT, output);

            // Extract usage
            String usage = responseJson.has("usage") ? 
                responseJson.get("usage").toString() : "{}";
            setAndStoreOutputParameter(USAGE, usage);

            // Extract error (if any)
            String error = responseJson.has("error") && !responseJson.get("error").isNull() ? 
                responseJson.get("error").asText() : null;
            setAndStoreOutputParameter(ERROR, error);

            LOGGER.info("Agent response - Status: {}, Has output: {}", 
                status, responseJson.has("output"));

        } catch (Exception e) {
            LOGGER.error("Error parsing agent response", e);
            setAndStoreOutputParameter(STATUS, "error");
            setAndStoreOutputParameter(ERROR, "Response parsing error: " + e.getMessage());
            setAndStoreOutputParameter(OUTPUT, "{}");
            setAndStoreOutputParameter(USAGE, "{}");
        }
    }

    /**
     * Public accessor for output parameters (for testing)
     */
    public Object getOutputParameter(String name) {
        return outputs.get(name);
    }

    /**
     * Helper to set output parameter and store in test-accessible map
     */
    private void setAndStoreOutputParameter(String name, Object value) {
        setOutputParameter(name, value);
        outputs.put(name, value);
    }
}
