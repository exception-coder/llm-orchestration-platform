package com.exceptioncoder.llm.infrastructure.agent.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

/**
 * 工具执行器
 * 通过反射调用已注册的工具方法
 */
@Slf4j
@Component
public class ToolExecutor {

    private final ToolRegistryImpl registry;
    private final ObjectMapper objectMapper;

    public ToolExecutor(ToolRegistryImpl registry) {
        this.registry = registry;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 执行工具
     * @param toolId 工具 ID
     * @param inputMap 参数字典（JSON 反序列化为 Map）
     * @return 工具执行结果（JSON 字符串）
     */
    public String execute(String toolId, Map<String, Object> inputMap) {
        var implOpt = registry.getImplementation(toolId);
        var defOpt = registry.getDefinition(toolId);

        if (implOpt.isEmpty() || defOpt.isEmpty()) {
            throw new IllegalArgumentException("工具不存在: " + toolId);
        }

        Object impl = implOpt.get();
        // 工具实现类中标注了 @Tool 的方法
        Method toolMethod = findToolMethod(impl.getClass());
        if (toolMethod == null) {
            throw new IllegalStateException("未找到 @Tool 标注的方法: " + toolId);
        }

        try {
            Object[] args = resolveArguments(toolMethod, inputMap);
            Object result = toolMethod.invoke(impl, args);

            // 结果序列化为 JSON 字符串
            if (result == null) {
                return "{\"success\": true}";
            }
            return objectMapper.writeValueAsString(Map.of("success", true, "result", result));
        } catch (Exception e) {
            log.error("工具执行失败: toolId={}", toolId, e);
            try {
                return objectMapper.writeValueAsString(Map.of(
                        "success", false,
                        "error", e.getMessage()
                ));
            } catch (JsonProcessingException ex) {
                return "{\"success\": false, \"error\": \"序列化失败\"}";
            }
        }
    }

    /**
     * 查找标注了 @Tool 的方法
     */
    private Method findToolMethod(Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Tool.class)) {
                return method;
            }
        }
        // 递归查找父类
        if (clazz.getSuperclass() != null && clazz.getSuperclass() != Object.class) {
            return findToolMethod(clazz.getSuperclass());
        }
        return null;
    }

    /**
     * 解析方法参数
     */
    private Object[] resolveArguments(Method method, Map<String, Object> inputMap) {
        Parameter[] params = method.getParameters();
        Object[] args = new Object[params.length];

        for (int i = 0; i < params.length; i++) {
            Parameter param = params[i];
            ToolParam annotation = param.getAnnotation(ToolParam.class);
            if (annotation == null) {
                args[i] = null;
                continue;
            }

            String paramName = annotation.value().isEmpty() ? param.getName() : annotation.value();
            Object value = inputMap.get(paramName);

            // 类型转换
            if (value != null) {
                args[i] = convertValue(value, param.getType());
            } else if (!annotation.defaultValue().isEmpty()) {
                try {
                    args[i] = objectMapper.readValue(annotation.defaultValue(), param.getType());
                } catch (Exception e) {
                    args[i] = null;
                }
            } else {
                args[i] = null;
            }
        }

        return args;
    }

    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) return null;
        if (targetType.isInstance(value)) return value;

        try {
            return objectMapper.convertValue(value, targetType);
        } catch (IllegalArgumentException e) {
            log.warn("参数类型转换失败: value={}, targetType={}", value, targetType);
            return value;
        }
    }
}
