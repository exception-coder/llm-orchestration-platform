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
 * 架构合规传感器（计算型，ms 级）
 * 检查设计文档中的全类名是否遵循 DDD 分层规则
 */
@Slf4j
@Order(1)
@Component
public class ArchComplianceSensor implements PlanSensor {

    private static final Pattern CLASS_NAME_PATTERN =
            Pattern.compile("`(com\\.exceptioncoder\\.llm\\.[a-z.]+\\.[A-Z]\\w+)`");

    @Override
    public ValidationResult validate(DevPlanDocument document) {
        List<String> issues = new ArrayList<>();
        String content = document.fullDocument();

        Matcher matcher = CLASS_NAME_PATTERN.matcher(content);
        while (matcher.find()) {
            String className = matcher.group(1);
            checkLayerCompliance(className, issues);
        }

        int score = issues.isEmpty() ? 25 : Math.max(0, 25 - issues.size() * 5);
        log.debug("架构合规检查完成，issues={}，score={}", issues.size(), score);
        return new ValidationResult(issues.isEmpty(), score, issues);
    }

    private void checkLayerCompliance(String className, List<String> issues) {
        if (className.contains(".api.controller.") && !className.endsWith("Controller")) {
            issues.add("Controller 类必须以 Controller 结尾: " + className);
        }
        if (className.contains(".application.usecase.") && !className.endsWith("UseCase")) {
            issues.add("UseCase 类必须以 UseCase 结尾: " + className);
        }
        if (className.contains(".repository.") && className.contains(".infrastructure.")
                && !className.startsWith("Jpa") && !className.endsWith("Impl")
                && !className.endsWith("Repository")) {
            issues.add("Repository 实现类命名不规范: " + className);
        }
    }
}
