package com.exceptioncoder.llm.infrastructure.repository.devplan;

import com.exceptioncoder.llm.domain.devplan.model.ProfileDimension;
import com.exceptioncoder.llm.domain.devplan.model.ProfileIndexStatus;
import com.exceptioncoder.llm.domain.devplan.repository.ProfileIndexStatusRepository;
import com.exceptioncoder.llm.infrastructure.entity.devplan.ProfileIndexStatusEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 画像索引状态仓储的 JPA 实现 -- 桥接 domain 模型与 JPA 实体。
 *
 * <p>通过 projectPath + dimension 唯一约束实现 upsert 语义：
 * 存在则更新，不存在则插入。
 *
 * @author zhangkai
 * @since 2026-04-08
 */
@Slf4j
@Repository
public class ProfileIndexStatusRepositoryImpl implements ProfileIndexStatusRepository {

    private final ProfileIndexStatusJpaRepository jpaRepository;

    public ProfileIndexStatusRepositoryImpl(ProfileIndexStatusJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<ProfileIndexStatus> findByProjectPathAndDimension(String projectPath, ProfileDimension dimension) {
        return jpaRepository.findByProjectPathAndDimension(projectPath, dimension.name())
                .map(this::toDomain);
    }

    @Override
    public List<ProfileIndexStatus> findByProjectPath(String projectPath) {
        return jpaRepository.findByProjectPath(projectPath).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public ProfileIndexStatus save(ProfileIndexStatus status) {
        // upsert：按 projectPath + dimension 查找已有记录
        Optional<ProfileIndexStatusEntity> existing = jpaRepository
                .findByProjectPathAndDimension(status.projectPath(), status.dimension().name());

        ProfileIndexStatusEntity entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.setProjectName(status.projectName());
            entity.setStatus(status.status());
            entity.setContentHash(status.contentHash());
            entity.setLastIndexedAt(status.lastIndexedAt());
        } else {
            entity = toEntity(status);
        }

        ProfileIndexStatusEntity saved = jpaRepository.save(entity);
        log.debug("画像索引状态已保存: project={}, dimension={}, status={}",
                saved.getProjectPath(), saved.getDimension(), saved.getStatus());
        return toDomain(saved);
    }

    @Override
    public List<ProfileIndexStatus> saveAll(List<ProfileIndexStatus> statuses) {
        return statuses.stream()
                .map(this::save)
                .toList();
    }

    private ProfileIndexStatus toDomain(ProfileIndexStatusEntity entity) {
        return new ProfileIndexStatus(
                entity.getProjectPath(),
                entity.getProjectName(),
                ProfileDimension.valueOf(entity.getDimension()),
                entity.getStatus(),
                entity.getLastIndexedAt(),
                entity.getContentHash()
        );
    }

    private ProfileIndexStatusEntity toEntity(ProfileIndexStatus status) {
        return ProfileIndexStatusEntity.builder()
                .projectPath(status.projectPath())
                .projectName(status.projectName())
                .dimension(status.dimension().name())
                .status(status.status())
                .contentHash(status.contentHash())
                .lastIndexedAt(status.lastIndexedAt())
                .build();
    }
}
