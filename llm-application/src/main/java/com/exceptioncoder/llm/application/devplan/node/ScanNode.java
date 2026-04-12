package com.exceptioncoder.llm.application.devplan.node;

import com.exceptioncoder.llm.domain.devplan.model.DevPlanState;
import com.exceptioncoder.llm.domain.devplan.service.ProfileCacheStrategy;
import com.exceptioncoder.llm.domain.devplan.service.ProfileGeneratorService;
import com.exceptioncoder.llm.domain.devplan.service.ProfileIndexService;
import com.exceptioncoder.llm.domain.devplan.service.ProfileReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

/**
 * 代码感知节点（ScanNode）—— 开发方案生成流程的第一个执行节点。
 *
 * <p>v2 重构：从"实时扫描 + LLM 综合"改为"Markdown 缓存 + SPI 生成器链"模式。
 * <ul>
 *   <li>缓存命中：直接读取 {@code docs/project-profile.md}，&lt; 100ms</li>
 *   <li>缓存未命中：通过 {@link ProfileGeneratorService} 按优先级尝试生成</li>
 *   <li>异步：生成完成后异步调用 {@link ProfileIndexService} 写入 Qdrant</li>
 * </ul>
 *
 * <p>本类仅依赖 Domain 层接口，通过 Spring 注入获取 Infrastructure 层实现。
 *
 * @author zhangkai
 * @since 2026-04-06
 */
@Slf4j
@Component
public class ScanNode {

    private final ProfileGeneratorService generatorService;
    private final ProfileReader profileReader;
    private final ProfileIndexService profileIndexService;
    private final ProfileCacheStrategy cacheStrategy;

    public ScanNode(ProfileGeneratorService generatorService,
                    ProfileReader profileReader,
                    ProfileIndexService profileIndexService,
                    ProfileCacheStrategy cacheStrategy) {
        this.generatorService = generatorService;
        this.profileReader = profileReader;
        this.profileIndexService = profileIndexService;
        this.cacheStrategy = cacheStrategy;
    }

    /**
     * 执行代码感知扫描。
     *
     * <p>处理步骤：</p>
     * <ol>
     *   <li>检查 project-profile.md 是否存在且未过期</li>
     *   <li>命中缓存则直接读取；否则走 ProfileGeneratorService</li>
     *   <li>异步触发 ProfileIndexService 按维度分片写入 Qdrant</li>
     *   <li>构建新的 {@link DevPlanState}，状态标记为 SCANNING_COMPLETE</li>
     * </ol>
     */
    public DevPlanState execute(DevPlanState state) {
        log.info("执行 ScanNode，projectPath={}", state.projectPath());
        long start = System.currentTimeMillis();
        String markdown;

        Path profilePath = Path.of(state.projectPath(), "docs", "project-profile.md");

        if (cacheStrategy.isFresh(profilePath, state.projectPath())) {
            try {
                markdown = profileReader.readFull(profilePath);
                log.info("Profile cache hit for {}", state.projectPath());
            } catch (IOException e) {
                log.warn("Failed to read cached profile, falling back to generation", e);
                markdown = generateAndPersist(state.projectPath(), profilePath);
            }
        } else {
            markdown = generateAndPersist(state.projectPath(), profilePath);
        }

        long elapsed = System.currentTimeMillis() - start;
        var timings = new HashMap<>(state.nodeTimings());
        timings.put("scan", elapsed);

        // 异步向量化
        asyncIndexProfile(markdown, state.projectPath());

        return DevPlanState.builder()
                .taskId(state.taskId())
                .projectPath(state.projectPath())
                .requirement(state.requirement())
                .projectProfile(markdown)
                .status("SCANNING_COMPLETE")
                .nodeTimings(timings)
                .agentTokenUsage(state.agentTokenUsage())
                .correctionCount(state.correctionCount())
                .reviewIssues(state.reviewIssues())
                .build();
    }

    private String generateAndPersist(String projectPath, Path profilePath) {
        String markdown = generatorService.generateOrThrow(projectPath);

        // ClaudeCode 会自己写文件，Fallback 需要手动写
        if (!Files.exists(profilePath)) {
            try {
                Files.createDirectories(profilePath.getParent());
                Files.writeString(profilePath, markdown);
                log.info("Profile written to {}", profilePath);
            } catch (IOException e) {
                log.warn("Failed to persist profile to {}", profilePath, e);
            }
        }

        return markdown;
    }

    @Async
    void asyncIndexProfile(String markdown, String projectPath) {
        try {
            profileIndexService.indexFromMarkdown(markdown, projectPath);
        } catch (Exception e) {
            log.warn("Async profile indexing failed for {}", projectPath, e);
        }
    }
}
