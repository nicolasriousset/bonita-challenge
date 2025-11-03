# Bonita AI Agent Connector - Copilot Instructions

## Project Overview
This project implements a generic AI Agent Connector for Bonita with an intelligent RAG-based agent.

**Components:**
1. **bonita-connector-ai-agent** - Java Maven connector using Bonita archetype
2. **rag-agent** - Python FastAPI service with vector store and reasoning
3. **Integration tests** - Automated tests demonstrating connector-agent communication

## Architecture
- Connector communicates with Agent via HTTP REST API
- Agent uses FAISS vector store for document retrieval
- Agent implements conflict detection and resolution logic
- Favors most recent documents when conflicts detected

## Development Status
- [x] Create project structure and copilot instructions
- [ ] Create Bonita Connector (Java Maven)
- [ ] Create RAG Agent (Python FastAPI)
- [ ] Add test documents
- [ ] Create integration tests
- [ ] Add Docker configuration
- [ ] Write comprehensive README
