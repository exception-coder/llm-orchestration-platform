package com.exceptioncoder.llm.infrastructure.devplan.tool;

import com.exceptioncoder.llm.domain.devplan.model.CodeIndexStatus;
import com.exceptioncoder.llm.domain.devplan.repository.CodeIndexStatusRepository;
import com.exceptioncoder.llm.infrastructure.agent.tool.Tool;
import com.exceptioncoder.llm.infrastructure.agent.tool.ToolParam;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 代码向量索引工具 -- 将项目 Java 源文件按类级别向量化到 Qdrant。
 *
 * <p>索引粒度：类级别，embedding = Javadoc + 类声明 + public 方法签名。
 * 索引去重：基于 file_hash 判断是否需要重建。
 *
 * @author zhangkai
 * @since 2026-04-07
 */
@Slf4j
@Component
public class CodeIndexTool {

    private final VectorStore vectorStore;
    private final CodeIndexStatusRepository indexStatusRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Pattern JAVADOC_PATTERN = Pattern.compile("/\\*\\*([\\s\\S]*?)\\*/\\s*(?:@\\w+\\s*(?:\\([^)]*\\)\\s*)*)*(?:public|protected)\\s+(?:abstract\\s+)?(?:class|interface|enum|record)", Pattern.MULTILINE);
    private static final Pattern CLASS_DECL_PATTERN = Pattern.compile("^public\\s+(?:abstract\\s+)?(?:class|interface|enum|record)\\s+.+$", Pattern.MULTILINE);
    private static final Pattern PUBLIC_METHOD_PATTERN = Pattern.compile("^\\s+public\\s+(?:static\\s+)?[\\w<>\\[\\],\\s]+?\\s+\\w+\\s*\\([^)]*\\)", Pattern.MULTILINE);
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^package\\s+([\\w.]+)\\s*;", Pattern.MULTILINE);
    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("^public\\s+(?:abstract\\s+)?(?:class|interface|enum|record)\\s+(\\w+)", Pattern.MULTILINE);

    public CodeIndexTool(VectorStore vectorStore, CodeIndexStatusRepository indexStatusRepository) {
        this.vectorStore = vectorStore;
        this.indexStatusRepository = indexStatusRepository;
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

            List<Document> documents = new ArrayList<>();
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (!file.toString().endsWith(".java")) return FileVisitResult.CONTINUE;
                    if (file.toString().contains("/test/")) return FileVisitResult.CONTINUE;

                    String content = Files.readString(file);
                    String embeddingText = extractEmbeddingText(content);
                    if (embeddingText.isBlank()) return FileVisitResult.CONTINUE;

                    String packageName = extractPackage(content);
                    String className = extractClassName(content);
                    String relativePath = root.relativize(file).toString();

                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("filePath", relativePath);
                    metadata.put("className", className != null ? className : "Unknown");
                    metadata.put("packageName", packageName != null ? packageName : "");
                    metadata.put("projectPath", projectPath);

                    documents.add(new Document(embeddingText, metadata));
                    return FileVisitResult.CONTINUE;
                }
            });

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

    private String extractEmbeddingText(String content) {
        StringBuilder sb = new StringBuilder();

        // Javadoc
        Matcher javadocMatcher = JAVADOC_PATTERN.matcher(content);
        if (javadocMatcher.find()) {
            sb.append(javadocMatcher.group(1).replaceAll("\\s*\\*\\s*", " ").trim()).append("\n");
        }

        // 类声明
        Matcher classDeclMatcher = CLASS_DECL_PATTERN.matcher(content);
        if (classDeclMatcher.find()) {
            sb.append(classDeclMatcher.group().trim()).append("\n");
        }

        // public 方法签名
        Matcher methodMatcher = PUBLIC_METHOD_PATTERN.matcher(content);
        while (methodMatcher.find()) {
            sb.append(methodMatcher.group().trim()).append("\n");
        }

        return sb.toString();
    }

    private String extractPackage(String content) {
        Matcher m = PACKAGE_PATTERN.matcher(content);
        return m.find() ? m.group(1) : null;
    }

    private String extractClassName(String content) {
        Matcher m = CLASS_NAME_PATTERN.matcher(content);
        return m.find() ? m.group(1) : null;
    }

    private String computeProjectHash(Path root) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.toString().endsWith(".java") && !file.toString().contains("/test/")) {
                    md.update(file.toString().getBytes());
                    md.update(Long.toString(attrs.size()).getBytes());
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
