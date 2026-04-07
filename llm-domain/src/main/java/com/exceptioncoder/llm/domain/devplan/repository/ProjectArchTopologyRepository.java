package com.exceptioncoder.llm.domain.devplan.repository;

import com.exceptioncoder.llm.domain.devplan.model.ArchTopology;

import java.util.Optional;

/**
 * 架构拓扑仓储接口 -- 结构化记忆中架构拓扑数据的持久化契约。
 *
 * <p>属于 Domain 层 devplan 模块。将 CODE_AWARENESS Agent 分析得到的
 * {@link ArchTopology} 持久化到数据库，作为结构化记忆的一部分。后续同一项目
 * 的方案生成任务可直接复用缓存的拓扑数据，避免重复分析。</p>
 *
 * @author zhangkai
 * @since 2026-04-06
 */
public interface ProjectArchTopologyRepository {

    /**
     * 根据项目路径查询缓存的架构拓扑。
     *
     * @param projectPath 目标项目的本地路径
     * @return 架构拓扑的 Optional 包装，不存在时返回 empty
     */
    Optional<ArchTopology> findByProjectPath(String projectPath);

    /**
     * 保存或更新架构拓扑数据。
     *
     * @param topology 待保存的架构拓扑实体
     */
    void save(ArchTopology topology);
}
