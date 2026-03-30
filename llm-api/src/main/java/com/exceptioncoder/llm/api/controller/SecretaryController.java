package com.exceptioncoder.llm.api.controller;

import com.exceptioncoder.llm.application.service.SecretaryService;
import com.exceptioncoder.llm.domain.model.AgentExecutionResult;
import com.exceptioncoder.llm.domain.model.SecretaryMemory;
import com.exceptioncoder.llm.domain.model.ToolDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * 个人秘书 REST 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/secretary")
public class SecretaryController {

    private final SecretaryService secretaryService;

    public SecretaryController(SecretaryService secretaryService) {
        this.secretaryService = secretaryService;
    }

    /**
     * 秘书对话（非流式）
     */
    @PostMapping("/chat")
    public ResponseEntity<AgentExecutionResult> chat(@RequestBody ChatRequest request) {
        AgentExecutionResult result = secretaryService.chat(request.message(), request.sessionId());
        return ResponseEntity.ok(result);
    }

    /**
     * 秘书对话（SSE 流式）
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody ChatRequest request) {
        return Flux.defer(() -> {
            AgentExecutionResult result = secretaryService.chat(request.message(), request.sessionId());
            String output = result.finalOutput() != null ? result.finalOutput() : "";
            // 按字符分块模拟流式输出
            List<String> chunks = splitIntoChunks(output, 20);
            return Flux.fromIterable(chunks)
                    .map(chunk -> "data: {\"content\":\"" + escapeJson(chunk) + "\"}\n\n")
                    .concatWith(Flux.just("data: [DONE]\n\n"));
        });
    }

    /**
     * 获取长期记忆
     */
    @GetMapping("/memory")
    public ResponseEntity<List<SecretaryMemory>> getMemory() {
        return ResponseEntity.ok(secretaryService.getMemory());
    }

    /**
     * 保存记忆条目
     */
    @PostMapping("/memory")
    public ResponseEntity<SecretaryMemory> saveMemory(@RequestBody MemoryRequest request) {
        SecretaryMemory saved = secretaryService.saveMemory(request.type(), request.content());
        return ResponseEntity.ok(saved);
    }

    /**
     * 清除长期记忆
     */
    @DeleteMapping("/memory")
    public ResponseEntity<Map<String, String>> clearMemory() {
        secretaryService.clearMemory();
        return ResponseEntity.ok(Map.of("message", "记忆已清除"));
    }

    /**
     * 获取工具列表
     */
    @GetMapping("/tools")
    public ResponseEntity<List<ToolDefinition>> getTools() {
        return ResponseEntity.ok(secretaryService.getTools());
    }

    // --- 内部 DTO ---

    public record ChatRequest(String message, String sessionId) {}

    public record MemoryRequest(SecretaryMemory.MemoryType type, String content) {}

    // --- 工具方法 ---

    private List<String> splitIntoChunks(String text, int size) {
        List<String> chunks = new java.util.ArrayList<>();
        for (int i = 0; i < text.length(); i += size) {
            chunks.add(text.substring(i, Math.min(i + size, text.length())));
        }
        if (chunks.isEmpty()) chunks.add("");
        return chunks;
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
