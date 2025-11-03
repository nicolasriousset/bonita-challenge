package com.bonitasoft.ai.ragagent.service;

import com.bonitasoft.ai.ragagent.model.AgentResponse;
import com.bonitasoft.ai.ragagent.model.Document;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG Service with Vector Store, conflict detection and resolution
 */
@Slf4j
@Service
public class RagService {

    @Value("${rag.confidence-threshold:0.65}")
    private double confidenceThreshold;

    @Value("${rag.max-sources:5}")
    private int maxSources;

    private final ObjectMapper objectMapper;
    private final SimpleVectorStore vectorStore;

    public RagService(ObjectMapper objectMapper, SimpleVectorStore vectorStore) {
        this.objectMapper = objectMapper;
        this.vectorStore = vectorStore;
    }

    @PostConstruct
    public void loadDocuments() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:documents/*.json");

            for (Resource resource : resources) {
                try {
                    Map<String, Object> docData = objectMapper.readValue(
                        resource.getInputStream(), 
                        new TypeReference<Map<String, Object>>() {}
                    );
                    
                    Document doc = Document.builder()
                        .title((String) docData.get("title"))
                        .content((String) docData.get("content"))
                        .date(LocalDate.parse((String) docData.get("date")))
                        .version((String) docData.get("version"))
                        .category((String) docData.get("category"))
                        .build();
                    
                    // Add document to vector store
                    vectorStore.addDocument(doc);
                    log.info("Loaded document into vector store: {}", doc.getTitle());
                } catch (Exception e) {
                    log.error("Error loading document {}: {}", resource.getFilename(), e.getMessage());
                }
            }
            
            log.info("Loaded {} documents into vector store", vectorStore.getAllDocuments().size());
        } catch (IOException e) {
            log.error("Error loading documents: {}", e.getMessage());
        }
    }

    /**
     * Process RAG query with vector search and conflict detection
     */
    public AgentResponse processQuery(String question) {
        log.info("Processing query: {}", question);

        // Retrieve relevant documents using vector search
        List<Document> relevantDocs = vectorStore.search(question, maxSources);
        log.debug("Found {} relevant documents using vector search", relevantDocs.size());

        if (relevantDocs.isEmpty()) {
            return AgentResponse.builder()
                .status("ok")
                .output(Map.of(
                    "answer", "No relevant information found",
                    "confidence", 0.0,
                    "sources", List.of()
                ))
                .usage(Map.of("documentsSearched", vectorStore.getAllDocuments().size()))
                .build();
        }

        // Detect conflicts
        ConflictDetectionResult conflictResult = detectConflicts(relevantDocs, question);

        // Build response
        Map<String, Object> output = new HashMap<>();
        output.put("answer", generateAnswer(relevantDocs, conflictResult));
        output.put("confidence", calculateConfidence(relevantDocs, conflictResult));
        output.put("sources", buildSources(relevantDocs));

        AgentResponse.AgentResponseBuilder responseBuilder = AgentResponse.builder()
            .status("ok")
            .output(output)
            .usage(Map.of(
                "documentsSearched", vectorStore.getAllDocuments().size(),
                "relevantDocuments", relevantDocs.size()
            ));

        if (conflictResult.hasConflict) {
            responseBuilder.conflictInfo(AgentResponse.ConflictInfo.builder()
                .detected(true)
                .conflictingSources(conflictResult.conflictingSources)
                .resolutionStrategy("most_recent")
                .reasoning(conflictResult.reasoning)
                .build());
        }

        return responseBuilder.build();
    }

    /**
     * Detect conflicts between documents
     */
    private ConflictDetectionResult detectConflicts(List<Document> docs, String question) {
        if (docs.size() < 2) {
            return new ConflictDetectionResult(false, List.of(), "");
        }

        // Group documents by category
        Map<String, List<Document>> byCategory = docs.stream()
            .collect(Collectors.groupingBy(Document::getCategory));

        // Check for conflicts within same category
        for (Map.Entry<String, List<Document>> entry : byCategory.entrySet()) {
            List<Document> categoryDocs = entry.getValue();
            if (categoryDocs.size() > 1) {
                // Check if documents have different dates
                Set<LocalDate> dates = categoryDocs.stream()
                    .map(Document::getDate)
                    .collect(Collectors.toSet());

                if (dates.size() > 1) {
                    List<String> conflictingSources = categoryDocs.stream()
                        .map(doc -> doc.getTitle() + " (" + doc.getDate() + ")")
                        .collect(Collectors.toList());

                    String reasoning = String.format(
                        "Multiple versions of %s policy found. Using most recent version from %s.",
                        entry.getKey(),
                        categoryDocs.get(0).getDate()
                    );

                    return new ConflictDetectionResult(true, conflictingSources, reasoning);
                }
            }
        }

        return new ConflictDetectionResult(false, List.of(), "");
    }

    /**
     * Generate answer from documents
     */
    private String generateAnswer(List<Document> docs, ConflictDetectionResult conflictResult) {
        if (conflictResult.hasConflict) {
            // Use most recent document
            Document mostRecent = docs.stream()
                .max(Comparator.comparing(Document::getDate))
                .orElse(docs.get(0));

            return extractRelevantContent(mostRecent);
        }

        // Use highest scoring document (first in list from vector search)
        return extractRelevantContent(docs.get(0));
    }

    /**
     * Extract relevant content from document
     */
    private String extractRelevantContent(Document doc) {
        // Simple extraction - return first 200 chars
        String content = doc.getContent();
        if (content.length() > 200) {
            return content.substring(0, 200) + "...";
        }
        return content;
    }

    /**
     * Calculate confidence score
     */
    private double calculateConfidence(List<Document> docs, ConflictDetectionResult conflictResult) {
        if (docs.isEmpty()) {
            return 0.0;
        }

        // Base confidence on number of relevant documents
        double baseConfidence = Math.min(0.9, 0.5 + (docs.size() * 0.1));

        // Reduce confidence if conflict detected
        if (conflictResult.hasConflict) {
            return Math.max(0.5, baseConfidence * 0.8);
        }

        return baseConfidence;
    }

    /**
     * Build sources list
     */
    private List<Map<String, Object>> buildSources(List<Document> docs) {
        return docs.stream()
            .map(doc -> {
                Map<String, Object> source = new HashMap<>();
                source.put("title", doc.getTitle());
                source.put("date", doc.getDate().toString());
                source.put("version", doc.getVersion());
                return source;
            })
            .collect(Collectors.toList());
    }

    // Helper class
    private static class ConflictDetectionResult {
        final boolean hasConflict;
        final List<String> conflictingSources;
        final String reasoning;

        ConflictDetectionResult(boolean hasConflict, List<String> conflictingSources, String reasoning) {
            this.hasConflict = hasConflict;
            this.conflictingSources = conflictingSources;
            this.reasoning = reasoning;
        }
    }
}
