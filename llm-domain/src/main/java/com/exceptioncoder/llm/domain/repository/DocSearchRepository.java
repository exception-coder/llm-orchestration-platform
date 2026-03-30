package com.exceptioncoder.llm.domain.repository;

import com.exceptioncoder.llm.domain.model.DocSearchResult;

import java.util.List;

/**
 * 文档向量检索仓储接口
 */
public interface DocSearchRepository {

    /**
     * 索引一批文档
     * @param paths 文件路径列表（相对于 classpath docs/）
     * @param contents 对应内容列表
     */
    void indexDocs(List<String> paths, List<String> contents);

    /**
     * 语义检索文档
     * @param query 查询文本
     * @param topK 返回数量
     * @return 检索结果
     */
    DocSearchResult search(String query, int topK);

    /**
     * 检查服务健康状态
     */
    boolean isHealthy();
}
