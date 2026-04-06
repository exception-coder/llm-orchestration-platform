package com.exceptioncoder.llm.domain.devplan.service;

import com.exceptioncoder.llm.domain.devplan.model.DevPlanDocument;
import com.exceptioncoder.llm.domain.devplan.model.ValidationResult;

/**
 * 方案验证传感器接口
 */
public interface PlanSensor {

    /**
     * 验证设计文档
     */
    ValidationResult validate(DevPlanDocument document);
}
