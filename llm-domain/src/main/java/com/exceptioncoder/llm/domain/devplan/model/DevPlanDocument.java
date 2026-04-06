package com.exceptioncoder.llm.domain.devplan.model;

import java.util.Map;

/**
 * 生成的设计文档
 */
public record DevPlanDocument(
        String fullDocument,
        Map<String, String> sections
) {
}
