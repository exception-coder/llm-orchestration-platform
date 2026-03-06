package com.exceptioncoder.llm.api.controller;

import com.exceptioncoder.llm.application.usecase.LLMModelConfigUseCase;
import com.exceptioncoder.llm.application.usecase.LLMModelConfigUseCase.ModelCreateRequest;
import com.exceptioncoder.llm.application.usecase.LLMModelConfigUseCase.ModelInfo;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * LLM 模型配置管理 API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/model-config")
public class LLMModelConfigController {
    
    private final LLMModelConfigUseCase modelConfigUseCase;
    
    public LLMModelConfigController(LLMModelConfigUseCase modelConfigUseCase) {
        this.modelConfigUseCase = modelConfigUseCase;
    }
    
    /**
     * 获取所有启用的模型
     */
    @GetMapping
    public List<ModelInfo> getAllModels() {
        return modelConfigUseCase.getAllEnabledModels();
    }
    
    /**
     * 根据提供商获取模型
     */
    @GetMapping("/provider/{provider}")
    public List<ModelInfo> getModelsByProvider(@PathVariable String provider) {
        return modelConfigUseCase.getModelsByProvider(provider);
    }
    
    /**
     * 根据模型代码获取模型
     */
    @GetMapping("/{modelCode}")
    public ResponseEntity<ModelInfo> getModel(@PathVariable String modelCode) {
        return modelConfigUseCase.getModelByCode(modelCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 创建或更新模型配置
     */
    @PostMapping
    public ResponseEntity<ModelInfo> saveModel(@RequestBody ModelRequest request) {
        log.info("保存模型配置: {}", request.getModelCode());
        
        ModelCreateRequest createRequest = ModelCreateRequest.builder()
                .modelCode(request.getModelCode())
                .provider(request.getProvider())
                .modelName(request.getModelName())
                .description(request.getDescription())
                .enabled(request.getEnabled())
                .sortOrder(request.getSortOrder())
                .build();
        
        ModelInfo saved = modelConfigUseCase.saveModel(createRequest);
        return ResponseEntity.ok(saved);
    }
    
    /**
     * 删除模型配置
     */
    @DeleteMapping("/{modelCode}")
    public ResponseEntity<Void> deleteModel(@PathVariable String modelCode) {
        log.info("删除模型配置: {}", modelCode);
        modelConfigUseCase.deleteModel(modelCode);
        return ResponseEntity.ok().build();
    }
    
    @Data
    public static class ModelRequest {
        private String modelCode;
        private String provider;
        private String modelName;
        private String description;
        private Boolean enabled;
        private Integer sortOrder;
    }
}

