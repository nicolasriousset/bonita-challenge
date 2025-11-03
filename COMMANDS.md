# 🚀 Command Reference

Quick reference for all common commands in this project.

## 📖 Documentation

```bash
# Start here for evaluation
cat FOR_EVALUATOR.md

# Quick navigation
cat INDEX.md

# Setup guide
cat QUICKSTART.md

# Complete status
cat PROJECT_SUMMARY.md

# AI usage details (35% weight!)
cat AI_USAGE_REPORT.md
```

## 🧪 Testing

### Automated Test Suite (Recommended)

**Windows PowerShell:**
```powershell
.\run-tests.ps1
```

**Linux/Mac:**
```bash
chmod +x run-tests.sh
./run-tests.sh
```

### Manual Testing

**Start Agent:**
```bash
cd rag-agent
pip install -r requirements.txt
python main.py
# Agent runs at http://localhost:8000
```

**Run Connector Tests:**
```bash
cd bonita-connector-ai-agent
mvn clean test
```

**Build Connector:**
```bash
cd bonita-connector-ai-agent
mvn clean install
# JAR: target/bonita-connector-ai-agent-1.0.0-SNAPSHOT-jar-with-dependencies.jar
```

## 🐳 Docker

**Start with Docker:**
```bash
docker-compose up -d
```

**Check status:**
```bash
docker-compose ps
docker-compose logs rag-agent
```

**Stop:**
```bash
docker-compose down
```

## 🔍 API Testing

### Health Check
```bash
curl http://localhost:8000/
```

### List Documents
```bash
curl http://localhost:8000/documents
```

### Simple Query (No Conflict)
```bash
curl -X POST http://localhost:8000/run \
  -H "Content-Type: application/json" \
  -d '{
    "task": "rag_qa",
    "input": {
      "question": "What is the deadline for completing employee onboarding?"
    },
    "params": {
      "top_k": 3,
      "min_confidence": 0.65
    }
  }'
```

**Expected**: Answer mentions "5 business days"

### Conflict Resolution Query
```bash
curl -X POST http://localhost:8000/run \
  -H "Content-Type: application/json" \
  -d '{
    "task": "rag_qa",
    "input": {
      "question": "How long do I have to report a data incident?"
    },
    "params": {
      "top_k": 5,
      "min_confidence": 0.65
    }
  }'
```

**Expected**: 
- Answer mentions 72h (2023) as current
- Mentions 48h (2022) as outdated
- `conflict_detected: true`
- Clear reasoning provided

### PowerShell Versions

**Health Check:**
```powershell
Invoke-WebRequest http://localhost:8000/
```

**Simple Query:**
```powershell
$body = @{
    task = "rag_qa"
    input = @{
        question = "What is the deadline for completing employee onboarding?"
    }
    params = @{
        top_k = 3
        min_confidence = 0.65
    }
} | ConvertTo-Json -Depth 3

Invoke-RestMethod -Uri http://localhost:8000/run -Method Post -Body $body -ContentType "application/json"
```

## 📊 Code Analysis

### Java Connector

**Main Class:**
```bash
cat bonita-connector-ai-agent/src/main/java/com/bonitasoft/connector/aiagent/AIAgentConnector.java
```

**Integration Tests:**
```bash
cat bonita-connector-ai-agent/src/test/java/com/bonitasoft/connector/aiagent/AIAgentConnectorIT.java
```

**Build Configuration:**
```bash
cat bonita-connector-ai-agent/pom.xml
```

### Python Agent

**Main Application:**
```bash
cat rag-agent/main.py
```

**Key Methods:**
- `detect_conflicts()` - Lines ~150-185
- `resolve_conflict()` - Lines ~190-200
- `generate_answer()` - Lines ~205-280

**Dependencies:**
```bash
cat rag-agent/requirements.txt
```

**Test Documents:**
```bash
ls rag-agent/documents/
cat rag-agent/documents/incident_policy_2022.txt
cat rag-agent/documents/incident_policy_2023.txt
cat rag-agent/documents/onboarding_policy.txt
```

## 🔧 Development

### Install Java Dependencies
```bash
cd bonita-connector-ai-agent
mvn clean install
```

### Install Python Dependencies
```bash
cd rag-agent
pip install -r requirements.txt
```

### Run Agent in Development Mode
```bash
cd rag-agent
python -m uvicorn main:app --reload --port 8000
```

### Clean Build
```bash
cd bonita-connector-ai-agent
mvn clean
```

## 📝 Validation

### Check All Tests Pass
```bash
cd bonita-connector-ai-agent
mvn test
# Expected: Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
```

### Verify Agent is Running
```bash
curl http://localhost:8000/ | grep '"status":"ok"'
```

### Count Documents Loaded
```bash
curl http://localhost:8000/documents | grep -c '"uri"'
# Expected: 3
```

## 🌐 Interactive Documentation

Once agent is running:
- **Swagger UI**: http://localhost:8000/docs
- **ReDoc**: http://localhost:8000/redoc
- **OpenAPI JSON**: http://localhost:8000/openapi.json

## 🐛 Troubleshooting

### Port Already in Use
```bash
# Check what's using port 8000
netstat -ano | findstr :8000     # Windows
lsof -i :8000                     # Linux/Mac

# Kill process
taskkill /PID <PID> /F           # Windows
kill -9 <PID>                     # Linux/Mac
```

### Python Issues
```bash
# Check Python version
python --version  # Should be 3.9+

# Reinstall dependencies
pip install --force-reinstall -r requirements.txt
```

### Maven Issues
```bash
# Update dependencies
mvn clean install -U

# Skip tests
mvn clean install -DskipTests
```

### Agent Not Starting
```bash
# Run with verbose output
cd rag-agent
python main.py

# Check if documents loaded
ls documents/
```

## 📦 Packaging

### Build Connector JAR
```bash
cd bonita-connector-ai-agent
mvn clean package
# Output: target/bonita-connector-ai-agent-1.0.0-SNAPSHOT-jar-with-dependencies.jar
```

### Build Docker Image
```bash
cd rag-agent
docker build -t bonita-rag-agent .
docker run -p 8000:8000 bonita-rag-agent
```

## 🎯 Quick Verification

Run this sequence to verify everything works:

```bash
# 1. Start agent
cd rag-agent && python main.py &
sleep 5

# 2. Test agent
curl http://localhost:8000/

# 3. Run connector tests
cd ../bonita-connector-ai-agent && mvn test

# 4. Stop agent
pkill -f "python main.py"
```

## 📚 Help

- **Setup issues**: See QUICKSTART.md
- **API questions**: Check rag-agent/README.md
- **Connector details**: See bonita-connector-ai-agent/README.md
- **Design decisions**: Read AI_USAGE_REPORT.md
- **Navigation**: Check INDEX.md

---

**Quick Start**: Run `.\run-tests.ps1` (Windows) or `./run-tests.sh` (Linux/Mac)
