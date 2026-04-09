package com.exceptioncoder.llm.infrastructure.agent.tool;

import com.exceptioncoder.llm.domain.model.DocStructureVersion;
import com.exceptioncoder.llm.domain.repository.DocStructureVersionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 文档目录结构 Agent 工具 -- 管理知识库文档的目录结构版本。
 *
 * <p>getLastDocStructure — 查询上一次解析的目录结构
 * <br>saveDocStructure — 保存新的目录结构版本
 *
 * <p><b>归属智能体：</b>通用（未绑定特定智能体）
 * <br><b>归属 Agent：</b>知识库解析流程
 * <br><b>调用阶段：</b>文档索引/重新解析时调用
 * <br><b>业务场景：</b>知识库文档重新解析时，先通过 getLastDocStructure 获取上次的目录结构
 * 和版本号，保持节点 path 一致性（避免已有引用失效）；解析完成后通过 saveDocStructure
 * 保存新版本，旧版本自动归档。
 */
@Slf4j
@Component
public class DocStructureTool {

    private final DocStructureVersionRepository repository;
    private final ObjectMapper objectMapper;

    public DocStructureTool(DocStructureVersionRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper();
    }

    @Tool(name = "getLastDocStructure",
          description = "查询上一次解析的文档目录结构和版本号，用于保持节点 path 一致性")
    public String getLastDocStructure() {
        return repository.findActive()
                .map(v -> {
                    try {
                        return objectMapper.writeValueAsString(java.util.Map.of(
                                "version", v.getVersion(),
                                "structure", v.getStructure(),
                                "diffSummary", v.getDiffSummary() != null ? v.getDiffSummary() : "",
                                "updatedAt", v.getCreatedAt() != null ? v.getCreatedAt().toString() : ""
                        ));
                    } catch (Exception e) {
                        log.error("序列化上一版本失败", e);
                        return "{\"version\": 0, \"structure\": [], \"message\": \"序列化失败\"}";
                    }
                })
                .orElse("{\"version\": 0, \"structure\": [], \"message\": \"首次解析，无历史版本\"}");
    }

    @Tool(name = "saveDocStructure",
          description = "保存新的文档目录结构，旧版本自动归档")
    public String saveDocStructure(
            @ToolParam(value = "structure", description = "DocTreeNode[] JSON 数组字符串") String structure,
            @ToolParam(value = "diffSummary", description = "本次变更描述，无变化填无变化") String diffSummary,
            @ToolParam(value = "readmeHash", description = "docs/README.md 内容的 SHA-256 hash") String readmeHash) {
        try {
            repository.deactivateAll();
            int newVersion = repository.getMaxVersion() + 1;
            repository.save(DocStructureVersion.builder()
                    .version(newVersion)
                    .structure(structure)
                    .diffSummary(diffSummary)
                    .readmeHash(readmeHash)
                    .active(true)
                    .build());
            log.info("文档目录结构已保存，version={}, diffSummary={}", newVersion, diffSummary);
            return "{\"success\": true, \"version\": " + newVersion + "}";
        } catch (Exception e) {
            log.error("保存文档目录结构失败", e);
            return "{\"success\": false, \"error\": \"" + e.getMessage() + "\"}";
        }
    }
}
