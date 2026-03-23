package com.exceptioncoder.llm.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 记录输入 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteInputDTO {

    /**
     * 用户输入的原始内容
     */
    @NotBlank(message = "内容不能为空")
    @Size(max = 10000, message = "内容长度不能超过10000字符")
    private String content;

    /**
     * 是否来自语音
     */
    private Boolean isVoice;

    /**
     * 使用的模型（可选）
     */
    private String model;
}
