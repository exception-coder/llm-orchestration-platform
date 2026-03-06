package com.exceptioncoder.llm.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 内容优化请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentOptimizationRequest {
    
    /**
     * 原始文本内容
     */
    private String originalContent;
    
    /**
     * 目标平台
     */
    private Platform platform;
    
    /**
     * 内容风格
     */
    private ContentStyle style;
    
    /**
     * 内容类型
     */
    private ContentType contentType;
    
    /**
     * 目标受众
     */
    private String targetAudience;
    
    /**
     * 额外要求
     */
    private String additionalRequirements;
    
    /**
     * 使用的模型
     */
    private String model;
    
    /**
     * 平台枚举
     */
    public enum Platform {
        XIAOHONGSHU("小红书", "年轻女性为主，注重生活方式和美学，喜欢emoji和分段"),
        DOUYIN("抖音", "短视频配文，需要吸引眼球，引发互动，适合口语化表达"),
        TIKTOK("TikTok", "国际化受众，简洁有力，注重趣味性和话题性"),
        WEIBO("微博", "简洁明快，适合热点话题，字数限制"),
        WECHAT("微信公众号", "深度内容，逻辑清晰，适合长文");
        
        private final String displayName;
        private final String characteristics;
        
        Platform(String displayName, String characteristics) {
            this.displayName = displayName;
            this.characteristics = characteristics;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getCharacteristics() {
            return characteristics;
        }
    }
    
    /**
     * 内容风格枚举
     */
    public enum ContentStyle {
        PROFESSIONAL("专业严谨", "使用专业术语，逻辑严密，数据支撑"),
        CASUAL("轻松随意", "口语化表达，亲切自然，贴近生活"),
        HUMOROUS("幽默风趣", "运用幽默元素，轻松活泼，引人发笑"),
        EMOTIONAL("情感共鸣", "触动情感，引发共鸣，真诚温暖"),
        INSPIRATIONAL("励志激励", "积极向上，鼓舞人心，传递正能量"),
        TRENDY("潮流时尚", "紧跟潮流，使用网络热词，年轻化表达");
        
        private final String displayName;
        private final String description;
        
        ContentStyle(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 内容类型枚举
     */
    public enum ContentType {
        PRODUCT_REVIEW("产品测评"),
        TUTORIAL("教程攻略"),
        LIFESTYLE("生活分享"),
        KNOWLEDGE("知识科普"),
        STORY("故事叙述"),
        OPINION("观点评论");
        
        private final String displayName;
        
        ContentType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}

