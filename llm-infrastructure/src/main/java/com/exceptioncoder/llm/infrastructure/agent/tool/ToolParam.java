package com.exceptioncoder.llm.infrastructure.agent.tool;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注工具方法的参数
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ToolParam {

    /**
     * 参数名称
     */
    String value() default "";

    /**
     * 参数描述（供 LLM 理解参数含义）
     */
    String description() default "";

    /**
     * 参数是否必填
     */
    boolean required() default true;

    /**
     * 参数默认值（JSON 格式字符串）
     */
    String defaultValue() default "";
}
