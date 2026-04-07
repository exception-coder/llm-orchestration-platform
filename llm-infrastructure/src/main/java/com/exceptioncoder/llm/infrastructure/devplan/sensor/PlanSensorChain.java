package com.exceptioncoder.llm.infrastructure.devplan.sensor;

import com.exceptioncoder.llm.domain.devplan.model.DevPlanDocument;
import com.exceptioncoder.llm.domain.devplan.model.ValidationResult;
import com.exceptioncoder.llm.domain.devplan.service.PlanSensor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 传感器链，按 {@code @Order} 顺序依次执行所有注册的 {@link PlanSensor}，并汇总验证结果。
 *
 * <p>属于 Infrastructure层 devplan/sensor 模块，自身也实现了 {@link PlanSensor} 接口，
 * 采用<b>组合模式（Composite Pattern）</b>将多个传感器聚合为一个统一的验证入口。
 *
 * <p><b>设计思路：</b>
 * <ul>
 *   <li>通过 Spring 的 {@code List<PlanSensor>} 自动注入所有 PlanSensor Bean，
 *       并按 {@code @Order} 注解排序，确保计算型传感器（ms 级）先于推理型传感器（s 级）执行</li>
 *   <li>构造时过滤掉自身（PlanSensorChain），避免无限递归</li>
 *   <li>总分为各传感器得分之和，是否通过由 {@link ValidationResult#PASS_THRESHOLD} 阈值决定</li>
 * </ul>
 *
 * <p><b>协作关系：</b>
 * <ul>
 *   <li>实现 {@link PlanSensor}（Domain 层接口），对外提供统一的验证入口</li>
 *   <li>内部聚合 {@link ArchComplianceSensor}、{@link NamingConventionSensor} 等具体传感器</li>
 *   <li>被 Domain 层的编排服务在审查阶段调用</li>
 * </ul>
 *
 * @author zhangkai
 * @since 2026-04-06
 */
@Slf4j
@Component("devPlanSensorChain")
public class PlanSensorChain implements PlanSensor {

    /** 有序的传感器列表，按 @Order 从小到大排列（已排除自身） */
    private final List<PlanSensor> sensors;

    /**
     * 构造传感器链，注入所有 PlanSensor Bean 并过滤自身。
     *
     * <p>Spring 会按 {@code @Order} 注解对注入的列表排序，
     * 因此无需手动排序。过滤自身是因为 PlanSensorChain 本身也实现了 PlanSensor，
     * 如果不排除会形成无限递归调用。
     *
     * @param sensors Spring 容器中所有 PlanSensor 实现（包含自身）
     */
    public PlanSensorChain(List<PlanSensor> sensors) {
        // 过滤掉自身，避免 validate 时递归调用
        this.sensors = sensors.stream()
                .filter(s -> !(s instanceof PlanSensorChain))
                .toList();
        log.info("传感器链初始化，共 {} 个传感器", this.sensors.size());
    }

    /**
     * 依次执行所有传感器，汇总验证结果。
     *
     * <p>执行流程：
     * <ol>
     *   <li>按 Order 顺序逐一调用每个传感器的 validate 方法</li>
     *   <li>累加各传感器得分，合并所有违规问题</li>
     *   <li>总分与阈值比较，决定整体是否通过</li>
     * </ol>
     *
     * @param document 待验证的设计文档
     * @return 汇总后的验证结果，包含总分、是否通过、所有传感器发现的问题列表
     */
    @Override
    public ValidationResult validate(DevPlanDocument document) {
        log.info("开始传感器链验证");
        List<String> allIssues = new ArrayList<>();
        int totalScore = 0;

        // 依次执行每个传感器，累加得分并收集问题
        for (PlanSensor sensor : sensors) {
            ValidationResult result = sensor.validate(document);
            totalScore += result.score();
            allIssues.addAll(result.issues());
            log.debug("传感器 {} 完成，score={}，issues={}",
                    sensor.getClass().getSimpleName(), result.score(), result.issues().size());
        }

        // 总分与阈值比较，决定是否通过
        boolean passed = totalScore >= ValidationResult.PASS_THRESHOLD;
        log.info("传感器链验证完成，totalScore={}，passed={}，issues={}",
                totalScore, passed, allIssues.size());
        return new ValidationResult(passed, totalScore, allIssues);
    }
}
