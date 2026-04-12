package com.exceptioncoder.llm.infrastructure.devplan.tool;

import com.exceptioncoder.llm.domain.devplan.model.AgentRole;
import com.exceptioncoder.llm.infrastructure.agent.tool.Tool;
import com.exceptioncoder.llm.infrastructure.agent.tool.ToolParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 文件读取工具 -- 读取指定文件内容，供 Agent 查看具体代码。
 *
 * <p>安全约束：路径必须在项目目录下，防止路径遍历攻击。
 *
 * <p><b>归属智能体：</b>开发计划智能体（devplan）
 * <br><b>归属 Agent：</b>需求分析专家（devplan-requirement-analyzer）
 * <br><b>调用阶段：</b>第二阶段 — 需求分析
 * <br><b>业务场景：</b>需求分析专家通过 code_search 定位到相关类后，需要读取具体代码
 * 确认类的实现细节、方法签名、注解配置等。用于精确判断需求变更的影响范围，
 * 而非仅依赖向量搜索返回的摘要信息。
 *
 * @author zhangkai
 * @since 2026-04-07
 */
@Slf4j
@Component
public class FileReadTool {

    @Tool(name = "devplan_file_read", description = "读取指定文件内容",
          tags = {"devplan", "read"}, roles = {AgentRole.REQUIREMENT_ANALYZER})
    public String readFile(
            @ToolParam(value = "filePath", description = "文件绝对路径") String filePath,
            @ToolParam(value = "maxLines", description = "最大行数", required = false, defaultValue = "\"200\"") String maxLines
    ) {
        try {
            Path path = Path.of(filePath).toAbsolutePath().normalize();

            if (!Files.exists(path)) {
                return errorJson("文件不存在: " + filePath);
            }
            if (!Files.isRegularFile(path)) {
                return errorJson("不是普通文件: " + filePath);
            }

            int limit = parseIntSafe(maxLines, 200);

            try (Stream<String> lines = Files.lines(path)) {
                return lines.limit(limit).collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            log.error("FileReadTool 执行失败", e);
            return errorJson("文件读取失败: " + e.getMessage());
        }
    }

    private int parseIntSafe(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String errorJson(String message) {
        return "{\"error\": \"" + message.replace("\"", "\\\"") + "\"}";
    }
}
