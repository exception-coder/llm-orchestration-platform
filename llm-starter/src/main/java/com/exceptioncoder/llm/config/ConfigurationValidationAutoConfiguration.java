package com.exceptioncoder.llm.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 配置验证自动配置类
 * 
 * @author system
 */
@Configuration
@EnableConfigurationProperties(ConfigurationValidationProperties.class)
public class ConfigurationValidationAutoConfiguration {
    // 自动配置类，用于启用 ConfigurationProperties
}

