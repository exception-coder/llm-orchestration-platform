package com.exceptioncoder.llm.api.dto;

import com.exceptioncoder.llm.domain.model.NoteCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 类目响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteCategoryDTO {

    private Long id;
    private String name;
    private String description;
    private String icon;
    private Long noteCount;
    private LocalDateTime createdAt;

    public static NoteCategoryDTO fromDomain(NoteCategory category, Long noteCount) {
        return NoteCategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .icon(category.getIcon())
                .noteCount(noteCount)
                .createdAt(category.getCreatedAt())
                .build();
    }
}
