package com.exceptioncoder.llm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 配置验证属性
 * 
 * @author system
 */
@ConfigurationProperties(prefix = "llm.config.validation")
public class ConfigurationValidationProperties {
    
    /**
     * 是否启用配置验证
     */
    private boolean enabled = true;
    
    /**
     * 验证失败时是否阻止启动
     */
    private boolean failOnError = false;
    
    /**
     * 需要验证的配置文件列表（相对于 config/{profile}/ 目录）
     */
    private String[] requiredFiles = {"datasource.yml", "spring-ai.yml", "logging.yml"};
    
    /**
     * 需要验证的关键配置属性（用于确认配置已加载）
     */
    private String[] requiredProperties = {
        "spring.datasource.url",
        "spring.ai.openai.api-key"
    };

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isFailOnError() {
        return failOnError;
    }

    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    public String[] getRequiredFiles() {
        return requiredFiles;
    }

    public void setRequiredFiles(String[] requiredFiles) {
        this.requiredFiles = requiredFiles;
    }

    public String[] getRequiredProperties() {
        return requiredProperties;
    }

    public void setRequiredProperties(String[] requiredProperties) {
        this.requiredProperties = requiredProperties;
    }
}

