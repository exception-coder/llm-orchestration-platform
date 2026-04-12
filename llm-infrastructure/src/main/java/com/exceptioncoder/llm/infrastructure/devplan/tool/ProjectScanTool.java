package com.exceptioncoder.llm.infrastructure.devplan.tool;

import com.exceptioncoder.llm.domain.devplan.model.AgentRole;
import com.exceptioncoder.llm.infrastructure.agent.tool.Tool;
import com.exceptioncoder.llm.infrastructure.agent.tool.ToolParam;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/**
 * 项目结构扫描工具 -- 机械提取目录结构、Maven 模块、文件统计。
 *
 * <p>只做提取，不做理解和判断。输出原始 JSON 供 Agent(LLM) 消费。
 *
 * <p><b>归属智能体：</b>开发计划智能体（devplan）
 * <br><b>归属 Agent：</b>代码感知分析专家（devplan-code-awareness）
 * <br><b>调用阶段：</b>第一阶段 — 代码感知，最先调用
 * <br><b>业务场景：</b>在生成开发计划前，需要先了解项目整体结构。本工具扫描项目根目录，
 * 提取模块列表、包树、Java 文件统计等原始数据，作为后续依赖分析、代码结构分析的输入基础。
 *
 * @author zhangkai
 * @since 2026-04-07
 */
@Slf4j
@Component
public class ProjectScanTool {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Tool(name = "devplan_project_scan", description = "扫描项目目录结构和Maven模块列表",
          tags = {"devplan", "scan"}, roles = {AgentRole.CODE_AWARENESS})
    public String scan(
            @ToolParam(value = "projectPath", description = "项目根目录绝对路径") String projectPath
    ) {
        try {
            Path root = Path.of(projectPath);
            if (!Files.isDirectory(root)) {
                return errorJson("路径不存在或不是目录: " + projectPath);
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("projectName", root.getFileName().toString());

            // 检测构建工具
            if (Files.exists(root.resolve("pom.xml"))) {
                result.put("buildTool", "maven");
            } else if (Files.exists(root.resolve("build.gradle")) || Files.exists(root.resolve("build.gradle.kts"))) {
                result.put("buildTool", "gradle");
            } else {
                result.put("buildTool", "unknown");
            }

            // 扫描 Maven 模块
            List<Map<String, String>> modules = scanMavenModules(root);
            result.put("modules", modules);

            // 构建包目录树
            Map<String, Object> packageTree = new LinkedHashMap<>();
            Map<String, Integer> filesByModule = new LinkedHashMap<>();
            AtomicInteger totalJavaFiles = new AtomicInteger(0);
            AtomicLong totalLines = new AtomicLong(0);

            if (modules.isEmpty()) {
                // 单模块项目
                Path sourceRoot = root.resolve("src/main/java");
                if (Files.isDirectory(sourceRoot)) {
                    int count = scanSourceRoot(sourceRoot, packageTree, totalLines);
                    totalJavaFiles.addAndGet(count);
                    filesByModule.put(root.getFileName().toString(), count);
                }
            } else {
                for (Map<String, String> module : modules) {
                    Path sourceRoot = root.resolve(module.get("sourceRoot"));
                    if (Files.isDirectory(sourceRoot)) {
                        Map<String, Object> moduleTree = new LinkedHashMap<>();
                        int count = scanSourceRoot(sourceRoot, moduleTree, totalLines);
                        totalJavaFiles.addAndGet(count);
                        filesByModule.put(module.get("name"), count);
                        mergeTree(packageTree, moduleTree);
                    }
                }
            }

            result.put("packageTree", packageTree);

            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("totalModules", Math.max(modules.size(), 1));
            stats.put("totalJavaFiles", totalJavaFiles.get());
            stats.put("totalLines", totalLines.get());
            stats.put("filesByModule", filesByModule);
            result.put("stats", stats);

            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("ProjectScanTool 执行失败", e);
            return errorJson("扫描失败: " + e.getMessage());
        }
    }

    private List<Map<String, String>> scanMavenModules(Path root) throws IOException {
        List<Map<String, String>> modules = new ArrayList<>();
        Path pomPath = root.resolve("pom.xml");
        if (!Files.exists(pomPath)) {
            return modules;
        }

        String pomContent = Files.readString(pomPath);
        // 简单正则提取 <module>xxx</module>
        var matcher = java.util.regex.Pattern.compile("<module>([^<]+)</module>").matcher(pomContent);
        while (matcher.find()) {
            String moduleName = matcher.group(1).trim();
            Map<String, String> module = new LinkedHashMap<>();
            module.put("name", moduleName);
            module.put("path", moduleName);
            module.put("sourceRoot", moduleName + "/src/main/java");
            modules.add(module);
        }
        return modules;
    }

    private int scanSourceRoot(Path sourceRoot, Map<String, Object> packageTree, AtomicLong totalLines) throws IOException {
        AtomicInteger count = new AtomicInteger(0);
        Files.walkFileTree(sourceRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".java")) {
                    count.incrementAndGet();
                    totalLines.addAndGet(Files.lines(file).count());

                    // 构建包树
                    Path relative = sourceRoot.relativize(file.getParent());
                    String packagePath = relative.toString().replace(java.io.File.separatorChar, '.');
                    buildPackageTree(packageTree, packagePath);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return count.get();
    }

    @SuppressWarnings("unchecked")
    private void buildPackageTree(Map<String, Object> tree, String packagePath) {
        if (packagePath.isEmpty()) return;
        String[] parts = packagePath.split("\\.");
        Map<String, Object> current = tree;
        for (String part : parts) {
            current = (Map<String, Object>) current.computeIfAbsent(part, k -> new LinkedHashMap<>());
        }
    }

    @SuppressWarnings("unchecked")
    private void mergeTree(Map<String, Object> target, Map<String, Object> source) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            if (target.containsKey(entry.getKey()) && target.get(entry.getKey()) instanceof Map) {
                mergeTree((Map<String, Object>) target.get(entry.getKey()),
                        (Map<String, Object>) entry.getValue());
            } else {
                target.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private String errorJson(String message) {
        return "{\"error\": \"" + message.replace("\"", "\\\"") + "\"}";
    }
}
