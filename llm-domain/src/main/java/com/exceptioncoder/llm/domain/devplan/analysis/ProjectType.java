package com.exceptioncoder.llm.domain.devplan.analysis;

/**
 * 项目技术栈类型枚举。
 *
 * <p>用于 {@link LanguageAnalyzer} 选择匹配的分析器。
 * 新增语言/框架类型时扩展此枚举。
 */
public enum ProjectType {

    /** Spring Boot / Spring Cloud Java 项目 */
    JAVA_SPRING("Java Spring", "java"),

    /** Vue.js 前端项目（预留） */
    VUE("Vue.js", "javascript"),

    /** React 前端项目（预留） */
    REACT("React", "javascript"),

    /** Python 项目（预留） */
    PYTHON("Python", "python"),

    /** 未知类型 */
    UNKNOWN("Unknown", "unknown");

    private final String displayName;
    private final String language;

    ProjectType(String displayName, String language) {
        this.displayName = displayName;
        this.language = language;
    }

    public String displayName() { return displayName; }
    public String language() { return language; }
}
