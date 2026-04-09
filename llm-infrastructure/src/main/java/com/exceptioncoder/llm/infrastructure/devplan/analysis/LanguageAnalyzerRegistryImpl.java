package com.exceptioncoder.llm.infrastructure.devplan.analysis;

import com.exceptioncoder.llm.domain.devplan.analysis.LanguageAnalyzer;
import com.exceptioncoder.llm.domain.devplan.analysis.LanguageAnalyzerRegistry;
import com.exceptioncoder.llm.domain.devplan.analysis.ProjectType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 语言分析器注册中心实现 -- 启动时自动收集所有 {@link LanguageAnalyzer} Bean。
 */
@Slf4j
@Component
public class LanguageAnalyzerRegistryImpl implements LanguageAnalyzerRegistry {

    private final List<LanguageAnalyzer> analyzers;
    private final Map<ProjectType, LanguageAnalyzer> analyzerMap;

    public LanguageAnalyzerRegistryImpl(List<LanguageAnalyzer> analyzers) {
        this.analyzers = analyzers;
        this.analyzerMap = analyzers.stream()
                .collect(Collectors.toMap(LanguageAnalyzer::projectType, Function.identity(),
                        (a, b) -> a));
        log.info("语言分析器注册完成，共 {} 个: {}", analyzers.size(),
                analyzers.stream().map(a -> a.projectType().displayName()).toList());
    }

    @Override
    public Optional<LanguageAnalyzer> detect(Path projectRoot) {
        for (LanguageAnalyzer analyzer : analyzers) {
            if (analyzer.supports(projectRoot)) {
                log.debug("项目 {} 匹配分析器: {}", projectRoot, analyzer.projectType().displayName());
                return Optional.of(analyzer);
            }
        }
        log.warn("项目 {} 未匹配到任何分析器", projectRoot);
        return Optional.empty();
    }

    @Override
    public Optional<LanguageAnalyzer> get(ProjectType type) {
        return Optional.ofNullable(analyzerMap.get(type));
    }
}
