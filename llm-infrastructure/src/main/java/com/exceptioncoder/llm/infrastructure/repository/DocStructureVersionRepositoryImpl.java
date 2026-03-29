package com.exceptioncoder.llm.infrastructure.repository;

import com.exceptioncoder.llm.domain.model.DocStructureVersion;
import com.exceptioncoder.llm.domain.repository.DocStructureVersionRepository;
import com.exceptioncoder.llm.infrastructure.entity.DocStructureVersionEntity;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class DocStructureVersionRepositoryImpl implements DocStructureVersionRepository {

    private final DocStructureVersionJpaRepository jpaRepository;

    public DocStructureVersionRepositoryImpl(DocStructureVersionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<DocStructureVersion> findActive() {
        return jpaRepository.findByActiveTrue().map(this::toDomain);
    }

    @Override
    public Optional<DocStructureVersion> findByVersion(int version) {
        return jpaRepository.findByVersion(version).map(this::toDomain);
    }

    @Override
    public List<DocStructureVersion> findAll() {
        return jpaRepository.findAllByOrderByVersionDesc().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DocStructureVersion save(DocStructureVersion version) {
        DocStructureVersionEntity entity = toEntity(version);
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    @Transactional
    public void deactivateAll() {
        jpaRepository.deactivateAll();
    }

    @Override
    public int getMaxVersion() {
        return jpaRepository.findMaxVersion();
    }

    private DocStructureVersion toDomain(DocStructureVersionEntity e) {
        return DocStructureVersion.builder()
                .id(e.getId())
                .version(e.getVersion())
                .structure(e.getStructure())
                .diffSummary(e.getDiffSummary())
                .readmeHash(e.getReadmeHash())
                .active(e.isActive())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private DocStructureVersionEntity toEntity(DocStructureVersion v) {
        return DocStructureVersionEntity.builder()
                .id(v.getId())
                .version(v.getVersion())
                .structure(v.getStructure())
                .diffSummary(v.getDiffSummary())
                .readmeHash(v.getReadmeHash())
                .active(v.isActive())
                .build();
    }
}
