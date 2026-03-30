package com.exceptioncoder.llm.domain.model;

import java.util.List;

/**
 * 文档检索结果
 */
public record DocSearchResult(
        List<Hit> hits,
        int total
) {
    public record Hit(
            String path,
            String name,
            String content,
            double score
    ) {}
}
