package com.exceptioncoder.llm.domain.devplan.model;

import java.util.Arrays;

/**
 * 项目画像 10 维度枚举。
 *
 * <p>每个维度独立向量化存储到 Qdrant，支持按维度过滤的语义检索。
 * Markdown 模板中 {@code ## N. 维度名} 的编号与 {@link #index()} 一一对应。
 *
 * @author zhangkai
 * @since 2026-04-08
 */
public enum ProfileDimension {

    OVERVIEW(1, "项目概述"),
    TECH_STACK(2, "技术栈"),
    CODE_STRUCTURE(3, "项目结构"),
    ARCH_SPEC(4, "分层架构"),
    DATA_MODEL(5, "数据模型"),
    SERVICE_CAPABILITY(6, "Service能力清单"),
    API(7, "API接口"),
    EXTERNAL_DEPENDENCY(8, "外部依赖服务"),
    CONFIG(9, "配置概要"),
    CODING_CONVENTION(10, "编码约定");

    private final int index;
    private final String label;

    ProfileDimension(int index, String label) {
        this.index = index;
        this.label = label;
    }

    public int index() {
        return index;
    }

    public String label() {
        return label;
    }

    public static ProfileDimension fromIndex(int index) {
        return Arrays.stream(values())
                .filter(d -> d.index == index)
                .findFirst().orElse(null);
    }
}
