package com.exceptioncoder.llm.infrastructure.devplan.sensor;

import com.exceptioncoder.llm.domain.devplan.model.DevPlanDocument;
import com.exceptioncoder.llm.domain.devplan.model.ValidationResult;
import com.exceptioncoder.llm.domain.devplan.service.PlanSensor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 架构合规传感器，检查设计文档中的全类名是否遵循 DDD 分层命名规则。
 *
 * <p>属于 Infrastructure层 devplan/sensor 模块，实现了 Domain 层定义的
 * {@link PlanSensor} 接口。这是一个<b>计算型传感器（ms 级）</b>，
 * 通过正则表达式从文档中提取全类名并校验其是否符合分层架构的命名契约。
 *
 * <p><b>设计思路：</b>
 * <ul>
 *   <li>仅做静态文本匹配，不调用 LLM，因此执行速度极快（毫秒级）</li>
 *   <li>通过 {@code @Order(1)} 确保在传感器链中优先执行，
 *       快速拦截明显的架构违规，避免后续推理型传感器浪费算力</li>
 *   <li>满分 25 分，每发现一个违规扣 5 分，最低 0 分</li>
 * </ul>
 *
 * <p><b>检查规则：</b>
 * <ul>
 *   <li>API 层（{@code .api.controller.}）的类必须以 Controller 结尾</li>
 *   <li>应用层（{@code .application.usecase.}）的类必须以 UseCase 结尾</li>
 *   <li>基础设施层仓储实现（{@code .infrastructure.*.repository.}）必须以 Impl 或 Repository 结尾，或以 Jpa 开头</li>
 * </ul>
 *
 * <p><b>协作关系：</b>被 {@link PlanSensorChain} 按 Order 顺序调用，
 * 验证结果汇总到链路总分中。
 *
 * @author zhangkai
 * @since 2026-04-06
 */
@Slf4j
@Order(1)
@Component
public class ArchComplianceSensor implements PlanSensor {

    /**
     * 全类名提取正则：匹配 Markdown 反引号中以 com.exceptioncoder.llm 开头的全类名。
     * 捕获组 1 为完整类名，如 {@code com.exceptioncoder.llm.api.controller.UserController}
     */
    private static final Pattern CLASS_NAME_PATTERN =
            Pattern.compile("`(com\\.exceptioncoder\\.llm\\.[a-z.]+\\.[A-Z]\\w+)`");

    /**
     * 验证设计文档中所有全类名的架构合规性。
     *
     * <p>执行流程：
     * <ol>
     *   <li>用正则从文档全文中提取所有全类名</li>
     *   <li>逐一检查每个类名是否符合分层命名规则</li>
     *   <li>计算得分（满分 25，每个违规扣 5 分）</li>
     * </ol>
     *
     * @param document 待验证的设计文档
     * @return 验证结果，包含是否通过、得分和具体违规项列表
     */
    @Override
    public ValidationResult validate(DevPlanDocument document) {
        List<String> issues = new ArrayList<>();
        String content = document.fullDocument();

        // 从文档中提取所有反引号包裹的全类名
        Matcher matcher = CLASS_NAME_PATTERN.matcher(content);
        while (matcher.find()) {
            String className = matcher.group(1);
            checkLayerCompliance(className, issues);
        }

        // 评分策略：满分 25，每个违规扣 5 分，最低 0 分
        int score = issues.isEmpty() ? 25 : Math.max(0, 25 - issues.size() * 5);
        log.debug("架构合规检查完成，issues={}，score={}", issues.size(), score);
        return new ValidationResult(issues.isEmpty(), score, issues);
    }

    /**
     * 检查单个全类名是否符合 DDD 分层命名契约。
     *
     * @param className 待检查的全类名
     * @param issues    违规问题列表，发现违规时向此列表追加
     */
    private void checkLayerCompliance(String className, List<String> issues) {
        // 规则 1：API 层 Controller 必须以 Controller 后缀结尾
        if (className.contains(".api.controller.") && !className.endsWith("Controller")) {
            issues.add("Controller 类必须以 Controller 结尾: " + className);
        }
        // 规则 2：应用层 UseCase 必须以 UseCase 后缀结尾
        if (className.contains(".application.usecase.") && !className.endsWith("UseCase")) {
            issues.add("UseCase 类必须以 UseCase 结尾: " + className);
        }
        // 规则 3：基础设施层 Repository 实现类命名需符合 Jpa 前缀或 Impl/Repository 后缀
        if (className.contains(".repository.") && className.contains(".infrastructure.")
                && !className.startsWith("Jpa") && !className.endsWith("Impl")
                && !className.endsWith("Repository")) {
            issues.add("Repository 实现类命名不规范: " + className);
        }
    }
}
