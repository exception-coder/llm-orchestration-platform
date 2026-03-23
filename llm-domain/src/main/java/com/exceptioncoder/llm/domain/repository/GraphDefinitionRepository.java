package com.exceptioncoder.llm.domain.repository;

import com.exceptioncoder.llm.domain.model.GraphDefinition;

import java.util.List;
import java.util.Optional;

/**
 * Graph 定义仓储接口
 */
public interface GraphDefinitionRepository {

    /**
     * 保存 Graph 定义（新增或更新）
     */
    GraphDefinition save(GraphDefinition graph);

    /**
     * 根据 ID 查询
     */
    Optional<GraphDefinition> findById(String id);

    /**
     * 获取所有启用的 Graph
     */
    List<GraphDefinition> findAllEnabled();

    /**
     * 获取所有 Graph
     */
    List<GraphDefinition> findAll();

    /**
     * 删除 Graph
     */
    void deleteById(String id);

    /**
     * 检查是否存在
     */
    boolean existsById(String id);
}
