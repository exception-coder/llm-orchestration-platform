package com.exceptioncoder.llm.domain.devplan.analysis;

import java.util.List;
import java.util.Map;

/**
 * 代码结构分析结果 -- 语言无关的标准输出模型。
 *
 * <p>所有 {@link LanguageAnalyzer} 实现须将分析结果映射到此模型，
 * 保证上层 Tool 和 LLM 消费侧看到的数据格式一致。
 *
 * @param controllers   控制器/入口类列表
 * @param services      业务服务类列表
 * @param entities      数据模型/实体类列表
 * @param repositories  仓储/DAO 类列表
 * @param layerDependencies 层间依赖（layer → 依赖的 layer 列表）
 * @param layerViolations   层间违规引用
 * @param extras        分析器自定义的扩展字段
 */
public record CodeStructureResult(
        List<ClassInfo> controllers,
        List<ClassInfo> services,
        List<ClassInfo> entities,
        List<ClassInfo> repositories,
        Map<String, List<String>> layerDependencies,
        List<LayerViolation> layerViolations,
        Map<String, Object> extras
) {

    /**
     * 类信息 -- 各种角色（Controller/Service/Entity/Repository）的标准描述。
     *
     * @param className     简短类名
     * @param fullClassName 完整类名（含包名）
     * @param filePath      相对于项目根的文件路径
     * @param annotations   类级注解列表（如 @RestController, @Service）
     * @param methods       public 方法签名列表
     * @param fields        字段列表（Entity 专用）
     * @param endpoints     API 端点列表（Controller 专用）
     * @param extras        扩展信息（如 tableName 等）
     */
    public record ClassInfo(
            String className,
            String fullClassName,
            String filePath,
            List<String> annotations,
            List<MethodInfo> methods,
            List<FieldInfo> fields,
            List<EndpointInfo> endpoints,
            Map<String, Object> extras
    ) {}

    public record MethodInfo(String returnType, String name, String params) {}

    public record FieldInfo(String type, String name) {}

    public record EndpointInfo(String httpMethod, String path) {}

    public record LayerViolation(String fromClass, String toClass, String filePath) {}
}
