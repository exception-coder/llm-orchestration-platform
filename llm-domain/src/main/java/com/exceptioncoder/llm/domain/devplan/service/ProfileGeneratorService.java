package com.exceptioncoder.llm.domain.devplan.service;

import com.exceptioncoder.llm.domain.devplan.exception.ProfileGenerationException;

/**
 * 项目画像生成编排服务 —— ScanNode 通过此接口触发画像生成，不直接依赖基础设施层。
 *
 * @author zhangkai
 * @since 2026-04-11
 */
public interface ProfileGeneratorService {

    /**
     * 按优先级依次尝试生成画像，第一个成功即返回。
     *
     * @param projectPath 项目根目录绝对路径
     * @return 生成的 Markdown 内容
     * @throws ProfileGenerationException 所有生成器均失败
     */
    String generateOrThrow(String projectPath);
}
