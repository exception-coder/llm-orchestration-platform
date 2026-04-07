package com.exceptioncoder.llm.api.dto.devplan;

/**
 * 开发方案生成请求 DTO —— 封装前端提交的方案生成参数。
 *
 * <p>本类属于 <b>API 层（DTO）</b>，是 {@code POST /api/v1/dev-plan/generate} 端点的请求体。
 * 使用 Java Record 实现不可变数据载体，在紧凑构造器中完成必填字段校验。</p>
 *
 * <h3>字段说明</h3>
 * <ul>
 *   <li>{@code projectPath} — 目标项目路径（必填），用于代码感知扫描</li>
 *   <li>{@code requirement} — 用户需求描述（必填），LLM 将基于此生成方案</li>
 *   <li>{@code templateType} — 方案模板类型（可选），预留扩展</li>
 *   <li>{@code forceReindex} — 是否强制重新索引项目（可选），默认 false</li>
 *   <li>{@code timeoutSeconds} — 超时时间秒数（可选），默认 300 秒</li>
 * </ul>
 *
 * @author zhangkai
 * @since 2026-04-06
 */
public record DevPlanRequest(
        /** 目标项目路径（必填） */
        String projectPath,
        /** 用户需求描述文本（必填） */
        String requirement,
        /** 方案模板类型（可选），预留扩展字段 */
        String templateType,
        /** 是否强制重新索引项目（可选） */
        Boolean forceReindex,
        /** 超时时间秒数（可选），默认 300 秒 */
        Integer timeoutSeconds
) {
    /**
     * 紧凑构造器 —— 执行必填字段校验。
     *
     * @throws IllegalArgumentException 当 projectPath 或 requirement 为空时抛出
     */
    public DevPlanRequest {
        if (projectPath == null || projectPath.isBlank()) {
            throw new IllegalArgumentException("projectPath 不能为空");
        }
        if (requirement == null || requirement.isBlank()) {
            throw new IllegalArgumentException("requirement 不能为空");
        }
    }

    /**
     * 获取解析后的超时时间。若未指定则返回默认值 300 秒。
     *
     * @return 超时时间（秒）
     */
    public int resolvedTimeoutSeconds() {
        return timeoutSeconds != null ? timeoutSeconds : 300;
    }
}
