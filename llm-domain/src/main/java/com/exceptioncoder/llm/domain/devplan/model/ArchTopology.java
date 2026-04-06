package com.exceptioncoder.llm.domain.devplan.model;

import java.util.List;
import java.util.Map;

/**
 * 架构拓扑 -- 描述目标项目的分层依赖关系。
 *
 * <p>属于 Domain 层 devplan 模块。由 CODE_AWARENESS Agent 通过静态分析提取，
 * 记录项目各层（Controller / Service / Repository / Domain）之间的调用依赖
 * 以及每层包含的类列表。方案设计与审查阶段利用此信息确保生成的方案符合
 * 项目既有架构规范，避免引入反向依赖。</p>
 *
 * @author zhangkai
 * @since 2026-04-06
 */
public record ArchTopology(
        String projectPath,                             // 目标项目的本地绝对路径
        Map<String, List<String>> layerDependencies,    // 层间依赖映射，key 为源层名称，value 为其依赖的目标层名称列表
        List<String> controllerClasses,                 // Controller 层类全限定名列表
        List<String> serviceClasses,                    // Service 层类全限定名列表
        List<String> repositoryClasses,                 // Repository 层类全限定名列表
        List<String> domainClasses                      // Domain 层类全限定名列表
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String projectPath;
        private Map<String, List<String>> layerDependencies = Map.of();
        private List<String> controllerClasses = List.of();
        private List<String> serviceClasses = List.of();
        private List<String> repositoryClasses = List.of();
        private List<String> domainClasses = List.of();

        public Builder projectPath(String projectPath) { this.projectPath = projectPath; return this; }
        public Builder layerDependencies(Map<String, List<String>> layerDependencies) { this.layerDependencies = layerDependencies; return this; }
        public Builder controllerClasses(List<String> controllerClasses) { this.controllerClasses = controllerClasses; return this; }
        public Builder serviceClasses(List<String> serviceClasses) { this.serviceClasses = serviceClasses; return this; }
        public Builder repositoryClasses(List<String> repositoryClasses) { this.repositoryClasses = repositoryClasses; return this; }
        public Builder domainClasses(List<String> domainClasses) { this.domainClasses = domainClasses; return this; }

        public ArchTopology build() {
            return new ArchTopology(projectPath, layerDependencies, controllerClasses,
                    serviceClasses, repositoryClasses, domainClasses);
        }
    }
}
