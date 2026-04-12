package com.exceptioncoder.llm.infrastructure.devplan.tool;

import com.exceptioncoder.llm.domain.devplan.model.AgentRole;
import com.exceptioncoder.llm.domain.devplan.model.ProfileDimension;
import com.exceptioncoder.llm.domain.devplan.model.ProfileIndexStatus;
import com.exceptioncoder.llm.domain.devplan.repository.ProfileIndexStatusRepository;
import com.exceptioncoder.llm.domain.devplan.service.ProfileIndexService;
import com.exceptioncoder.llm.infrastructure.agent.tool.Tool;
import com.exceptioncoder.llm.infrastructure.agent.tool.ToolParam;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 项目画像向量索引工具 -- 将 ProjectProfile 按 7 维度分片 upsert 到 Qdrant。
 *
 * <p>索引粒度：项目×维度，每个维度一条向量记录。
 * 元数据包含 projectName、projectPath、dimension，支持精确过滤 + 语义检索。
 * 通过 contentHash 判断是否需要更新，避免无变更时重复写入。
 *
 * <p><b>归属智能体：</b>开发计划智能体（devplan）
 * <br><b>归属 Agent：</b>代码感知分析专家（devplan-code-awareness）
 * <br><b>调用阶段：</b>第一阶段 — 代码感知，在 LLM 生成 ProjectProfile 之后调用
 * <br><b>业务场景：</b>代码感知专家扫描完项目后，LLM 会总结出 7 维度的项目画像
 * （概览、技术栈、代码结构、API、数据模型、架构规范、配置）。本工具将画像
 * 按维度分片写入 Qdrant，使后续需求分析和方案设计阶段可通过 profile_search
 * 语义检索项目画像，支持跨项目对比和复用参考。
 *
 * @author zhangkai
 * @since 2026-04-08
 */
@Slf4j
@Component
public class ProfileIndexTool implements ProfileIndexService {

    private final VectorStore profileVectorStore;
    private final ProfileIndexStatusRepository indexStatusRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 维度 JSON key → ProfileDimension 枚举的映射。
     * ProjectProfile JSON 中的 key 必须与此映射一致。
     */
    private static final Map<String, ProfileDimension> DIMENSION_KEY_MAP = Map.of(
            "overview", ProfileDimension.OVERVIEW,
            "techStack", ProfileDimension.TECH_STACK,
            "codeStructure", ProfileDimension.CODE_STRUCTURE,
            "api", ProfileDimension.API,
            "dataModel", ProfileDimension.DATA_MODEL,
            "archSpec", ProfileDimension.ARCH_SPEC,
            "config", ProfileDimension.CONFIG
    );

    public ProfileIndexTool(@Qualifier("profileVectorStore") VectorStore profileVectorStore,
                            ProfileIndexStatusRepository indexStatusRepository) {
        this.profileVectorStore = profileVectorStore;
        this.indexStatusRepository = indexStatusRepository;
    }

    @Tool(name = "devplan_profile_index",
            description = "将ProjectProfile按7维度分片向量化到Qdrant，支持跨项目语义检索",
            tags = {"devplan", "index"}, roles = {AgentRole.CODE_AWARENESS})
    public String indexProfile(
            @ToolParam(value = "projectPath", description = "项目根目录绝对路径") String projectPath,
            @ToolParam(value = "projectName", description = "项目名称，用于跨项目过滤") String projectName,
            @ToolParam(value = "profileJson", description = "LLM生成的ProjectProfile JSON字符串，包含7个维度key") String profileJson
    ) {
        try {
            Map<String, Object> profile = objectMapper.readValue(
                    profileJson, new TypeReference<>() {});

            List<Document> documents = new ArrayList<>();
            List<ProfileIndexStatus> statuses = new ArrayList<>();
            int indexedCount = 0;
            int skippedCount = 0;

            for (var entry : DIMENSION_KEY_MAP.entrySet()) {
                String jsonKey = entry.getKey();
                ProfileDimension dimension = entry.getValue();

                Object dimensionValue = profile.get(jsonKey);
                if (dimensionValue == null) {
                    log.debug("维度 {} 在 profileJson 中不存在，标记 unavailable", jsonKey);
                    statuses.add(new ProfileIndexStatus(
                            projectPath, projectName, dimension,
                            "FAILED", null, ""));
                    continue;
                }

                // 将维度内容转为文本用于 embedding
                String content = dimensionValue instanceof String
                        ? (String) dimensionValue
                        : objectMapper.writeValueAsString(dimensionValue);

                String contentHash = hashString(content);

                // 检查是否需要更新
                Optional<ProfileIndexStatus> existing =
                        indexStatusRepository.findByProjectPathAndDimension(projectPath, dimension);
                if (existing.isPresent() && existing.get().isReady()
                        && contentHash.equals(existing.get().contentHash())) {
                    skippedCount++;
                    continue;
                }

                // 构建 embedding 文本：维度标签 + 项目名 + 内容
                String embeddingText = dimension.label() + " - " + projectName + "\n" + content;

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("projectName", projectName);
                metadata.put("projectPath", projectPath);
                metadata.put("dimension", dimension.name());
                metadata.put("dimensionLabel", dimension.label());

                // 使用 projectPath::dimension 作为 document id 实现 upsert
                String docId = hashString(projectPath + "::" + dimension.name());
                Document doc = new Document(docId, embeddingText, metadata);
                documents.add(doc);

                statuses.add(new ProfileIndexStatus(
                        projectPath, projectName, dimension,
                        "READY", LocalDateTime.now(), contentHash));
                indexedCount++;
            }

            // 批量写入 VectorStore
            if (!documents.isEmpty()) {
                profileVectorStore.add(documents);
            }

            // 批量更新状态
            if (!statuses.isEmpty()) {
                indexStatusRepository.saveAll(statuses);
            }

            log.info("画像索引完成: project={}, indexed={}, skipped={}",
                    projectName, indexedCount, skippedCount);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("projectName", projectName);
            result.put("projectPath", projectPath);
            result.put("collection", "project_profile");
            result.put("indexedDimensions", indexedCount);
            result.put("skippedDimensions", skippedCount);
            result.put("totalDimensions", ProfileDimension.values().length);
            return objectMapper.writeValueAsString(result);

        } catch (Exception e) {
            log.error("ProfileIndexTool 执行失败: project={}", projectPath, e);
            return errorJson("画像索引失败: " + e.getMessage());
        }
    }

    /**
     * 从 Markdown 按 {@code ## } 标题分片写入 Qdrant（v2 新增入口）。
     *
     * @param markdown project-profile.md 的完整内容
     * @param projectPath 项目根目录绝对路径
     */
    public void indexFromMarkdown(String markdown, String projectPath) {
        try {
            var reader = new com.exceptioncoder.llm.infrastructure.devplan.profile.ProfileMarkdownReader();
            Map<ProfileDimension, String> dimensions = reader.parse(markdown);
            String projectName = extractProjectName(projectPath);

            List<Document> documents = new ArrayList<>();
            List<ProfileIndexStatus> statuses = new ArrayList<>();
            int indexedCount = 0;
            int skippedCount = 0;

            for (var entry : dimensions.entrySet()) {
                ProfileDimension dimension = entry.getKey();
                String content = entry.getValue();
                String contentHash = hashString(content);

                Optional<ProfileIndexStatus> existing =
                        indexStatusRepository.findByProjectPathAndDimension(projectPath, dimension);
                if (existing.isPresent() && existing.get().isReady()
                        && contentHash.equals(existing.get().contentHash())) {
                    skippedCount++;
                    continue;
                }

                String embeddingText = dimension.label() + " - " + projectName + "\n" + content;
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("projectName", projectName);
                metadata.put("projectPath", projectPath);
                metadata.put("dimension", dimension.name());
                metadata.put("dimensionLabel", dimension.label());

                String docId = hashString(projectPath + "::" + dimension.name());
                documents.add(new Document(docId, embeddingText, metadata));

                statuses.add(new ProfileIndexStatus(
                        projectPath, projectName, dimension,
                        "READY", LocalDateTime.now(), contentHash));
                indexedCount++;
            }

            if (!documents.isEmpty()) {
                profileVectorStore.add(documents);
            }
            if (!statuses.isEmpty()) {
                indexStatusRepository.saveAll(statuses);
            }

            log.info("Markdown 画像索引完成: project={}, indexed={}, skipped={}",
                    projectName, indexedCount, skippedCount);
        } catch (Exception e) {
            log.error("indexFromMarkdown 失败: project={}", projectPath, e);
        }
    }

    private String extractProjectName(String projectPath) {
        return java.nio.file.Path.of(projectPath).getFileName().toString();
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
