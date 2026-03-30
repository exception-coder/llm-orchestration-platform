package com.exceptioncoder.llm.domain.repository;

import com.exceptioncoder.llm.domain.model.DocStructureVersion;

import java.util.List;
import java.util.Optional;

/**
 * 文档目录结构版本仓储接口
 */
public interface DocStructureVersionRepository {

    /** 查询当前生效版本（is_active = true） */
    Optional<DocStructureVersion> findActive();

    /** 查询指定版本号 */
    Optional<DocStructureVersion> findByVersion(int version);

    /** 查询所有版本（按 version 降序） */
    List<DocStructureVersion> findAll();

    /** 保存新版本 */
    DocStructureVersion save(DocStructureVersion version);

    /** 将所有版本置为非活跃 */
    void deactivateAll();

    /** 获取当前最大版本号，无数据返回 0 */
    int getMaxVersion();
}
