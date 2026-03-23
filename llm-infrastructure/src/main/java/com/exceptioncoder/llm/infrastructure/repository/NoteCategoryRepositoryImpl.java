package com.exceptioncoder.llm.infrastructure.repository;

import com.exceptioncoder.llm.domain.model.NoteCategory;
import com.exceptioncoder.llm.domain.repository.NoteCategoryRepository;
import com.exceptioncoder.llm.infrastructure.entity.NoteCategoryEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 记录类目仓储实现
 */
@Slf4j
@Repository
public class NoteCategoryRepositoryImpl implements NoteCategoryRepository {

    private final NoteCategoryJpaRepository jpaRepository;

    public NoteCategoryRepositoryImpl(NoteCategoryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<NoteCategory> findByName(String name) {
        return jpaRepository.findByName(name).map(this::toDomain);
    }

    @Override
    public Optional<NoteCategory> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<NoteCategory> findAllOrderBySortOrder() {
        return jpaRepository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public NoteCategory save(NoteCategory category) {
        NoteCategoryEntity entity = toEntity(category);
        if (entity.getId() == null) {
            log.info("创建新类目: {}", category.getName());
        } else {
            log.info("更新类目: {}", category.getName());
        }
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
        log.info("删除类目: {}", id);
    }

    private NoteCategory toDomain(NoteCategoryEntity entity) {
        return NoteCategory.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .icon(entity.getIcon())
                .sortOrder(entity.getSortOrder())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private NoteCategoryEntity toEntity(NoteCategory domain) {
        return NoteCategoryEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .description(domain.getDescription())
                .icon(domain.getIcon())
                .sortOrder(domain.getSortOrder())
                .build();
    }
}
