package com.exceptioncoder.llm.infrastructure.devplan.tool;

import com.exceptioncoder.llm.domain.devplan.analysis.CodeStructureResult;
import com.exceptioncoder.llm.domain.devplan.model.AgentRole;
import com.exceptioncoder.llm.domain.devplan.analysis.LanguageAnalyzer;
import com.exceptioncoder.llm.domain.devplan.analysis.LanguageAnalyzerRegistry;
import com.exceptioncoder.llm.infrastructure.agent.tool.Tool;
import com.exceptioncoder.llm.infrastructure.agent.tool.ToolParam;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * 代码结构分析工具 -- 委托 {@link LanguageAnalyzer} SPI 进行 AST 级代码分析。
 *
 * <p>替代原正则方案，通过 {@link LanguageAnalyzerRegistry} 自动路由到匹配的分析器
 * （当前支持 Java Spring，后续可扩展 Vue、React 等）。
 *
 * <p><b>归属智能体：</b>开发计划智能体（devplan）
 * <br><b>归属 Agent：</b>代码感知分析专家（devplan-code-awareness）
 * <br><b>调用阶段：</b>第一阶段 — 代码感知，在 project_scan、dependency_analysis 之后
 * <br><b>业务场景：</b>精确提取项目中的分层注解（@RestController、@Service、@Entity 等），
 * 输出各层类清单、API 端点、实体字段、层间依赖和违规引用。
 * 基于 JavaParser AST 解析，支持泛型、多行注解、内部类等复杂场景。
 *
 * @author zhangkai
 * @since 2026-04-07
 */
@Slf4j
@Component
public class CodeStructureAnalysisTool {

    private final LanguageAnalyzerRegistry analyzerRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CodeStructureAnalysisTool(LanguageAnalyzerRegistry analyzerRegistry) {
        this.analyzerRegistry = analyzerRegistry;
    }

    @Tool(name = "devplan_code_structure", description = "扫描Java注解提取Controller/Entity/Service清单和层间依赖",
          tags = {"devplan", "scan"}, roles = {AgentRole.CODE_AWARENESS})
    public String analyze(
            @ToolParam(value = "projectPath", description = "项目根目录绝对路径") String projectPath
    ) {
        try {
            Path root = Path.of(projectPath);
            if (!Files.isDirectory(root)) {
                return errorJson("路径不存在: " + projectPath);
            }

            Optional<LanguageAnalyzer> analyzer = analyzerRegistry.detect(root);
            if (analyzer.isEmpty()) {
                return errorJson("未识别的项目类型，当前支持: Java Spring");
            }

            CodeStructureResult result = analyzer.get().analyzeCodeStructure(root);
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("CodeStructureAnalysisTool 执行失败", e);
            return errorJson("代码结构分析失败: " + e.getMessage());
        }
    }

    private String errorJson(String message) {
        return "{\"error\": \"" + message.replace("\"", "\\\"") + "\"}";
    }
}
