package com.exceptioncoder.llm.infrastructure.agent.tool;

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
}
