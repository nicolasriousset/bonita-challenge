package com.bonitasoft.connector.aiagent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.bonitasoft.engine.connector.ConnectorException;
import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for AI Agent Connector
 * 
 * Tests the end-to-end communication between the Bonita connector and the RAG agent,
 * including conflict resolution scenarios.
 */
class AIAgentConnectorIT {

    private static WireMockServer wireMockServer;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private AIAgentConnector connector;

    @BeforeAll
    static void setupServer() {
        wireMockServer = new WireMockServer(options().port(8089));
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);
    }

    @AfterAll
    static void teardownServer() {
        wireMockServer.stop();
    }

    @BeforeEach
    void setup() {
        wireMockServer.resetAll();
        connector = new AIAgentConnector();
    }

    @Test
    @DisplayName("Test 1: Simple query without conflict - Employee onboarding")
    void testSimpleQueryOnboarding() throws Exception {
        // Given
        String question = "What is the deadline for completing the employee onboarding process?";
        
        String mockResponse = """
            {
                "status": "ok",
                "output": {
                    "answer": "New employees must complete onboarding within 5 business days.",
                    "sources": [
                        {
                            "title": "Employee Onboarding Procedure",
                            "version": "2023-06",
                            "uri": "onboarding_policy.txt"
                        }
                    ],
                    "confidence": 0.95,
                    "reasoning": "Clear answer found in onboarding policy with no conflicts."
                },
                "usage": {
                    "latency_ms": 245,
                    "tokens_in": 150,
                    "tokens_out": 45,
                    "model": "text-embedding-ada-002"
                }
            }
            """;

        stubFor(post(urlEqualTo("/run"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(mockResponse)));

        Map<String, Object> inputs = new HashMap<>();
        inputs.put(AIAgentConnector.AGENT_URL, "http://localhost:8089/run");
        inputs.put(AIAgentConnector.TASK, "rag_qa");
        inputs.put(AIAgentConnector.INPUT_DATA, String.format("{\"question\": \"%s\"}", question));
        inputs.put(AIAgentConnector.PARAMS, "{\"top_k\": 3, \"min_confidence\": 0.65}");

        connector.setInputParameters(inputs);

        // When
        connector.validateInputParameters();
        connector.executeBusinessLogic();

        // Then
        assertThat(connector.getOutputParameter(AIAgentConnector.STATUS)).isEqualTo("ok");
        
        String output = (String) connector.getOutputParameter(AIAgentConnector.OUTPUT);
        Map<String, Object> outputMap = objectMapper.readValue(output, Map.class);
        
        assertThat(outputMap.get("answer")).asString().contains("5 business days");
        assertThat(outputMap.get("confidence")).isEqualTo(0.95);
        
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> sources = (java.util.List<Map<String, Object>>) outputMap.get("sources");
        assertThat(sources).hasSize(1);
        assertThat(sources.get(0).get("title")).isEqualTo("Employee Onboarding Procedure");
    }

    @Test
    @DisplayName("Test 2: Conflict resolution - Data incident reporting deadline")
    void testConflictResolutionIncidentReporting() throws Exception {
        // Given
        String question = "How long do I have to report a data incident?";
        
        String mockResponse = """
            {
                "status": "ok",
                "output": {
                    "answer": "Current policy requires reporting within 72 hours (based on the 2023 procedure). The 2022 version required 48 hours but is outdated.",
                    "sources": [
                        {
                            "title": "Security Incident Procedure - 2023",
                            "version": "2023-12",
                            "uri": "incident_policy_2023.txt",
                            "page": 0
                        },
                        {
                            "title": "Security Incident Procedure - 2022",
                            "version": "2022-07",
                            "uri": "incident_policy_2022.txt",
                            "page": 0
                        }
                    ],
                    "confidence": 0.92,
                    "reasoning": "Detected conflict: 2022 policy states 48 hours, 2023 policy states 72 hours. Favoring most recent version (2023-12).",
                    "conflict_detected": true,
                    "resolution_strategy": "favor_recent_version"
                },
                "usage": {
                    "latency_ms": 387,
                    "tokens_in": 320,
                    "tokens_out": 95,
                    "model": "text-embedding-ada-002"
                }
            }
            """;

        stubFor(post(urlEqualTo("/run"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(mockResponse)));

        Map<String, Object> inputs = new HashMap<>();
        inputs.put(AIAgentConnector.AGENT_URL, "http://localhost:8089/run");
        inputs.put(AIAgentConnector.TASK, "rag_qa");
        inputs.put(AIAgentConnector.INPUT_DATA, String.format("{\"question\": \"%s\"}", question));
        inputs.put(AIAgentConnector.PARAMS, "{\"top_k\": 5, \"min_confidence\": 0.65, \"require_sources\": true}");

        connector.setInputParameters(inputs);

        // When
        connector.validateInputParameters();
        connector.executeBusinessLogic();

        // Then
        assertThat(connector.getOutputParameter(AIAgentConnector.STATUS)).isEqualTo("ok");
        
        String output = (String) connector.getOutputParameter(AIAgentConnector.OUTPUT);
        Map<String, Object> outputMap = objectMapper.readValue(output, Map.class);
        
        // Verify the answer favors the 2023 version
        assertThat(outputMap.get("answer")).asString()
            .contains("72 hours")
            .contains("2023")
            .contains("48 hours")
            .contains("outdated");
        
        // Verify reasoning explains the conflict resolution
        assertThat(outputMap.get("reasoning")).asString()
            .contains("conflict")
            .contains("2022")
            .contains("2023")
            .contains("Favoring");
        
        // Verify both sources are present
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> sources = (java.util.List<Map<String, Object>>) outputMap.get("sources");
        assertThat(sources).hasSize(2);
        
        assertThat(sources.get(0).get("version")).isEqualTo("2023-12");
        assertThat(sources.get(1).get("version")).isEqualTo("2022-07");
        
        // Verify conflict was detected
        assertThat(outputMap.get("conflict_detected")).isEqualTo(true);
        assertThat(outputMap.get("resolution_strategy")).isEqualTo("favor_recent_version");
        
        // Verify confidence is good but not perfect (due to conflict)
        assertThat((Double) outputMap.get("confidence")).isBetween(0.85, 0.95);
    }

    @Test
    @DisplayName("Test 3: Low confidence scenario")
    void testLowConfidenceScenario() throws Exception {
        // Given
        String vaguQuestion = "Tell me about policies";
        
        String mockResponse = """
            {
                "status": "low_confidence",
                "output": {
                    "answer": "I found multiple policies in the knowledge base, but your question is too vague. Could you please be more specific? Available topics: onboarding, security incidents.",
                    "sources": [],
                    "confidence": 0.42,
                    "reasoning": "Question too broad, multiple potential matches with low relevance scores."
                },
                "usage": {
                    "latency_ms": 198,
                    "tokens_in": 95,
                    "tokens_out": 38,
                    "model": "text-embedding-ada-002"
                }
            }
            """;

        stubFor(post(urlEqualTo("/run"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(mockResponse)));

        Map<String, Object> inputs = new HashMap<>();
        inputs.put(AIAgentConnector.AGENT_URL, "http://localhost:8089/run");
        inputs.put(AIAgentConnector.TASK, "rag_qa");
        inputs.put(AIAgentConnector.INPUT_DATA, String.format("{\"question\": \"%s\"}", vaguQuestion));
        inputs.put(AIAgentConnector.PARAMS, "{\"top_k\": 3, \"min_confidence\": 0.65}");

        connector.setInputParameters(inputs);

        // When
        connector.validateInputParameters();
        connector.executeBusinessLogic();

        // Then
        assertThat(connector.getOutputParameter(AIAgentConnector.STATUS)).isEqualTo("low_confidence");
        
        String output = (String) connector.getOutputParameter(AIAgentConnector.OUTPUT);
        Map<String, Object> outputMap = objectMapper.readValue(output, Map.class);
        
        assertThat(outputMap.get("answer")).asString()
            .contains("too vague")
            .contains("more specific");
        
        assertThat((Double) outputMap.get("confidence")).isLessThan(0.65);
    }

    @Test
    @DisplayName("Test 4: Agent error handling")
    void testAgentErrorHandling() throws Exception {
        // Given
        stubFor(post(urlEqualTo("/run"))
            .willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\": \"Internal agent error\"}")));

        Map<String, Object> inputs = new HashMap<>();
        inputs.put(AIAgentConnector.AGENT_URL, "http://localhost:8089/run");
        inputs.put(AIAgentConnector.TASK, "rag_qa");
        inputs.put(AIAgentConnector.INPUT_DATA, "{\"question\": \"test\"}");

        connector.setInputParameters(inputs);

        // When
        connector.validateInputParameters();
        assertThatThrownBy(() -> connector.executeBusinessLogic())
            .isInstanceOf(ConnectorException.class);

        // Then
        assertThat(connector.getOutputParameter(AIAgentConnector.STATUS)).isEqualTo("error");
        assertThat(connector.getOutputParameter(AIAgentConnector.ERROR)).asString()
            .contains("500");
    }

    @Test
    @DisplayName("Test 5: Input validation - missing URL")
    void testValidationMissingUrl() {
        // Given
        Map<String, Object> inputs = new HashMap<>();
        inputs.put(AIAgentConnector.INPUT_DATA, "{\"question\": \"test\"}");
        connector.setInputParameters(inputs);

        // When & Then
        assertThatThrownBy(() -> connector.validateInputParameters())
            .isInstanceOf(ConnectorValidationException.class)
            .hasMessageContaining("Agent URL is required");
    }

    @Test
    @DisplayName("Test 6: Input validation - invalid JSON")
    void testValidationInvalidJson() {
        // Given
        Map<String, Object> inputs = new HashMap<>();
        inputs.put(AIAgentConnector.AGENT_URL, "http://localhost:8089/run");
        inputs.put(AIAgentConnector.INPUT_DATA, "not valid json");
        connector.setInputParameters(inputs);

        // When & Then
        assertThatThrownBy(() -> connector.validateInputParameters())
            .isInstanceOf(ConnectorValidationException.class)
            .hasMessageContaining("must be valid JSON");
    }

    @Test
    @DisplayName("Test 7: Authentication header support")
    void testAuthenticationHeader() throws Exception {
        // Given
        String mockResponse = """
            {
                "status": "ok",
                "output": {"answer": "Authenticated response"},
                "usage": {"latency_ms": 100}
            }
            """;

        stubFor(post(urlEqualTo("/run"))
            .withHeader("Authorization", equalTo("Bearer secret-token"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(mockResponse)));

        Map<String, Object> inputs = new HashMap<>();
        inputs.put(AIAgentConnector.AGENT_URL, "http://localhost:8089/run");
        inputs.put(AIAgentConnector.AUTH_HEADER, "Bearer secret-token");
        inputs.put(AIAgentConnector.INPUT_DATA, "{\"question\": \"test\"}");

        connector.setInputParameters(inputs);

        // When
        connector.validateInputParameters();
        connector.executeBusinessLogic();

        // Then
        assertThat(connector.getOutputParameter(AIAgentConnector.STATUS)).isEqualTo("ok");
        verify(postRequestedFor(urlEqualTo("/run"))
            .withHeader("Authorization", equalTo("Bearer secret-token")));
    }
}
