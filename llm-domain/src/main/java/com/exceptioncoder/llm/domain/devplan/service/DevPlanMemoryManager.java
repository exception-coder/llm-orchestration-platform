package com.exceptioncoder.llm.domain.devplan.service;

import com.exceptioncoder.llm.domain.devplan.model.ArchTopology;
import com.exceptioncoder.llm.domain.devplan.model.DevPlanState;

import java.util.List;
import java.util.Map;

/**
 * 三级记忆管理接口
 */
public interface DevPlanMemoryManager {

    /**
     * 加载上下文：短期 + 长期 + 结构化记忆
     */
    Map<String, Object> loadContext(String taskId, String query);

    /**
     * 持久化记忆
     */
    void persist(String taskId, DevPlanState state);

    /**
     * 获取缓存的架构拓扑（结构化记忆）
     */
    ArchTopology getCachedTopology(String projectPath);

    /**
     * 搜索相关代码片段（长期记忆）
     */
    List<String> searchRelevantCode(String query, int topK);
}
