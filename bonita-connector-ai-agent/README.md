# Bonita AI Agent Connector

## Description

Generic connector for Bonita BPM that enables communication with external AI agents via HTTP. This connector provides a flexible interface for integrating AI capabilities into Bonita processes.

## Features

- ✅ HTTP/HTTPS communication with external AI agents
- ✅ Flexible input/output JSON structures
- ✅ Support for authentication headers
- ✅ Configurable timeouts
- ✅ Comprehensive error handling
- ✅ Multiple task types support (RAG Q&A, summarization, etc.)

## Connector Parameters

### Inputs

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `agentUrl` | String | Yes | URL of the AI agent endpoint |
| `authHeader` | String | No | Authentication header (e.g., `Bearer token`) |
| `task` | String | No | Task type (default: `rag_qa`) |
| `inputData` | JSON | Yes | Input data as JSON string |
| `params` | JSON | No | Optional parameters as JSON string |
| `timeoutMs` | Integer | No | Request timeout in milliseconds (default: 30000) |

### Outputs

| Parameter | Type | Description |
|-----------|------|-------------|
| `status` | String | Response status: `ok`, `low_confidence`, or `error` |
| `output` | JSON | Agent response as JSON string |
| `usage` | JSON | Performance metrics as JSON string |
| `error` | String | Error message if status is `error` |

## Building

```bash
mvn clean install
```

This will create:
- `bonita-connector-ai-agent-1.0.0-SNAPSHOT.jar` - Main connector
- `bonita-connector-ai-agent-1.0.0-SNAPSHOT-jar-with-dependencies.jar` - Connector with all dependencies

## Testing

```bash
mvn test
```

Integration tests cover:
1. Simple queries without conflicts
2. Conflict detection and resolution
3. Low confidence scenarios
4. Error handling
5. Input validation
6. Authentication

## Installation in Bonita Studio

1. Build the connector: `mvn clean install`
2. Copy the JAR with dependencies to Bonita Studio:
   - Windows: `%BONITA_STUDIO%\workspace\default\lib`
   - Linux/Mac: `$BONITA_STUDIO/workspace/default/lib`
3. Restart Bonita Studio
4. The connector will appear in the connector palette

## Usage Example

```groovy
// In a Bonita process, configure the connector:

// Input: agentUrl
"http://localhost:8000/run"

// Input: inputData (JSON string)
"""
{
  "question": "What is the deadline for completing employee onboarding?"
}
"""

// Input: params (JSON string)
"""
{
  "top_k": 3,
  "min_confidence": 0.65,
  "require_sources": true
}
"""

// Process output in a script:
import groovy.json.JsonSlurper

def jsonSlurper = new JsonSlurper()
def output = jsonSlurper.parseText(agentOutput)

println "Answer: ${output.answer}"
println "Confidence: ${output.confidence}"
println "Sources: ${output.sources}"
```

## Architecture

```
┌─────────────────────┐
│  Bonita Process     │
│                     │
│  ┌───────────────┐  │
│  │ AI Agent      │  │
│  │ Connector     │  │
│  └───────┬───────┘  │
└──────────┼──────────┘
           │ HTTP/JSON
           ▼
    ┌──────────────┐
    │  RAG Agent   │
    │  (FastAPI)   │
    └──────────────┘
```

## Error Handling

The connector handles various error scenarios:

- **Network errors**: Connection timeout, unreachable host
- **HTTP errors**: 4xx, 5xx status codes
- **Validation errors**: Invalid JSON, missing required parameters
- **Agent errors**: Processing failures, low confidence

All errors are captured in the `status` and `error` output parameters.

## Requirements

- Java 17+
- Bonita 7.14.0+
- Maven 3.6+

## License

This connector is provided as part of the Bonitasoft technical challenge.
