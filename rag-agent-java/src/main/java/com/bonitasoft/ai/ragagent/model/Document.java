package com.bonitasoft.ai.ragagent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Document representation for RAG
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Builder.Default
    private String id = UUID.randomUUID().toString();
    private String title;
    private String content;
    private LocalDate date;
    private String version;
    private String category;

    /**
     * Calculate similarity score with a query (simple text matching)
     */
    public double calculateSimilarity(String query) {
        if (query == null || query.isEmpty()) {
            return 0.0;
        }

        String queryLower = query.toLowerCase();
        String contentLower = content.toLowerCase();
        String titleLower = title.toLowerCase();

        // Count matching words
        String[] queryWords = queryLower.split("\\s+");
        int matches = 0;
        int totalWords = queryWords.length;

        for (String word : queryWords) {
            if (word.length() > 2) { // Ignore short words
                if (contentLower.contains(word) || titleLower.contains(word)) {
                    matches++;
                }
            }
        }

        return totalWords > 0 ? (double) matches / totalWords : 0.0;
    }
}
