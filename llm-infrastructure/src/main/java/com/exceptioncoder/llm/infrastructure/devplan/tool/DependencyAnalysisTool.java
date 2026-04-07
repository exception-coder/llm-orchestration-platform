package com.exceptioncoder.llm.infrastructure.devplan.tool;

import com.exceptioncoder.llm.infrastructure.agent.tool.Tool;
import com.exceptioncoder.llm.infrastructure.agent.tool.ToolParam;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.*;
import java.util.*;

/**
 * 依赖分析工具 -- 机械解析 pom.xml，提取依赖清单、版本号、模块间依赖。
 *
 * @author zhangkai
 * @since 2026-04-07
 */
@Slf4j
@Component
public class DependencyAnalysisTool {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Tool(name = "devplan_dependency_analysis", description = "解析pom.xml提取依赖清单和版本信息", tags = {"devplan", "scan"})
    public String analyze(
            @ToolParam(value = "projectPath", description = "项目根目录绝对路径") String projectPath
    ) {
        try {
            Path root = Path.of(projectPath);
            Path pomPath = root.resolve("pom.xml");
            if (!Files.exists(pomPath)) {
                return errorJson("未检测到 pom.xml");
            }

            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder().parse(pomPath.toFile());
            doc.getDocumentElement().normalize();

            Map<String, Object> result = new LinkedHashMap<>();

            // parent
            NodeList parentNodes = doc.getElementsByTagName("parent");
            if (parentNodes.getLength() > 0) {
                Element parent = (Element) parentNodes.item(0);
                Map<String, String> parentInfo = new LinkedHashMap<>();
                parentInfo.put("groupId", getTagText(parent, "groupId"));
                parentInfo.put("artifactId", getTagText(parent, "artifactId"));
                parentInfo.put("version", getTagText(parent, "version"));
                result.put("parent", parentInfo);
            }

            // properties
            Map<String, String> properties = new LinkedHashMap<>();
            NodeList propsNodes = doc.getElementsByTagName("properties");
            if (propsNodes.getLength() > 0) {
                Element propsEl = (Element) propsNodes.item(0);
                NodeList children = propsEl.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    if (children.item(i) instanceof Element el) {
                        properties.put(el.getTagName(), el.getTextContent().trim());
                    }
                }
            }
            result.put("properties", properties);

            // 子模块 pom 依赖聚合
            List<Map<String, String>> allDeps = new ArrayList<>();
            Map<String, List<String>> moduleDeps = new LinkedHashMap<>();

            // 根 pom 中的 dependencyManagement
            collectDependencies(doc, allDeps, properties);

            // 扫描子模块 pom
            String projectGroupId = getTagText(doc.getDocumentElement(), "groupId");
            NodeList moduleNodes = doc.getElementsByTagName("module");
            for (int i = 0; i < moduleNodes.getLength(); i++) {
                String moduleName = moduleNodes.item(i).getTextContent().trim();
                Path modulePom = root.resolve(moduleName).resolve("pom.xml");
                if (Files.exists(modulePom)) {
                    Document modulePomDoc = DocumentBuilderFactory.newInstance()
                            .newDocumentBuilder().parse(modulePom.toFile());
                    collectDependencies(modulePomDoc, allDeps, properties);

                    // 提取模块间依赖
                    List<String> internalDeps = extractInternalDependencies(modulePomDoc, projectGroupId);
                    if (!internalDeps.isEmpty()) {
                        moduleDeps.put(moduleName, internalDeps);
                    }
                }
            }

            // 去重
            Set<String> seen = new HashSet<>();
            List<Map<String, String>> uniqueDeps = new ArrayList<>();
            for (Map<String, String> dep : allDeps) {
                String key = dep.get("groupId") + ":" + dep.get("artifactId");
                if (seen.add(key)) {
                    uniqueDeps.add(dep);
                }
            }

            result.put("dependencies", uniqueDeps);
            result.put("moduleDependencies", moduleDeps);

            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("DependencyAnalysisTool 执行失败", e);
            return errorJson("pom.xml 解析失败: " + e.getMessage());
        }
    }

    private void collectDependencies(Document doc, List<Map<String, String>> deps, Map<String, String> properties) {
        NodeList depNodes = doc.getElementsByTagName("dependency");
        for (int i = 0; i < depNodes.getLength(); i++) {
            Element dep = (Element) depNodes.item(i);
            Map<String, String> depInfo = new LinkedHashMap<>();
            depInfo.put("groupId", resolveProperty(getTagText(dep, "groupId"), properties));
            depInfo.put("artifactId", resolveProperty(getTagText(dep, "artifactId"), properties));
            String version = getTagText(dep, "version");
            if (version != null && !version.isEmpty()) {
                depInfo.put("version", resolveProperty(version, properties));
            }
            String scope = getTagText(dep, "scope");
            depInfo.put("scope", scope != null && !scope.isEmpty() ? scope : "compile");
            deps.add(depInfo);
        }
    }

    private List<String> extractInternalDependencies(Document modulePomDoc, String projectGroupId) {
        List<String> internalDeps = new ArrayList<>();
        if (projectGroupId == null) return internalDeps;

        NodeList depNodes = modulePomDoc.getElementsByTagName("dependency");
        for (int i = 0; i < depNodes.getLength(); i++) {
            Element dep = (Element) depNodes.item(i);
            String groupId = getTagText(dep, "groupId");
            if (projectGroupId.equals(groupId)) {
                internalDeps.add(getTagText(dep, "artifactId"));
            }
        }
        return internalDeps;
    }

    private String resolveProperty(String value, Map<String, String> properties) {
        if (value == null) return null;
        if (value.startsWith("${") && value.endsWith("}")) {
            String propName = value.substring(2, value.length() - 1);
            return properties.getOrDefault(propName, value);
        }
        return value;
    }

    private String getTagText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            // 只取直接子节点，避免嵌套元素
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getParentNode() == parent) {
                    return node.getTextContent().trim();
                }
            }
            return nodes.item(0).getTextContent().trim();
        }
        return null;
    }

    private String errorJson(String message) {
        return "{\"error\": \"" + message.replace("\"", "\\\"") + "\"}";
    }
}
