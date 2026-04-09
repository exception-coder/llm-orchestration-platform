package com.exceptioncoder.llm.domain.devplan.model;

/**
 * 项目画像 7 维度枚举。
 *
 * <p>每个维度独立向量化存储到 Qdrant，支持按维度过滤的语义检索。
 *
 * @author zhangkai
 * @since 2026-04-08
 */
public enum ProfileDimension {

    OVERVIEW("项目概述"),
    TECH_STACK("技术栈"),
    CODE_STRUCTURE("代码结构"),
    API("现有API"),
    DATA_MODEL("数据模型"),
    ARCH_SPEC("架构规范"),
    CONFIG("配置概要");

    private final String label;

    ProfileDimension(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
