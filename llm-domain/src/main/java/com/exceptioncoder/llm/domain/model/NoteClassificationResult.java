package com.exceptioncoder.llm.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 记录分类结果（LLM 返回的结构）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteClassificationResult {

    /**
     * 类目名称
     */
    private String category;

    /**
     * 新类目描述（仅当 AI 新建类目时使用）
     */
    private String categoryDescription;

    /**
     * 新类目图标（仅当 AI 新建类目时使用）
     */
    private String categoryIcon;

    /**
     * 标题（15字以内）
     */
    private String title;

    /**
     * 一句话摘要
     */
    private String summary;

    /**
     * 整理后的内容（Markdown 格式）
     */
    private String structuredContent;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 是否包含敏感信息
     */
    private Boolean isSensitive;
}
