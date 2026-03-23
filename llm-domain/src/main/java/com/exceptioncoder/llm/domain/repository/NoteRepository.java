package com.exceptioncoder.llm.domain.repository;

import com.exceptioncoder.llm.domain.model.Note;

import java.util.List;
import java.util.Optional;

/**
 * 记录仓储接口
 */
public interface NoteRepository {

    /**
     * 根据ID查找记录
     */
    Optional<Note> findById(Long id);

    /**
     * 根据类目ID查找记录（按时间倒序）
     */
    List<Note> findByCategoryIdOrderByCreatedAtDesc(Long categoryId);

    /**
     * 获取所有记录（按时间倒序）
     */
    List<Note> findAllOrderByCreatedAtDesc();

    /**
     * 搜索记录（按标题或内容模糊匹配）
     */
    List<Note> search(String keyword);

    /**
     * 保存记录
     */
    Note save(Note note);

    /**
     * 删除记录
     */
    void deleteById(Long id);

    /**
     * 统计类目下的记录数
     */
    long countByCategoryId(Long categoryId);
}
