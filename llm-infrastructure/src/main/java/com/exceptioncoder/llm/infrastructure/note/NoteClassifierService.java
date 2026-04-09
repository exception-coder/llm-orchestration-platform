package com.exceptioncoder.llm.infrastructure.note;

import com.exceptioncoder.llm.domain.model.LLMRequest;
import com.exceptioncoder.llm.domain.model.LLMResponse;
import com.exceptioncoder.llm.domain.model.Message;
import com.exceptioncoder.llm.domain.model.NoteCategory;
import com.exceptioncoder.llm.domain.model.NoteClassificationResult;
import com.exceptioncoder.llm.domain.repository.NoteCategoryRepository;
import com.exceptioncoder.llm.domain.service.NoteClassifier;
import com.exceptioncoder.llm.infrastructure.provider.LLMProviderRouter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 记录分类服务
 * 调用 LLM 分析用户输入，返回结构化的分类结果
 */
@Slf4j
@Service
public class NoteClassifierService implements NoteClassifier {

    private final LLMProviderRouter providerRouter;
    private final NoteCategoryRepository categoryRepository;
    private final ObjectMapper objectMapper;

    public NoteClassifierService(
            LLMProviderRouter providerRouter,
            NoteCategoryRepository categoryRepository,
            ObjectMapper objectMapper) {
        this.providerRouter = providerRouter;
        this.categoryRepository = categoryRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 调用 LLM 对用户输入进行分类和结构化
     */
    public NoteClassificationResult classify(String rawText) {
        List<NoteCategory> existingCategories = categoryRepository.findAllOrderBySortOrder();
        String categoryList = existingCategories.isEmpty()
                ? "（暂无已有类目，将自动创建新类目）"
                : existingCategories.stream()
                        .map(c -> String.format("【%s】%s", c.getName(), c.getDescription() != null ? c.getDescription() : ""))
                        .collect(Collectors.joining("\n"));

        String promptText = buildPrompt(rawText, categoryList);

        LLMRequest request = LLMRequest.builder()
                .prompt(promptText)
                .messages(List.of(Message.builder().role("user").content(promptText).build()))
                .temperature(0.3)
                .maxTokens(2000)
                .build();

        log.info("开始对输入内容进行分类，长度: {} 字", rawText.length());
        LLMResponse response = providerRouter.getDefault().chat(request);

        return parseClassificationResult(response.getContent());
    }

    private String buildPrompt(String rawText, String categoryList) {
        return String.format("""
                你是一个个人记录智能助手。分析以下用户输入，返回结构化的 JSON。

                ## 用户输入
                %s

                ## 已有类目（如果合适就归入已有类目，否则新建）
                %s

                ## 返回格式（严格 JSON，不要 markdown 代码块）
                {
                  "category": "类目名称（简短中文，15字以内，如已有类目匹配则用已有的）",
                  "categoryDescription": "新类目需要描述（一句话），已有类目可省略",
                  "categoryIcon": "一个合适的 emoji",
                  "title": "简洁标题（15字以内）",
                  "summary": "一句话摘要（30字以内）",
                  "structuredContent": "整理后的内容（Markdown 格式，保留关键信息，如账号密码等敏感信息要原样保留）",
                  "tags": ["标签1", "标签2", "标签3"],
                  "isSensitive": true/false  // 是否包含账号、密码、密钥、证件号等敏感信息
                }
                """, rawText, categoryList);
    }

    private NoteClassificationResult parseClassificationResult(String jsonContent) {
        // 去除可能的 markdown 代码块标记
        String cleaned = jsonContent.trim();
        if (cleaned.startsWith("```")) {
            int firstNewline = cleaned.indexOf('\n');
            int lastBackticks = cleaned.lastIndexOf("```");
            cleaned = cleaned.substring(firstNewline + 1, lastBackticks).trim();
        }

        try {
            return objectMapper.readValue(cleaned, NoteClassificationResult.class);
        } catch (JsonProcessingException e) {
            log.error("解析分类结果失败，原始内容: {}", jsonContent, e);
            // 降级处理：返回默认结果
            return NoteClassificationResult.builder()
                    .category("未分类")
                    .categoryDescription("无法自动分类的记录")
                    .categoryIcon("📝")
                    .title(cleaned.substring(0, Math.min(15, cleaned.length())))
                    .summary("未能自动生成摘要")
                    .structuredContent(cleaned)
                    .tags(List.of())
                    .isSensitive(false)
                    .build();
        }
    }
}
