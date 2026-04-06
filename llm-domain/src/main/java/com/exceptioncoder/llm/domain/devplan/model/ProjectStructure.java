package com.exceptioncoder.llm.domain.devplan.model;

import java.util.List;
import java.util.Map;

/**
 * 项目结构信息
 */
public record ProjectStructure(
        String projectPath,
        String basePackage,
        List<String> modules,
        Map<String, List<String>> fileTree
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String projectPath;
        private String basePackage;
        private List<String> modules = List.of();
        private Map<String, List<String>> fileTree = Map.of();

        public Builder projectPath(String projectPath) { this.projectPath = projectPath; return this; }
        public Builder basePackage(String basePackage) { this.basePackage = basePackage; return this; }
        public Builder modules(List<String> modules) { this.modules = modules; return this; }
        public Builder fileTree(Map<String, List<String>> fileTree) { this.fileTree = fileTree; return this; }

        public ProjectStructure build() {
            return new ProjectStructure(projectPath, basePackage, modules, fileTree);
        }
    }
}
