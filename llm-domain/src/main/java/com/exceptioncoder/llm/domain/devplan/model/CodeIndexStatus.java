package com.exceptioncoder.llm.domain.devplan.model;

import java.time.LocalDateTime;

/**
 * 代码索引状态 -- 记录目标项目在向量数据库中的索引进度与健康状况。
 *
 * <p>属于 Domain 层 devplan 模块。CODE_AWARENESS Agent 在执行代码感知前会检查
 * 索引状态：若已就绪（READY）且文件哈希未变，则跳过重复索引以节省资源；
 * 否则触发增量或全量索引流程。通过 {@link #isReady()} 方法封装就绪判断逻辑。</p>
 *
 * @author zhangkai
 * @since 2026-04-06
 */
public record CodeIndexStatus(
        String projectPath,             // 目标项目的本地绝对路径
        String collectionName,          // 向量数据库中的 Collection 名称
        int docCount,                   // 已索引的文档（代码文件）数量
        String status,                  // 当前索引状态，取值参考 {@link IndexState} 枚举
        LocalDateTime lastIndexedAt,    // 最近一次索引完成时间
        String fileHash                 // 项目文件的哈希摘要，用于判断是否需要重新索引
) {
    /**
     * 索引状态枚举，定义索引生命周期中所有合法状态。
     */
    public enum IndexState {
        IDLE,       // 空闲，尚未开始索引
        INDEXING,   // 正在索引中
        READY,      // 索引就绪，可供查询
        FAILED      // 索引失败
    }

    /**
     * 判断索引是否已就绪、可供下游查询使用。
     *
     * @return 若状态为 READY 则返回 true
     */
    public boolean isReady() {
        return IndexState.READY.name().equals(status);
    }
}
