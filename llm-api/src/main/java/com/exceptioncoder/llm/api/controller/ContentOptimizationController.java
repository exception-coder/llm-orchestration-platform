package com.exceptioncoder.llm.api.controller;

import com.exceptioncoder.llm.api.dto.ContentOptimizationRequestDTO;
import com.exceptioncoder.llm.api.dto.ContentOptimizationResponseDTO;
import com.exceptioncoder.llm.application.usecase.ContentOptimizationUseCase;
import com.exceptioncoder.llm.domain.model.ContentOptimizationRequest;
import com.exceptioncoder.llm.domain.model.ContentOptimizationResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 内容优化 API 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/content")
public class ContentOptimizationController {
    
    private final ContentOptimizationUseCase optimizationUseCase;
    
    public ContentOptimizationController(ContentOptimizationUseCase optimizationUseCase) {
        this.optimizationUseCase = optimizationUseCase;
    }
    
    /**
     * 优化内容
     */
    @PostMapping(value = "/optimize", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object optimizeContent(@Valid @RequestBody ContentOptimizationRequestDTO requestDTO) {
        log.info("收到内容优化请求，平台: {}, 风格: {}", requestDTO.getPlatform(), requestDTO.getStyle());
        
        ContentOptimizationRequest request = convertToRequest(requestDTO);
        
        // 判断是单次还是多次生成
        if (requestDTO.getCount() != null && requestDTO.getCount() > 1) {
            List<ContentOptimizationResponse> responses = 
                    optimizationUseCase.executeMultiple(request, requestDTO.getCount());
            return responses.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } else {
            ContentOptimizationResponse response = optimizationUseCase.execute(request);
            return convertToDTO(response);
        }
    }
    
    /**
     * 获取支持的平台列表
     */
    @GetMapping("/platforms")
    public List<Map<String, String>> getPlatforms() {
        return List.of(
                createPlatformInfo("XIAOHONGSHU", "小红书", "年轻女性为主，注重生活方式和美学"),
                createPlatformInfo("DOUYIN", "抖音", "短视频配文，需要吸引眼球"),
                createPlatformInfo("TIKTOK", "TikTok", "国际化受众，简洁有力"),
                createPlatformInfo("WEIBO", "微博", "简洁明快，适合热点话题"),
                createPlatformInfo("WECHAT", "微信公众号", "深度内容，逻辑清晰")
        );
    }
    
    /**
     * 获取支持的风格列表
     */
    @GetMapping("/styles")
    public List<Map<String, String>> getStyles() {
        return List.of(
                createStyleInfo("PROFESSIONAL", "专业严谨", "使用专业术语，逻辑严密"),
                createStyleInfo("CASUAL", "轻松随意", "口语化表达，亲切自然"),
                createStyleInfo("HUMOROUS", "幽默风趣", "运用幽默元素，轻松活泼"),
                createStyleInfo("EMOTIONAL", "情感共鸣", "触动情感，引发共鸣"),
                createStyleInfo("INSPIRATIONAL", "励志激励", "积极向上，鼓舞人心"),
                createStyleInfo("TRENDY", "潮流时尚", "紧跟潮流，使用网络热词")
        );
    }
    
    /**
     * 获取支持的内容类型列表
     */
    @GetMapping("/content-types")
    public List<Map<String, String>> getContentTypes() {
        return List.of(
                createContentTypeInfo("PRODUCT_REVIEW", "产品测评"),
                createContentTypeInfo("TUTORIAL", "教程攻略"),
                createContentTypeInfo("LIFESTYLE", "生活分享"),
                createContentTypeInfo("KNOWLEDGE", "知识科普"),
                createContentTypeInfo("STORY", "故事叙述"),
                createContentTypeInfo("OPINION", "观点评论")
        );
    }
    
    private ContentOptimizationRequest convertToRequest(ContentOptimizationRequestDTO dto) {
        return ContentOptimizationRequest.builder()
                .originalContent(dto.getOriginalContent())
                .platform(ContentOptimizationRequest.Platform.valueOf(dto.getPlatform()))
                .style(ContentOptimizationRequest.ContentStyle.valueOf(dto.getStyle()))
                .contentType(dto.getContentType() != null ? 
                        ContentOptimizationRequest.ContentType.valueOf(dto.getContentType()) : null)
                .targetAudience(dto.getTargetAudience())
                .additionalRequirements(dto.getAdditionalRequirements())
                .model(dto.getModel())
                .build();
    }
    
    private ContentOptimizationResponseDTO convertToDTO(ContentOptimizationResponse response) {
        return ContentOptimizationResponseDTO.builder()
                .optimizedContent(response.getOptimizedContent())
                .suggestedTitles(response.getSuggestedTitles())
                .suggestedTags(response.getSuggestedTags())
                .optimizationNotes(response.getOptimizationNotes())
                .platform(response.getPlatform())
                .style(response.getStyle())
                .tokenUsage(response.getTokenUsage() != null ? 
                        ContentOptimizationResponseDTO.TokenUsageDTO.builder()
                                .promptTokens(response.getTokenUsage().getPromptTokens())
                                .completionTokens(response.getTokenUsage().getCompletionTokens())
                                .totalTokens(response.getTokenUsage().getTotalTokens())
                                .build() : null)
                .build();
    }
    
    private Map<String, String> createPlatformInfo(String code, String name, String description) {
        Map<String, String> info = new HashMap<>();
        info.put("code", code);
        info.put("name", name);
        info.put("description", description);
        return info;
    }
    
    private Map<String, String> createStyleInfo(String code, String name, String description) {
        Map<String, String> info = new HashMap<>();
        info.put("code", code);
        info.put("name", name);
        info.put("description", description);
        return info;
    }
    
    private Map<String, String> createContentTypeInfo(String code, String name) {
        Map<String, String> info = new HashMap<>();
        info.put("code", code);
        info.put("name", name);
        return info;
    }
}

