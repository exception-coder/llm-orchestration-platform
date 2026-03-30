package com.exceptioncoder.llm.infrastructure.agent.tool;

import com.exceptioncoder.llm.domain.model.ToolDefinition;
import com.exceptioncoder.llm.domain.model.ToolType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 启动时扫描所有 @Tool 标注的方法，自动注册到 ToolRegistry
 */
@Slf4j
@Component
public class ToolScanner implements ApplicationListener<ApplicationReadyEvent> {

    private final ToolRegistryImpl registry;

    public ToolScanner(ToolRegistryImpl registry) {
        this.registry = registry;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        scanAndRegister(event);
    }

    private void scanAndRegister(ApplicationReadyEvent event) {
        var context = event.getApplicationContext();
        Map<String, Object> beans = context.getBeansOfType(Object.class);

        int registeredCount = 0;
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            Object bean = entry.getValue();
            Class<?> beanClass = bean.getClass();

            for (Method method : beanClass.getDeclaredMethods()) {
                Tool toolAnn = method.getAnnotation(Tool.class);
                if (toolAnn == null) continue;

                String toolId = StringUtils.hasText(toolAnn.name())
                        ? toolAnn.name()
                        : entry.getKey() + "." + method.getName();

                ToolDefinition definition = buildDefinition(toolId, toolAnn, method);
                registry.register(definition, bean);
                registeredCount++;
            }
        }

        log.info("工具扫描完成，共注册 {} 个工具", registeredCount);
    }

    private ToolDefinition buildDefinition(String toolId, Tool tool, Method method) {
        // 从方法签名构建 inputSchema
        StringBuilder schema = new StringBuilder("{");
        Parameter[] params = method.getParameters();
        for (int i = 0; i < params.length; i++) {
            Parameter param = params[i];
            ToolParam paramAnn = param.getAnnotation(ToolParam.class);
            String name = paramAnn != null && !paramAnn.value().isEmpty()
                    ? paramAnn.value() : param.getName();
            String desc = paramAnn != null ? paramAnn.description() : "";

            if (i > 0) schema.append(",");
            schema.append("\"").append(name).append("\": {");
            schema.append("\"type\": \"").append(simpleTypeName(param.getType())).append("\",");
            schema.append("\"description\": \"").append(desc.replace("\"", "\\\"")).append("\"");
            if (paramAnn != null && !paramAnn.required()) {
                schema.append(",\"required\": false");
            }
            schema.append("}");
        }
        schema.append("}");

        return ToolDefinition.builder()
                .id(toolId)
                .name(tool.name())
                .description(tool.description())
                .inputSchema(schema.toString())
                .type(ToolType.FUNCTION)
                .isAsync(false)
                .build();
    }

    private String simpleTypeName(Class<?> type) {
        if (type == String.class) return "string";
        if (type == int.class || type == Integer.class) return "integer";
        if (type == long.class || type == Long.class) return "integer";
        if (type == double.class || type == Double.class) return "number";
        if (type == boolean.class || type == Boolean.class) return "boolean";
        if (type.isArray() || Iterable.class.isAssignableFrom(type)) return "array";
        return "object";
    }
}
