package com.exceptioncoder.llm.domain.devplan.service;

import java.util.Optional;

/**
 * 项目画像生成器 SPI。
 *
 * <p>不同实现对应不同的编码工具（Claude Code / Cursor / 内部 Fallback）。
 * {@link com.exceptioncoder.llm.infrastructure.devplan.profile.ProfileGeneratorChain}
 * 按 {@link #order()} 依次尝试，第一个成功即返回。
 *
 * @author zhangkai
 * @since 2026-04-11
 */
public interface ProfileGenerator {

    /**
     * 生成项目画像 Markdown。
     *
     * @param projectPath 项目根目录绝对路径
     * @return 生成的 Markdown 内容；生成失败返回 empty
     */
    Optional<String> generate(String projectPath);

    /**
     * 当前生成器是否可用（配置已启用 + 外部依赖可达）。
     */
    boolean isAvailable();

    /**
     * 优先级，数值越小优先级越高。
     */
    int order();

    /**
     * 是否基于大模型 CLI 编程工具（如 Claude Code / Cursor）。
     *
     * <p>{@code require-llm-generator=true} 时，只有此方法返回 {@code true}
     * 的生成器参与 SPI 链，内部 Tool 模板拼装类生成器会被跳过。
     */
    default boolean isLlmBased() {
        return false;
    }
}
