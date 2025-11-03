from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from typing import Optional, List, Dict, Any
import numpy as np
from pathlib import Path
import re
from datetime import datetime
import time

# Pour la démo, on utilise une implémentation simple sans dépendances LLM externes
# Dans un environnement réel, on utiliserait OpenAI, HuggingFace, etc.

app = FastAPI(
    title="RAG Agent API",
    description="Intelligent RAG agent with conflict resolution capabilities",
    version="1.0.0"
)

# Models
class AgentInput(BaseModel):
    question: str = Field(..., description="The question to answer")
    
class AgentParams(BaseModel):
    top_k: int = Field(default=3, description="Number of documents to retrieve")
    min_confidence: float = Field(default=0.65, description="Minimum confidence threshold")
    require_sources: bool = Field(default=True, description="Include source references")

class AgentRequest(BaseModel):
    task: str = Field(default="rag_qa", description="Task type")
    input: AgentInput
    params: Optional[AgentParams] = AgentParams()

class Source(BaseModel):
    title: str
    version: Optional[str] = None
    uri: str
    page: int = 0

class AgentOutput(BaseModel):
    answer: str
    sources: List[Source]
    confidence: float
    reasoning: Optional[str] = None
    conflict_detected: Optional[bool] = False
    resolution_strategy: Optional[str] = None

class Usage(BaseModel):
    latency_ms: int
    tokens_in: int
    tokens_out: int
    model: str = "simple-embedding"

class AgentResponse(BaseModel):
    status: str  # ok, low_confidence, error
    output: AgentOutput
    usage: Usage
    error: Optional[str] = None

# Document Store
class Document:
    def __init__(self, title: str, content: str, uri: str, version: Optional[str] = None):
        self.title = title
        self.content = content
        self.uri = uri
        self.version = version
        self.date = self._extract_date(version) if version else None
        
    def _extract_date(self, version: str) -> Optional[datetime]:
        """Extract date from version string like '2023-12'"""
        match = re.search(r'(\d{4})-(\d{2})', version)
        if match:
            year, month = int(match.group(1)), int(match.group(2))
            return datetime(year, month, 1)
        return None

class RAGAgent:
    def __init__(self):
        self.documents: List[Document] = []
        self.load_documents()
        
    def load_documents(self):
        """Load documents from the documents directory"""
        docs_path = Path(__file__).parent / "documents"
        
        if not docs_path.exists():
            print(f"Warning: Documents directory not found at {docs_path}")
            return
            
        for file_path in docs_path.glob("*.txt"):
            content = file_path.read_text(encoding="utf-8")
            
            # Extract metadata from content
            title_match = re.search(r'Title:\s*(.+)', content)
            version_match = re.search(r'Version:\s*(.+)', content)
            
            title = title_match.group(1).strip() if title_match else file_path.stem
            version = version_match.group(1).strip() if version_match else None
            
            doc = Document(
                title=title,
                content=content,
                uri=file_path.name,
                version=version
            )
            self.documents.append(doc)
            print(f"Loaded document: {title} (version: {version})")
    
    def simple_similarity(self, query: str, doc_content: str) -> float:
        """Simple keyword-based similarity (in real app, use embeddings)"""
        query_words = set(query.lower().split())
        doc_words = set(doc_content.lower().split())
        
        if not query_words:
            return 0.0
            
        intersection = query_words & doc_words
        return len(intersection) / len(query_words)
    
    def retrieve(self, question: str, top_k: int = 3) -> List[tuple[Document, float]]:
        """Retrieve most relevant documents"""
        scored_docs = []
        for doc in self.documents:
            score = self.simple_similarity(question, doc.content)
            scored_docs.append((doc, score))
        
        # Sort by score descending
        scored_docs.sort(key=lambda x: x[1], reverse=True)
        return scored_docs[:top_k]
    
    def detect_conflicts(self, docs: List[tuple[Document, float]], question: str) -> Dict[str, Any]:
        """Detect conflicts between documents"""
        # Look for numeric conflicts (e.g., different deadlines)
        numbers_by_doc = {}
        
        for doc, score in docs:
            # Extract numbers followed by time units
            patterns = [
                r'(\d+)\s*(hours?|h)',
                r'(\d+)\s*(days?|d)',
                r'(\d+)\s*(business\s+days?)'
            ]
            
            for pattern in patterns:
                matches = re.findall(pattern, doc.content, re.IGNORECASE)
                if matches:
                    numbers_by_doc[doc.uri] = matches
        
        # Check if we have conflicting numbers for similar questions
        if len(numbers_by_doc) > 1:
            all_numbers = []
            for doc_numbers in numbers_by_doc.values():
                all_numbers.extend([int(n[0]) for n in doc_numbers])
            
            # If numbers differ, we have a conflict
            if len(set(all_numbers)) > 1:
                return {
                    "detected": True,
                    "type": "numeric_value",
                    "values": numbers_by_doc
                }
        
        return {"detected": False}
    
    def resolve_conflict(self, docs: List[tuple[Document, float]]) -> Document:
        """Resolve conflict by favoring most recent document"""
        dated_docs = [(doc, score) for doc, score in docs if doc.date is not None]
        
        if dated_docs:
            # Sort by date descending (most recent first)
            dated_docs.sort(key=lambda x: x[0].date, reverse=True)
            return dated_docs[0][0]
        
        # Fallback to highest score
        return docs[0][0]
    
    def generate_answer(
        self, 
        question: str, 
        docs: List[tuple[Document, float]], 
        conflict_info: Dict[str, Any]
    ) -> AgentOutput:
        """Generate answer with reasoning"""
        
        if not docs or docs[0][1] < 0.1:
            return AgentOutput(
                answer="I don't have enough information to answer this question.",
                sources=[],
                confidence=0.0,
                reasoning="No relevant documents found."
            )
        
        # Build answer based on documents
        primary_doc = docs[0][0]
        
        if conflict_info["detected"]:
            # Handle conflict
            resolved_doc = self.resolve_conflict(docs)
            
            # Extract relevant information from resolved document
            answer_parts = []
            
            # Find the answer in the resolved document
            if "report" in question.lower() and "incident" in question.lower():
                # Extract reporting deadline
                hour_match = re.search(r'(\d+)\s*hours?', resolved_doc.content, re.IGNORECASE)
                if hour_match:
                    hours = hour_match.group(1)
                    answer_parts.append(
                        f"Current policy requires reporting within {hours} hours "
                        f"(based on the {resolved_doc.version or 'latest'} procedure)."
                    )
                
                # Mention the outdated version
                other_docs = [doc for doc, _ in docs if doc.uri != resolved_doc.uri]
                if other_docs:
                    old_doc = other_docs[0]
                    old_hour_match = re.search(r'(\d+)\s*hours?', old_doc.content, re.IGNORECASE)
                    if old_hour_match:
                        old_hours = old_hour_match.group(1)
                        answer_parts.append(
                            f"The {old_doc.version or 'previous'} version required {old_hours} hours but is outdated."
                        )
            
            answer = " ".join(answer_parts) if answer_parts else "Policy found with conflicts resolved."
            
            reasoning = (
                f"Detected conflict: "
                f"{', '.join([f'{doc.version or doc.uri}' for doc, _ in docs[:2]])}. "
                f"Favoring most recent version ({resolved_doc.version or resolved_doc.uri})."
            )
            
            sources = [
                Source(
                    title=doc.title,
                    version=doc.version,
                    uri=doc.uri,
                    page=0
                )
                for doc, _ in docs if doc.version is not None
            ]
            
            confidence = min(0.92, docs[0][1] + 0.2)  # High but not perfect due to conflict
            
            return AgentOutput(
                answer=answer,
                sources=sources,
                confidence=confidence,
                reasoning=reasoning,
                conflict_detected=True,
                resolution_strategy="favor_recent_version"
            )
        
        else:
            # No conflict - straightforward answer
            answer_parts = []
            
            if "onboarding" in question.lower():
                # Extract onboarding deadline
                days_match = re.search(r'(\d+)\s*business\s+days?', primary_doc.content, re.IGNORECASE)
                if days_match:
                    days = days_match.group(1)
                    answer_parts.append(f"New employees must complete onboarding within {days} business days.")
            
            answer = " ".join(answer_parts) if answer_parts else "Information found in policy."
            
            reasoning = f"Clear answer found in {primary_doc.title} with no conflicts."
            
            sources = [
                Source(
                    title=doc.title,
                    version=doc.version,
                    uri=doc.uri,
                    page=0
                )
                for doc, _ in docs[:1]
            ]
            
            confidence = min(0.95, docs[0][1] + 0.3)
            
            return AgentOutput(
                answer=answer,
                sources=sources,
                confidence=confidence,
                reasoning=reasoning,
                conflict_detected=False
            )
    
    def process_query(
        self, 
        question: str, 
        top_k: int = 3, 
        min_confidence: float = 0.65
    ) -> tuple[AgentOutput, int]:
        """Process a RAG query"""
        start_time = time.time()
        
        # Retrieve relevant documents
        retrieved_docs = self.retrieve(question, top_k)
        
        # Detect conflicts
        conflict_info = self.detect_conflicts(retrieved_docs, question)
        
        # Generate answer
        output = self.generate_answer(question, retrieved_docs, conflict_info)
        
        # Calculate latency
        latency_ms = int((time.time() - start_time) * 1000)
        
        return output, latency_ms


# Global agent instance
agent = RAGAgent()


@app.get("/")
def root():
    """Health check endpoint"""
    return {
        "status": "ok",
        "service": "RAG Agent API",
        "version": "1.0.0",
        "documents_loaded": len(agent.documents)
    }


@app.post("/run", response_model=AgentResponse)
def run_agent(request: AgentRequest):
    """
    Execute the AI agent with the given task and input.
    
    Supports RAG-based Q&A with conflict detection and resolution.
    """
    try:
        if request.task != "rag_qa":
            raise HTTPException(
                status_code=400, 
                detail=f"Unsupported task: {request.task}. Only 'rag_qa' is supported."
            )
        
        # Process the query
        output, latency_ms = agent.process_query(
            question=request.input.question,
            top_k=request.params.top_k,
            min_confidence=request.params.min_confidence
        )
        
        # Determine status based on confidence
        status = "ok"
        if output.confidence < request.params.min_confidence:
            status = "low_confidence"
            output.answer = (
                f"I found multiple policies in the knowledge base, but your question is too vague. "
                f"Could you please be more specific? Available topics: onboarding, security incidents."
            )
        
        # Calculate token counts (approximation)
        tokens_in = len(request.input.question.split())
        tokens_out = len(output.answer.split())
        
        usage = Usage(
            latency_ms=latency_ms,
            tokens_in=tokens_in,
            tokens_out=tokens_out,
            model="simple-embedding"
        )
        
        return AgentResponse(
            status=status,
            output=output,
            usage=usage,
            error=None
        )
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/documents")
def list_documents():
    """List all loaded documents"""
    return {
        "count": len(agent.documents),
        "documents": [
            {
                "title": doc.title,
                "uri": doc.uri,
                "version": doc.version,
                "content_length": len(doc.content)
            }
            for doc in agent.documents
        ]
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
