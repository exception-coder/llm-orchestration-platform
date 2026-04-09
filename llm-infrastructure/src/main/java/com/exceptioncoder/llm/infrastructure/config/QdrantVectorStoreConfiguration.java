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
 * 手动创建三个 collection 的 VectorStore Bean：
 * 1. jobPostingsVectorStore（@Primary）- 岗位向量检索
 * 2. docVectorStore - 文档向量检索
 * 3. profileVectorStore - 项目画像跨项目检索
 *
 * <p>spring-ai 1.0.1+ 使用 Builder 模式创建 QdrantVectorStore，
 * 每个 Bean 独立 QdrantClient 实例避免连接池冲突。
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.ai.vectorstore.qdrant", name = "enabled", havingValue = "true", matchIfMissing = false)
public class QdrantVectorStoreConfiguration {

    @Value("${spring.ai.vectorstore.qdrant.host:localhost}")
    private String host;

    @Value("${spring.ai.vectorstore.qdrant.port:6334}")
    private int port;

    @Value("${spring.ai.vectorstore.qdrant.api-key:}")
    private String apiKey;

    @Value("${spring.ai.vectorstore.qdrant.use-tls:false}")
    private boolean useTls;

    @Value("${spring.ai.vectorstore.qdrant.collection-name:jobPostings}")
    private String jobPostingsCollection;

    @Value("${spring.ai.vectorstore-qdrant-docs.collection-name:docs_vectors}")
    private String docsCollection;

    @Value("${spring.ai.vectorstore-qdrant-profile.collection-name:project_profile}")
    private String profileCollection;

    @Value("${spring.ai.vectorstore.qdrant.initialize-schema:false}")
    private boolean initializeSchema;

    @Bean
    @Primary
    public VectorStore jobPostingsVectorStore(EmbeddingModel embeddingModel) {
        return QdrantVectorStore.builder(createClient(), embeddingModel)
                .collectionName(jobPostingsCollection)
                .initializeSchema(initializeSchema)
                .build();
    }

    @Bean
    public VectorStore docVectorStore(EmbeddingModel embeddingModel) {
        return QdrantVectorStore.builder(createClient(), embeddingModel)
                .collectionName(docsCollection)
                .initializeSchema(initializeSchema)
                .build();
    }

    @Bean
    public VectorStore profileVectorStore(EmbeddingModel embeddingModel) {
        return QdrantVectorStore.builder(createClient(), embeddingModel)
                .collectionName(profileCollection)
                .initializeSchema(initializeSchema)
                .build();
    }

    private QdrantClient createClient() {
        QdrantGrpcClient.Builder builder = QdrantGrpcClient.newBuilder(host, port, useTls);
        if (apiKey != null && !apiKey.isBlank()) {
            builder.withApiKey(apiKey);
        }
        return new QdrantClient(builder.build());
    }
}
