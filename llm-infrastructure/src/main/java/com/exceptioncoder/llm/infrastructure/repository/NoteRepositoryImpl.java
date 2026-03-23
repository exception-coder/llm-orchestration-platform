package com.exceptioncoder.llm.infrastructure.repository;

import com.exceptioncoder.llm.domain.model.Note;
import com.exceptioncoder.llm.domain.repository.NoteRepository;
import com.exceptioncoder.llm.infrastructure.entity.NoteEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 记录仓储实现
 */
@Slf4j
@Repository
public class NoteRepositoryImpl implements NoteRepository {

    private final NoteJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;

    public NoteRepositoryImpl(NoteJpaRepository jpaRepository, ObjectMapper objectMapper) {
        this.jpaRepository = jpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<Note> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Note> findByCategoryIdOrderByCreatedAtDesc(Long categoryId) {
        return jpaRepository.findByCategoryIdOrderByCreatedAtDesc(categoryId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Note> findAllOrderByCreatedAtDesc() {
        return jpaRepository.findAllOrderByCreatedAtDesc().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Note> search(String keyword) {
        return jpaRepository.search(keyword).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Note save(Note note) {
        if (note.getId() == null) {
            log.info("创建新记录: {}", note.getTitle());
        } else {
            log.info("更新记录: {}", note.getId());
        }
        return toDomain(jpaRepository.save(toEntity(note)));
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
        log.info("删除记录: {}", id);
    }

    @Override
    public long countByCategoryId(Long categoryId) {
        return jpaRepository.countByCategoryId(categoryId);
    }

    private Note toDomain(NoteEntity entity) {
        return Note.builder()
                .id(entity.getId())
                .categoryId(entity.getCategoryId())
                .title(entity.getTitle())
                .rawInput(entity.getRawInput())
                .content(entity.getContent())
                .summary(entity.getSummary())
                .isEncrypted(entity.getIsEncrypted())
                .isVoice(entity.getIsVoice())
                .tags(parseTags(entity.getTags()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private NoteEntity toEntity(Note domain) {
        return NoteEntity.builder()
                .id(domain.getId())
                .categoryId(domain.getCategoryId())
                .title(domain.getTitle())
                .rawInput(domain.getRawInput())
                .content(domain.getContent())
                .summary(domain.getSummary())
                .isEncrypted(domain.getIsEncrypted())
                .isVoice(domain.getIsVoice())
                .tags(serializeTags(domain.getTags()))
                .build();
    }

    private List<String> parseTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(tags, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.warn("解析 tags 失败: {}", tags, e);
            return Collections.emptyList();
        }
    }

    private String serializeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(tags);
        } catch (JsonProcessingException e) {
            log.warn("序列化 tags 失败", e);
            return null;
        }
    }
}
