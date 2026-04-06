package com.exceptioncoder.llm.api.controller;

import com.exceptioncoder.llm.api.dto.devplan.DevPlanRequest;
import com.exceptioncoder.llm.api.dto.devplan.DevPlanResponse;
import com.exceptioncoder.llm.api.dto.devplan.TaskStatusResponse;
import com.exceptioncoder.llm.application.usecase.DevPlanUseCase;
import com.exceptioncoder.llm.domain.devplan.model.DevPlanState;
import com.exceptioncoder.llm.domain.devplan.model.DevPlanTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 开发方案 REST 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/dev-plan")
public class DevPlanController {

    private final DevPlanUseCase devPlanUseCase;

    public DevPlanController(DevPlanUseCase devPlanUseCase) {
        this.devPlanUseCase = devPlanUseCase;
    }

    /**
     * 生成开发方案
     */
    @PostMapping("/generate")
    public ResponseEntity<DevPlanResponse> generate(@RequestBody DevPlanRequest request) {
        log.info("收到方案生成请求，projectPath={}", request.projectPath());

        DevPlanState result = devPlanUseCase.generateDevPlan(
                request.projectPath(),
                request.requirement(),
                request.resolvedTimeoutSeconds()
        );

        return ResponseEntity.ok(toResponse(result));
    }

    /**
     * 查询任务状态
     */
    @GetMapping("/task/{taskId}")
    public ResponseEntity<TaskStatusResponse> getTaskStatus(@PathVariable String taskId) {
        return devPlanUseCase.getTaskStatus(taskId)
                .map(this::toTaskStatusResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private DevPlanResponse toResponse(DevPlanState state) {
        DevPlanResponse.ImpactAnalysisVO impact = null;
        if (state.impact() != null) {
            impact = new DevPlanResponse.ImpactAnalysisVO(
                    state.impact().affectedClasses(),
                    state.impact().affectedModules(),
                    state.impact().dependencyChain()
            );
        }

        DevPlanResponse.ValidationResultVO validation = null;
        if (state.validation() != null) {
            validation = new DevPlanResponse.ValidationResultVO(
                    state.validation().passed(),
                    state.validation().score(),
                    state.validation().issues()
            );
        }

        long totalElapsed = state.nodeTimings().values().stream()
                .mapToLong(Long::longValue).sum();

        DevPlanResponse.MetadataVO metadata = new DevPlanResponse.MetadataVO(
                null, // traceId — TODO
                state.nodeTimings(),
                state.agentTokenUsage(),
                totalElapsed,
                state.correctionCount()
        );

        return new DevPlanResponse(
                state.taskId(),
                state.document() != null ? state.document().fullDocument() : null,
                impact,
                validation,
                metadata
        );
    }

    private TaskStatusResponse toTaskStatusResponse(DevPlanTask task) {
        long elapsed = task.startedAt() != null
                ? Duration.between(task.startedAt(), LocalDateTime.now()).toMillis()
                : 0;
        return new TaskStatusResponse(
                task.taskId(),
                task.status(),
                task.currentNode(),
                elapsed
        );
    }
}
