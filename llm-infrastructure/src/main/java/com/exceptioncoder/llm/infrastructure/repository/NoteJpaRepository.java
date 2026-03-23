package com.exceptioncoder.llm.infrastructure.repository;

import com.exceptioncoder.llm.infrastructure.entity.NoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 记录 JPA Repository
 */
@Repository
public interface NoteJpaRepository extends JpaRepository<NoteEntity, Long> {

    List<NoteEntity> findByCategoryIdOrderByCreatedAtDesc(Long categoryId);

    @Query("SELECT n FROM NoteEntity n ORDER BY n.createdAt DESC")
    List<NoteEntity> findAllOrderByCreatedAtDesc();

    @Query("SELECT n FROM NoteEntity n WHERE n.title LIKE %:keyword% OR n.content LIKE %:keyword% OR n.rawInput LIKE %:keyword% ORDER BY n.createdAt DESC")
    List<NoteEntity> search(@Param("keyword") String keyword);

    long countByCategoryId(Long categoryId);
}
