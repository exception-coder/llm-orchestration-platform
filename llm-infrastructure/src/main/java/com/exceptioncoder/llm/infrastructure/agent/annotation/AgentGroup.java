package com.exceptioncoder.llm.infrastructure.agent.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注一个 AgentInitializer 类所属的智能体（Graph）分组。
 *
 * <p>启动时 {@link AgentGroupScanner} 扫描所有带此注解的 Bean，
 * 自动创建对应的 GraphDefinition 并将其下的 Agent 注册为 Graph 节点。
 *
 * @author zhangkai
 * @since 2026-04-09
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AgentGroup {

    /**
     * 智能体（Graph）唯一标识
     */
    String id();

    /**
     * 智能体名称
     */
    String name();

    /**
     * 智能体描述
     */
    String description() default "";
}
