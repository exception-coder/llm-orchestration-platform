package com.exceptioncoder.llm.domain.devplan.analysis;

import java.util.List;
import java.util.Map;

/**
 * 配置分析结果 -- 语言无关的标准输出模型。
 *
 * @param configFiles      发现的配置文件列表
 * @param profiles         配置 profile 列表
 * @param server           服务器配置（port、context-path 等）
 * @param datasource       数据源配置
 * @param externalServices 外部服务配置（Redis、MQ、向量库等）
 * @param customProperties 自定义业务配置
 */
public record ConfigResult(
        List<String> configFiles,
        List<String> profiles,
        Map<String, Object> server,
        Map<String, Object> datasource,
        List<ExternalService> externalServices,
        Map<String, Object> customProperties
) {
    public record ExternalService(String name, String configKey, String configValue) {}
}
