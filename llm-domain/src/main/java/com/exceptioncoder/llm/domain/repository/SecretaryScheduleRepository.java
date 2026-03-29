package com.exceptioncoder.llm.domain.repository;

import com.exceptioncoder.llm.domain.model.SecretarySchedule;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 日程仓储接口
 */
public interface SecretaryScheduleRepository {

    SecretarySchedule save(SecretarySchedule schedule);

    Optional<SecretarySchedule> findById(Long id);

    List<SecretarySchedule> findByUserId(String userId);

    List<SecretarySchedule> findUpcoming(String userId, LocalDateTime from, LocalDateTime to);

    void deleteById(Long id);

    SecretarySchedule markDone(Long id);
}
