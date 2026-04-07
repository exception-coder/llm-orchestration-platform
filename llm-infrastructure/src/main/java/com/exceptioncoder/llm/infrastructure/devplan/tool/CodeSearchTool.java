package com.exceptioncoder.llm.infrastructure.devplan.tool;

import com.exceptioncoder.llm.infrastructure.agent.tool.Tool;
import com.exceptioncoder.llm.infrastructure.agent.tool.ToolParam;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 代码语义搜索工具 -- 基于 VectorStore 在已索引代码中检索相关类。
 *
 * <p>REQUIREMENT_ANALYZER 和 SOLUTION_ARCHITECT 共用。
 * 降级策略：VectorStore 不可用时返回空结果 + WARN 日志。
 *
 * @author zhangkai
 * @since 2026-04-07
 */
@Slf4j
@Component
public class CodeSearchTool {

    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CodeSearchTool(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Tool(name = "devplan_code_search", description = "语义搜索已索引的代码库", tags = {"devplan", "search"})
    public String search(
            @ToolParam(value = "query", description = "搜索查询文本") String query,
            @ToolParam(value = "topK", description = "返回结果数量", required = false, defaultValue = "\"5\"") String topK
    ) {
        try {
            int k = parseIntSafe(topK, 5);

            List<Document> results = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(query)
                            .topK(k)
                            .build()
            );

            List<Map<String, Object>> output = new ArrayList<>();
            for (Document doc : results) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("className", doc.getMetadata().getOrDefault("className", "Unknown"));
                item.put("filePath", doc.getMetadata().getOrDefault("filePath", ""));
                item.put("packageName", doc.getMetadata().getOrDefault("packageName", ""));
                item.put("snippet", truncate(doc.getText(), 500));
                output.add(item);
            }

            return objectMapper.writeValueAsString(output);
        } catch (Exception e) {
            log.warn("CodeSearchTool 执行失败（降级为空结果）: {}", e.getMessage());
            return "[]";
        }
    }

    private int parseIntSafe(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
