package com.exceptioncoder.llm.infrastructure.devplan.tool;

import com.exceptioncoder.llm.infrastructure.agent.tool.Tool;
import com.exceptioncoder.llm.infrastructure.agent.tool.ToolParam;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 配置扫描工具 -- 机械读取 application*.yml/properties，提取关键配置项（敏感信息脱敏）。
 *
 * @author zhangkai
 * @since 2026-04-07
 */
@Slf4j
@Component
public class ConfigScanTool {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "secret", "key", "token", "api-key", "apikey", "credential"
    );

    @Tool(name = "devplan_config_scan", description = "读取application配置文件提取关键配置项", tags = {"devplan", "scan"})
    public String scan(
            @ToolParam(value = "projectPath", description = "项目根目录绝对路径") String projectPath
    ) {
        try {
            Path root = Path.of(projectPath);
            if (!Files.isDirectory(root)) {
                return errorJson("路径不存在: " + projectPath);
            }

            // 查找所有配置文件
            List<String> configFiles = new ArrayList<>();
            try (Stream<Path> paths = Files.walk(root, 5)) {
                paths.filter(p -> {
                    String name = p.getFileName().toString();
                    return (name.startsWith("application") &&
                            (name.endsWith(".yml") || name.endsWith(".yaml") || name.endsWith(".properties")))
                            && !p.toString().contains("/test/");
                }).forEach(p -> configFiles.add(root.relativize(p).toString()));
            }

            if (configFiles.isEmpty()) {
                return objectMapper.writeValueAsString(Map.of(
                        "warning", "未找到配置文件",
                        "configFiles", List.of()
                ));
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("configFiles", configFiles);

            // 提取 profiles
            Set<String> profiles = new LinkedHashSet<>();
            profiles.add("default");
            for (String configFile : configFiles) {
                String name = Path.of(configFile).getFileName().toString();
                Matcher m = Pattern.compile("application-([\\w]+)\\.(yml|yaml|properties)").matcher(name);
                if (m.matches()) {
                    profiles.add(m.group(1));
                }
            }
            result.put("profiles", new ArrayList<>(profiles));

            // 解析主配置文件
            Map<String, Object> mergedConfig = new LinkedHashMap<>();
            for (String configFile : configFiles) {
                Path configPath = root.resolve(configFile);
                String content = Files.readString(configPath);
                if (configFile.endsWith(".yml") || configFile.endsWith(".yaml")) {
                    parseYamlFlat(content, mergedConfig);
                } else {
                    parseProperties(content, mergedConfig);
                }
            }

            // 分类提取
            Map<String, Object> server = extractByPrefix(mergedConfig, "server.");
            result.put("server", server);

            Map<String, Object> datasource = extractByPrefix(mergedConfig, "spring.datasource.");
            result.put("datasource", datasource);

            // 外部服务
            List<Map<String, String>> externalServices = new ArrayList<>();
            addExternalService(externalServices, mergedConfig, "spring.ai.vectorstore.qdrant", "qdrant");
            addExternalService(externalServices, mergedConfig, "spring.data.redis", "redis");
            addExternalService(externalServices, mergedConfig, "spring.ai.ollama", "ollama");
            result.put("externalServices", externalServices);

            // 自定义配置（非 spring.* 和 server.*）
            Map<String, Object> customProps = new LinkedHashMap<>();
            for (var entry : mergedConfig.entrySet()) {
                if (!entry.getKey().startsWith("spring.") && !entry.getKey().startsWith("server.")) {
                    customProps.put(entry.getKey(), maskSensitive(entry.getKey(), String.valueOf(entry.getValue())));
                }
            }
            result.put("customProperties", customProps);

            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("ConfigScanTool 执行失败", e);
            return errorJson("配置扫描失败: " + e.getMessage());
        }
    }

    /**
     * 简易 YAML 扁平化解析（不依赖 SnakeYAML 库的直接 API，通过缩进解析）。
     */
    private void parseYamlFlat(String content, Map<String, Object> result) {
        String[] lines = content.split("\n");
        Deque<String> keyStack = new ArrayDeque<>();
        Deque<Integer> indentStack = new ArrayDeque<>();
        indentStack.push(-1);

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("---")) continue;

            int indent = 0;
            while (indent < line.length() && line.charAt(indent) == ' ') indent++;

            // 弹出比当前缩进大或相等的层级
            while (indentStack.size() > 1 && indentStack.peek() >= indent) {
                indentStack.pop();
                if (!keyStack.isEmpty()) keyStack.pop();
            }

            if (trimmed.contains(":")) {
                int colonIdx = trimmed.indexOf(':');
                String key = trimmed.substring(0, colonIdx).trim();
                String value = trimmed.substring(colonIdx + 1).trim();

                if (!value.isEmpty()) {
                    // key: value 形式
                    String fullKey = buildFullKey(keyStack, key);
                    result.put(fullKey, maskSensitive(fullKey, value));
                } else {
                    // key: 后面是子层级
                    keyStack.push(key);
                    indentStack.push(indent);
                }
            }
        }
    }

    private void parseProperties(String content, Map<String, Object> result) {
        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            int eqIdx = trimmed.indexOf('=');
            if (eqIdx > 0) {
                String key = trimmed.substring(0, eqIdx).trim();
                String value = trimmed.substring(eqIdx + 1).trim();
                result.put(key, maskSensitive(key, value));
            }
        }
    }

    private String buildFullKey(Deque<String> keyStack, String key) {
        if (keyStack.isEmpty()) return key;
        StringBuilder sb = new StringBuilder();
        Iterator<String> it = keyStack.descendingIterator();
        while (it.hasNext()) {
            sb.append(it.next()).append(".");
        }
        sb.append(key);
        return sb.toString();
    }

    private Map<String, Object> extractByPrefix(Map<String, Object> config, String prefix) {
        Map<String, Object> extracted = new LinkedHashMap<>();
        for (var entry : config.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                String shortKey = entry.getKey().substring(prefix.length());
                extracted.put(shortKey, entry.getValue());
            }
        }
        return extracted;
    }

    private void addExternalService(List<Map<String, String>> services, Map<String, Object> config,
                                     String prefix, String serviceName) {
        for (var entry : config.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                services.add(Map.of("name", serviceName, "config", entry.getKey() + "=" + entry.getValue()));
                return;
            }
        }
    }

    private String maskSensitive(String key, String value) {
        String keyLower = key.toLowerCase();
        for (String sensitive : SENSITIVE_KEYS) {
            if (keyLower.contains(sensitive)) {
                return "***";
            }
        }
        return value;
    }

    private String errorJson(String message) {
        return "{\"error\": \"" + message.replace("\"", "\\\"") + "\"}";
    }
}
