package com.exceptioncoder.llm.infrastructure.devplan.repository;

import com.exceptioncoder.llm.domain.devplan.model.ArchTopology;
import com.exceptioncoder.llm.domain.devplan.repository.ProjectArchTopologyRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 项目架构拓扑仓储的内存实现。
 *
 * <p>属于 Infrastructure层 devplan/repository 模块，实现了 Domain 层定义的
 * {@link ProjectArchTopologyRepository} 接口。采用 {@link ConcurrentHashMap} 存储
 * 项目路径到架构拓扑的映射关系。
 *
 * <p><b>设计思路：</b>与 {@link InMemoryDevPlanTaskRepository} 同理，
 * 一期使用内存存储快速验证，后续切换为 JPA 实现（持久化到 MySQL）。
 * 架构拓扑数据量较小（每个项目一条），内存存储在一期完全够用。
 *
 * <p><b>协作关系：</b>
 * <ul>
 *   <li>实现 {@link ProjectArchTopologyRepository}（Domain 层接口）</li>
 *   <li>被 {@link com.exceptioncoder.llm.infrastructure.devplan.memory.DevPlanMemoryManagerImpl}
 *       调用，用于架构拓扑的缓存读写</li>
 * </ul>
 *
 * @author zhangkai
 * @since 2026-04-06
 */
@Repository
public class InMemoryProjectArchTopologyRepository implements ProjectArchTopologyRepository {

    /** 内存存储，key 为项目路径（projectPath），value 为架构拓扑对象 */
    private final Map<String, ArchTopology> store = new ConcurrentHashMap<>();

    /**
     * 根据项目路径查找架构拓扑。
     *
     * @param projectPath 项目路径
     * @return 包含架构拓扑的 Optional，不存在时返回 empty
     */
    @Override
    public Optional<ArchTopology> findByProjectPath(String projectPath) {
        return Optional.ofNullable(store.get(projectPath));
    }

    /**
     * 保存或更新项目的架构拓扑。
     *
     * <p>以 projectPath 为 key，相同项目路径的拓扑会被覆盖（最新一次扫描结果生效）。
     *
     * @param topology 待保存的架构拓扑对象
     */
    @Override
    public void save(ArchTopology topology) {
        store.put(topology.projectPath(), topology);
    }
}
