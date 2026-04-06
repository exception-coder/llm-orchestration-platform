package com.exceptioncoder.llm.domain.devplan.model;

import java.util.List;
import java.util.Map;

/**
 * 架构拓扑 — 分层依赖关系
 */
public record ArchTopology(
        String projectPath,
        Map<String, List<String>> layerDependencies,
        List<String> controllerClasses,
        List<String> serviceClasses,
        List<String> repositoryClasses,
        List<String> domainClasses
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
