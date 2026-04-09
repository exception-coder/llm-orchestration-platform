package com.exceptioncoder.llm.api.controller.management;

import com.exceptioncoder.llm.application.usecase.PromptTemplateManagementUseCase;
import com.exceptioncoder.llm.application.usecase.PromptTemplateManagementUseCase.TemplateCreateRequest;
import com.exceptioncoder.llm.application.usecase.PromptTemplateManagementUseCase.TemplateInfo;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Prompt 模板管理 API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/prompt-templates")
public class PromptTemplateController {

    private final PromptTemplateManagementUseCase templateManagementUseCase;

    public PromptTemplateController(PromptTemplateManagementUseCase templateManagementUseCase) {
        this.templateManagementUseCase = templateManagementUseCase;
    }

    /**
     * 获取所有模板
     */
    @GetMapping
    public List<TemplateInfo> getAllTemplates() {
        return templateManagementUseCase.getAllTemplates();
    }

    /**
     * 根据名称获取模板
     */
    @GetMapping("/{name}")
    public ResponseEntity<TemplateInfo> getTemplate(@PathVariable String name) {
        return templateManagementUseCase.getTemplateByName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 根据分类获取模板
     */
    @GetMapping("/category/{category}")
    public List<TemplateInfo> getTemplatesByCategory(@PathVariable String category) {
        return templateManagementUseCase.getTemplatesByCategory(category);
    }

    /**
     * 创建或更新模板
     */
    @PostMapping
    public ResponseEntity<TemplateInfo> saveTemplate(@RequestBody TemplateRequest request) {
        log.info("保存模板: {}", request.getTemplateName());

        TemplateCreateRequest createRequest = TemplateCreateRequest.builder()
                .templateName(request.getTemplateName())
                .templateContent(request.getTemplateContent())
                .category(request.getCategory())
                .description(request.getDescription())
                .variableExamples(request.getVariableExamples())
                .build();

        TemplateInfo saved = templateManagementUseCase.saveTemplate(createRequest);
        return ResponseEntity.ok(saved);
    }

    /**
     * 删除模板
     */
    @DeleteMapping("/{name}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable String name) {
        log.info("删除模板: {}", name);
        templateManagementUseCase.deleteTemplate(name);
        return ResponseEntity.ok().build();
    }

    @Data
    public static class TemplateRequest {
        private String templateName;
        private String templateContent;
        private String category;
        private String description;
        private String variableExamples;  // JSON 格式的变量示例
    }
}
