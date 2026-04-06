package com.exceptioncoder.llm.domain.devplan.service;

import com.exceptioncoder.llm.domain.devplan.model.ArchTopology;
import com.exceptioncoder.llm.domain.devplan.model.DevPlanState;

import java.util.List;
import java.util.Map;

/**
 * 三级记忆管理接口 -- 管理 Agent 执行过程中的短期、长期和结构化记忆。
 *
 * <p>属于 Domain 层 devplan 模块。采用三级记忆架构：</p>
 * <ul>
 *   <li><b>短期记忆</b>：当前任务执行过程中的上下文（如中间产出物）</li>
 *   <li><b>长期记忆</b>：向量化存储的代码片段，支持语义检索</li>
 *   <li><b>结构化记忆</b>：缓存的架构拓扑等结构化数据，避免重复分析</li>
 * </ul>
 *
 * @author zhangkai
 * @since 2026-04-06
 */
public interface DevPlanMemoryManager {

    /**
     * 加载混合上下文，融合三级记忆中与当前查询相关的信息。
     *
     * @param taskId 任务唯一标识，用于定位短期记忆
     * @param query  查询文本，用于在长期记忆中进行语义检索
     * @return 融合后的上下文 Map，key 为记忆类型标识，value 为对应内容
     */
    Map<String, Object> loadContext(String taskId, String query);

    /**
     * 将当前任务状态持久化到记忆存储中。
     *
     * <p>在每个 Node 执行完毕后调用，确保中间产出物不会因异常丢失。</p>
     *
     * @param taskId 任务唯一标识
     * @param state  当前全局状态快照
     */
    void persist(String taskId, DevPlanState state);

    /**
     * 从结构化记忆中获取缓存的架构拓扑。
     *
     * <p>若缓存命中且未过期，可跳过 CODE_AWARENESS 阶段的拓扑分析步骤。</p>
     *
     * @param projectPath 目标项目的本地路径
     * @return 缓存的架构拓扑，若不存在则返回 null
     */
    ArchTopology getCachedTopology(String projectPath);

    /**
     * 从长期记忆（向量数据库）中检索与查询语义相关的代码片段。
     *
     * @param query 查询文本
     * @param topK  返回最相关的前 K 条结果
     * @return 相关代码片段列表，按相关度降序排列
     */
    List<String> searchRelevantCode(String query, int topK);
}
