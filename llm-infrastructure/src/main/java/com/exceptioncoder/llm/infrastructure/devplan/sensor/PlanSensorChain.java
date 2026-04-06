package com.exceptioncoder.llm.infrastructure.devplan.sensor;

import com.exceptioncoder.llm.domain.devplan.model.DevPlanDocument;
import com.exceptioncoder.llm.domain.devplan.model.ValidationResult;
import com.exceptioncoder.llm.domain.devplan.service.PlanSensor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 传感器链 — 按 @Order 顺序执行所有传感器，汇总结果
 * 计算型（ms 级）先于推理型（s 级）
 */
@Slf4j
@Component("devPlanSensorChain")
public class PlanSensorChain implements PlanSensor {

    private final List<PlanSensor> sensors;

    public PlanSensorChain(List<PlanSensor> sensors) {
        // 过滤掉自身，避免递归
        this.sensors = sensors.stream()
                .filter(s -> !(s instanceof PlanSensorChain))
                .toList();
        log.info("传感器链初始化，共 {} 个传感器", this.sensors.size());
    }

    @Override
    public ValidationResult validate(DevPlanDocument document) {
        log.info("开始传感器链验证");
        List<String> allIssues = new ArrayList<>();
        int totalScore = 0;

        for (PlanSensor sensor : sensors) {
            ValidationResult result = sensor.validate(document);
            totalScore += result.score();
            allIssues.addAll(result.issues());
            log.debug("传感器 {} 完成，score={}，issues={}",
                    sensor.getClass().getSimpleName(), result.score(), result.issues().size());
        }

        boolean passed = totalScore >= ValidationResult.PASS_THRESHOLD;
        log.info("传感器链验证完成，totalScore={}，passed={}，issues={}",
                totalScore, passed, allIssues.size());
        return new ValidationResult(passed, totalScore, allIssues);
    }
}
