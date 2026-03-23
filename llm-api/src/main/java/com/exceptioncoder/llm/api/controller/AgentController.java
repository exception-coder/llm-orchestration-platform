package com.exceptioncoder.llm.api.controller;

import com.exceptioncoder.llm.application.usecase.AgentExecutionUseCase;
import com.exceptioncoder.llm.domain.model.AgentDefinition;
import com.exceptioncoder.llm.domain.model.AgentExecutionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    /** 执行 Agent */
    @PostMapping("/{agentId}/execute")
    public ResponseEntity<AgentExecutionResult> execute(
            @PathVariable String agentId,
            @RequestBody AgentExecuteRequest request
    ) {
        AgentExecutionResult result = agentExecutionUseCase.execute(
                agentId,
                request.input(),
                request.context()
        );
        return ResponseEntity.ok(result);
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
}
