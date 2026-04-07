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
 * 命名规范传感器，检查设计文档中的类名是否符合 PascalCase 命名约定。
 *
 * <p>属于 Infrastructure层 devplan/sensor 模块，实现了 Domain 层定义的
 * {@link PlanSensor} 接口。这是一个<b>计算型传感器（ms 级）</b>，
 * 通过正则表达式从文档中提取短类名并校验其是否符合 PascalCase 规范。
 *
 * <p><b>设计思路：</b>
 * <ul>
 *   <li>与 {@link ArchComplianceSensor} 互补：前者检查分层规则，本传感器检查命名风格</li>
 *   <li>通过 {@code @Order(2)} 排在架构合规传感器之后执行</li>
 *   <li>满分 25 分，每发现一个违规扣 5 分，最低 0 分</li>
 * </ul>
 *
 * <p><b>协作关系：</b>被 {@link PlanSensorChain} 按 Order 顺序调用，
 * 验证结果汇总到链路总分中。
 *
 * @author zhangkai
 * @since 2026-04-06
 */
@Slf4j
@Order(2)
@Component
public class NamingConventionSensor implements PlanSensor {

    /**
     * 短类名提取正则：匹配以点号开头、大写字母起始的标识符（反引号结尾），
     * 捕获组 1 为短类名，如 {@code UserController}
     */
    private static final Pattern SHORT_CLASS_NAME_PATTERN =
            Pattern.compile("\\.([A-Z]\\w+)`");

    /** PascalCase 校验正则：首字母大写，后续为字母或数字，至少两个字符 */
    private static final Pattern PASCAL_CASE = Pattern.compile("^[A-Z][a-zA-Z0-9]+$");

    /**
     * 验证设计文档中所有类名是否符合 PascalCase 命名规范。
     *
     * <p>执行流程：
     * <ol>
     *   <li>用正则从文档中提取所有短类名（大写字母开头的标识符）</li>
     *   <li>逐一校验是否匹配 PascalCase 模式</li>
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

        // 从文档中提取所有短类名
        Matcher matcher = SHORT_CLASS_NAME_PATTERN.matcher(content);
        while (matcher.find()) {
            String className = matcher.group(1);
            // 校验是否符合 PascalCase：首字母大写 + 后续字母数字
            if (!PASCAL_CASE.matcher(className).matches()) {
                issues.add("类名不符合 PascalCase: " + className);
            }
        }

        // 评分策略：满分 25，每个违规扣 5 分，最低 0 分
        int score = issues.isEmpty() ? 25 : Math.max(0, 25 - issues.size() * 5);
        log.debug("命名规范检查完成，issues={}，score={}", issues.size(), score);
        return new ValidationResult(issues.isEmpty(), score, issues);
    }
}
