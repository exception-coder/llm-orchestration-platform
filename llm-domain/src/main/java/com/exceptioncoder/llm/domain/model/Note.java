package com.exceptioncoder.llm.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 记录领域模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Note {

    private Long id;

    /**
     * 类目ID
     */
    private Long categoryId;

    /**
     * 类目名称（冗余展示用）
     */
    private String categoryName;

    /**
     * 标题
     */
    private String title;

    /**
     * 用户原始输入
     */
    private String rawInput;

    /**
     * 整理后的内容（加密时存密文）
     */
    private String content;

    /**
     * AI生成的一句话摘要
     */
    private String summary;

    /**
     * 是否加密
     */
    private Boolean isEncrypted;

    /**
     * 是否来自语音
     */
    private Boolean isVoice;

    /**
     * 标签列表
     */
    private List<String> tags;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
