package com.exceptioncoder.llm.api.controller.management;

import com.exceptioncoder.llm.application.usecase.AgentExecutionUseCase;
import com.exceptioncoder.llm.domain.executor.AgentIterationEvent;
import com.exceptioncoder.llm.domain.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Agent REST 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/agents")
public class AgentController {

    private final AgentExecutionUseCase agentExecutionUseCase;

    @Value("${agent.async.sse-timeout:600000}")
    private long sseTimeout;

    public AgentController(AgentExecutionUseCase agentExecutionUseCase) {
        this.agentExecutionUseCase = agentExecutionUseCase;
    }

    /** 获取所有 Agent */
    @GetMapping
    public ResponseEntity<List<AgentDefinition>> listAgents() {
        return ResponseEntity.ok(agentExecutionUseCase.getAllAgents());
    }

    /** 获取单个 Agent */
    @GetMapping("/{agentId}")
    public ResponseEntity<AgentDefinition> getAgent(@PathVariable String agentId) {
        return ResponseEntity.ok(agentExecutionUseCase.getAgent(agentId));
    }

    /** 创建或更新 Agent 定义 */
    @PostMapping
    public ResponseEntity<AgentDefinition> saveAgent(@RequestBody AgentDefinitionRequest request) {
        AgentDefinition agent = AgentDefinition.builder()
                .id(request.id())
                .name(request.name())
                .description(request.description())
                .systemPrompt(request.systemPrompt())
                .toolIds(request.toolIds())
                .llmProvider(request.llmProvider())
                .llmModel(request.llmModel())
                .maxIterations(request.maxIterations())
                .timeoutSeconds(request.timeoutSeconds())
                .enabled(true)
                .build();
        return ResponseEntity.ok(agentExecutionUseCase.saveAgent(agent));
    }

    /** 删除 Agent */
    @DeleteMapping("/{agentId}")
    public ResponseEntity<Void> deleteAgent(@PathVariable String agentId) {
        agentExecutionUseCase.deleteAgent(agentId);
        return ResponseEntity.noContent().build();
    }

    /** 异步提交 Agent 执行 */
    @PostMapping("/{agentId}/execute")
    public ResponseEntity<?> execute(
            @PathVariable String agentId,
            @RequestBody AgentExecuteRequest request
    ) {
        AgentTask task = agentExecutionUseCase.submitAsync(
                agentId, request.input(), request.context());

        if (task == null) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Agent 执行并发数已达上限"));
        }

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new AgentExecuteAsyncResponse(
                        task.executionId(), task.agentId(), task.status().name()));
    }

    /** 查询 Agent 执行状态 */
    @GetMapping("/executions/{executionId}")
    public ResponseEntity<?> getExecutionStatus(@PathVariable String executionId) {
        return agentExecutionUseCase.getExecutionStatus(executionId)
                .map(task -> ResponseEntity.ok(toStatusResponse(task)))
                .orElse(ResponseEntity.notFound().build());
    }

    /** SSE 实时推送执行过程 */
    @GetMapping(value = "/executions/{executionId}/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamExecution(@PathVariable String executionId) {
        SseEmitter emitter = new SseEmitter(sseTimeout);

        var taskOpt = agentExecutionUseCase.getExecutionStatus(executionId);
        if (taskOpt.isEmpty()) {
            emitter.completeWithError(new IllegalArgumentException("执行记录不存在: " + executionId));
            return emitter;
        }

        AgentTask task = taskOpt.get();
        if (task.status() == AgentTask.Status.COMPLETED
                || task.status() == AgentTask.Status.FAILED
                || task.status() == AgentTask.Status.TIMED_OUT) {
            // 已结束，直接发送最终状态后关闭
            try {
                emitter.send(SseEmitter.event()
                        .name(task.status() == AgentTask.Status.COMPLETED ? "complete" : "error")
                        .data(toStatusResponse(task)));
                emitter.send(SseEmitter.event().data("[DONE]"));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        // 运行中，订阅事件流并推送到 SseEmitter
        agentExecutionUseCase.streamExecution(executionId)
                .subscribe(
                        event -> {
                            try {
                                emitter.send(SseEmitter.event()
                                        .name(event.type().name().toLowerCase())
                                        .data(event.data()));
                            } catch (IOException e) {
                                log.debug("SSE 发送失败: executionId={}", executionId);
                            }
                        },
                        error -> emitter.completeWithError(error),
                        () -> {
                            try {
                                emitter.send(SseEmitter.event().data("[DONE]"));
                                emitter.complete();
                            } catch (IOException e) {
                                log.debug("SSE 完成发送失败: executionId={}", executionId);
                            }
                        }
                );

        return emitter;
    }

    private AgentExecutionStatusResponse toStatusResponse(AgentTask task) {
        return new AgentExecutionStatusResponse(
                task.executionId(),
                task.agentId(),
                task.status().name(),
                task.currentIteration(),
                task.maxIterations(),
                task.finalOutput(),
                task.errorMessage(),
                task.thoughtHistory(),
                task.toolCalls(),
                task.elapsedMs()
        );
    }

    /** 获取 Agent 关联的 Tool 详情列表 */
    @GetMapping("/{agentId}/tools")
    public ResponseEntity<List<ToolDefinition>> getAgentTools(@PathVariable String agentId) {
        return ResponseEntity.ok(agentExecutionUseCase.getAgentTools(agentId));
    }

    /** 按流水号查询执行流水明细 */
    @GetMapping("/traces/{traceId}")
    public ResponseEntity<ExecutionTrace> getTrace(@PathVariable String traceId) {
        return ResponseEntity.ok(agentExecutionUseCase.getTrace(traceId));
    }

    /** 查询指定 Agent 的最近执行记录 */
    @GetMapping("/{agentId}/traces")
    public ResponseEntity<List<ExecutionTrace>> getAgentTraces(
            @PathVariable String agentId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(agentExecutionUseCase.getAgentTraces(agentId, limit));
    }

    /** 查询最近所有执行记录 */
    @GetMapping("/traces")
    public ResponseEntity<List<ExecutionTrace>> getRecentTraces(
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(agentExecutionUseCase.getRecentTraces(limit));
    }

    // ---- DTO Records ----

    public record AgentDefinitionRequest(
            String id,
            String name,
            String description,
            String systemPrompt,
            List<String> toolIds,
            String llmProvider,
            String llmModel,
            Integer maxIterations,
            Integer timeoutSeconds
    ) {}

    public record AgentExecuteRequest(
            String input,
            Map<String, Object> context
    ) {}

    public record AgentExecuteAsyncResponse(
            String executionId,
            String agentId,
            String status
    ) {}

    public record AgentExecutionStatusResponse(
            String executionId,
            String agentId,
            String status,
            int currentIteration,
            int maxIterations,
            String finalOutput,
            String errorMessage,
            List<String> thoughtHistory,
            List<ToolCall> toolCalls,
            long elapsedMs
    ) {}
}
