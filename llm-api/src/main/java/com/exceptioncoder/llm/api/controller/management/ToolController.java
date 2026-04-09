package com.exceptioncoder.llm.api.controller.management;

import com.exceptioncoder.llm.domain.model.ExecutionTrace;
import com.exceptioncoder.llm.domain.model.ToolDefinition;
import com.exceptioncoder.llm.domain.registry.ToolRegistry;
import com.exceptioncoder.llm.domain.repository.ExecutionTraceRepository;
import com.exceptioncoder.llm.domain.service.ToolExecutionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 工具 REST 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tools")
public class ToolController {

    private final ToolRegistry toolRegistry;
    private final ToolExecutionService toolExecutionService;
    private final ExecutionTraceRepository traceRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ToolController(ToolRegistry toolRegistry,
                          ToolExecutionService toolExecutionService,
                          ExecutionTraceRepository traceRepository) {
        this.toolRegistry = toolRegistry;
        this.toolExecutionService = toolExecutionService;
        this.traceRepository = traceRepository;
    }

    /** 获取所有已注册工具 */
    @GetMapping
    public ResponseEntity<List<ToolDefinition>> listTools() {
        return ResponseEntity.ok(toolRegistry.getAllTools());
    }

    /** 获取单个工具定义 */
    @GetMapping("/{toolId}")
    public ResponseEntity<ToolDefinition> getTool(@PathVariable String toolId) {
        return toolRegistry.getDefinition(toolId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** 执行工具（调试用），返回结果含 traceId */
    @PostMapping("/{toolId}/execute")
    public ResponseEntity<Map<String, Object>> executeTool(
            @PathVariable String toolId,
            @RequestBody Map<String, Object> params
    ) {
        if (!toolRegistry.contains(toolId)) {
            return ResponseEntity.notFound().build();
        }

        String traceId = UUID.randomUUID().toString();
        String toolName = toolRegistry.getDefinition(toolId).map(ToolDefinition::name).orElse(toolId);
        String inputJson;
        try { inputJson = objectMapper.writeValueAsString(params); } catch (Exception e) { inputJson = params.toString(); }

        long start = System.currentTimeMillis();
        String result = toolExecutionService.execute(toolId, params);
        long duration = System.currentTimeMillis() - start;

        // 判断执行是否成功
        boolean success = true;
        String errorMessage = null;
        try {
            var parsed = objectMapper.readTree(result);
            if (parsed.has("success") && !parsed.get("success").asBoolean()) {
                success = false;
                errorMessage = parsed.has("error") ? parsed.get("error").asText() : "执行失败";
            }
        } catch (Exception e) {
            log.debug("Tool 结果非标准 JSON，视为成功: {}", e.getMessage());
        }

        // 持久化流水
        try {
            ExecutionTrace trace = ExecutionTrace.forToolCall(
                    traceId, toolId, toolName, inputJson, result, duration, success, errorMessage);
            traceRepository.save(trace);
        } catch (Exception e) {
            log.warn("Tool 执行流水持久化失败: traceId={}", traceId, e);
        }

        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("traceId", traceId);
        response.put("toolId", toolId);
        response.put("result", result);
        response.put("durationMs", duration);
        response.put("success", success);
        return ResponseEntity.ok(response);
    }

    /** 注销工具 */
    @DeleteMapping("/{toolId}")
    public ResponseEntity<Void> unregisterTool(@PathVariable String toolId) {
        toolRegistry.unregister(toolId);
        return ResponseEntity.noContent().build();
    }
}
