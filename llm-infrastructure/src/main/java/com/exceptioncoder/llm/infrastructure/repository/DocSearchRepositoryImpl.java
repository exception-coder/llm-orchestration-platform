package com.exceptioncoder.llm.infrastructure.repository;

import com.exceptioncoder.llm.domain.model.DocSearchResult;
import com.exceptioncoder.llm.domain.repository.DocSearchRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文档向量检索实现
 */
@Slf4j
@Repository
public class DocSearchRepositoryImpl implements DocSearchRepository {

    private final VectorStore vectorStore;

    public DocSearchRepositoryImpl(VectorStore docVectorStore) {
        this.vectorStore = docVectorStore;
    }

    @Override
    public void indexDocs(List<String> paths, List<String> contents) {
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < paths.size(); i++) {
            Map<String, Object> metadata = Map.of("path", paths.get(i));
            Document doc = new Document(paths.get(i), contents.get(i), metadata);
            documents.add(doc);
        }
        vectorStore.add(documents);
        log.info("文档索引构建完成: {} 个文档", documents.size());
    }

    @Override
    public DocSearchResult search(String query, int topK) {
        SearchRequest request = SearchRequest.builder().query(query).topK(topK).build();
        List<Document> documents = vectorStore.similaritySearch(request);
        List<DocSearchResult.Hit> hits = documents.stream()
                .map(this::toHit)
                .collect(Collectors.toList());
        return new DocSearchResult(hits, hits.size());
    }

    @Override
    public boolean isHealthy() {
        try {
            vectorStore.similaritySearch(SearchRequest.builder().query("health_check").topK(1).build());
            return true;
        } catch (Exception e) {
            log.warn("Qdrant 健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    private DocSearchResult.Hit toHit(Document doc) {
        String path = doc.getMetadata().containsKey("path")
                ? String.valueOf(doc.getMetadata().get("path"))
                : doc.getId();
        // 截取前200字符作为摘要
        String snippet = doc.getText().length() > 200
                ? doc.getText().substring(0, 200) + "..."
                : doc.getText();
        return new DocSearchResult.Hit(
                path,
                extractFileName(path),
                snippet,
                1.0
        );
    }

    private String extractFileName(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }
}
