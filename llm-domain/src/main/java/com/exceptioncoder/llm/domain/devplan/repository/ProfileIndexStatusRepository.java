package com.exceptioncoder.llm.domain.devplan.repository;

import com.exceptioncoder.llm.domain.devplan.model.ProfileDimension;
import com.exceptioncoder.llm.domain.devplan.model.ProfileIndexStatus;

import java.util.List;
import java.util.Optional;

/**
 * 画像索引状态仓储接口 -- ProjectProfile 维度级向量索引的持久化访问契约。
 *
 * @author zhangkai
 * @since 2026-04-08
 */
public interface ProfileIndexStatusRepository {

    /**
     * 查询指定项目、指定维度的索引状态。
     */
    Optional<ProfileIndexStatus> findByProjectPathAndDimension(String projectPath, ProfileDimension dimension);

    /**
     * 查询指定项目的所有维度索引状态。
     */
    List<ProfileIndexStatus> findByProjectPath(String projectPath);

    /**
     * 保存索引状态（新增或覆盖更新）。
     */
    ProfileIndexStatus save(ProfileIndexStatus status);

    /**
     * 批量保存索引状态。
     */
    List<ProfileIndexStatus> saveAll(List<ProfileIndexStatus> statuses);
}
