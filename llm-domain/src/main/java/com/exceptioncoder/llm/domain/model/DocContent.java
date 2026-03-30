package com.exceptioncoder.llm.domain.model;

/**
 * 文档内容响应模型
 */
public record DocContent(
        String path,
        String name,
        String content,
        long size
) {}
