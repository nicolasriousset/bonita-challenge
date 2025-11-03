package com.bonitasoft.ai.ragagent.config;

import com.bonitasoft.ai.ragagent.service.SimpleVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Vector Store
 * Uses a simple in-memory vector store with TF-IDF embeddings
 */
@Configuration
public class VectorStoreConfig {

    /**
     * Configure the in-memory vector store
     */
    @Bean
    public SimpleVectorStore vectorStore() {
        return new SimpleVectorStore();
    }
}
