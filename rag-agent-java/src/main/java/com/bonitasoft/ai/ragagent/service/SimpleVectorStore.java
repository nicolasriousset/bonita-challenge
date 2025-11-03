package com.bonitasoft.ai.ragagent.service;

import com.bonitasoft.ai.ragagent.model.Document;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Simple in-memory Vector Store implementation
 * Uses TF-IDF for document vectorization and cosine similarity for retrieval
 */
@Slf4j
@Component
public class SimpleVectorStore {

    private final List<Document> documents = new ArrayList<>();
    private final Map<String, Map<String, Double>> tfidfVectors = new HashMap<>();
    private final Map<String, Integer> documentFrequency = new HashMap<>();
    private int totalDocuments = 0;

    /**
     * Add a document to the vector store
     */
    public void addDocument(Document document) {
        documents.add(document);
        totalDocuments++;
        
        // Update document frequency
        Set<String> uniqueTerms = extractTerms(document.getContent());
        for (String term : uniqueTerms) {
            documentFrequency.merge(term, 1, Integer::sum);
        }
        
        // Recompute TF-IDF vectors for all documents
        recomputeTfidfVectors();
        
        log.debug("Added document to vector store: {}", document.getTitle());
    }

    /**
     * Search for similar documents using cosine similarity
     */
    public List<Document> search(String query, int topK) {
        if (documents.isEmpty()) {
            return Collections.emptyList();
        }

        // Create query vector
        Map<String, Double> queryVector = createTfidfVector(query);

        // Calculate cosine similarity with all documents
        List<DocumentScore> scores = new ArrayList<>();
        for (Document doc : documents) {
            double similarity = cosineSimilarity(queryVector, tfidfVectors.get(doc.getId()));
            scores.add(new DocumentScore(doc, similarity));
        }

        // Sort by similarity and return top K
        return scores.stream()
                .sorted(Comparator.comparingDouble(DocumentScore::getScore).reversed())
                .limit(topK)
                .map(DocumentScore::getDocument)
                .collect(Collectors.toList());
    }

    /**
     * Get all documents in the store
     */
    public List<Document> getAllDocuments() {
        return new ArrayList<>(documents);
    }

    /**
     * Clear all documents from the store
     */
    public void clear() {
        documents.clear();
        tfidfVectors.clear();
        documentFrequency.clear();
        totalDocuments = 0;
    }

    /**
     * Recompute TF-IDF vectors for all documents
     */
    private void recomputeTfidfVectors() {
        tfidfVectors.clear();
        for (Document doc : documents) {
            tfidfVectors.put(doc.getId(), createTfidfVector(doc.getContent()));
        }
    }

    /**
     * Create TF-IDF vector for text
     */
    private Map<String, Double> createTfidfVector(String text) {
        Map<String, Double> vector = new HashMap<>();
        List<String> terms = extractTermsList(text);
        
        if (terms.isEmpty()) {
            return vector;
        }

        // Calculate term frequency (TF)
        Map<String, Integer> termCounts = new HashMap<>();
        for (String term : terms) {
            termCounts.merge(term, 1, Integer::sum);
        }

        // Calculate TF-IDF for each term
        for (Map.Entry<String, Integer> entry : termCounts.entrySet()) {
            String term = entry.getKey();
            int count = entry.getValue();
            
            double tf = (double) count / terms.size();
            double idf = Math.log((double) (totalDocuments + 1) / 
                                 (documentFrequency.getOrDefault(term, 0) + 1));
            double tfidf = tf * idf;
            
            vector.put(term, tfidf);
        }

        return normalizeVector(vector);
    }

    /**
     * Normalize vector to unit length
     */
    private Map<String, Double> normalizeVector(Map<String, Double> vector) {
        double magnitude = Math.sqrt(
            vector.values().stream()
                  .mapToDouble(v -> v * v)
                  .sum()
        );

        if (magnitude == 0) {
            return vector;
        }

        Map<String, Double> normalized = new HashMap<>();
        for (Map.Entry<String, Double> entry : vector.entrySet()) {
            normalized.put(entry.getKey(), entry.getValue() / magnitude);
        }
        return normalized;
    }

    /**
     * Calculate cosine similarity between two vectors
     */
    private double cosineSimilarity(Map<String, Double> v1, Map<String, Double> v2) {
        if (v1 == null || v2 == null || v1.isEmpty() || v2.isEmpty()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        for (Map.Entry<String, Double> entry : v1.entrySet()) {
            String term = entry.getKey();
            if (v2.containsKey(term)) {
                dotProduct += entry.getValue() * v2.get(term);
            }
        }

        return dotProduct; // Vectors are already normalized
    }

    /**
     * Extract terms from text (tokenization + normalization)
     */
    private Set<String> extractTerms(String text) {
        return new HashSet<>(extractTermsList(text));
    }

    /**
     * Extract terms as list (for TF calculation)
     */
    private List<String> extractTermsList(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(text.toLowerCase()
                        .replaceAll("[^a-z0-9\\s]", " ")
                        .split("\\s+"))
                .filter(term -> term.length() > 2) // Remove short terms
                .filter(term -> !STOP_WORDS.contains(term)) // Remove stop words
                .collect(Collectors.toList());
    }

    /**
     * Common English stop words
     */
    private static final Set<String> STOP_WORDS = Set.of(
        "the", "and", "for", "are", "but", "not", "you", "all", "can", "her", "was", 
        "one", "our", "out", "day", "get", "has", "him", "his", "how", "its", "may",
        "she", "who", "will", "with", "this", "that", "from", "have", "been", "they",
        "what", "when", "your", "more", "into", "than", "some", "time", "very", "would"
    );

    /**
     * Helper class to store document with similarity score
     */
    private static class DocumentScore {
        private final Document document;
        private final double score;

        public DocumentScore(Document document, double score) {
            this.document = document;
            this.score = score;
        }

        public Document getDocument() {
            return document;
        }

        public double getScore() {
            return score;
        }
    }
}
