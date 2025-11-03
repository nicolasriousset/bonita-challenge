# RAG Agent

## Description

Intelligent RAG (Retrieval-Augmented Generation) agent with conflict detection and resolution capabilities. This agent can retrieve relevant documents, detect contradictions, and provide reasoned answers with source citations.

## Features

- 📚 Document retrieval with similarity scoring
- ⚠️ Automatic conflict detection between sources
- 🧠 Intelligent resolution strategy (favors most recent documents)
- 📖 Source citation and transparency
- 🔍 Confidence scoring
- ⚡ Fast response times

## API Endpoints

### POST /run

Execute a RAG query with conflict resolution.

**Request:**
```json
{
  "task": "rag_qa",
  "input": {
    "question": "How long do I have to report a data incident?"
  },
  "params": {
    "top_k": 3,
    "min_confidence": 0.65,
    "require_sources": true
  }
}
```

**Response:**
```json
{
  "status": "ok",
  "output": {
    "answer": "Current policy requires reporting within 72 hours...",
    "sources": [
      {
        "title": "Security Incident Procedure - 2023",
        "version": "2023-12",
        "uri": "incident_policy_2023.txt",
        "page": 0
      }
    ],
    "confidence": 0.92,
    "reasoning": "Detected conflict between 48h and 72h. Favoring most recent version.",
    "conflict_detected": true,
    "resolution_strategy": "favor_recent_version"
  },
  "usage": {
    "latency_ms": 387,
    "tokens_in": 320,
    "tokens_out": 95,
    "model": "simple-embedding"
  },
  "error": null
}
```

### GET /

Health check endpoint.

### GET /documents

List all loaded documents.

## Installation

### Local Development

```bash
# Install dependencies
pip install -r requirements.txt

# Run the server
python main.py
```

The API will be available at `http://localhost:8000`

### Docker

```bash
# Build and run with docker-compose (from project root)
docker-compose up -d

# Or build manually
cd rag-agent
docker build -t rag-agent .
docker run -p 8000:8000 rag-agent
```

## Configuration

### Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `top_k` | 3 | Number of documents to retrieve |
| `min_confidence` | 0.65 | Confidence threshold |
| `require_sources` | true | Include source references |

### Documents

Documents are loaded from the `documents/` directory. Each document should be a `.txt` file with metadata:

```
Title: Document Title
Version: YYYY-MM
Department: Department Name

Document content here...
```

## Conflict Resolution

The agent detects conflicts by:
1. Retrieving top-k relevant documents
2. Extracting numeric values and dates
3. Comparing values across documents
4. Detecting contradictions

Resolution strategy:
- **Favor recent version**: When documents have dates, uses the most recent
- **Fallback to score**: Uses the document with highest relevance score

## Testing

Test the API manually:

```bash
curl -X POST http://localhost:8000/run \
  -H "Content-Type: application/json" \
  -d '{
    "task": "rag_qa",
    "input": {"question": "What is the onboarding deadline?"},
    "params": {"top_k": 3}
  }'
```

Or use the interactive docs at `http://localhost:8000/docs`

## Architecture

```
┌──────────────────────────────────────┐
│         RAG Agent (FastAPI)          │
├──────────────────────────────────────┤
│                                      │
│  ┌────────────┐    ┌──────────────┐ │
│  │  Document  │───▶│   Retrieval  │ │
│  │   Store    │    │   Engine     │ │
│  └────────────┘    └──────┬───────┘ │
│                           │          │
│                           ▼          │
│                    ┌──────────────┐  │
│                    │   Conflict   │  │
│                    │   Detection  │  │
│                    └──────┬───────┘  │
│                           │          │
│                           ▼          │
│                    ┌──────────────┐  │
│                    │  Resolution  │  │
│                    │   Strategy   │  │
│                    └──────┬───────┘  │
│                           │          │
│                           ▼          │
│                    ┌──────────────┐  │
│                    │   Answer     │  │
│                    │ Generation   │  │
│                    └──────────────┘  │
└──────────────────────────────────────┘
```

## Requirements

- Python 3.9+
- FastAPI
- Uvicorn
- NumPy (for future vector operations)

## Future Enhancements

- Real embeddings (OpenAI, HuggingFace)
- FAISS vector store integration
- Multiple resolution strategies
- Confidence calibration
- Query expansion
- Multi-document reasoning

## License

This agent is provided as part of the Bonitasoft technical challenge.
