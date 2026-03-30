package com.exceptioncoder.llm.domain.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档目录结构版本领域模型
 */
@Data
@Builder
public class DocStructureVersion {

    private Long id;

    /** 版本号，从 1 开始单调递增 */
    private int version;

    /** 目录结构 JSON（DocTreeNode[] 数组序列化） */
    private String structure;

    /** 本次与上一版本的差异描述（由 LLM 生成） */
    private String diffSummary;

    /** docs/README.md 内容的 SHA-256 hash，用于幂等检测 */
    private String readmeHash;

    /** 是否为当前生效版本，任意时刻只有一条为 true */
    private boolean active;

    private LocalDateTime createdAt;
}
