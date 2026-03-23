package com.exceptioncoder.llm.api.controller;

import com.exceptioncoder.llm.domain.model.ToolDefinition;
import com.exceptioncoder.llm.domain.registry.ToolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工具 REST 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tools")
public class ToolController {

    private final ToolRegistry toolRegistry;

    public ToolController(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    /** 获取所有已注册工具 */
    @GetMapping
    public ResponseEntity<List<ToolDefinition>> listTools() {
        return ResponseEntity.ok(toolRegistry.getAllTools());
    }

    /** 获取单个工具定义 */
    @GetMapping("/{toolId}")
    public ResponseEntity<ToolDefinition> getTool(@PathVariable String toolId) {
        return toolRegistry.getDefinition(toolId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** 注销工具 */
    @DeleteMapping("/{toolId}")
    public ResponseEntity<Void> unregisterTool(@PathVariable String toolId) {
        toolRegistry.unregister(toolId);
        return ResponseEntity.noContent().build();
    }
}
