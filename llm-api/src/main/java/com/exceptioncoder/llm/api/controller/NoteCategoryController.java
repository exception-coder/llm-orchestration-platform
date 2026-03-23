package com.exceptioncoder.llm.api.controller;

import com.exceptioncoder.llm.api.dto.NoteCategoryDTO;
import com.exceptioncoder.llm.application.usecase.NoteCategoryUseCase;
import com.exceptioncoder.llm.domain.repository.NoteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 记录类目 API 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/notes/categories")
public class NoteCategoryController {

    private final NoteCategoryUseCase categoryUseCase;
    private final NoteRepository noteRepository;

    public NoteCategoryController(NoteCategoryUseCase categoryUseCase, NoteRepository noteRepository) {
        this.categoryUseCase = categoryUseCase;
        this.noteRepository = noteRepository;
    }

    /**
     * 获取所有类目
     */
    @GetMapping
    public List<NoteCategoryDTO> getAllCategories() {
        return categoryUseCase.getAllCategories().stream()
                .map(c -> {
                    long count = noteRepository.countByCategoryId(c.getId());
                    return NoteCategoryDTO.fromDomain(c, count);
                })
                .collect(Collectors.toList());
    }

    /**
     * 删除类目
     */
    @DeleteMapping("/{id}")
    public void deleteCategory(@PathVariable Long id) {
        categoryUseCase.deleteCategory(id);
    }
}
