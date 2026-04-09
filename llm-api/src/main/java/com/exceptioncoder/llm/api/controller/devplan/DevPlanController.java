package com.exceptioncoder.llm.api.controller.devplan;

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
 * 开发方案生成 REST 控制器 —— devplan 模块的 HTTP 入口。
 *
 * <p>本类属于 <b>API 层（Controller Layer）</b>，负责处理开发方案相关的 HTTP 请求，
 * 包括方案生成和任务状态查询两个端点。控制器只做协议处理（入参接收、响应组装），
 * 业务逻辑全部委托给应用层的 {@link DevPlanUseCase}。</p>
 *
 * <h3>端点列表</h3>
 * <ul>
 *   <li>{@code POST /api/v1/dev-plan/generate} — 提交方案生成请求，同步返回生成结果</li>
 *   <li>{@code GET /api/v1/dev-plan/task/{taskId}} — 查询指定任务的当前状态</li>
 * </ul>
 *
 * <h3>设计思路</h3>
 * <ul>
 *   <li>严格遵循"Controller 只做协议处理"原则，不包含任何业务判断逻辑</li>
 *   <li>领域模型到 DTO 的转换通过私有方法 {@code toResponse} / {@code toTaskStatusResponse} 完成</li>
 * </ul>
 *
 * <h3>协作关系</h3>
 * <ul>
 *   <li>{@link DevPlanUseCase} — 应用层用例，处理方案生成和任务状态查询的业务编排</li>
 *   <li>{@link DevPlanRequest} — 请求 DTO，携带项目路径、需求描述等输入参数</li>
 *   <li>{@link DevPlanResponse} — 响应 DTO，封装方案文档、影响面、验证结果和元数据</li>
 *   <li>{@link TaskStatusResponse} — 任务状态响应 DTO</li>
 * </ul>
 *
 * @author zhangkai
 * @since 2026-04-06
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/dev-plan")
public class DevPlanController {

    /** 开发方案生成用例 —— 应用层入口 */
    private final DevPlanUseCase devPlanUseCase;

    /**
     * 构造 DevPlanController 实例。
     *
     * @param devPlanUseCase 开发方案生成用例
     */
    public DevPlanController(DevPlanUseCase devPlanUseCase) {
        this.devPlanUseCase = devPlanUseCase;
    }

    /**
     * 生成开发方案。
     *
     * <p>接收前端提交的方案生成请求，委托 {@link DevPlanUseCase} 执行完整流程，
     * 然后将领域模型转换为响应 DTO 返回。</p>
     *
     * @param request 方案生成请求，包含项目路径和需求描述（必填）
     * @return 包含方案文档、影响面分析、验证结果和元数据的响应体
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
     * 查询指定任务的当前状态。
     *
     * @param taskId 任务唯一标识（路径参数）
     * @return 任务存在时返回 200 和状态信息，不存在时返回 404
     */
    @GetMapping("/task/{taskId}")
    public ResponseEntity<TaskStatusResponse> getTaskStatus(@PathVariable String taskId) {
        return devPlanUseCase.getTaskStatus(taskId)
                .map(this::toTaskStatusResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 将领域模型 {@link DevPlanState} 转换为 API 响应 DTO {@link DevPlanResponse}。
     *
     * <p>转换内容包括：影响面分析、验证结果、节点耗时统计、Token 使用量和修正次数。</p>
     *
     * @param state 流程执行完成后的最终状态
     * @return API 响应 DTO
     */
    private DevPlanResponse toResponse(DevPlanState state) {
        // 1. 转换影响面分析（可能为 null，ScanNode/AnalyzeNode 未产出时跳过）
        DevPlanResponse.ImpactAnalysisVO impact = null;
        if (state.impact() != null) {
            impact = new DevPlanResponse.ImpactAnalysisVO(
                    state.impact().affectedClasses(),
                    state.impact().affectedModules(),
                    state.impact().dependencyChain()
            );
        }

        // 2. 转换审查验证结果（可能为 null，流程异常中断时跳过）
        DevPlanResponse.ValidationResultVO validation = null;
        if (state.validation() != null) {
            validation = new DevPlanResponse.ValidationResultVO(
                    state.validation().passed(),
                    state.validation().score(),
                    state.validation().issues()
            );
        }

        // 3. 汇总各节点耗时，计算流程总耗时
        long totalElapsed = state.nodeTimings().values().stream()
                .mapToLong(Long::longValue).sum();

        // 4. 组装流程执行元数据
        DevPlanResponse.MetadataVO metadata = new DevPlanResponse.MetadataVO(
                null, // traceId — TODO：接入链路追踪后填充
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

    /**
     * 将领域模型 {@link DevPlanTask} 转换为任务状态响应 DTO {@link TaskStatusResponse}。
     *
     * @param task 任务领域对象
     * @return 任务状态响应 DTO，包含已运行时长等信息
     */
    private TaskStatusResponse toTaskStatusResponse(DevPlanTask task) {
        // 计算任务已运行时长，未启动时为 0
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
