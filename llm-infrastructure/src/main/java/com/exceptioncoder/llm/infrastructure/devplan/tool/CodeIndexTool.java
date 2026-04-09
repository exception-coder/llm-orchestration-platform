package com.exceptioncoder.llm.infrastructure.devplan.tool;

import com.exceptioncoder.llm.domain.devplan.analysis.EmbeddingText;
import com.exceptioncoder.llm.domain.devplan.analysis.LanguageAnalyzer;
import com.exceptioncoder.llm.domain.devplan.analysis.LanguageAnalyzerRegistry;
import com.exceptioncoder.llm.domain.devplan.model.CodeIndexStatus;
import com.exceptioncoder.llm.domain.devplan.repository.CodeIndexStatusRepository;
import com.exceptioncoder.llm.infrastructure.agent.tool.Tool;
import com.exceptioncoder.llm.infrastructure.agent.tool.ToolParam;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 代码向量索引工具 -- 委托 {@link LanguageAnalyzer} SPI 提取嵌入文本，写入 Qdrant。
 *
 * <p>替代原正则提取嵌入文本方案，通过 JavaParser AST 精确提取
 * Javadoc + 类声明 + public 方法签名，生成更高质量的向量索引。
 *
 * <p><b>归属智能体：</b>开发计划智能体（devplan）
 * <br><b>归属 Agent：</b>代码感知分析专家（devplan-code-awareness）
 * <br><b>调用阶段：</b>第一阶段 — 代码感知，在结构扫描完成后调用
 * <br><b>业务场景：</b>将项目源码按类级别向量化写入 Qdrant，为后续需求分析和方案设计阶段
 * 提供语义搜索能力。需求分析专家和方案架构师通过 code_search 检索相关类时，
 * 依赖本工具预先建立的向量索引。
 *
 * @author zhangkai
 * @since 2026-04-07
 */
@Slf4j
@Component
public class CodeIndexTool {

    private final VectorStore vectorStore;
    private final CodeIndexStatusRepository indexStatusRepository;
    private final LanguageAnalyzerRegistry analyzerRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CodeIndexTool(VectorStore vectorStore,
                         CodeIndexStatusRepository indexStatusRepository,
                         LanguageAnalyzerRegistry analyzerRegistry) {
        this.vectorStore = vectorStore;
        this.indexStatusRepository = indexStatusRepository;
        this.analyzerRegistry = analyzerRegistry;
    }

    @Tool(name = "devplan_code_index", description = "将项目Java源文件向量化索引到Qdrant", tags = {"devplan", "index"})
    public String indexIfNeeded(
            @ToolParam(value = "projectPath", description = "项目根目录") String projectPath,
            @ToolParam(value = "forceReindex", description = "是否强制重建索引", required = false, defaultValue = "\"false\"") String forceReindex
    ) {
        try {
            Path root = Path.of(projectPath);
            if (!Files.isDirectory(root)) {
                return errorJson("路径不存在: " + projectPath);
            }

            boolean force = "true".equalsIgnoreCase(forceReindex);
            String collectionName = "devplan_" + hashString(projectPath).substring(0, 8);

            // 计算文件 hash
            String fileHash = computeProjectHash(root);

            // 检查是否需要重建
            Optional<CodeIndexStatus> existing = indexStatusRepository.findByProjectPath(projectPath);
            if (!force && existing.isPresent() && existing.get().isReady()
                    && fileHash.equals(existing.get().fileHash())) {
                log.info("索引已是最新，跳过: projectPath={}", projectPath);
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("collectionName", existing.get().collectionName());
                result.put("docCount", existing.get().docCount());
                result.put("status", "READY");
                result.put("skipped", true);
                return objectMapper.writeValueAsString(result);
            }

            // 开始索引
            indexStatusRepository.save(new CodeIndexStatus(
                    projectPath, collectionName, 0, "INDEXING", null, fileHash));

            // 委托 LanguageAnalyzer 提取嵌入文本
            List<Document> documents = new ArrayList<>();
            Optional<LanguageAnalyzer> analyzer = analyzerRegistry.detect(root);
            if (analyzer.isPresent()) {
                List<EmbeddingText> texts = analyzer.get().extractEmbeddingTexts(root);
                for (EmbeddingText et : texts) {
                    Map<String, Object> metadata = new HashMap<>(et.metadata());
                    metadata.put("projectPath", projectPath);
                    documents.add(new Document(et.text(), metadata));
                }
            } else {
                log.warn("未匹配分析器，跳过嵌入文本提取: {}", projectPath);
            }

            // 批量写入 VectorStore
            if (!documents.isEmpty()) {
                vectorStore.add(documents);
            }

            // 更新状态
            CodeIndexStatus status = new CodeIndexStatus(
                    projectPath, collectionName, documents.size(),
                    "READY", LocalDateTime.now(), fileHash);
            indexStatusRepository.save(status);

            log.info("索引完成: projectPath={}, docCount={}", projectPath, documents.size());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("collectionName", collectionName);
            result.put("docCount", documents.size());
            result.put("status", "READY");
            result.put("skipped", false);
            return objectMapper.writeValueAsString(result);

        } catch (Exception e) {
            log.warn("CodeIndexTool 执行失败（降级为空结果）", e);
            indexStatusRepository.save(new CodeIndexStatus(
                    projectPath, "", 0, "FAILED", null, ""));
            return errorJson("索引失败: " + e.getMessage());
        }
    }

    private String computeProjectHash(Path root) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                String path = file.toString().replace('\\', '/');
                if (path.endsWith(".java") && !path.contains("/test/")) {
                    md.update(path.getBytes());
                    md.update(Long.toString(attrs.size()).getBytes());
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                String name = dir.getFileName().toString();
                if (name.equals("target") || name.equals("build") || name.equals(".git")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return HexFormat.of().formatHex(md.digest());
    }

    private String hashString(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(md.digest(input.getBytes()));
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }

    private String errorJson(String message) {
        return "{\"error\": \"" + message.replace("\"", "\\\"") + "\"}";
    }
}
