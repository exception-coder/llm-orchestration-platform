package com.exceptioncoder.llm.infrastructure.devplan.memory;

import com.exceptioncoder.llm.domain.devplan.model.ArchTopology;
import com.exceptioncoder.llm.domain.devplan.model.DevPlanState;
import com.exceptioncoder.llm.domain.devplan.repository.ProjectArchTopologyRepository;
import com.exceptioncoder.llm.domain.devplan.service.DevPlanMemoryManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 三级记忆管理实现
 * 短期：会话上下文（当前简化为 in-memory）
 * 长期：代码向量（Qdrant — 通过 VectorStoreRepository）
 * 结构化：架构拓扑（MySQL — 通过 ProjectArchTopologyRepository）
 */
@Slf4j
@Service
public class DevPlanMemoryManagerImpl implements DevPlanMemoryManager {

    private final ProjectArchTopologyRepository topologyRepository;

    public DevPlanMemoryManagerImpl(ProjectArchTopologyRepository topologyRepository) {
        this.topologyRepository = topologyRepository;
    }

    @Override
    public Map<String, Object> loadContext(String taskId, String query) {
        Map<String, Object> context = new HashMap<>();
        // TODO: 加载短期记忆（Redis）
        // TODO: 加载长期记忆（Qdrant 向量检索）
        log.debug("加载记忆上下文，taskId={}", taskId);
        return context;
    }

    @Override
    public void persist(String taskId, DevPlanState state) {
        // 持久化结构化记忆
        if (state.topology() != null) {
            topologyRepository.save(state.topology());
            log.debug("持久化架构拓扑，projectPath={}", state.projectPath());
        }
        // TODO: 持久化短期记忆到 Redis
        // TODO: 持久化长期记忆到 Qdrant
    }

    @Override
    public ArchTopology getCachedTopology(String projectPath) {
        return topologyRepository.findByProjectPath(projectPath).orElse(null);
    }

    @Override
    public List<String> searchRelevantCode(String query, int topK) {
        // TODO: 通过 VectorStoreRepository 检索
        return List.of();
    }
}
