package com.exceptioncoder.llm.infrastructure.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Qdrant VectorStore 配置
 * 手动创建两个 collection 的 VectorStore Bean：
 * 1. jobPostingsVectorStore（@Primary）- 岗位向量检索
 * 2. docVectorStore - 文档向量检索
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.ai.vectorstore.qdrant", name = "enabled", havingValue = "true", matchIfMissing = false)
public class QdrantVectorStoreConfiguration {

    @Value("${spring.ai.vectorstore.qdrant.host:localhost}")
    private String host;

    @Value("${spring.ai.vectorstore.qdrant.port:6334}")
    private int port;

    @Value("${spring.ai.vectorstore.qdrant.collection-name:jobPostings}")
    private String jobPostingsCollection;

    @Value("${spring.ai.vectorstore-qdrant-docs.collection-name:docs_vectors}")
    private String docsCollection;

    @Bean
    @Primary
    public VectorStore jobPostingsVectorStore(EmbeddingModel embeddingModel) {
        QdrantClient client = new QdrantClient(
                QdrantGrpcClient.newBuilder(host, port, false).build());
        return new QdrantVectorStore(client, jobPostingsCollection, embeddingModel, true);
    }

    @Bean
    public VectorStore docVectorStore(EmbeddingModel embeddingModel) {
        QdrantClient client = new QdrantClient(
                QdrantGrpcClient.newBuilder(host, port, false).build());
        return new QdrantVectorStore(client, docsCollection, embeddingModel, true);
    }
}
