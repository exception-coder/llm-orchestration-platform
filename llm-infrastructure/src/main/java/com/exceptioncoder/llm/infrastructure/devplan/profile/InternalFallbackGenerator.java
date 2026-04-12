package com.exceptioncoder.llm.infrastructure.devplan.profile;

import com.exceptioncoder.llm.domain.devplan.service.ProfileGenerator;
import com.exceptioncoder.llm.infrastructure.devplan.tool.CodeStructureAnalysisTool;
import com.exceptioncoder.llm.infrastructure.devplan.tool.ConfigScanTool;
import com.exceptioncoder.llm.infrastructure.devplan.tool.DependencyAnalysisTool;
import com.exceptioncoder.llm.infrastructure.devplan.tool.ProjectScanTool;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 内部兜底画像生成器 —— 调用 4 个扫描 Tool + 模板拼装，不经 LLM。
 *
 * <p>覆盖约 70% 维度，"编码约定"维度标注"未检测到"。
 * 当 {@code require-llm-generator=true} 时本生成器不参与 SPI 链。
 *
 * @author zhangkai
 * @since 2026-04-11
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "devplan.profile.generators.internal-fallback", name = "enabled", havingValue = "true", matchIfMissing = true)
public class InternalFallbackGenerator implements ProfileGenerator {

    private final ProjectScanTool projectScanTool;
    private final DependencyAnalysisTool dependencyAnalysisTool;
    private final CodeStructureAnalysisTool codeStructureAnalysisTool;
    private final ConfigScanTool configScanTool;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public InternalFallbackGenerator(ProjectScanTool projectScanTool,
                                     DependencyAnalysisTool dependencyAnalysisTool,
                                     CodeStructureAnalysisTool codeStructureAnalysisTool,
                                     ConfigScanTool configScanTool) {
        this.projectScanTool = projectScanTool;
        this.dependencyAnalysisTool = dependencyAnalysisTool;
        this.codeStructureAnalysisTool = codeStructureAnalysisTool;
        this.configScanTool = configScanTool;
    }

    @Override
    public Optional<String> generate(String projectPath) {
        try {
            String scanJson = projectScanTool.scan(projectPath);
            String depsJson = dependencyAnalysisTool.analyze(projectPath);
            String structureJson = codeStructureAnalysisTool.analyze(projectPath);
            String configJson = configScanTool.scan(projectPath);

            String markdown = renderMarkdown(projectPath, scanJson, depsJson, structureJson, configJson);
            return Optional.of(markdown);
        } catch (Exception e) {
            log.error("InternalFallbackGenerator failed for {}", projectPath, e);
            return Optional.empty();
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public int order() {
        return 99;
    }

    @SuppressWarnings("unchecked")
    private String renderMarkdown(String projectPath, String scanJson, String depsJson,
                                  String structureJson, String configJson) throws Exception {
        Map<String, Object> scan = parseJson(scanJson);
        Map<String, Object> deps = parseJson(depsJson);
        Map<String, Object> structure = parseJson(structureJson);
        Map<String, Object> config = parseJson(configJson);

        String projectName = getOrDefault(scan, "projectName", "unknown");
        String buildTool = getOrDefault(scan, "buildTool", "unknown");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        StringBuilder md = new StringBuilder();

        // Header
        md.append("# ").append(projectName).append(" 项目画像\n\n");
        md.append("> 自动生成于 ").append(timestamp).append(" | 生成工具：InternalFallbackGenerator\n");
        md.append("> 项目路径：`").append(projectPath).append("`\n\n---\n\n");

        // 1. 项目概述
        md.append("## 1. 项目概述\n\n");
        md.append("- **项目名称**：").append(projectName).append("\n");
        md.append("- **构建工具**：").append(buildTool).append("\n");
        appendIfPresent(md, scan, "totalModules", "模块数");
        appendIfPresent(md, scan, "totalJavaFiles", "Java 文件数");
        appendIfPresent(md, scan, "totalLines", "总代码行数");
        md.append("\n");

        // 2. 技术栈
        md.append("## 2. 技术栈\n\n");
        md.append("| 依赖 | GroupId | Version | Scope |\n");
        md.append("|------|---------|---------|-------|\n");
        List<Map<String, Object>> dependencies = (List<Map<String, Object>>) deps.get("dependencies");
        if (dependencies != null) {
            for (Map<String, Object> dep : dependencies) {
                md.append("| ").append(getOrDefault(dep, "artifactId", ""))
                        .append(" | ").append(getOrDefault(dep, "groupId", ""))
                        .append(" | ").append(getOrDefault(dep, "version", ""))
                        .append(" | ").append(getOrDefault(dep, "scope", "compile"))
                        .append(" |\n");
            }
        }
        md.append("\n");

        // 3. 项目结构
        md.append("## 3. 项目结构\n\n");
        Object packageTree = scan.get("packageTree");
        if (packageTree != null) {
            md.append("```text\n");
            md.append(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(packageTree));
            md.append("\n```\n\n");
        }

        // 4. 分层架构
        md.append("## 4. 分层架构\n\n");
        Map<String, Object> layerDeps = (Map<String, Object>) structure.get("layerDependencies");
        if (layerDeps != null && !layerDeps.isEmpty()) {
            md.append("| 层 | 依赖 |\n|---|---|\n");
            for (var entry : layerDeps.entrySet()) {
                md.append("| ").append(entry.getKey()).append(" | ").append(entry.getValue()).append(" |\n");
            }
        }
        List<Object> violations = (List<Object>) structure.get("layerViolations");
        if (violations != null && !violations.isEmpty()) {
            md.append("\n**违规项**：").append(violations).append("\n");
        } else {
            md.append("\n**违规项**：无违规\n");
        }
        md.append("\n");

        // 5. 数据模型
        md.append("## 5. 数据模型\n\n");
        appendClassTable(md, (List<Map<String, Object>>) structure.get("entities"), "Entity");

        // 6. Service 能力清单
        md.append("## 6. Service 能力清单\n\n");
        appendClassTable(md, (List<Map<String, Object>>) structure.get("services"), "Service");

        // 7. API 接口
        md.append("## 7. API 接口\n\n");
        List<Map<String, Object>> controllers = (List<Map<String, Object>>) structure.get("controllers");
        if (controllers != null && !controllers.isEmpty()) {
            md.append("| Controller | Method | URL | 说明 |\n");
            md.append("|-----------|--------|-----|------|\n");
            for (Map<String, Object> ctrl : controllers) {
                String className = getOrDefault(ctrl, "className", "");
                List<Map<String, Object>> endpoints = (List<Map<String, Object>>) ctrl.get("endpoints");
                if (endpoints != null) {
                    for (Map<String, Object> ep : endpoints) {
                        md.append("| ").append(className)
                                .append(" | ").append(getOrDefault(ep, "method", ""))
                                .append(" | ").append(getOrDefault(ep, "path", ""))
                                .append(" | ").append(getOrDefault(ep, "description", ""))
                                .append(" |\n");
                    }
                }
            }
        }
        md.append("\n");

        // 8. 外部依赖服务
        md.append("## 8. 外部依赖服务\n\n");
        Map<String, Object> externalServices = (Map<String, Object>) config.get("externalServices");
        if (externalServices != null && !externalServices.isEmpty()) {
            md.append("| 服务 | 配置 |\n|------|------|\n");
            for (var entry : externalServices.entrySet()) {
                md.append("| ").append(entry.getKey()).append(" | ").append(entry.getValue()).append(" |\n");
            }
        } else {
            md.append("> 未检测到外部依赖服务\n");
        }
        md.append("\n");

        // 9. 配置概要
        md.append("## 9. 配置概要\n\n");
        Map<String, Object> configItems = (Map<String, Object>) config.get("customProperties");
        if (configItems != null && !configItems.isEmpty()) {
            md.append("| 配置项 | 值 |\n|--------|-----|\n");
            for (var entry : configItems.entrySet()) {
                md.append("| ").append(entry.getKey()).append(" | ").append(entry.getValue()).append(" |\n");
            }
        }
        md.append("\n");

        // 10. 编码约定
        md.append("## 10. 编码约定\n\n");
        md.append("> 未检测到统一约定。建议使用编码工具执行 `generate-project-profile` Skill 获得完整画像。\n");

        return md.toString();
    }

    @SuppressWarnings("unchecked")
    private void appendClassTable(StringBuilder md, List<Map<String, Object>> classes, String type) {
        if (classes != null && !classes.isEmpty()) {
            md.append("| 类名 | 包路径 | 方法数 |\n");
            md.append("|------|--------|-------|\n");
            for (Map<String, Object> cls : classes) {
                List<?> methods = (List<?>) cls.get("methods");
                md.append("| ").append(getOrDefault(cls, "className", ""))
                        .append(" | ").append(getOrDefault(cls, "fullClassName", ""))
                        .append(" | ").append(methods != null ? methods.size() : 0)
                        .append(" |\n");
            }
        } else {
            md.append("> 未检测到 ").append(type).append(" 类\n");
        }
        md.append("\n");
    }

    private Map<String, Object> parseJson(String json) throws Exception {
        return objectMapper.readValue(json, new TypeReference<>() {});
    }

    @SuppressWarnings("unchecked")
    private String getOrDefault(Map<String, Object> map, String key, String defaultValue) {
        Object val = map.get(key);
        return val != null ? val.toString() : defaultValue;
    }

    private void appendIfPresent(StringBuilder md, Map<String, Object> map, String key, String label) {
        Object val = map.get(key);
        if (val != null) {
            md.append("- **").append(label).append("**：").append(val).append("\n");
        }
    }
}
