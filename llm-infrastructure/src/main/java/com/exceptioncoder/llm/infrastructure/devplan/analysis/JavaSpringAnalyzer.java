package com.exceptioncoder.llm.infrastructure.devplan.analysis;

import com.exceptioncoder.llm.domain.devplan.analysis.*;
import com.exceptioncoder.llm.domain.devplan.analysis.CodeStructureResult.*;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Stream;

/**
 * Java Spring Boot/Cloud 项目分析器 -- 基于 JavaParser AST 分析。
 *
 * <p><b>归属智能体：</b>开发计划智能体（devplan）
 * <br><b>归属 Agent：</b>代码感知分析专家（devplan-code-awareness）
 * <br><b>业务场景：</b>为 Spring Boot/Cloud 项目提供精确的代码结构分析、
 * 依赖分析、配置解析和代码嵌入文本提取，替代原有正则方案。
 *
 * <p><b>技术方案：</b>
 * <ul>
 *   <li>代码结构：JavaParser AST 解析，精确识别注解、泛型、方法签名</li>
 *   <li>依赖分析：maven-model 官方 API 解析 pom.xml</li>
 *   <li>配置解析：SnakeYAML 解析 application*.yml</li>
 *   <li>嵌入文本：JavaParser 提取 Javadoc + 类声明 + public 方法签名</li>
 * </ul>
 *
 * @author zhangkai
 * @since 2026-04-09
 */
@Slf4j
@Component
public class JavaSpringAnalyzer implements LanguageAnalyzer {

    private static final Set<String> CONTROLLER_ANNOTATIONS = Set.of(
            "RestController", "Controller"
    );
    private static final Set<String> SERVICE_ANNOTATIONS = Set.of(
            "Service", "Component"
    );
    private static final Set<String> ENTITY_ANNOTATIONS = Set.of(
            "Entity", "Table", "Document"
    );
    private static final Set<String> REPO_ANNOTATIONS = Set.of(
            "Repository"
    );
    private static final Set<String> MAPPING_ANNOTATIONS = Set.of(
            "GetMapping", "PostMapping", "PutMapping", "DeleteMapping",
            "PatchMapping", "RequestMapping"
    );
    private static final Map<String, String> MAPPING_HTTP_METHODS = Map.of(
            "GetMapping", "GET", "PostMapping", "POST", "PutMapping", "PUT",
            "DeleteMapping", "DELETE", "PatchMapping", "PATCH", "RequestMapping", "REQUEST"
    );
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "secret", "key", "token", "api-key", "apikey", "credential"
    );

    private final JavaParser javaParser = new JavaParser();

    @Override
    public ProjectType projectType() {
        return ProjectType.JAVA_SPRING;
    }

    @Override
    public boolean supports(Path projectRoot) {
        return Files.exists(projectRoot.resolve("pom.xml"))
                || Files.exists(projectRoot.resolve("build.gradle"))
                || Files.exists(projectRoot.resolve("build.gradle.kts"));
    }

    // ========== 代码结构分析 ==========

    @Override
    public CodeStructureResult analyzeCodeStructure(Path projectRoot) {
        List<ClassInfo> controllers = new ArrayList<>();
        List<ClassInfo> services = new ArrayList<>();
        List<ClassInfo> entities = new ArrayList<>();
        List<ClassInfo> repositories = new ArrayList<>();
        Map<String, Set<String>> layerImports = new LinkedHashMap<>();
        layerImports.put("api", new LinkedHashSet<>());
        layerImports.put("application", new LinkedHashSet<>());
        layerImports.put("domain", new LinkedHashSet<>());
        layerImports.put("infrastructure", new LinkedHashSet<>());
        List<LayerViolation> violations = new ArrayList<>();

        walkJavaFiles(projectRoot, (file, cu) -> {
            cu.getTypes().forEach(type -> {
                TypeDeclaration<?> td = type;
                String className = td.getNameAsString();
                String packageName = cu.getPackageDeclaration()
                        .map(p -> p.getNameAsString()).orElse("");
                String fullClassName = packageName.isEmpty() ? className : packageName + "." + className;
                String filePath = projectRoot.relativize(file).toString().replace('\\', '/');
                String layer = classifyLayer(packageName);

                // 层间依赖分析
                if (layer != null) {
                    for (ImportDeclaration imp : cu.getImports()) {
                        String importName = imp.getNameAsString();
                        String importLayer = classifyLayer(importName);
                        if (importLayer != null && !importLayer.equals(layer)) {
                            layerImports.get(layer).add(importLayer);
                            if ("domain".equals(layer) && "infrastructure".equals(importLayer)) {
                                violations.add(new LayerViolation(fullClassName, importName, filePath));
                            }
                        }
                    }
                }

                Set<String> annNames = extractAnnotationNames(td);

                if (hasAny(annNames, CONTROLLER_ANNOTATIONS)) {
                    controllers.add(buildClassInfo(td, className, fullClassName, filePath, annNames, true));
                }
                if (hasAny(annNames, ENTITY_ANNOTATIONS)) {
                    entities.add(buildClassInfo(td, className, fullClassName, filePath, annNames, false));
                }
                if (hasAny(annNames, SERVICE_ANNOTATIONS) && !hasAny(annNames, CONTROLLER_ANNOTATIONS)) {
                    services.add(buildClassInfo(td, className, fullClassName, filePath, annNames, false));
                }
                if (hasAny(annNames, REPO_ANNOTATIONS) || className.endsWith("Repository")) {
                    repositories.add(buildClassInfo(td, className, fullClassName, filePath, annNames, false));
                }
            });
        });

        Map<String, List<String>> layerDeps = new LinkedHashMap<>();
        layerImports.forEach((k, v) -> layerDeps.put(k, new ArrayList<>(v)));

        return new CodeStructureResult(
                controllers, services, entities, repositories,
                layerDeps, violations, Map.of()
        );
    }

    private ClassInfo buildClassInfo(TypeDeclaration<?> td, String className,
                                     String fullClassName, String filePath,
                                     Set<String> annNames, boolean isController) {
        List<MethodInfo> methods = new ArrayList<>();
        List<FieldInfo> fields = new ArrayList<>();
        List<EndpointInfo> endpoints = new ArrayList<>();
        Map<String, Object> extras = new LinkedHashMap<>();

        // 类级 RequestMapping 基础路径
        String basePath = isController ? extractMappingPath(td, "RequestMapping") : "";

        td.getMembers().forEach(member -> {
            if (member instanceof MethodDeclaration md) {
                if (md.isPublic()) {
                    String params = md.getParameters().stream()
                            .map(p -> p.getTypeAsString() + " " + p.getNameAsString())
                            .reduce((a, b) -> a + ", " + b).orElse("");
                    methods.add(new MethodInfo(md.getTypeAsString(), md.getNameAsString(), params));

                    // 提取端点
                    if (isController) {
                        extractEndpoints(md, basePath, endpoints);
                    }
                }
            } else if (member instanceof FieldDeclaration fd) {
                fd.getVariables().forEach(v ->
                        fields.add(new FieldInfo(v.getTypeAsString(), v.getNameAsString()))
                );
            }
        });

        // Entity 额外提取 tableName
        if (hasAny(annNames, ENTITY_ANNOTATIONS)) {
            extractTableName(td).ifPresent(t -> extras.put("tableName", t));
        }

        return new ClassInfo(
                className, fullClassName, filePath,
                annNames.stream().map(a -> "@" + a).toList(),
                methods, fields, endpoints, extras
        );
    }

    private void extractEndpoints(MethodDeclaration md, String basePath,
                                  List<EndpointInfo> endpoints) {
        for (AnnotationExpr ann : md.getAnnotations()) {
            String annName = ann.getNameAsString();
            String httpMethod = MAPPING_HTTP_METHODS.get(annName);
            if (httpMethod == null) continue;

            String path = extractAnnotationValue(ann);
            endpoints.add(new EndpointInfo(httpMethod, basePath + path));
        }
    }

    private String extractMappingPath(TypeDeclaration<?> td, String annotationName) {
        for (AnnotationExpr ann : td.getAnnotations()) {
            if (ann.getNameAsString().equals(annotationName)) {
                return extractAnnotationValue(ann);
            }
        }
        return "";
    }

    private String extractAnnotationValue(AnnotationExpr ann) {
        if (ann instanceof SingleMemberAnnotationExpr sma) {
            return unquote(sma.getMemberValue().toString());
        }
        if (ann instanceof NormalAnnotationExpr nae) {
            for (var pair : nae.getPairs()) {
                if ("value".equals(pair.getNameAsString()) || "path".equals(pair.getNameAsString())) {
                    return unquote(pair.getValue().toString());
                }
            }
            // 如果只有一个 pair 且没有匹配的名字，取第一个
            if (nae.getPairs().size() == 1) {
                return unquote(nae.getPairs().get(0).getValue().toString());
            }
        }
        return "";
    }

    private Optional<String> extractTableName(TypeDeclaration<?> td) {
        for (AnnotationExpr ann : td.getAnnotations()) {
            if ("Table".equals(ann.getNameAsString()) && ann instanceof NormalAnnotationExpr nae) {
                for (var pair : nae.getPairs()) {
                    if ("name".equals(pair.getNameAsString())) {
                        return Optional.of(unquote(pair.getValue().toString()));
                    }
                }
            }
        }
        return Optional.empty();
    }

    // ========== 依赖分析 ==========

    @Override
    public DependencyResult analyzeDependencies(Path projectRoot) {
        Path pomPath = projectRoot.resolve("pom.xml");
        if (!Files.exists(pomPath)) {
            return new DependencyResult(null, Map.of(), List.of(), Map.of());
        }

        try {
            Model rootModel = readPom(pomPath);

            // parent
            DependencyResult.ParentInfo parentInfo = null;
            Parent parent = rootModel.getParent();
            if (parent != null) {
                parentInfo = new DependencyResult.ParentInfo(
                        parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
            }

            // properties
            Map<String, String> properties = new LinkedHashMap<>();
            rootModel.getProperties().forEach((k, v) -> properties.put(k.toString(), v.toString()));

            // 收集所有依赖（根 + 子模块）
            Set<String> seen = new LinkedHashSet<>();
            List<DependencyResult.DependencyInfo> allDeps = new ArrayList<>();
            Map<String, List<String>> moduleDeps = new LinkedHashMap<>();

            collectDeps(rootModel, allDeps, seen, properties);

            String projectGroupId = rootModel.getGroupId() != null
                    ? rootModel.getGroupId()
                    : (parent != null ? parent.getGroupId() : null);

            for (String moduleName : rootModel.getModules()) {
                Path modulePom = projectRoot.resolve(moduleName).resolve("pom.xml");
                if (!Files.exists(modulePom)) continue;

                try {
                    Model moduleModel = readPom(modulePom);
                    collectDeps(moduleModel, allDeps, seen, properties);

                    // 模块间依赖
                    List<String> internal = new ArrayList<>();
                    for (Dependency dep : moduleModel.getDependencies()) {
                        if (dep.getGroupId() != null && dep.getGroupId().equals(projectGroupId)) {
                            internal.add(dep.getArtifactId());
                        }
                    }
                    if (!internal.isEmpty()) {
                        moduleDeps.put(moduleName, internal);
                    }
                } catch (Exception e) {
                    log.warn("解析子模块 pom 失败: {}", modulePom, e);
                }
            }

            return new DependencyResult(parentInfo, properties, allDeps, moduleDeps);
        } catch (Exception e) {
            log.error("依赖分析失败: {}", pomPath, e);
            return new DependencyResult(null, Map.of(), List.of(), Map.of());
        }
    }

    private Model readPom(Path pomPath) throws Exception {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        try (FileReader fr = new FileReader(pomPath.toFile())) {
            return reader.read(fr);
        }
    }

    private void collectDeps(Model model, List<DependencyResult.DependencyInfo> allDeps,
                             Set<String> seen, Map<String, String> properties) {
        for (Dependency dep : model.getDependencies()) {
            String key = dep.getGroupId() + ":" + dep.getArtifactId();
            if (!seen.add(key)) continue;

            allDeps.add(new DependencyResult.DependencyInfo(
                    resolveProperty(dep.getGroupId(), properties),
                    resolveProperty(dep.getArtifactId(), properties),
                    resolveProperty(dep.getVersion(), properties),
                    dep.getScope() != null ? dep.getScope() : "compile"
            ));
        }
    }

    // ========== 配置分析 ==========

    @Override
    public ConfigResult analyzeConfig(Path projectRoot) {
        List<String> configFiles = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(projectRoot, 5)) {
            paths.filter(p -> {
                String name = p.getFileName().toString();
                return name.startsWith("application")
                        && (name.endsWith(".yml") || name.endsWith(".yaml") || name.endsWith(".properties"))
                        && !p.toString().replace('\\', '/').contains("/test/");
            }).forEach(p -> configFiles.add(projectRoot.relativize(p).toString().replace('\\', '/')));
        } catch (IOException e) {
            log.warn("扫描配置文件失败", e);
        }

        if (configFiles.isEmpty()) {
            return new ConfigResult(configFiles, List.of(), Map.of(), Map.of(), List.of(), Map.of());
        }

        // 提取 profiles
        Set<String> profiles = new LinkedHashSet<>();
        profiles.add("default");
        for (String cf : configFiles) {
            String name = Path.of(cf).getFileName().toString();
            int dashIdx = name.indexOf('-');
            int dotIdx = name.lastIndexOf('.');
            if (dashIdx > 0 && dotIdx > dashIdx) {
                profiles.add(name.substring(dashIdx + 1, dotIdx));
            }
        }

        // 合并所有配置为扁平 Map
        Map<String, Object> merged = new LinkedHashMap<>();
        Yaml yaml = new Yaml();
        for (String cf : configFiles) {
            Path cfPath = projectRoot.resolve(cf);
            try {
                if (cf.endsWith(".yml") || cf.endsWith(".yaml")) {
                    parseYaml(cfPath, yaml, merged);
                } else {
                    parseProperties(cfPath, merged);
                }
            } catch (Exception e) {
                log.warn("解析配置文件失败: {}", cf, e);
            }
        }

        // 分类
        Map<String, Object> server = extractByPrefix(merged, "server.");
        Map<String, Object> datasource = extractByPrefix(merged, "spring.datasource.");

        List<ConfigResult.ExternalService> externalServices = new ArrayList<>();
        detectExternalService(merged, "spring.ai.vectorstore.qdrant", "qdrant", externalServices);
        detectExternalService(merged, "spring.data.redis", "redis", externalServices);
        detectExternalService(merged, "spring.ai.ollama", "ollama", externalServices);
        detectExternalService(merged, "spring.rabbitmq", "rabbitmq", externalServices);
        detectExternalService(merged, "spring.kafka", "kafka", externalServices);

        Map<String, Object> customProps = new LinkedHashMap<>();
        merged.forEach((k, v) -> {
            if (!k.startsWith("spring.") && !k.startsWith("server.")) {
                customProps.put(k, maskSensitive(k, String.valueOf(v)));
            }
        });

        return new ConfigResult(
                configFiles, new ArrayList<>(profiles),
                server, datasource, externalServices, customProps
        );
    }

    @SuppressWarnings("unchecked")
    private void parseYaml(Path path, Yaml yaml, Map<String, Object> result) throws IOException {
        try (InputStream is = Files.newInputStream(path)) {
            Iterable<Object> docs = yaml.loadAll(is);
            for (Object doc : docs) {
                if (doc instanceof Map<?, ?> map) {
                    flattenMap("", (Map<String, Object>) map, result);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void flattenMap(String prefix, Map<String, Object> map, Map<String, Object> result) {
        for (var entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map) {
                flattenMap(key, (Map<String, Object>) value, result);
            } else {
                result.put(key, maskSensitive(key, String.valueOf(value)));
            }
        }
    }

    private void parseProperties(Path path, Map<String, Object> result) throws IOException {
        Properties props = new Properties();
        try (InputStream is = Files.newInputStream(path)) {
            props.load(is);
        }
        props.forEach((k, v) -> result.put(k.toString(), maskSensitive(k.toString(), v.toString())));
    }

    // ========== 嵌入文本提取 ==========

    @Override
    public List<EmbeddingText> extractEmbeddingTexts(Path projectRoot) {
        List<EmbeddingText> results = new ArrayList<>();

        walkJavaFiles(projectRoot, (file, cu) -> {
            cu.getTypes().forEach(type -> {
                TypeDeclaration<?> td = type;

                StringBuilder sb = new StringBuilder();

                // Javadoc
                td.getJavadocComment().ifPresent(jd ->
                        sb.append(jd.getContent()
                                .replaceAll("\\s*\\*\\s*", " ")
                                .replaceAll("@\\w+.*", "")
                                .trim()).append("\n"));

                // 类声明行
                sb.append(td.isPublic() ? "public " : "");
                if (td instanceof ClassOrInterfaceDeclaration cid) {
                    sb.append(cid.isInterface() ? "interface " : "class ");
                } else if (td instanceof EnumDeclaration) {
                    sb.append("enum ");
                } else if (td instanceof RecordDeclaration) {
                    sb.append("record ");
                }
                sb.append(td.getNameAsString());
                if (td instanceof ClassOrInterfaceDeclaration cid) {
                    if (!cid.getExtendedTypes().isEmpty()) {
                        sb.append(" extends ").append(cid.getExtendedTypes().get(0));
                    }
                    if (!cid.getImplementedTypes().isEmpty()) {
                        sb.append(" implements ").append(
                                cid.getImplementedTypes().stream()
                                        .map(Object::toString)
                                        .reduce((a, b) -> a + ", " + b).orElse(""));
                    }
                }
                sb.append("\n");

                // public 方法签名
                td.getMethods().stream()
                        .filter(MethodDeclaration::isPublic)
                        .forEach(md -> {
                            sb.append(md.getTypeAsString()).append(" ")
                                    .append(md.getNameAsString()).append("(");
                            sb.append(md.getParameters().stream()
                                    .map(p -> p.getTypeAsString() + " " + p.getNameAsString())
                                    .reduce((a, b) -> a + ", " + b).orElse(""));
                            sb.append(")\n");
                        });

                String text = sb.toString().trim();
                if (text.isEmpty()) return;

                String packageName = cu.getPackageDeclaration()
                        .map(p -> p.getNameAsString()).orElse("");
                String className = td.getNameAsString();
                String filePath = projectRoot.relativize(file).toString().replace('\\', '/');

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("filePath", filePath);
                metadata.put("className", className);
                metadata.put("packageName", packageName);

                results.add(new EmbeddingText(text, metadata));
            });
        });

        return results;
    }

    // ========== 工具方法 ==========

    private void walkJavaFiles(Path projectRoot, JavaFileVisitor visitor) {
        try {
            Files.walkFileTree(projectRoot, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String path = file.toString().replace('\\', '/');
                    if (!path.endsWith(".java") || path.contains("/test/")) {
                        return FileVisitResult.CONTINUE;
                    }
                    try {
                        ParseResult<CompilationUnit> result = javaParser.parse(file);
                        result.getResult().ifPresent(cu -> visitor.visit(file, cu));
                    } catch (Exception e) {
                        log.debug("JavaParser 解析失败，跳过: {}", file, e);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    String name = dir.getFileName().toString();
                    if (name.equals("target") || name.equals("build") || name.equals(".git")
                            || name.equals("node_modules")) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("遍历 Java 文件失败: {}", projectRoot, e);
        }
    }

    @FunctionalInterface
    private interface JavaFileVisitor {
        void visit(Path file, CompilationUnit cu);
    }

    private Set<String> extractAnnotationNames(NodeWithAnnotations<?> node) {
        Set<String> names = new LinkedHashSet<>();
        node.getAnnotations().forEach(ann -> names.add(ann.getNameAsString()));
        return names;
    }

    private boolean hasAny(Set<String> set, Set<String> targets) {
        for (String t : targets) {
            if (set.contains(t)) return true;
        }
        return false;
    }

    private String classifyLayer(String packageOrClassName) {
        if (packageOrClassName == null) return null;
        if (packageOrClassName.contains(".controller.") || packageOrClassName.contains(".api.")) return "api";
        if (packageOrClassName.contains(".application.") || packageOrClassName.contains(".usecase.")) return "application";
        if (packageOrClassName.contains(".domain.")) return "domain";
        if (packageOrClassName.contains(".infrastructure.") || packageOrClassName.contains(".config.")) return "infrastructure";
        return null;
    }

    private String resolveProperty(String value, Map<String, String> properties) {
        if (value == null) return null;
        if (value.startsWith("${") && value.endsWith("}")) {
            String propName = value.substring(2, value.length() - 1);
            return properties.getOrDefault(propName, value);
        }
        return value;
    }

    private Map<String, Object> extractByPrefix(Map<String, Object> config, String prefix) {
        Map<String, Object> extracted = new LinkedHashMap<>();
        config.forEach((k, v) -> {
            if (k.startsWith(prefix)) {
                extracted.put(k.substring(prefix.length()), v);
            }
        });
        return extracted;
    }

    private void detectExternalService(Map<String, Object> config, String prefix, String name,
                                       List<ConfigResult.ExternalService> services) {
        for (var entry : config.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                services.add(new ConfigResult.ExternalService(
                        name, entry.getKey(), String.valueOf(entry.getValue())));
                return;
            }
        }
    }

    private String maskSensitive(String key, String value) {
        String keyLower = key.toLowerCase();
        for (String s : SENSITIVE_KEYS) {
            if (keyLower.contains(s)) return "***";
        }
        return value;
    }

    private String unquote(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }
}
