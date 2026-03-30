package com.exceptioncoder.llm.infrastructure.repository.secretary;

import com.exceptioncoder.llm.infrastructure.entity.secretary.SecretaryScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SecretaryScheduleJpaRepository extends JpaRepository<SecretaryScheduleEntity, Long> {

    List<SecretaryScheduleEntity> findByUserIdOrderByStartTimeAsc(String userId);

    @Query("SELECT s FROM SecretaryScheduleEntity s WHERE s.userId = :userId " +
            "AND s.startTime >= :from AND s.startTime <= :to ORDER BY s.startTime ASC")
    List<SecretaryScheduleEntity> findUpcoming(
            @Param("userId") String userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
