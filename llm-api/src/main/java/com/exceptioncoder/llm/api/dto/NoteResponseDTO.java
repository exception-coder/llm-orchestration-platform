package com.exceptioncoder.llm.api.dto;

import com.exceptioncoder.llm.domain.model.Note;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 记录响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteResponseDTO {

    private Long id;
    private Long categoryId;
    private String categoryName;
    private String categoryIcon;
    private String title;
    private String rawInput;
    private String content;
    private String summary;
    private Boolean isEncrypted;
    private Boolean isVoice;
    private List<String> tags;
    private LocalDateTime createdAt;

    public static NoteResponseDTO fromDomain(Note note, String categoryIcon) {
        return NoteResponseDTO.builder()
                .id(note.getId())
                .categoryId(note.getCategoryId())
                .categoryName(note.getCategoryName())
                .categoryIcon(categoryIcon)
                .title(note.getTitle())
                .rawInput(note.getRawInput())
                .content(note.getContent())
                .summary(note.getSummary())
                .isEncrypted(note.getIsEncrypted())
                .isVoice(note.getIsVoice())
                .tags(note.getTags())
                .createdAt(note.getCreatedAt())
                .build();
    }
}
