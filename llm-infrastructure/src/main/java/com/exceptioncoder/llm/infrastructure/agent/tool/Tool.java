package com.exceptioncoder.llm.infrastructure.agent.tool;

import com.exceptioncoder.llm.domain.devplan.model.AgentRole;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注一个 Bean 方法为可被 Agent 调用的工具
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Tool {

    /**
     * 工具名称（唯一标识）
     */
    String name();

    /**
     * 工具描述（供 LLM 理解工具用途）
     */
    String description() default "";

    /**
     * 工具标签（用于分类筛选）
     */
    String[] tags() default {};

    /**
     * 工具归属的 Agent 角色。
     *
     * <p>一个工具可被多个角色共享。启动时 ToolScanner 读取此字段并写入
     * {@link com.exceptioncoder.llm.domain.model.ToolDefinition#roles()}，
     * DevPlanToolRegistry 据此动态构建角色→工具映射，无需手动维护静态 Map。
     */
    AgentRole[] roles() default {};
}
