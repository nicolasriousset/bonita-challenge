# Quick Start Guide

## Prerequisites

- Java 17+ and Maven 3.6+
- Python 3.9+
- Docker (optional)

## Option 1: Local Development (Recommended for testing)

### Step 1: Start the RAG Agent

```powershell
cd rag-agent
pip install -r requirements.txt
python main.py
```

The agent will start at `http://localhost:8000`

Verify it's running:
```powershell
curl http://localhost:8000/
```

### Step 2: Test the Agent Directly

```powershell
curl -X POST http://localhost:8000/run `
  -H "Content-Type: application/json" `
  -d '{\"task\": \"rag_qa\", \"input\": {\"question\": \"What is the deadline for completing employee onboarding?\"}}'
```

### Step 3: Build the Connector

```powershell
cd bonita-connector-ai-agent
mvn clean install
```

### Step 4: Run Integration Tests

```powershell
mvn test
```

Expected output: All 7 tests should pass ✅

## Option 2: Docker Deployment

### Start the Agent with Docker

```powershell
# From project root
docker-compose up -d
```

The agent will be available at `http://localhost:8000`

### Check Status

```powershell
docker-compose ps
docker-compose logs rag-agent
```

### Stop the Agent

```powershell
docker-compose down
```

## Option 3: Bonita Studio Integration

### Install the Connector

1. Build the connector:
   ```powershell
   cd bonita-connector-ai-agent
   mvn clean package
   ```

2. Locate the JAR:
   - `target/bonita-connector-ai-agent-1.0.0-SNAPSHOT-jar-with-dependencies.jar`

3. Copy to Bonita Studio:
   - Windows: `%BONITA_STUDIO%\workspace\default\lib\`
   - Linux/Mac: `$BONITA_STUDIO/workspace/default/lib/`

4. Restart Bonita Studio

### Create a Test Process

1. **Create a new diagram** in Bonita Studio
2. **Add a Service Task**
3. **Add the AI Agent Connector**:
   - Agent URL: `http://localhost:8000/run`
   - Task: `rag_qa`
   - Input Data: `{"question": "What is the deadline for completing employee onboarding?"}`
   - Params: `{"top_k": 3, "min_confidence": 0.65}`

4. **Configure outputs** to process variables
5. **Run the process** and check the results

## Test Scenarios

### Scenario 1: Simple Query (No Conflict)

**Question**: "What is the deadline for completing the employee onboarding process?"

**Expected**:
- ✅ Status: `ok`
- ✅ Answer mentions "5 business days"
- ✅ Source: Onboarding Procedure (2023-06)
- ✅ Confidence: ~0.95

### Scenario 2: Conflict Resolution

**Question**: "How long do I have to report a data incident?"

**Expected**:
- ✅ Status: `ok`
- ✅ Answer mentions "72 hours" (2023 policy)
- ✅ Mentions "48 hours" was outdated (2022 policy)
- ✅ Reasoning explains the conflict
- ✅ Both sources listed
- ✅ `conflict_detected: true`
- ✅ Confidence: ~0.92

### Scenario 3: Low Confidence

**Question**: "Tell me about policies"

**Expected**:
- ⚠️ Status: `low_confidence`
- ⚠️ Answer asks for clarification
- ⚠️ Confidence: <0.65

## Troubleshooting

### Agent not starting

```powershell
# Check Python version
python --version  # Should be 3.9+

# Check if port 8000 is available
netstat -ano | findstr :8000

# Try running with explicit host
python -m uvicorn main:app --host 0.0.0.0 --port 8000
```

### Tests failing

```powershell
# Ensure WireMock port 8089 is free
netstat -ano | findstr :8089

# Clean and rebuild
mvn clean install -U
```

### Connector not appearing in Bonita

1. Check JAR is in correct directory
2. Restart Bonita Studio completely
3. Check Studio logs for errors
4. Verify connector definition file is valid

## API Documentation

Once the agent is running, visit:
- **Interactive API docs**: http://localhost:8000/docs
- **Alternative docs**: http://localhost:8000/redoc
- **Health check**: http://localhost:8000/
- **Document list**: http://localhost:8000/documents

## Next Steps

1. ✅ Run all integration tests
2. ✅ Test the agent API directly
3. ✅ Deploy with Docker (optional)
4. ✅ Install in Bonita Studio (optional)
5. ✅ Create a test process (optional)
6. 📖 Read the AI Usage Report: `AI_USAGE_REPORT.md`

## Support

For issues or questions:
1. Check the README files in each component
2. Review the AI Usage Report for design decisions
3. Examine the integration tests for usage examples
4. Check the FastAPI docs for agent API details
