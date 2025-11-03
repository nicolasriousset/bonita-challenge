#!/bin/bash
# Test Automation Script (Unix/Linux/Mac)
# This script runs all validation steps for the challenge

set -e

echo "================================================"
echo "  Bonita AI Agent Connector - Test Suite"
echo "================================================"
echo ""

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
AGENT_PATH="$PROJECT_ROOT/rag-agent"
CONNECTOR_PATH="$PROJECT_ROOT/bonita-connector-ai-agent"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
GRAY='\033[0;37m'
NC='\033[0m' # No Color

# Step 1: Check Python
echo -e "${YELLOW}[1/6] Checking Python...${NC}"
if command -v python3 &> /dev/null; then
    PYTHON_VERSION=$(python3 --version)
    echo -e "${GREEN}  ✓ Found: $PYTHON_VERSION${NC}"
    PYTHON_CMD=python3
elif command -v python &> /dev/null; then
    PYTHON_VERSION=$(python --version)
    echo -e "${GREEN}  ✓ Found: $PYTHON_VERSION${NC}"
    PYTHON_CMD=python
else
    echo -e "${RED}  ✗ Python not found!${NC}"
    exit 1
fi

# Step 2: Check Java
echo -e "${YELLOW}[2/6] Checking Java...${NC}"
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1)
    echo -e "${GREEN}  ✓ Found: $JAVA_VERSION${NC}"
else
    echo -e "${RED}  ✗ Java not found!${NC}"
    exit 1
fi

# Step 3: Check Maven
echo -e "${YELLOW}[3/6] Checking Maven...${NC}"
if command -v mvn &> /dev/null; then
    MAVEN_VERSION=$(mvn -version | head -n 1)
    echo -e "${GREEN}  ✓ Found: $MAVEN_VERSION${NC}"
else
    echo -e "${RED}  ✗ Maven not found!${NC}"
    exit 1
fi

echo ""
echo "================================================"
echo "  Starting RAG Agent"
echo "================================================"
echo ""

# Step 4: Start RAG Agent
echo -e "${YELLOW}[4/6] Installing Python dependencies...${NC}"
cd "$AGENT_PATH"
$PYTHON_CMD -m pip install -q -r requirements.txt
echo -e "${GREEN}  ✓ Dependencies installed${NC}"

echo -e "${YELLOW}[4/6] Starting RAG Agent...${NC}"
$PYTHON_CMD main.py &> /tmp/agent.log &
AGENT_PID=$!
echo -e "${GRAY}  Agent PID: $AGENT_PID${NC}"

echo -e "${YELLOW}  ⏳ Waiting for agent to start...${NC}"
sleep 5

# Check if agent is running
if curl -s http://localhost:8000/ > /dev/null; then
    echo -e "${GREEN}  ✓ Agent running at http://localhost:8000${NC}"
else
    echo -e "${RED}  ✗ Agent failed to start!${NC}"
    kill $AGENT_PID 2>/dev/null || true
    exit 1
fi

echo ""
echo "================================================"
echo "  Testing RAG Agent"
echo "================================================"
echo ""

# Step 5: Test agent with sample query
echo -e "${YELLOW}[5/6] Testing agent with sample query...${NC}"
RESPONSE=$(curl -s -X POST http://localhost:8000/run \
    -H "Content-Type: application/json" \
    -d '{"task":"rag_qa","input":{"question":"What is the deadline for completing employee onboarding?"},"params":{"top_k":3,"min_confidence":0.65}}')

if echo "$RESPONSE" | grep -q '"status":"ok"'; then
    echo -e "${GREEN}  ✓ Agent responded successfully${NC}"
    ANSWER=$(echo "$RESPONSE" | grep -o '"answer":"[^"]*"' | head -c 80)
    echo -e "${GRAY}    $ANSWER...${NC}"
else
    echo -e "${YELLOW}  ⚠ Agent responded with status: $(echo $RESPONSE | grep -o '"status":"[^"]*"')${NC}"
fi

echo ""
echo "================================================"
echo "  Running Connector Integration Tests"
echo "================================================"
echo ""

# Step 6: Run Maven tests
echo -e "${YELLOW}[6/6] Running integration tests...${NC}"
cd "$CONNECTOR_PATH"

if mvn test; then
    echo -e "${GREEN}  ✓ All tests passed!${NC}"
    TEST_RESULT=0
else
    echo -e "${RED}  ✗ Some tests failed${NC}"
    TEST_RESULT=1
fi

# Cleanup
echo ""
echo "================================================"
echo "  Cleanup"
echo "================================================"
echo ""

echo -e "${YELLOW}Stopping RAG Agent...${NC}"
kill $AGENT_PID 2>/dev/null || true
echo -e "${GREEN}  ✓ Agent stopped${NC}"

cd "$PROJECT_ROOT"

echo ""
echo "================================================"
echo "  Test Suite Complete!"
echo "================================================"
echo ""

if [ $TEST_RESULT -eq 0 ]; then
    echo -e "${GREEN}✓ All validation steps passed!${NC}"
    echo ""
    echo -e "${CYAN}Next steps:${NC}"
    echo "  1. Review the test output above"
    echo "  2. Check AI_USAGE_REPORT.md for AI tool usage details"
    echo "  3. See PROJECT_SUMMARY.md for complete challenge status"
    echo "  4. Read QUICKSTART.md for manual testing"
    exit 0
else
    echo -e "${YELLOW}⚠ Some tests failed - review output above${NC}"
    exit 1
fi
