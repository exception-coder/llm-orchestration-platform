package com.exceptioncoder.llm.infrastructure.devplan.tool;

import com.exceptioncoder.llm.domain.devplan.analysis.DependencyResult;
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
 * 依赖分析工具 -- 委托 {@link LanguageAnalyzer} SPI 使用 maven-model 官方 API 解析依赖。
 *
 * <p>替代原 DOM 手动解析方案，完整支持 parent 继承、property 占位符解析、模块间依赖提取。
 *
 * <p><b>归属智能体：</b>开发计划智能体（devplan）
 * <br><b>归属 Agent：</b>代码感知分析专家（devplan-code-awareness）
 * <br><b>调用阶段：</b>第一阶段 — 代码感知，在 project_scan 之后
 * <br><b>业务场景：</b>解析项目的 Maven 依赖树，提取第三方库版本、模块间内部依赖关系。
 * 供 LLM 判断新需求是否需要引入新依赖，以及现有模块间的依赖方向是否符合架构规范。
 *
 * @author zhangkai
 * @since 2026-04-07
 */
@Slf4j
@Component
public class DependencyAnalysisTool {

    private final LanguageAnalyzerRegistry analyzerRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DependencyAnalysisTool(LanguageAnalyzerRegistry analyzerRegistry) {
        this.analyzerRegistry = analyzerRegistry;
    }

    @Tool(name = "devplan_dependency_analysis", description = "解析pom.xml提取依赖清单和版本信息", tags = {"devplan", "scan"})
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

            DependencyResult result = analyzer.get().analyzeDependencies(root);
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("DependencyAnalysisTool 执行失败", e);
            return errorJson("依赖分析失败: " + e.getMessage());
        }
    }

    private String errorJson(String message) {
        return "{\"error\": \"" + message.replace("\"", "\\\"") + "\"}";
    }
}
