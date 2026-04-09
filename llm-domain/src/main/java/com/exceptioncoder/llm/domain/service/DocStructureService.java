package com.exceptioncoder.llm.domain.service;

/**
 * 文档目录结构服务 -- 领域层接口，定义文档目录解析与更新的契约。
 *
 * <p>由 infrastructure 层的 DocStructureAgent 实现。
 * application 层通过此接口调用，避免直接依赖 infrastructure 层。
 *
 * @author zhangkai
 * @since 2026-04-08
 */
public interface DocStructureService {

    /**
     * 执行文档目录结构更新。
     *
     * @param readmeContent docs/README.md 的完整内容
     * @param readmeHash    README.md 内容的 SHA-256 hash
     * @return Agent 最终输出文本
     */
    String execute(String readmeContent, String readmeHash);
}
