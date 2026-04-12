package com.exceptioncoder.llm.infrastructure.devplan.tool;

import com.exceptioncoder.llm.domain.devplan.model.AgentRole;
import com.exceptioncoder.llm.domain.devplan.service.ProfileGeneratorService;
import com.exceptioncoder.llm.infrastructure.agent.tool.Tool;
import com.exceptioncoder.llm.infrastructure.agent.tool.ToolParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 项目画像生成工具 -- 将 ProfileGeneratorService（SPI 链）包装为可被 Agent 调用的 Tool。
 *
 * <p>内部委托 {@link ProfileGeneratorService} 按优先级依次尝试生成画像
 * （Claude Code CLI → 内部扫描拼装），第一个成功即返回 Markdown 结果。
 *
 * <p><b>归属智能体：</b>开发计划智能体（devplan）
 * <br><b>归属 Agent：</b>代码感知分析专家（devplan-code-awareness）
 * <br><b>业务场景：</b>在代码感知阶段生成项目画像，为后续需求分析和方案设计提供全局上下文。
 *
 * @author zhangkai
 * @since 2026-04-12
 */
@Slf4j
@Component
public class ProfileGenerationTool {

    private final ProfileGeneratorService generatorService;

    public ProfileGenerationTool(ProfileGeneratorService generatorService) {
        this.generatorService = generatorService;
    }

    @Tool(name = "devplan_profile_generate",
          description = "为指定项目生成完整的项目画像 Markdown，自动选择最优生成策略（Claude Code CLI > 内部扫描拼装）",
          tags = {"devplan", "profile"}, roles = {AgentRole.CODE_AWARENESS})
    public String generate(
            @ToolParam(value = "projectPath", description = "项目根目录绝对路径") String projectPath) {
        log.info("ProfileGenerationTool 开始生成画像: {}", projectPath);
        try {
            String markdown = generatorService.generateOrThrow(projectPath);
            log.info("ProfileGenerationTool 生成完成, 内容长度: {} 字符", markdown.length());
            return markdown;
        } catch (Exception e) {
            log.error("ProfileGenerationTool 生成失败: {}", projectPath, e);
            return "{\"error\": \"画像生成失败: " + e.getMessage().replace("\"", "\\\"") + "\"}";
        }
    }
}
