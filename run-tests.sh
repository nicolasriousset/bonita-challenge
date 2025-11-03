#!/bin/bash
# Test Automation Script (Unix/Linux/Mac)
# This script runs all validation steps for the challenge

set -e

echo "================================================"
echo "  Bonita AI Agent Connector - Test Suite"
echo "================================================"
echo ""

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
AGENT_PATH="$PROJECT_ROOT/rag-agent-java"
CONNECTOR_PATH="$PROJECT_ROOT/bonita-connector-ai-agent"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
GRAY='\033[0;37m'
NC='\033[0m' # No Color

# Step 1: Check Java
echo -e "${YELLOW}[1/4] Checking Java...${NC}"
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1)
    echo -e "${GREEN}  ✓ Found: $JAVA_VERSION${NC}"
else
    echo -e "${RED}  ✗ Java not found!${NC}"
    exit 1
fi

# Step 2: Check Maven
echo -e "${YELLOW}[2/4] Checking Maven...${NC}"
if command -v mvn &> /dev/null; then
    MAVEN_VERSION=$(mvn -version | head -n 1)
    echo -e "${GREEN}  ✓ Found: $MAVEN_VERSION${NC}"
else
    echo -e "${RED}  ✗ Maven not found!${NC}"
    exit 1
fi

echo ""
echo "================================================"
echo "  Starting RAG Agent (Java/Spring Boot)"
echo "================================================"
echo ""

# Step 3: Build and Start RAG Agent
echo -e "${YELLOW}[3/4] Building RAG Agent...${NC}"
cd "$AGENT_PATH"
mvn clean package -DskipTests -q
if [ $? -ne 0 ]; then
    echo -e "${RED}  ✗ Build failed!${NC}"
    exit 1
fi
echo -e "${GREEN}  ✓ Build successful${NC}"

echo -e "${YELLOW}[3/4] Starting RAG Agent...${NC}"
java -jar target/rag-agent-1.0.0-SNAPSHOT.jar &> /tmp/agent.log &
AGENT_PID=$!
echo -e "${GRAY}  Agent PID: $AGENT_PID${NC}"

echo -e "${YELLOW}  ⏳ Waiting for agent to start...${NC}"
sleep 8

# Check if agent is running
if curl -s http://localhost:8000/health | grep -q '"status":"ok"'; then
    echo -e "${GREEN}  ✓ Agent running at http://localhost:8000${NC}"
else
    echo -e "${RED}  ✗ Agent failed to start!${NC}"
    echo -e "${RED}  Check /tmp/agent.log for details${NC}"
    kill $AGENT_PID 2>/dev/null || true
    exit 1
fi

echo ""
echo "================================================"
echo "  Testing RAG Agent"
echo "================================================"
echo ""

# Step 4: Test agent with sample query
echo -e "${YELLOW}[4/4] Testing agent with sample query...${NC}"
RESPONSE=$(curl -s -X POST http://localhost:8000/run \
    -H "Content-Type: application/json" \
    -d '{"task":"rag_qa","input_data":{"question":"What is the deadline for completing employee onboarding?"},"params":{"top_k":3,"min_confidence":0.65}}')

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

# Step 5: Run Maven tests
echo -e "${YELLOW}[4/4] Running integration tests...${NC}"
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
