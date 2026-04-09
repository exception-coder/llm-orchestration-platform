package com.exceptioncoder.llm.api.controller.management;

import com.exceptioncoder.llm.application.service.DocViewerService;
import com.exceptioncoder.llm.application.usecase.DocRefreshUseCase;
import com.exceptioncoder.llm.domain.model.DocContent;
import com.exceptioncoder.llm.domain.model.DocSearchResult;
import com.exceptioncoder.llm.domain.model.DocStructureVersion;
import com.exceptioncoder.llm.domain.model.DocTreeNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 文档浏览器 REST 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/docs")
public class DocViewerController {

    private final DocViewerService docViewerService;
    private final DocRefreshUseCase docRefreshUseCase;

    public DocViewerController(DocViewerService docViewerService, DocRefreshUseCase docRefreshUseCase) {
        this.docViewerService = docViewerService;
        this.docRefreshUseCase = docRefreshUseCase;
    }

    /**
     * 获取文档目录树
     */
    @GetMapping("/tree")
    public ResponseEntity<Map<String, Object>> getDocTree() {
        List<DocTreeNode> tree = docViewerService.getDocTree();
        return ResponseEntity.ok(Map.of("items", tree));
    }

    /**
     * 读取文档内容
     */
    @GetMapping("/content")
    public ResponseEntity<DocContent> getContent(@RequestParam String path) {
        try {
            DocContent content = docViewerService.getContent(path);
            return ResponseEntity.ok(content);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * AI 语义检索文档
     */
    @GetMapping("/search")
    public ResponseEntity<DocSearchResult> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "5") int topK
    ) {
        try {
            DocSearchResult result = docViewerService.searchDocs(keyword, topK);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(503)
                    .body(new DocSearchResult(List.of(), 0));
        }
    }

    /**
     * 手动触发文档索引构建
     */
    @PostMapping("/index")
    public ResponseEntity<Map<String, Object>> indexDocs() {
        int count = docViewerService.indexDocs();
        return ResponseEntity.ok(Map.of("message", "索引构建完成", "count", count));
    }

    /**
     * 手动触发目录结构刷新（LLM 解析 README.md）
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(
            @RequestParam(defaultValue = "false") boolean force
    ) {
        try {
            String result = docRefreshUseCase.refresh(force);
            return ResponseEntity.ok(Map.of("message", result));
        } catch (Exception e) {
            log.error("目录刷新失败", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 查询所有目录结构版本历史
     */
    @GetMapping("/versions")
    public ResponseEntity<List<DocStructureVersion>> getVersions() {
//        return ResponseEntity.ok(docViewerService.getVersion());
        return null;
    }

    /**
     * 查询指定版本的目录结构
     */
    @GetMapping("/versions/{version}")
    public ResponseEntity<DocStructureVersion> getVersion(@PathVariable int version) {
//        return docViewerService.getVersion(version)
//                .map(ResponseEntity::ok)
//                .orElse(ResponseEntity.notFound().build());
        return null;
    }
}
