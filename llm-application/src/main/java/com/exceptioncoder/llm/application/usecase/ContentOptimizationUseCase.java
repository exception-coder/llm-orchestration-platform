package com.exceptioncoder.llm.application.usecase;

import com.exceptioncoder.llm.application.service.ContentOptimizationService;
import com.exceptioncoder.llm.domain.model.ContentOptimizationRequest;
import com.exceptioncoder.llm.domain.model.ContentOptimizationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 内容优化用例
 * 编排内容优化的完整业务流程
 */
@Slf4j
@Component
public class ContentOptimizationUseCase {
    
    private final ContentOptimizationService optimizationService;
    
    public ContentOptimizationUseCase(ContentOptimizationService optimizationService) {
        this.optimizationService = optimizationService;
    }
    
    /**
     * 执行单次优化
     */
    public ContentOptimizationResponse execute(ContentOptimizationRequest request) {
        validateRequest(request);
        return optimizationService.optimize(request);
    }
    
    /**
     * 执行多版本优化
     */
    public List<ContentOptimizationResponse> executeMultiple(
            ContentOptimizationRequest request, int count) {
        validateRequest(request);
        
        if (count < 1 || count > 5) {
            throw new IllegalArgumentException("生成数量必须在 1-5 之间");
        }
        
        return optimizationService.optimizeMultiple(request, count);
    }
    
    /**
     * 校验请求
     */
    private void validateRequest(ContentOptimizationRequest request) {
        if (request.getOriginalContent() == null || request.getOriginalContent().trim().isEmpty()) {
            throw new IllegalArgumentException("原始内容不能为空");
        }
        
        if (request.getPlatform() == null) {
            throw new IllegalArgumentException("必须指定目标平台");
        }
        
        if (request.getStyle() == null) {
            throw new IllegalArgumentException("必须指定内容风格");
        }
        
        if (request.getOriginalContent().length() > 5000) {
            throw new IllegalArgumentException("原始内容长度不能超过 5000 字符");
        }
    }
}

