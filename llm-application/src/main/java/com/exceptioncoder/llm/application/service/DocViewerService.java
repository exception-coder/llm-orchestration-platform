package com.exceptioncoder.llm.application.service;

import com.exceptioncoder.llm.domain.model.DocContent;
import com.exceptioncoder.llm.domain.model.DocSearchResult;
import com.exceptioncoder.llm.domain.model.DocStructureVersion;
import com.exceptioncoder.llm.domain.model.DocTreeNode;
import com.exceptioncoder.llm.domain.repository.DocSearchRepository;
import com.exceptioncoder.llm.domain.repository.DocStructureVersionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档浏览服务
 */
@Slf4j
@Service
public class DocViewerService {

    private static final String DEFAULT_DOCS_BASE = "classpath:docs/";

    private final DocStructureVersionRepository versionRepository;
    private final DocSearchRepository docSearchRepository;
    private final ResourceLoader resourceLoader;
    private final String docsBasePath;
    private final boolean searchEnabled;
    private final int searchTopK;
    private final ObjectMapper objectMapper;

    public DocViewerService(
            DocViewerProperties properties,
            DocStructureVersionRepository versionRepository,
            DocSearchRepository docSearchRepository,
            ResourceLoader resourceLoader
    ) {
        this.versionRepository = versionRepository;
        this.docSearchRepository = docSearchRepository;
        this.resourceLoader = resourceLoader;
        this.docsBasePath = properties.getDocsPath() != null ? properties.getDocsPath() : DEFAULT_DOCS_BASE;
        this.searchEnabled = properties.isSearchEnabled();
        this.searchTopK = properties.getSearchTopK();
        this.objectMapper = new ObjectMapper();
    }

    @org.springframework.stereotype.Component
    public static class DocViewerProperties {
        @Value("${doc-viewer.docs-path:classpath:docs/}")
        private String docsPath;

        @Value("${doc-viewer.search-enabled:true}")
        private boolean searchEnabled;

        @Value("${doc-viewer.search-top-k:5}")
        private int searchTopK;

        public String getDocsPath() { return docsPath; }
        public boolean isSearchEnabled() { return searchEnabled; }
        public int getSearchTopK() { return searchTopK; }
    }

    /**
     * 从 DB 读取当前生效版本的目录树
     */
    public List<DocTreeNode> getDocTree() {
        return versionRepository.findActive()
                .map(v -> {
                    try {
                        return objectMapper.readValue(v.getStructure(), new TypeReference<List<DocTreeNode>>() {});
                    } catch (Exception e) {
                        log.error("解析目录结构 JSON 失败: version={}", v.getVersion(), e);
                        throw new IllegalStateException("目录结构数据损坏，请重新刷新");
                    }
                })
                .orElseThrow(() -> new IllegalStateException("目录树未初始化，请先调用 /docs/refresh"));
    }

    /**
     * 读取文档内容（从文件系统，不经过 DB）
     */
    public DocContent getContent(String path) {
        validatePath(path);
        String resourcePath = "classpath:" + path;
        Resource resource = resourceLoader.getResource(resourcePath);
        if (!resource.exists()) {
            throw new IllegalArgumentException("文档不存在: " + path);
        }
        try (InputStream is = resource.getInputStream()) {
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return new DocContent(path, extractFileName(path), content, content.length());
        } catch (IOException e) {
            throw new RuntimeException("读取文档失败: " + e.getMessage(), e);
        }
    }

    /**
     * AI 语义检索
     */
    public DocSearchResult searchDocs(String keyword, int topK) {
        if (!searchEnabled) {
            throw new IllegalStateException("检索功能未启用");
        }
        if (!docSearchRepository.isHealthy()) {
            throw new IllegalStateException("检索服务不可用，请确保 Qdrant 已启动");
        }
        return docSearchRepository.search(keyword, topK > 0 ? topK : searchTopK);
    }

    /**
     * 构建文档向量索引
     */
    public int indexDocs() {
        List<String> paths = new ArrayList<>();
        List<String> contents = new ArrayList<>();
        String base = stripResourcePrefix(docsBasePath);
        for (String category : new String[]{"design", "guides", "dev", "sql"}) {
            collectAllMarkdownFiles(base + "/" + category, paths, contents);
        }
        if (!paths.isEmpty()) {
            docSearchRepository.indexDocs(paths, contents);
        }
        return paths.size();
    }

    /**
     * 查询所有历史版本（按 version 降序）
     */
    public List<DocStructureVersion> listVersions() {
        return versionRepository.findAll();
    }

    /**
     * 查询指定版本
     */
    public DocStructureVersion getVersion(int version) {
        return versionRepository.findByVersion(version)
                .orElseThrow(() -> new IllegalArgumentException("版本不存在: " + version));
    }

    private void collectAllMarkdownFiles(String basePath, List<String> paths, List<String> contents) {
        try {
            Resource resource = resourceLoader.getResource("classpath:" + basePath);
            if (!resource.exists()) return;
            java.io.File[] entries = resource.getFile().listFiles();
            if (entries == null) return;
            for (java.io.File entry : entries) {
                String fullPath = basePath + "/" + entry.getName();
                if (entry.isDirectory()) {
                    collectAllMarkdownFiles(fullPath, paths, contents);
                } else if (entry.getName().endsWith(".md")) {
                    try (InputStream is = resourceLoader.getResource("classpath:" + fullPath).getInputStream()) {
                        paths.add(fullPath);
                        contents.add(new String(is.readAllBytes(), StandardCharsets.UTF_8));
                    }
                }
            }
        } catch (IOException e) {
            log.warn("遍历目录失败: basePath={}, error={}", basePath, e.getMessage());
        }
    }

    private void validatePath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("path 参数不能为空");
        }
        if (!path.startsWith("docs/")) {
            throw new IllegalArgumentException("非法路径：仅支持 docs/ 目录下的文件");
        }
        if (path.contains("..")) {
            throw new IllegalArgumentException("非法路径：禁止路径穿越");
        }
        if (!path.endsWith(".md")) {
            throw new IllegalArgumentException("仅支持 .md 文件");
        }
    }

    private String stripResourcePrefix(String path) {
        return path.startsWith("classpath:") ? path.substring(10) : path;
    }

    private String extractFileName(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }
}
