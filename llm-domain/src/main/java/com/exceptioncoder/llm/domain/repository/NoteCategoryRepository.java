package com.exceptioncoder.llm.domain.repository;

import com.exceptioncoder.llm.domain.model.NoteCategory;

import java.util.List;
import java.util.Optional;

/**
 * 记录类目仓储接口
 */
public interface NoteCategoryRepository {

    /**
     * 根据名称查找类目
     */
    Optional<NoteCategory> findByName(String name);

    /**
     * 根据ID查找类目
     */
    Optional<NoteCategory> findById(Long id);

    /**
     * 获取所有类目（按排序顺序）
     */
    List<NoteCategory> findAllOrderBySortOrder();

    /**
     * 保存类目
     */
    NoteCategory save(NoteCategory category);

    /**
     * 删除类目
     */
    void deleteById(Long id);
}
