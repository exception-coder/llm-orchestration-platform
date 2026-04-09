package com.exceptioncoder.llm.domain.devplan.analysis;

import java.nio.file.Path;
import java.util.List;

/**
 * 语言/框架分析器 SPI -- 所有项目结构分析能力的统一接口。
 *
 * <p>每种技术栈（Java Spring、Vue、React 等）提供一个实现。
 * 由 {@link LanguageAnalyzerRegistry} 根据项目类型路由到对应分析器。
 *
 * <p><b>扩展方式：</b>
 * <ol>
 *   <li>在 {@link ProjectType} 中新增枚举值</li>
 *   <li>创建对应的 LanguageAnalyzer 实现类（标注 @Component）</li>
 *   <li>实现 {@link #supports(Path)} 和各分析方法</li>
 *   <li>LanguageAnalyzerRegistry 会自动发现并注册</li>
 * </ol>
 */
public interface LanguageAnalyzer {

    /**
     * 该分析器支持的项目类型。
     */
    ProjectType projectType();

    /**
     * 判断给定目录是否是本分析器能处理的项目。
     *
     * @param projectRoot 项目根目录
     * @return true 表示匹配（如检测到 pom.xml + Spring 依赖）
     */
    boolean supports(Path projectRoot);

    /**
     * 分析代码结构（类、注解、层间依赖等）。
     */
    CodeStructureResult analyzeCodeStructure(Path projectRoot);

    /**
     * 分析项目依赖。
     */
    DependencyResult analyzeDependencies(Path projectRoot);

    /**
     * 分析项目配置。
     */
    ConfigResult analyzeConfig(Path projectRoot);

    /**
     * 提取代码的嵌入文本（用于向量化索引）。
     */
    List<EmbeddingText> extractEmbeddingTexts(Path projectRoot);
}
