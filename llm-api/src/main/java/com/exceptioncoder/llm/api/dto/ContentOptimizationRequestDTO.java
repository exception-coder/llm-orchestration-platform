package com.exceptioncoder.llm.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 内容优化请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentOptimizationRequestDTO {
    
    /**
     * 原始内容
     */
    @NotBlank(message = "原始内容不能为空")
    @Size(max = 5000, message = "内容长度不能超过5000字符")
    private String originalContent;
    
    /**
     * 目标平台
     */
    @NotNull(message = "必须指定目标平台")
    private String platform;
    
    /**
     * 内容风格
     */
    @NotNull(message = "必须指定内容风格")
    private String style;
    
    /**
     * 内容类型（可选）
     */
    private String contentType;
    
    /**
     * 目标受众（可选）
     */
    private String targetAudience;
    
    /**
     * 额外要求（可选）
     */
    private String additionalRequirements;
    
    /**
     * 使用的模型（可选）
     */
    private String model;
    
    /**
     * 生成数量（可选，默认1）
     */
    private Integer count;
}

