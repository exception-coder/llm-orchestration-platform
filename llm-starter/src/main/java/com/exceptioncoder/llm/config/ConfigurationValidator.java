package com.exceptioncoder.llm.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置验证器
 * 在应用启动后检查环境配置文件是否正确加载
 * 
 * @author system
 */
@Component
@Order(1) // 确保在其他 ApplicationRunner 之前执行
public class ConfigurationValidator implements ApplicationRunner {
    
    private static final Logger log = LoggerFactory.getLogger(ConfigurationValidator.class);
    
    private final Environment environment;
    private final ResourceLoader resourceLoader;
    private final ConfigurationValidationProperties validationProperties;
    
    public ConfigurationValidator(
            Environment environment,
            ResourceLoader resourceLoader,
            ConfigurationValidationProperties validationProperties) {
        this.environment = environment;
        this.resourceLoader = resourceLoader;
        this.validationProperties = validationProperties;
    }
    
    @Override
    public void run(ApplicationArguments args) {
        if (!validationProperties.isEnabled()) {
            log.debug("配置验证已禁用，跳过检查");
            return;
        }
        
        log.info("开始执行配置验证检查...");
        
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // 1. 检查当前激活的 profile
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length == 0) {
            warnings.add("未检测到激活的 Spring Profile，将使用默认配置");
        } else {
            log.info("当前激活的 Profile: {}", String.join(", ", activeProfiles));
            
            // 2. 检查每个激活的 profile 对应的配置文件
            for (String profile : activeProfiles) {
                validateProfileConfiguration(profile, errors, warnings);
            }
        }
        
        // 3. 验证关键配置属性是否已加载
        validateRequiredProperties(errors, warnings);
        
        // 4. 输出检查结果
        logValidationResults(errors, warnings);
        
        // 5. 根据配置决定是否阻止启动
        if (!errors.isEmpty() && validationProperties.isFailOnError()) {
            throw new IllegalStateException(
                "配置验证失败，应用启动被阻止。请检查上述错误信息并修复配置问题。");
        }
    }
    
    /**
     * 验证指定 profile 的配置文件
     */
    private void validateProfileConfiguration(String profile, List<String> errors, List<String> warnings) {
        log.info("验证 Profile [{}] 的配置文件...", profile);
        
        String[] requiredFiles = validationProperties.getRequiredFiles();
        for (String fileName : requiredFiles) {
            String resourcePath = String.format("classpath:config/%s/%s", profile, fileName);
            Resource resource = resourceLoader.getResource(resourcePath);
            
            if (!resource.exists()) {
                String error = String.format(
                    "Profile [%s] 的配置文件不存在: %s (期望路径: %s)",
                    profile, fileName, resourcePath);
                errors.add(error);
                log.error(error);
            } else {
                log.debug("✓ 配置文件存在: {}", resourcePath);
            }
        }
    }
    
    /**
     * 验证关键配置属性是否已加载
     */
    private void validateRequiredProperties(List<String> errors, List<String> warnings) {
        log.info("验证关键配置属性是否已加载...");
        
        String[] requiredProperties = validationProperties.getRequiredProperties();
        for (String propertyKey : requiredProperties) {
            String value = environment.getProperty(propertyKey);
            
            if (!StringUtils.hasText(value)) {
                String warning = String.format(
                    "关键配置属性未设置或为空: %s (可能使用了默认值或环境变量)",
                    propertyKey);
                warnings.add(warning);
                log.warn(warning);
            } else {
                // 不输出实际值，避免敏感信息泄露
                log.debug("✓ 配置属性已加载: {}", propertyKey);
            }
        }
    }
    
    /**
     * 输出验证结果
     */
    private void logValidationResults(List<String> errors, List<String> warnings) {
        if (errors.isEmpty() && warnings.isEmpty()) {
            log.info("✓ 配置验证通过，所有检查项均正常");
            return;
        }
        
        log.info("配置验证完成，发现 {} 个错误，{} 个警告", errors.size(), warnings.size());
        
        if (!errors.isEmpty()) {
            log.error("=== 配置验证错误 ===");
            errors.forEach(error -> log.error("  ✗ {}", error));
        }
        
        if (!warnings.isEmpty()) {
            log.warn("=== 配置验证警告 ===");
            warnings.forEach(warning -> log.warn("  ⚠ {}", warning));
        }
        
        if (!errors.isEmpty() && !validationProperties.isFailOnError()) {
            log.warn("配置验证发现错误，但 fail-on-error 为 false，应用将继续启动");
        }
    }
}

