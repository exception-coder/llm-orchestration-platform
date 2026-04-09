package com.exceptioncoder.llm.infrastructure.devplan.tool;

import com.exceptioncoder.llm.domain.devplan.analysis.ConfigResult;
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
 * 配置扫描工具 -- 委托 {@link LanguageAnalyzer} SPI 使用 SnakeYAML 解析配置。
 *
 * <p>替代原自写缩进式 YAML 解析，完整支持锚点、多文档、flow 语法等 YAML 特性。
 *
 * <p><b>归属智能体：</b>开发计划智能体（devplan）
 * <br><b>归属 Agent：</b>代码感知分析专家（devplan-code-awareness）
 * <br><b>调用阶段：</b>第一阶段 — 代码感知，与 code_structure 同阶段
 * <br><b>业务场景：</b>提取项目的配置文件（含多 profile）中的数据源、外部服务地址、
 * 自定义配置项等。让 LLM 了解项目运行时依赖的中间件和外部服务，
 * 在设计方案时正确引用已有配置键，避免重复定义。
 *
 * @author zhangkai
 * @since 2026-04-07
 */
@Slf4j
@Component
public class ConfigScanTool {

    private final LanguageAnalyzerRegistry analyzerRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ConfigScanTool(LanguageAnalyzerRegistry analyzerRegistry) {
        this.analyzerRegistry = analyzerRegistry;
    }

    @Tool(name = "devplan_config_scan", description = "读取application配置文件提取关键配置项", tags = {"devplan", "scan"})
    public String scan(
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

            ConfigResult result = analyzer.get().analyzeConfig(root);
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("ConfigScanTool 执行失败", e);
            return errorJson("配置扫描失败: " + e.getMessage());
        }
    }

    private String errorJson(String message) {
        return "{\"error\": \"" + message.replace("\"", "\\\"") + "\"}";
    }
}
