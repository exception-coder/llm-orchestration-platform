package com.exceptioncoder.llm.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 加密记录输入 DTO（前端加密后提交）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteEncryptedInputDTO {

    @NotNull(message = "类目ID不能为空")
    private Long categoryId;

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "原始内容不能为空")
    private String rawInput;

    /**
     * 加密后的内容（AES-GCM 加密后的 Base64 字符串）
     */
    @NotBlank(message = "加密内容不能为空")
    private String encryptedContent;

    /**
     * 是否来自语音
     */
    private Boolean isVoice;

    /**
     * 标签列表
     */
    private List<String> tags;
}
