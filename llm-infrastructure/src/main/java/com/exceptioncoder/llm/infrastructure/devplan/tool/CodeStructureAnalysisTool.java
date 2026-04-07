package com.exceptioncoder.llm.infrastructure.devplan.tool;

import com.exceptioncoder.llm.infrastructure.agent.tool.Tool;
import com.exceptioncoder.llm.infrastructure.agent.tool.ToolParam;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.*;

/**
 * 代码结构分析工具 -- 机械扫描 Java 注解，提取 Controller/Entity/Service/Repository 清单和层间依赖。
 *
 * <p>合并了原 ArchTopologyTool 的职责（层依赖分析是 import 扫描的一部分）。
 * 只做正则提取，"这些类是什么业务能力"由 Agent(LLM) 判断。
 *
 * @author zhangkai
 * @since 2026-04-07
 */
@Slf4j
@Component
public class CodeStructureAnalysisTool {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 正则模式
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^package\\s+([\\w.]+)\\s*;", Pattern.MULTILINE);
    private static final Pattern CLASS_PATTERN = Pattern.compile("^public\\s+(?:abstract\\s+)?(?:class|interface|enum|record)\\s+(\\w+)", Pattern.MULTILINE);
    private static final Pattern IMPORT_PATTERN = Pattern.compile("^import\\s+([\\w.]+)\\s*;", Pattern.MULTILINE);
    private static final Pattern ANNOTATION_PATTERN = Pattern.compile("^@(\\w+)(?:\\((.*)\\))?", Pattern.MULTILINE);
    private static final Pattern METHOD_PATTERN = Pattern.compile("^\\s+public\\s+(?:static\\s+)?([\\w<>\\[\\],\\s]+?)\\s+(\\w+)\\s*\\(([^)]*)\\)", Pattern.MULTILINE);
    private static final Pattern REQUEST_MAPPING_PATTERN = Pattern.compile("@(?:Request|Get|Post|Put|Delete|Patch)Mapping\\s*(?:\\(.*?(?:value\\s*=\\s*)?\"([^\"]*)\"|\\(\"([^\"]*)\"\\))?", Pattern.DOTALL);
    private static final Pattern TABLE_PATTERN = Pattern.compile("@Table\\s*\\(.*?name\\s*=\\s*\"([^\"]*)\"", Pattern.DOTALL);
    private static final Pattern ENTITY_FIELD_PATTERN = Pattern.compile("private\\s+([\\w<>\\[\\]]+)\\s+(\\w+)\\s*;");

    @Tool(name = "devplan_code_structure", description = "扫描Java注解提取Controller/Entity/Service清单和层间依赖", tags = {"devplan", "scan"})
    public String analyze(
            @ToolParam(value = "projectPath", description = "项目根目录绝对路径") String projectPath
    ) {
        try {
            Path root = Path.of(projectPath);
            if (!Files.isDirectory(root)) {
                return errorJson("路径不存在: " + projectPath);
            }

            List<Map<String, Object>> controllers = new ArrayList<>();
            List<Map<String, Object>> entities = new ArrayList<>();
            List<Map<String, Object>> services = new ArrayList<>();
            List<Map<String, Object>> repositories = new ArrayList<>();

            // 层间依赖统计
            Map<String, Set<String>> layerImports = new LinkedHashMap<>();
            layerImports.put("api", new LinkedHashSet<>());
            layerImports.put("application", new LinkedHashSet<>());
            layerImports.put("domain", new LinkedHashSet<>());
            layerImports.put("infrastructure", new LinkedHashSet<>());

            List<Map<String, String>> violations = new ArrayList<>();

            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (!file.toString().endsWith(".java")) return FileVisitResult.CONTINUE;
                    if (file.toString().contains("/test/")) return FileVisitResult.CONTINUE;

                    String content = Files.readString(file);
                    String relativePath = root.relativize(file).toString();

                    String packageName = extractPackage(content);
                    String className = extractClassName(content);
                    if (className == null) return FileVisitResult.CONTINUE;

                    String fullClassName = packageName != null ? packageName + "." + className : className;
                    String layer = classifyLayer(packageName);

                    // 提取 import 关系 → 层依赖
                    if (layer != null) {
                        List<String> imports = extractImports(content);
                        for (String imp : imports) {
                            String importLayer = classifyLayer(imp);
                            if (importLayer != null && !importLayer.equals(layer)) {
                                layerImports.get(layer).add(importLayer);

                                // 检测违规：domain 不应 import infrastructure
                                if ("domain".equals(layer) && "infrastructure".equals(importLayer)) {
                                    Map<String, String> violation = new LinkedHashMap<>();
                                    violation.put("from", fullClassName);
                                    violation.put("to", imp);
                                    violation.put("file", relativePath);
                                    violations.add(violation);
                                }
                            }
                        }
                    }

                    // 检测注解分类
                    Set<String> classAnnotations = extractClassAnnotations(content);

                    if (classAnnotations.contains("RestController") || classAnnotations.contains("Controller")) {
                        controllers.add(buildControllerInfo(content, className, fullClassName, relativePath, classAnnotations));
                    }
                    if (classAnnotations.contains("Entity")) {
                        entities.add(buildEntityInfo(content, className, fullClassName, relativePath));
                    }
                    if (classAnnotations.contains("Service")) {
                        services.add(buildServiceInfo(content, className, fullClassName, relativePath, classAnnotations));
                    }
                    if (classAnnotations.contains("Repository") || className.endsWith("Repository")) {
                        repositories.add(buildRepositoryInfo(className, fullClassName, relativePath));
                    }

                    return FileVisitResult.CONTINUE;
                }
            });

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("controllers", controllers);
            result.put("entities", entities);
            result.put("services", services);
            result.put("repositories", repositories);

            // 转 layerImports Set → List
            Map<String, Map<String, List<String>>> layerDeps = new LinkedHashMap<>();
            for (var entry : layerImports.entrySet()) {
                Map<String, List<String>> depInfo = new LinkedHashMap<>();
                depInfo.put("imports", new ArrayList<>(entry.getValue()));
                layerDeps.put(entry.getKey(), depInfo);
            }
            result.put("layerDependencies", layerDeps);
            result.put("layerViolations", violations);

            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("CodeStructureAnalysisTool 执行失败", e);
            return errorJson("代码结构分析失败: " + e.getMessage());
        }
    }

    private String extractPackage(String content) {
        Matcher m = PACKAGE_PATTERN.matcher(content);
        return m.find() ? m.group(1) : null;
    }

    private String extractClassName(String content) {
        Matcher m = CLASS_PATTERN.matcher(content);
        return m.find() ? m.group(1) : null;
    }

    private List<String> extractImports(String content) {
        List<String> imports = new ArrayList<>();
        Matcher m = IMPORT_PATTERN.matcher(content);
        while (m.find()) {
            imports.add(m.group(1));
        }
        return imports;
    }

    private Set<String> extractClassAnnotations(String content) {
        Set<String> annotations = new LinkedHashSet<>();
        // 只提取类声明前的注解（粗略：取 class/interface 关键词之前的部分）
        int classIdx = content.indexOf("public class ");
        if (classIdx == -1) classIdx = content.indexOf("public interface ");
        if (classIdx == -1) classIdx = content.indexOf("public enum ");
        if (classIdx == -1) classIdx = content.indexOf("public record ");
        if (classIdx == -1) return annotations;

        String beforeClass = content.substring(0, classIdx);
        Matcher m = ANNOTATION_PATTERN.matcher(beforeClass);
        while (m.find()) {
            annotations.add(m.group(1));
        }
        return annotations;
    }

    private String classifyLayer(String packageOrClassName) {
        if (packageOrClassName == null) return null;
        if (packageOrClassName.contains(".controller.") || packageOrClassName.contains(".api.") || packageOrClassName.contains(".web.")) {
            return "api";
        }
        if (packageOrClassName.contains(".application.") || packageOrClassName.contains(".usecase.")) {
            return "application";
        }
        if (packageOrClassName.contains(".domain.")) {
            return "domain";
        }
        if (packageOrClassName.contains(".infrastructure.") || packageOrClassName.contains(".config.")) {
            return "infrastructure";
        }
        return null;
    }

    private Map<String, Object> buildControllerInfo(String content, String className, String fullClassName,
                                                     String filePath, Set<String> annotations) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("className", className);
        info.put("fullClassName", fullClassName);
        info.put("filePath", filePath);
        info.put("annotations", annotations.stream()
                .map(a -> "@" + a)
                .toList());

        // 提取端点
        List<Map<String, String>> endpoints = new ArrayList<>();
        Matcher m = REQUEST_MAPPING_PATTERN.matcher(content);
        // 先找类级别的 base path
        String basePath = "";
        Matcher basePathMatcher = Pattern.compile("@RequestMapping\\s*\\(.*?\"([^\"]*)\"").matcher(content);
        if (basePathMatcher.find()) {
            basePath = basePathMatcher.group(1);
        }

        // 找方法级别的映射
        Matcher methodMatcher = Pattern.compile(
                "@(Get|Post|Put|Delete|Patch)Mapping\\s*(?:\\(.*?(?:value\\s*=\\s*)?\"([^\"]*)\"\\s*\\)|\\(\"([^\"]*)\"\\))?\\s*\n\\s*public"
        ).matcher(content);
        while (methodMatcher.find()) {
            Map<String, String> endpoint = new LinkedHashMap<>();
            endpoint.put("method", methodMatcher.group(1).toUpperCase());
            String path = methodMatcher.group(2) != null ? methodMatcher.group(2) : methodMatcher.group(3);
            endpoint.put("path", basePath + (path != null ? path : ""));
            endpoints.add(endpoint);
        }

        info.put("endpoints", endpoints);
        return info;
    }

    private Map<String, Object> buildEntityInfo(String content, String className, String fullClassName, String filePath) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("className", className);
        info.put("fullClassName", fullClassName);
        info.put("filePath", filePath);

        // 提取表名
        Matcher tableMatcher = TABLE_PATTERN.matcher(content);
        info.put("tableName", tableMatcher.find() ? tableMatcher.group(1) : className.toLowerCase());

        // 提取字段
        List<Map<String, String>> fields = new ArrayList<>();
        Matcher fieldMatcher = ENTITY_FIELD_PATTERN.matcher(content);
        while (fieldMatcher.find()) {
            Map<String, String> field = new LinkedHashMap<>();
            field.put("type", fieldMatcher.group(1));
            field.put("name", fieldMatcher.group(2));
            fields.add(field);
        }
        info.put("fields", fields);
        return info;
    }

    private Map<String, Object> buildServiceInfo(String content, String className, String fullClassName,
                                                  String filePath, Set<String> annotations) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("className", className);
        info.put("fullClassName", fullClassName);
        info.put("filePath", filePath);
        info.put("annotations", annotations.stream().map(a -> "@" + a).toList());

        List<Map<String, String>> methods = new ArrayList<>();
        Matcher methodMatcher = METHOD_PATTERN.matcher(content);
        while (methodMatcher.find()) {
            Map<String, String> method = new LinkedHashMap<>();
            method.put("returnType", methodMatcher.group(1).trim());
            method.put("name", methodMatcher.group(2));
            method.put("params", methodMatcher.group(3).trim());
            methods.add(method);
        }
        info.put("publicMethods", methods);
        return info;
    }

    private Map<String, Object> buildRepositoryInfo(String className, String fullClassName, String filePath) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("className", className);
        info.put("fullClassName", fullClassName);
        info.put("filePath", filePath);
        return info;
    }

    private String errorJson(String message) {
        return "{\"error\": \"" + message.replace("\"", "\\\"") + "\"}";
    }
}
