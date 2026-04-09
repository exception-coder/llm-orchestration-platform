package com.exceptioncoder.llm.domain.devplan.analysis;

import java.nio.file.Path;
import java.util.Optional;

/**
 * 语言分析器注册中心 -- 根据项目路径自动选择匹配的 {@link LanguageAnalyzer}。
 *
 * <p>Infrastructure 层提供实现，启动时自动收集所有 LanguageAnalyzer Bean。
 */
public interface LanguageAnalyzerRegistry {

    /**
     * 根据项目路径探测并返回匹配的分析器。
     *
     * @param projectRoot 项目根目录
     * @return 匹配的分析器，未匹配时返回 empty
     */
    Optional<LanguageAnalyzer> detect(Path projectRoot);

    /**
     * 按类型获取分析器。
     */
    Optional<LanguageAnalyzer> get(ProjectType type);
}
