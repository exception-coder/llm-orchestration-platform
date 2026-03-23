package com.exceptioncoder.llm.application.usecase;

import com.exceptioncoder.llm.domain.model.NoteCategory;
import com.exceptioncoder.llm.domain.repository.NoteCategoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 记录类目用例
 */
@Slf4j
@Service
public class NoteCategoryUseCase {

    private final NoteCategoryRepository categoryRepository;

    public NoteCategoryUseCase(NoteCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * 获取所有类目
     */
    public List<NoteCategory> getAllCategories() {
        return categoryRepository.findAllOrderBySortOrder();
    }

    /**
     * 获取或创建类目
     */
    @Transactional
    public NoteCategory getOrCreateCategory(String name, String description, String icon) {
        return categoryRepository.findByName(name)
                .orElseGet(() -> {
                    NoteCategory newCategory = NoteCategory.builder()
                            .name(name)
                            .description(description)
                            .icon(icon)
                            .sortOrder(0)
                            .build();
                    log.info("AI 新建类目: {}", name);
                    return categoryRepository.save(newCategory);
                });
    }

    /**
     * 删除类目（级联删除关联记录）
     */
    @Transactional
    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }
}
