package com.exceptioncoder.llm.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 记录类目领域模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteCategory {

    private Long id;

    /**
     * 类目名称
     */
    private String name;

    /**
     * 类目描述
     */
    private String description;

    /**
     * emoji 图标
     */
    private String icon;

    /**
     * 排序顺序
     */
    private Integer sortOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
