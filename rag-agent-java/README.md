# RAG Agent - Java/Spring Boot

Intelligent RAG (Retrieval-Augmented Generation) Agent built with **Java 17** and **Spring Boot 3.2**.

## Features

- 🚀 **Spring Boot** REST API
- � **Vector Store** with TF-IDF embeddings (in-memory)
- 🔍 **Cosine Similarity** for document retrieval
- ⚔️ **Conflict Detection** between multiple document versions
- 🎯 **Smart Resolution** using most recent documents
- 📊 **Confidence Scoring** for answers
- 🏥 **Health Checks** for monitoring

## Architecture

```
rag-agent-java/
├── src/
│   ├── main/
│   │   ├── java/com/bonitasoft/ai/ragagent/
│   │   │   ├── RagAgentApplication.java      # Spring Boot main
│   │   │   ├── controller/
│   │   │   │   └── AgentController.java      # REST endpoints
│   │   │   ├── service/
│   │   │   │   ├── RagService.java           # RAG logic
│   │   │   │   └── SimpleVectorStore.java    # Vector store (TF-IDF)
│   │   │   ├── config/
│   │   │   │   └── VectorStoreConfig.java    # Vector store bean
│   │   │   └── model/
│   │   │       ├── AgentRequest.java         # Request DTO
│   │   │       ├── AgentResponse.java        # Response DTO
│   │   │       └── Document.java             # Document model
│   │   └── resources/
│   │       ├── application.yml               # Configuration
│   │       └── documents/                    # Document store
│   │           ├── incident_policy_2022.json
│   │           ├── incident_policy_2023.json
│   │           └── onboarding_policy.json
├── Dockerfile                                 # Multi-stage build
└── pom.xml                                    # Maven dependencies
```

## Vector Store Implementation

The RAG agent uses a **custom in-memory vector store** with the following features:

### TF-IDF Vectorization
- **Term Frequency (TF)**: Measures how frequently a term appears in a document
- **Inverse Document Frequency (IDF)**: Measures how important a term is across all documents
- **Formula**: `TF-IDF = TF * log((N + 1) / (DF + 1))`

### Cosine Similarity
- Measures similarity between query and documents using normalized vectors
- Returns documents ranked by relevance score

### Features
- **Stop words filtering**: Removes common English words
- **Text normalization**: Lowercasing, special char removal
- **Automatic reindexing**: Updates vectors when documents are added
- **Fast in-memory search**: No external dependencies required

## API Endpoints

### Health Check
```bash
GET /health
```

**Response:**
```json
{
  "status": "ok",
  "service": "rag-agent"
}
```

### Run RAG Query
```bash
POST /run
Content-Type: application/json

{
  "task": "rag_qa",
  "input_data": {
    "question": "How long do I have to report a security incident?"
  }
}
```

**Response:**
```json
{
  "status": "ok",
  "output": {
    "answer": "Security incidents must be reported within 72 hours...",
    "confidence": 0.95,
    "sources": [
      {
        "title": "Security Incident Procedure - 2023",
        "date": "2023-12-01",
        "version": "2023-12",
        "relevance": 0.92
      }
    ]
  },
  "usage": {
    "documentsSearched": 3,
    "relevantDocuments": 2
  },
  "conflict_info": {
    "detected": true,
    "conflictingSources": [
      "Security Incident Procedure - 2022 (2022-07-01)",
      "Security Incident Procedure - 2023 (2023-12-01)"
    ],
    "resolutionStrategy": "most_recent",
    "reasoning": "Multiple versions of security policy found. Using most recent version from 2023-12-01."
  }
}
```

## Building

### Local Build
```bash
cd rag-agent-java
mvn clean package
```

### Run Locally
```bash
java -jar target/rag-agent-1.0.0-SNAPSHOT.jar
```

### Docker Build
```bash
docker build -t rag-agent-java .
```

### Docker Run
```bash
docker run -p 8000:8000 rag-agent-java
```

## Configuration

Edit `src/main/resources/application.yml`:

```yaml
server:
  port: 8000

rag:
  confidence-threshold: 0.65
  max-sources: 5
```

## Dependencies

- **Spring Boot 3.2.0** - Framework
- **Java 17** - Runtime
- **Jackson** - JSON processing
- **Apache Commons Text** - Text utilities
- **Lombok** - Boilerplate reduction
- **SLF4J** - Logging

## Testing

```bash
# Run unit tests
mvn test

# Test health endpoint
curl http://localhost:8000/health

# Test RAG query
curl -X POST http://localhost:8000/run \
  -H "Content-Type: application/json" \
  -d '{"task":"rag_qa","input_data":{"question":"How long to report incident?"}}'
```

## Integration with Bonita

The Bonita AI Agent Connector (`bonita-connector-ai-agent`) communicates with this service via HTTP REST API.

See parent README for full integration details.
