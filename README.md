# 🧩 Bonita AI Agent Connector Challenge

> **For Evaluators**: Start with [FOR_EVALUATOR.md](FOR_EVALUATOR.md) for a quick evaluation guide! 📋

## 📋 Overview

This project implements a **generic AI Agent Connector for Bonita** with an **intelligent RAG-based agent** capable of reasoning, detecting conflicts, and explaining its decisions.

### Components

1. **AI Agent Connector** (Java) - Bonita 10.2.0 connector for communicating with external AI agents
2. **RAG Agent** (Java/Spring Boot 3.2) - Intelligent agent with conflict resolution capabilities
3. **Integration Tests** - Automated tests demonstrating end-to-end functionality

## 🏗️ Architecture

```
┌─────────────────┐      HTTP/JSON      ┌──────────────────┐
│ Bonita Process  │ ───────────────────▶ │   RAG Agent      │
│   + Connector   │ ◀─────────────────── │  (Spring Boot)   │
└─────────────────┘                      │                  │
                                         │  - Documents     │
                                         │  - Reasoning     │
                                         │  - Conflicts     │
                                         └──────────────────┘
```

## 🚀 Quick Start

> **TL;DR**: See `QUICKSTART.md` for detailed step-by-step instructions.

### Prerequisites

- Java 17+ and Maven 3.9+
- Docker (optional, for containerized deployment)
- Bonita 10.2.0+ (for Studio integration)

### 1. Start the RAG Agent

```powershell
cd rag-agent-java
mvn clean package
java -jar target/rag-agent-1.0.0-SNAPSHOT.jar
```

The agent will be available at `http://localhost:8000`

Verify: `curl http://localhost:8000/health`

### 2. Build and Install the Connector

```powershell
cd bonita-connector-ai-agent
mvn clean package
```

**To install in Bonita Studio:** See [BONITA_INSTALLATION.md](BONITA_INSTALLATION.md) 📖

### 3. Run Integration Tests ⭐

```powershell
cd bonita-connector-ai-agent
mvn test
```

**Expected**: All 7 tests pass ✅

This demonstrates the complete connector-agent integration including conflict resolution!

## 🧠 How It Works

### Conflict Resolution Example

When asked: _"How long do I have to report a data incident?"_

1. **Retrieval**: Agent finds both 2022 (48h) and 2023 (72h) policies
2. **Conflict Detection**: Identifies contradiction in deadlines
3. **Decision**: Favors 2023 version (most recent)
4. **Explanation**: Provides reasoning with sources

**Response:**
```json
{
  "status": "ok",
  "output": {
    "answer": "Current policy requires reporting within 72 hours...",
    "sources": [
      {"title": "Security Incident Procedure - 2023", "version": "2023-12"},
      {"title": "Security Incident Procedure - 2022", "version": "2022-07"}
    ],
    "confidence": 0.92,
    "reasoning": "Detected conflict between 48h (2022) and 72h (2023). Favoring most recent version."
  }
}
```

## 📚 Test Documents

The agent is pre-loaded with three internal policy documents:

- **incident_policy_2022.txt** - Security incident reporting (48h deadline)
- **incident_policy_2023.txt** - Updated security incident reporting (72h deadline)
- **onboarding_policy.txt** - Employee onboarding process (5 days)

## 🧪 Test Scenarios

1. ✅ **Simple Query** - "What is the deadline for completing the employee onboarding process?"
2. ⚠️ **Conflict Resolution** - "How long do I have to report a data incident?"
3. 🤔 **Low Confidence** - Vague or ambiguous questions

## 🤖 AI-Assisted Development Report

### AI Tools Used

- **GitHub Copilot** - Code completion and suggestions
- **ChatGPT/Claude** - Architecture design and documentation
- **AI-Generated vs Manual**:
  - 70% AI-assisted (boilerplate, API contracts, test cases)
  - 30% manual (reasoning logic, conflict resolution algorithm, integration)

### Key Prompts & Iterations

1. "Design a Bonita connector for AI agent communication with proper input/output parameters"
2. "Implement RAG agent with document similarity scoring and conflict detection"
3. "Create reasoning logic to detect contradictions between document versions"

### Design Decisions

- **Chosen Spring Boot 3.2** for agent (enterprise-grade, robust, well-documented)
- **Implemented similarity scoring** for document retrieval (no external dependencies)
- **Implemented custom conflict resolution** based on document dates
- **Added confidence scoring** to handle uncertain responses

## 📦 Project Structure

```
bonita-challenge2/
├── bonita-connector-ai-agent/    # Java connector project (Bonita 10.2.0)
│   ├── src/
│   │   ├── main/java/            # Connector implementation
│   │   │   └── com/bonitasoft/connector/aiagent/
│   │   │       └── AIAgentConnector.java
│   │   ├── main/resources/       # Connector definition
│   │   │   └── ai-agent-connector.def
│   │   └── test/java/            # Integration tests (7 scenarios)
│   │       └── com/bonitasoft/connector/aiagent/
│   │           └── AIAgentConnectorIT.java
│   ├── pom.xml
│   └── README.md
├── rag-agent-java/                # Java/Spring Boot agent
│   ├── src/main/java/com/bonitasoft/ai/
│   │   ├── RagAgentApplication.java     # Spring Boot main
│   │   ├── controller/
│   │   │   └── AgentController.java     # REST endpoints
│   │   ├── service/
│   │   │   └── RagService.java          # RAG logic with conflict resolution
│   │   └── model/                       # DTOs
│   ├── src/main/resources/
│   │   ├── application.yml              # Spring configuration
│   │   └── documents/                   # Test documents (JSON)
│   │       ├── incident_policy_2022.json   # 48h deadline
│   │       ├── incident_policy_2023.json   # 72h deadline (conflict!)
│   │       └── onboarding_policy.json      # 5 business days
│   ├── pom.xml
│   ├── Dockerfile                       # Multi-stage build
│   └── README.md
├── .github/
│   └── copilot-instructions.md
├── docker-compose.yml
├── .gitignore
├── README.md                      # This file
├── QUICKSTART.md                  # Step-by-step setup guide
├── AI_USAGE_REPORT.md            # Detailed AI tool usage analysis
├── MIGRATION_PYTHON_TO_JAVA.md   # Migration documentation
└── PROJECT_SUMMARY.md            # Complete challenge summary
```

## 🔧 Configuration

### Connector Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `agentUrl` | String | URL of the Agent API |
| `authHeader` | String | Optional authentication token |
| `task` | Enum | Task type (rag_qa, summarize) |
| `input` | JSON | Question and context |
| `params` | JSON | Optional parameters (top_k, min_confidence) |
| `timeoutMs` | Integer | Request timeout |

### Agent Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `top_k` | Integer | 3 | Number of documents to retrieve |
| `min_confidence` | Float | 0.65 | Minimum confidence threshold |
| `require_sources` | Boolean | true | Include source references |

## 🐳 Docker Deployment

```bash
docker-compose up -d
```

This starts the RAG agent on port 8000.

## 📝 License

This project was created for the Bonitasoft technical challenge (November 2025).

---

## 📚 Additional Documentation

- **`QUICKSTART.md`** - Step-by-step setup and testing guide
- **`AI_USAGE_REPORT.md`** - Detailed analysis of AI tool usage (35% criteria)
- **`PROJECT_SUMMARY.md`** - Complete challenge completion status
- **`MIGRATION_PYTHON_TO_JAVA.md`** - Python → Java migration documentation
- **`bonita-connector-ai-agent/README.md`** - Connector technical details
- **`rag-agent-java/README.md`** - Agent architecture and API docs

## ✅ Challenge Requirements Met

- ✅ **Part 1**: Generic AI Agent Connector (Java/Maven, Bonita 10.2.0)
- ✅ **Part 2**: RAG Agent with reasoning and conflict resolution (Java/Spring Boot 3.2)
- ✅ **Part 3**: Integration tests (Option B) - 7 comprehensive scenarios
- ✅ **Part 4**: Complete documentation including AI usage report
- ✅ **Bonus**: Docker deployment, Bonita Studio integration guide
- ✅ **Migration**: Complete Python → Java migration with documentation

**Status**: All requirements completed and tested ✅

---
```

This starts the RAG agent on port 8000.

## 📝 License

This project was created for the Bonitasoft technical challenge (November 2025).

---

## � Additional Documentation

- **`QUICKSTART.md`** - Step-by-step setup and testing guide
- **`AI_USAGE_REPORT.md`** - Detailed analysis of AI tool usage (35% criteria)
- **`PROJECT_SUMMARY.md`** - Complete challenge completion status
- **`bonita-connector-ai-agent/README.md`** - Connector technical details
- **`rag-agent/README.md`** - Agent architecture and API docs

## ✅ Challenge Requirements Met

- ✅ **Part 1**: Generic AI Agent Connector (Java/Maven)
- ✅ **Part 2**: RAG Agent with reasoning and conflict resolution
- ✅ **Part 3**: Integration tests (Option B) - 7 comprehensive scenarios
- ✅ **Part 4**: Complete documentation including AI usage report
- ✅ **Bonus**: Docker deployment, Bonita Studio integration guide

**Status**: All requirements completed and tested ✅

---

## 🙋 Questions & Support

For questions or issues, please refer to:
1. `QUICKSTART.md` for setup problems
2. `AI_USAGE_REPORT.md` for design decisions
3. Component READMEs for technical details
4. Interactive API docs at http://localhost:8000/docs

---

**Estimated Development Time**: ~4 hours  
**Completion Date**: November 2025  
**AI Contribution**: ~70% (structure/boilerplate), ~30% manual (logic/design)
