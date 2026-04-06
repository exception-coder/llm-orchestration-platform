package com.exceptioncoder.llm.domain.devplan.model;

import java.util.Map;

/**
 * 生成的设计文档 -- SOLUTION_ARCHITECT Agent 按模板逐章节生成的开发方案。
 *
 * <p>属于 Domain 层 devplan 模块。同时保留完整文档文本和按章节拆分的 Map，
 * 便于审查阶段对单独章节进行质量评审与针对性修正。</p>
 *
 * @author zhangkai
 * @since 2026-04-06
 */
public record DevPlanDocument(
        String fullDocument,                // 完整的设计文档文本（Markdown 格式）
        Map<String, String> sections        // 按章节拆分的内容映射，key 为章节标题，value 为章节正文
) {
}
