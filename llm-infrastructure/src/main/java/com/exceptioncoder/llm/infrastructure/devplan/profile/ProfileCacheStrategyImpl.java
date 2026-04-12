package com.exceptioncoder.llm.infrastructure.devplan.profile;

import com.exceptioncoder.llm.domain.devplan.service.ProfileCacheStrategy;
import com.exceptioncoder.llm.infrastructure.devplan.config.DevPlanProfileProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * 画像缓存过期判断策略实现。
 *
 * <p>支持三种策略：
 * <ul>
 *   <li>{@code git-commit-time}（默认）：profile 修改时间晚于最近 git commit 时间</li>
 *   <li>{@code file-age}：profile 文件年龄不超过配置的最大时长</li>
 *   <li>{@code always-fresh}：始终视为有效（调试用）</li>
 * </ul>
 *
 * @author zhangkai
 * @since 2026-04-11
 */
@Slf4j
@Component
public class ProfileCacheStrategyImpl implements ProfileCacheStrategy {

    private final DevPlanProfileProperties properties;

    public ProfileCacheStrategyImpl(DevPlanProfileProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean isFresh(Path profilePath, String projectPath) {
        if (!Files.exists(profilePath)) {
            return false;
        }
        try {
            String strategy = properties.getCache().getFreshnessCheck();
            Instant profileTime = Files.getLastModifiedTime(profilePath).toInstant();

            if ("always-fresh".equals(strategy)) {
                return true;
            }
            if ("file-age".equals(strategy)) {
                int maxAgeHours = properties.getCache().getMaxAgeHours();
                return profileTime.isAfter(Instant.now().minusSeconds(maxAgeHours * 3600L));
            }
            // 默认 git-commit-time
            Instant lastCommit = getLastCommitTime(projectPath);
            return lastCommit != null && profileTime.isAfter(lastCommit);
        } catch (Exception e) {
            log.debug("Failed to check profile freshness, treating as stale", e);
            return false;
        }
    }

    private Instant getLastCommitTime(String projectPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "log", "-1", "--format=%ct");
            pb.directory(Path.of(projectPath).toFile());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = reader.readLine();
            }

            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return null;
            }
            if (process.exitValue() != 0 || output == null || output.isBlank()) {
                return null;
            }
            return Instant.ofEpochSecond(Long.parseLong(output.trim()));
        } catch (Exception e) {
            log.debug("Failed to get last git commit time", e);
            return null;
        }
    }
}
