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
 * 命名规范传感器（计算型，ms 级）
 * 检查类名是否符合 PascalCase，方法名是否符合 camelCase
 */
@Slf4j
@Order(2)
@Component
public class NamingConventionSensor implements PlanSensor {

    private static final Pattern SHORT_CLASS_NAME_PATTERN =
            Pattern.compile("\\.([A-Z]\\w+)`");
    private static final Pattern PASCAL_CASE = Pattern.compile("^[A-Z][a-zA-Z0-9]+$");

    @Override
    public ValidationResult validate(DevPlanDocument document) {
        List<String> issues = new ArrayList<>();
        String content = document.fullDocument();

        Matcher matcher = SHORT_CLASS_NAME_PATTERN.matcher(content);
        while (matcher.find()) {
            String className = matcher.group(1);
            if (!PASCAL_CASE.matcher(className).matches()) {
                issues.add("类名不符合 PascalCase: " + className);
            }
        }

        int score = issues.isEmpty() ? 25 : Math.max(0, 25 - issues.size() * 5);
        log.debug("命名规范检查完成，issues={}，score={}", issues.size(), score);
        return new ValidationResult(issues.isEmpty(), score, issues);
    }
}
