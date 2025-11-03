# Bonita AI Agent Connector - Copilot Instructions

## Project Overview
This project implements a generic AI Agent Connector for Bonita with an intelligent RAG-based agent.

**Components:**
1. **bonita-connector-ai-agent** - Java Maven connector using Bonita archetype
2. **rag-agent-java** - Java/Spring Boot 3.2 service with vector store and reasoning
3. **Integration tests** - Automated tests demonstrating connector-agent communication

## Architecture
- Connector communicates with Agent via HTTP REST API
- Agent implements document similarity scoring for retrieval
- Agent implements conflict detection and resolution logic
- Favors most recent documents when conflicts detected

## Development Status
- [x] Create project structure and copilot instructions
- [x] Create Bonita Connector (Java Maven) - Migrated to Bonita 10.2.0
- [x] Create RAG Agent (Java/Spring Boot 3.2)
- [x] Add test documents
- [x] Create integration tests
- [x] Add Docker configuration
- [x] Write comprehensive README
