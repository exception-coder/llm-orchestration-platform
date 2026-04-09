package com.exceptioncoder.llm.infrastructure.repository.devplan;

import com.exceptioncoder.llm.infrastructure.entity.devplan.ProfileIndexStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 画像索引状态 Spring Data JPA 仓储。
 *
 * @author zhangkai
 * @since 2026-04-08
 */
public interface ProfileIndexStatusJpaRepository extends JpaRepository<ProfileIndexStatusEntity, Long> {

    Optional<ProfileIndexStatusEntity> findByProjectPathAndDimension(String projectPath, String dimension);

    List<ProfileIndexStatusEntity> findByProjectPath(String projectPath);

    List<ProfileIndexStatusEntity> findByProjectName(String projectName);
}
