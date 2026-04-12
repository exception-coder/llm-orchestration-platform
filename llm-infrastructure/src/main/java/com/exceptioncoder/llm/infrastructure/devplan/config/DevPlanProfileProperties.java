package com.exceptioncoder.llm.infrastructure.devplan.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 项目画像生成相关配置。
 *
 * @author zhangkai
 * @since 2026-04-11
 */
@Data
@Component
@ConfigurationProperties(prefix = "devplan.profile")
public class DevPlanProfileProperties {

    /**
     * 是否强制要求通过大模型 CLI 编程工具生成画像。
     * true 时 InternalFallbackGenerator 不参与 SPI 链，LLM 生成器全部失败直接报错。
     */
    private boolean requireLlmGenerator = false;

    private Generators generators = new Generators();
    private Cache cache = new Cache();

    @Data
    public static class Generators {
        private ClaudeCode claudeCode = new ClaudeCode();
        private InternalFallback internalFallback = new InternalFallback();
    }

    @Data
    public static class ClaudeCode {
        private boolean enabled = true;
        /**
         * 调用模式：sdk（推荐）或 cli（备选）。
         */
        private String mode = "sdk";
        private String cliPath = "claude";
        private String nodePath = "node";
        private String sdkBridgePath = "scripts/claude-code-bridge.mjs";
        /**
         * 空闲超时（秒）：连续多长时间无输出则判定为卡死并终止。
         */
        private int idleTimeoutSeconds = 120;
        /**
         * 总超时上限（秒）：兜底保护，0 表示不限制。
         */
        private int maxTimeoutSeconds = 1800;
        private String skillName = "generate-project-profile";
    }

    @Data
    public static class InternalFallback {
        private boolean enabled = true;
    }

    @Data
    public static class Cache {
        private String freshnessCheck = "git-commit-time";
        private int maxAgeHours = 24;
    }
}
