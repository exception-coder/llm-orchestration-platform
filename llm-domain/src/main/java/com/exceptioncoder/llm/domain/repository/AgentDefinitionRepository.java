package com.exceptioncoder.llm.domain.repository;

import com.exceptioncoder.llm.domain.model.AgentDefinition;

import java.util.List;
import java.util.Optional;

/**
 * Agent 定义仓储接口
 */
public interface AgentDefinitionRepository {

    /**
     * 保存 Agent 定义（新增或更新）
     */
    AgentDefinition save(AgentDefinition agent);

    /**
     * 根据 ID 查询
     */
    Optional<AgentDefinition> findById(String id);

    /**
     * 获取所有启用的 Agent
     */
    List<AgentDefinition> findAllEnabled();

    /**
     * 获取所有 Agent
     */
    List<AgentDefinition> findAll();

    /**
     * 删除 Agent
     */
    void deleteById(String id);

    /**
     * 检查是否存在
     */
    boolean existsById(String id);
}
