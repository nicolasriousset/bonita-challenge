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
 * RAG Service with conflict detection and resolution
 */
@Slf4j
@Service
public class RagService {

    @Value("${rag.confidence-threshold:0.65}")
    private double confidenceThreshold;

    @Value("${rag.max-sources:5}")
    private int maxSources;

    private final ObjectMapper objectMapper;
    private final List<Document> documents = new ArrayList<>();

    public RagService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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
                    
                    documents.add(doc);
                    log.info("Loaded document: {}", doc.getTitle());
                } catch (Exception e) {
                    log.error("Error loading document {}: {}", resource.getFilename(), e.getMessage());
                }
            }
            
            log.info("Loaded {} documents", documents.size());
        } catch (IOException e) {
            log.error("Error loading documents: {}", e.getMessage());
        }
    }

    /**
     * Process RAG query with conflict detection
     */
    public AgentResponse processQuery(String question) {
        log.info("Processing query: {}", question);

        // Retrieve relevant documents
        List<DocumentScore> scoredDocs = documents.stream()
            .map(doc -> new DocumentScore(doc, doc.calculateSimilarity(question)))
            .filter(ds -> ds.score > 0.1) // Minimum relevance threshold
            .sorted(Comparator.comparingDouble(DocumentScore::getScore).reversed())
            .limit(maxSources)
            .collect(Collectors.toList());

        if (scoredDocs.isEmpty()) {
            return AgentResponse.builder()
                .status("ok")
                .output(Map.of(
                    "answer", "No relevant information found",
                    "confidence", 0.0,
                    "sources", List.of()
                ))
                .usage(Map.of("documentsSearched", documents.size()))
                .build();
        }

        // Detect conflicts
        ConflictDetectionResult conflictResult = detectConflicts(scoredDocs, question);

        // Build response
        Map<String, Object> output = new HashMap<>();
        output.put("answer", generateAnswer(scoredDocs, conflictResult));
        output.put("confidence", calculateConfidence(scoredDocs, conflictResult));
        output.put("sources", buildSources(scoredDocs));

        AgentResponse.AgentResponseBuilder responseBuilder = AgentResponse.builder()
            .status("ok")
            .output(output)
            .usage(Map.of(
                "documentsSearched", documents.size(),
                "relevantDocuments", scoredDocs.size()
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
    private ConflictDetectionResult detectConflicts(List<DocumentScore> scoredDocs, String question) {
        if (scoredDocs.size() < 2) {
            return new ConflictDetectionResult(false, List.of(), "");
        }

        // Group documents by category
        Map<String, List<DocumentScore>> byCategory = scoredDocs.stream()
            .collect(Collectors.groupingBy(ds -> ds.document.getCategory()));

        // Check for conflicts within same category
        for (Map.Entry<String, List<DocumentScore>> entry : byCategory.entrySet()) {
            List<DocumentScore> categoryDocs = entry.getValue();
            if (categoryDocs.size() > 1) {
                // Check if documents have different dates
                Set<LocalDate> dates = categoryDocs.stream()
                    .map(ds -> ds.document.getDate())
                    .collect(Collectors.toSet());

                if (dates.size() > 1) {
                    List<String> conflictingSources = categoryDocs.stream()
                        .map(ds -> ds.document.getTitle() + " (" + ds.document.getDate() + ")")
                        .collect(Collectors.toList());

                    String reasoning = String.format(
                        "Multiple versions of %s policy found. Using most recent version from %s.",
                        entry.getKey(),
                        categoryDocs.get(0).document.getDate()
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
    private String generateAnswer(List<DocumentScore> scoredDocs, ConflictDetectionResult conflictResult) {
        if (conflictResult.hasConflict) {
            // Use most recent document
            Document mostRecent = scoredDocs.stream()
                .max(Comparator.comparing(ds -> ds.document.getDate()))
                .map(ds -> ds.document)
                .orElse(scoredDocs.get(0).document);

            return extractRelevantContent(mostRecent);
        }

        // Use highest scoring document
        return extractRelevantContent(scoredDocs.get(0).document);
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
    private double calculateConfidence(List<DocumentScore> scoredDocs, ConflictDetectionResult conflictResult) {
        if (scoredDocs.isEmpty()) {
            return 0.0;
        }

        double baseConfidence = scoredDocs.get(0).score;

        // Reduce confidence if conflict detected
        if (conflictResult.hasConflict) {
            return Math.max(0.5, baseConfidence * 0.8);
        }

        return Math.min(0.95, baseConfidence);
    }

    /**
     * Build sources list
     */
    private List<Map<String, Object>> buildSources(List<DocumentScore> scoredDocs) {
        return scoredDocs.stream()
            .map(ds -> Map.<String, Object>of(
                "title", ds.document.getTitle(),
                "date", ds.document.getDate().toString(),
                "version", ds.document.getVersion(),
                "relevance", Math.round(ds.score * 100.0) / 100.0
            ))
            .collect(Collectors.toList());
    }

    // Helper classes
    private static class DocumentScore {
        final Document document;
        final double score;

        DocumentScore(Document document, double score) {
            this.document = document;
            this.score = score;
        }

        public double getScore() {
            return score;
        }
    }

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
