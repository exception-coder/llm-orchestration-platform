package com.exceptioncoder.llm.api.controller.management;

import com.exceptioncoder.llm.api.dto.PromptTestRequestDTO;
import com.exceptioncoder.llm.api.dto.PromptTestResponseDTO;
import com.exceptioncoder.llm.application.usecase.LLMModelConfigUseCase;
import com.exceptioncoder.llm.application.usecase.PromptTemplateManagementUseCase;
import com.exceptioncoder.llm.application.usecase.PromptTestUseCase;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Prompt 测试 API 控制器
 * 用于前端验证不同模板和模型的输出效果
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/prompt-test")
public class PromptTestController {

    private final PromptTestUseCase promptTestUseCase;
    private final LLMModelConfigUseCase modelConfigUseCase;
    private final PromptTemplateManagementUseCase templateManagementUseCase;
    private final ObjectMapper objectMapper;

    public PromptTestController(
            PromptTestUseCase promptTestUseCase,
            LLMModelConfigUseCase modelConfigUseCase,
            PromptTemplateManagementUseCase templateManagementUseCase,
            ObjectMapper objectMapper) {
        this.promptTestUseCase = promptTestUseCase;
        this.modelConfigUseCase = modelConfigUseCase;
        this.templateManagementUseCase = templateManagementUseCase;
        this.objectMapper = objectMapper;
    }

    /**
     * 执行 Prompt 测试
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public PromptTestResponseDTO testPrompt(@Valid @RequestBody PromptTestRequestDTO request) {
        log.info("收到 Prompt 测试请求，模板: {}, 模型: {}",
                request.getTemplateName(), request.getModel());

        PromptTestUseCase.PromptTestResult result = promptTestUseCase.execute(
                request.getTemplateName(),
                request.getVariables(),
                request.getModel(),
                request.getProvider(),
                request.getTemperature(),
                request.getMaxTokens()
        );

        return PromptTestResponseDTO.builder()
                .renderedPrompt(result.getRenderedPrompt())
                .output(result.getOutput())
                .model(result.getModel())
                .provider(request.getProvider())
                .tokenUsage(result.getTokenUsage() != null ?
                        PromptTestResponseDTO.TokenUsageDTO.builder()
                                .promptTokens(result.getTokenUsage().getPromptTokens())
                                .completionTokens(result.getTokenUsage().getCompletionTokens())
                                .totalTokens(result.getTokenUsage().getTotalTokens())
                                .build() : null)
                .executionTime(result.getExecutionTime())
                .build();
    }

    /**
     * 获取可用的模型列表（从数据库）
     */
    @GetMapping("/models")
    public List<Map<String, String>> getAvailableModels() {
        return modelConfigUseCase.getAllEnabledModels().stream()
                .map(model -> {
                    Map<String, String> info = new HashMap<>();
                    info.put("code", model.getModelCode());
                    info.put("provider", model.getProvider());
                    info.put("name", model.getModelName());
                    info.put("description", model.getDescription());
                    return info;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取模板变量示例（从数据库）
     */
    @GetMapping("/template-variables/{templateName}")
    public Map<String, Object> getTemplateVariables(@PathVariable String templateName) {
        return templateManagementUseCase.getTemplateByName(templateName)
                .map(template -> {
                    String variableExamples = template.getVariableExamples();
                    if (variableExamples != null && !variableExamples.isEmpty()) {
                        try {
                            // 解析 JSON 格式的变量示例
                            Map<String, Object> result = objectMapper.readValue(variableExamples,
                                    new TypeReference<Map<String, Object>>() {});
                            return result;
                        } catch (Exception e) {
                            log.error("解析变量示例失败: {}", templateName, e);
                            Map<String, Object> errorMap = new HashMap<>();
                            errorMap.put("error", "变量示例格式错误");
                            return errorMap;
                        }
                    }
                    Map<String, Object> defaultMap = new HashMap<>();
                    defaultMap.put("example", "该模板未配置变量示例");
                    return defaultMap;
                })
                .orElseGet(() -> {
                    Map<String, Object> errorMap = new HashMap<>();
                    errorMap.put("error", "模板不存在");
                    return errorMap;
                });
    }
}
