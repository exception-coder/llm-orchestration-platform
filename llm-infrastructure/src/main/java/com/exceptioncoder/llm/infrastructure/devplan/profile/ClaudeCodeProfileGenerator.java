package com.exceptioncoder.llm.infrastructure.devplan.profile;

import com.exceptioncoder.llm.domain.devplan.service.ProfileGenerator;
import com.exceptioncoder.llm.infrastructure.devplan.config.DevPlanProfileProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * 基于 Claude Code 的画像生成器。
 *
 * <p>支持两种调用模式（通过 {@code mode} 配置切换）：
 * <ul>
 *     <li><b>sdk</b>（默认推荐）— 通过 Node.js 桥接脚本调用 {@code @anthropic-ai/claude-code} SDK，
 *         支持完整工具链、Skills 加载和结构化事件流。</li>
 *     <li><b>cli</b>（备选）— 直接调用 {@code claude --print} CLI，轻量但 Skills 不可用。</li>
 * </ul>
 *
 * @author zhangkai
 * @since 2026-04-11
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "devplan.profile.generators.claude-code", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ClaudeCodeProfileGenerator implements ProfileGenerator {

    private static final String MODE_SDK = "sdk";

    private final DevPlanProfileProperties.ClaudeCode config;

    public ClaudeCodeProfileGenerator(DevPlanProfileProperties properties) {
        this.config = properties.getGenerators().getClaudeCode();
    }

    @Override
    public Optional<String> generate(String projectPath) {
        String mode = config.getMode();
        log.info("ClaudeCodeProfileGenerator mode={}, project={}", mode, projectPath);

        return MODE_SDK.equalsIgnoreCase(mode)
                ? generateViaSdk(projectPath)
                : generateViaCli(projectPath);
    }

    // ==================== SDK 模式（推荐） ====================

    /**
     * 通过 Claude Code SDK 桥接脚本生成画像。
     *
     * <p>调用链：Java → node claude-code-bridge.mjs → @anthropic-ai/claude-code SDK
     * <br>优势：完整 Skills 加载、结构化事件流、工具链支持。
     */
    private Optional<String> generateViaSdk(String projectPath) {
        try {
            Path profilePath = Path.of(projectPath, "docs", "project-profile.md");

            String jsonConfig = String.format(
                    "{\"prompt\":null,\"cwd\":\"%s\",\"skillName\":\"%s\"}",
                    escapeJson(projectPath), escapeJson(config.getSkillName()));

            ProcessBuilder pb = new ProcessBuilder(
                    config.getNodePath(),
                    config.getSdkBridgePath(),
                    jsonConfig
            );
            pb.directory(new File(projectPath));
            pb.redirectErrorStream(true);

            log.info("[SDK] Starting Claude Code SDK bridge for: {}", projectPath);

            return executeAndWait(pb, projectPath, profilePath, line -> {
                // 解析桥接脚本的 JSON 行输出，提取关键信息打印
                if (line.contains("\"type\":\"progress\"")) {
                    log.info("[SDK] progress: {}", extractJsonValue(line, "text"));
                } else if (line.contains("\"type\":\"tool_use\"")) {
                    log.info("[SDK] tool: {} → {}", extractJsonValue(line, "tool"),
                            extractJsonValue(line, "input"));
                } else if (line.contains("\"type\":\"result\"")) {
                    log.info("[SDK] {}", line);
                } else if (line.contains("\"type\":\"error\"")) {
                    log.error("[SDK] error: {}", extractJsonValue(line, "message"));
                } else {
                    log.debug("[SDK] {}", line);
                }
            });

        } catch (IOException | InterruptedException e) {
            log.error("[SDK] Failed for {}", projectPath, e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return Optional.empty();
        }
    }

    // ==================== CLI 模式（备选） ====================

    /**
     * 直接调用 Claude Code CLI（--print 模式）生成画像。
     *
     * <p>备选方案：轻量、无需 Node.js 环境，但 --print 模式下 Skills 不会被加载，
     * Claude 仅根据 prompt 文本推断执行内容。
     */
    private Optional<String> generateViaCli(String projectPath) {
        try {
            Path profilePath = Path.of(projectPath, "docs", "project-profile.md");

            String prompt = String.format(
                    "请执行 %s skill，分析项目并生成画像文档。项目路径：%s",
                    config.getSkillName(), projectPath);

            ProcessBuilder pb = new ProcessBuilder(
                    config.getCliPath(),
                    "--print",
                    "--dangerously-skip-permissions",
                    "--output-format", "text",
                    prompt
            );
            pb.directory(new File(projectPath));
            pb.redirectInput(ProcessBuilder.Redirect.from(new File("/dev/null")));
            pb.redirectErrorStream(true);

            log.info("[CLI] Starting Claude Code CLI for: {}", projectPath);

            return executeAndWait(pb, projectPath, profilePath,
                    line -> log.info("[CLI] {}", line));

        } catch (IOException | InterruptedException e) {
            log.error("[CLI] Failed for {}", projectPath, e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return Optional.empty();
        }
    }

    // ==================== 公共：进程执行 + 空闲超时监控 ====================

    /**
     * 启动子进程并监控执行，支持空闲超时和兜底总超时。
     *
     * @param pb          已配置好的 ProcessBuilder
     * @param projectPath 项目路径（用于日志）
     * @param profilePath 期望的输出文件路径
     * @param lineHandler 每行输出的处理逻辑
     * @return 生成的 Markdown 内容
     */
    private Optional<String> executeAndWait(ProcessBuilder pb, String projectPath,
                                            Path profilePath, Consumer<String> lineHandler)
            throws IOException, InterruptedException {

        long startTime = System.currentTimeMillis();
        Process process = pb.start();

        AtomicLong lastActivityTime = new AtomicLong(System.currentTimeMillis());

        Thread outputReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lastActivityTime.set(System.currentTimeMillis());
                    lineHandler.accept(line);
                }
            } catch (IOException e) {
                log.warn("Error reading process output", e);
            }
        }, "claude-code-output-reader");
        outputReader.setDaemon(true);
        outputReader.start();

        long idleTimeoutMs = config.getIdleTimeoutSeconds() * 1000L;
        long maxTimeoutMs = config.getMaxTimeoutSeconds() * 1000L;

        while (process.isAlive()) {
            long now = System.currentTimeMillis();
            long idleMs = now - lastActivityTime.get();
            long totalMs = now - startTime;

            if (idleMs > idleTimeoutMs) {
                process.destroyForcibly();
                outputReader.join(3000);
                log.error("Idle timeout: no output for {}s (threshold: {}s), project: {}",
                        idleMs / 1000, config.getIdleTimeoutSeconds(), projectPath);
                return Optional.empty();
            }

            if (maxTimeoutMs > 0 && totalMs > maxTimeoutMs) {
                process.destroyForcibly();
                outputReader.join(3000);
                log.error("Max timeout after {}s for {}", totalMs / 1000, projectPath);
                return Optional.empty();
            }

            process.waitFor(1, TimeUnit.SECONDS);
        }

        outputReader.join(5000);

        if (process.exitValue() != 0) {
            log.error("Process exited with code {} for {}", process.exitValue(), projectPath);
            return Optional.empty();
        }

        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        if (Files.exists(profilePath)) {
            String content = Files.readString(profilePath);
            log.info("Profile generated at {} (took {}s)", profilePath, elapsed);
            return Optional.of(content);
        }

        log.warn("Process completed ({}s) but project-profile.md not found at {}", elapsed, profilePath);
        return Optional.empty();
    }

    // ==================== 辅助方法 ====================

    @Override
    public boolean isAvailable() {
        return MODE_SDK.equalsIgnoreCase(config.getMode())
                ? isSdkAvailable()
                : isCliAvailable();
    }

    private boolean isSdkAvailable() {
        try {
            Path bridgePath = Path.of(config.getSdkBridgePath());
            if (!Files.exists(bridgePath)) {
                log.warn("SDK bridge script not found: {}", bridgePath);
                return false;
            }
            Process p = new ProcessBuilder(config.getNodePath(), "--version")
                    .redirectErrorStream(true)
                    .start();
            boolean ok = p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0;
            if (!ok) {
                p.destroyForcibly();
            }
            return ok;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isCliAvailable() {
        try {
            Process p = new ProcessBuilder(config.getCliPath(), "--version")
                    .redirectErrorStream(true)
                    .start();
            boolean ok = p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0;
            if (!ok) {
                p.destroyForcibly();
            }
            return ok;
        } catch (Exception e) {
            return false;
        }
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * 从 JSON 行中简单提取指定 key 的 string 值（轻量解析，避免引入 Jackson 依赖）。
     */
    private static String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) {
            return "";
        }
        start += search.length();
        int end = json.indexOf("\"", start);
        return end > start ? json.substring(start, end) : "";
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public boolean isLlmBased() {
        return true;
    }
}
