package com.exceptioncoder.llm.infrastructure.devplan.tool;

import com.exceptioncoder.llm.infrastructure.agent.tool.Tool;
import com.exceptioncoder.llm.infrastructure.agent.tool.ToolParam;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 模板渲染工具 -- 用预定义模板渲染设计文档骨架。
 *
 * <p>模板内置为 Java 常量，后续可迁移到 prompt_template 表。
 *
 * <p><b>归属智能体：</b>开发计划智能体（devplan）
 * <br><b>归属 Agent：</b>方案架构师（devplan-solution-architect）
 * <br><b>调用阶段：</b>第三阶段 — 方案设计
 * <br><b>业务场景：</b>方案架构师完成分析后，使用预定义模板（STANDARD / LIGHTWEIGHT）
 * 将设计文档结构化输出。模板包含功能背景、接口设计、类设计、数据库设计等章节占位符，
 * LLM 填充内容后生成规范统一的设计文档，确保团队产出文档格式一致。
 *
 * @author zhangkai
 * @since 2026-04-07
 */
@Slf4j
@Component
public class TemplateRenderTool {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Map<String, String> TEMPLATES = Map.of(
            "STANDARD", """
                    # 功能设计文档

                    ## 1. 基本信息
                    - 功能名称：{featureName}
                    - 所属模块：{moduleName}
                    - 负责人：{author}

                    ## 2. 背景与目标
                    {background}

                    ## 3. 功能范围
                    {scope}

                    ## 4. 业务流程设计
                    {businessFlow}

                    ## 5. 接口设计
                    {apiDesign}

                    ## 6. 类设计
                    {classDesign}

                    ## 7. 数据库设计
                    {databaseDesign}

                    ## 8. 核心业务规则
                    {businessRules}

                    ## 9. 异常处理
                    {exceptionHandling}

                    ## 10. 测试要点
                    {testPlan}
                    """,
            "LIGHTWEIGHT", """
                    # {featureName} - 轻量设计

                    ## 概述
                    {background}

                    ## 接口设计
                    {apiDesign}

                    ## 类设计
                    {classDesign}

                    ## 核心规则
                    {businessRules}
                    """
    );

    @Tool(name = "devplan_template_render", description = "使用预定义模板渲染设计文档", tags = {"devplan", "render"})
    public String render(
            @ToolParam(value = "templateName", description = "模板名称：STANDARD 或 LIGHTWEIGHT") String templateName,
            @ToolParam(value = "context", description = "JSON格式的模板变量") String context
    ) {
        try {
            String template = TEMPLATES.get(templateName.toUpperCase());
            if (template == null) {
                return errorJson("未知模板: " + templateName + "，可选: STANDARD, LIGHTWEIGHT");
            }

            Map<String, String> variables = objectMapper.readValue(context, new TypeReference<>() {});

            String rendered = template;
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                rendered = rendered.replace("{" + entry.getKey() + "}", entry.getValue());
            }

            // 清理未替换的占位符
            rendered = rendered.replaceAll("\\{\\w+}", "（待填写）");

            return rendered;
        } catch (Exception e) {
            log.error("TemplateRenderTool 执行失败", e);
            return errorJson("模板渲染失败: " + e.getMessage());
        }
    }

    private String errorJson(String message) {
        return "{\"error\": \"" + message.replace("\"", "\\\"") + "\"}";
    }
}
