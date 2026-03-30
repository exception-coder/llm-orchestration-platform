package com.exceptioncoder.llm.infrastructure.repository.secretary;

import com.exceptioncoder.llm.domain.model.SecretarySchedule;
import com.exceptioncoder.llm.domain.repository.SecretaryScheduleRepository;
import com.exceptioncoder.llm.infrastructure.entity.secretary.SecretaryScheduleEntity;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class SecretaryScheduleRepositoryImpl implements SecretaryScheduleRepository {

    private final SecretaryScheduleJpaRepository jpaRepository;

    public SecretaryScheduleRepositoryImpl(SecretaryScheduleJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public SecretarySchedule save(SecretarySchedule schedule) {
        SecretaryScheduleEntity entity = SecretaryScheduleEntity.builder()
                .id(schedule.id())
                .userId(schedule.userId())
                .title(schedule.title())
                .description(schedule.description())
                .startTime(schedule.startTime())
                .endTime(schedule.endTime())
                .reminder(schedule.reminder())
                .done(schedule.done())
                .build();
        SecretaryScheduleEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<SecretarySchedule> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<SecretarySchedule> findByUserId(String userId) {
        return jpaRepository.findByUserIdOrderByStartTimeAsc(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<SecretarySchedule> findUpcoming(String userId, LocalDateTime from, LocalDateTime to) {
        return jpaRepository.findUpcoming(userId, from, to).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public SecretarySchedule markDone(Long id) {
        SecretaryScheduleEntity entity = jpaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("日程不存在: " + id));
        entity.setDone(true);
        return toDomain(jpaRepository.save(entity));
    }

    private SecretarySchedule toDomain(SecretaryScheduleEntity e) {
        return new SecretarySchedule(
                e.getId(), e.getUserId(), e.getTitle(), e.getDescription(),
                e.getStartTime(), e.getEndTime(), e.isReminder(), e.isDone(), e.getCreatedAt());
    }
}
