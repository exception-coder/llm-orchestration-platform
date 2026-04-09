package com.exceptioncoder.llm.domain.devplan.analysis;

import java.util.List;
import java.util.Map;

/**
 * 依赖分析结果 -- 语言无关的标准输出模型。
 *
 * @param parent           父级依赖信息（Maven parent / 无则为 null）
 * @param properties       配置属性（Maven properties / package.json scripts 等）
 * @param dependencies     第三方依赖列表
 * @param moduleDependencies 模块间内部依赖（moduleName → 依赖的 module 列表）
 */
public record DependencyResult(
        ParentInfo parent,
        Map<String, String> properties,
        List<DependencyInfo> dependencies,
        Map<String, List<String>> moduleDependencies
) {

    public record ParentInfo(String groupId, String artifactId, String version) {}

    public record DependencyInfo(
            String groupId,
            String artifactId,
            String version,
            String scope
    ) {}
}
