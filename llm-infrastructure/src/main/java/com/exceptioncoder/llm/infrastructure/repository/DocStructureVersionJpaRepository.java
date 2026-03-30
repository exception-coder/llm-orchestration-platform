package com.exceptioncoder.llm.infrastructure.repository;

import com.exceptioncoder.llm.infrastructure.entity.DocStructureVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface DocStructureVersionJpaRepository extends JpaRepository<DocStructureVersionEntity, Long> {

    Optional<DocStructureVersionEntity> findByActiveTrue();

    Optional<DocStructureVersionEntity> findByVersion(int version);

    List<DocStructureVersionEntity> findAllByOrderByVersionDesc();

    @Query("SELECT COALESCE(MAX(e.version), 0) FROM DocStructureVersionEntity e")
    int findMaxVersion();

    @Transactional
    @Modifying
    @Query("UPDATE DocStructureVersionEntity e SET e.active = false")
    void deactivateAll();
}
