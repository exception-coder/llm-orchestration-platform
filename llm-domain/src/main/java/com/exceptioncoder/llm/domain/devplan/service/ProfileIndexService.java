package com.exceptioncoder.llm.domain.devplan.service;

/**
 * 项目画像向量索引服务 —— 供 Application 层触发异步索引，不直接依赖 Tool 实现。
 *
 * @author zhangkai
 * @since 2026-04-11
 */
public interface ProfileIndexService {

    /**
     * 从 Markdown 按维度分片写入向量存储。
     *
     * @param markdown project-profile.md 的完整内容
     * @param projectPath 项目根目录绝对路径
     */
    void indexFromMarkdown(String markdown, String projectPath);
}
