package com.exceptioncoder.llm.domain.devplan.model;

import java.util.List;
import java.util.Map;

/**
 * 项目结构信息 -- 代码感知阶段扫描得到的项目骨架描述。
 *
 * <p>属于 Domain 层 devplan 模块。由 CODE_AWARENESS Agent 扫描目标项目后生成，
 * 包含项目路径、基础包名、Maven/Gradle 模块列表以及按目录聚合的文件树。
 * 下游节点（需求分析、方案设计）依赖此信息定位代码位置与模块边界。</p>
 *
 * @author zhangkai
 * @since 2026-04-06
 */
public record ProjectStructure(
        String projectPath,                     // 目标项目的本地绝对路径
        String basePackage,                     // 项目根包名，如 com.example.app
        List<String> modules,                   // 子模块列表（多模块项目适用）
        Map<String, List<String>> fileTree      // 文件树，key 为目录路径，value 为该目录下的文件名列表
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
