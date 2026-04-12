package com.exceptioncoder.llm.domain.devplan.service;

import java.nio.file.Path;

/**
 * 画像缓存过期判断策略 —— 供 Application 层判断画像是否需要重新生成。
 *
 * @author zhangkai
 * @since 2026-04-11
 */
public interface ProfileCacheStrategy {

    /**
     * 判断画像文件是否仍然有效（未过期）。
     *
     * @param profilePath 画像文件路径
     * @param projectPath 项目根目录路径
     * @return true 表示缓存有效，无需重新生成
     */
    boolean isFresh(Path profilePath, String projectPath);
}
