package com.exceptioncoder.llm.infrastructure.devplan.tool;

import com.exceptioncoder.llm.domain.devplan.model.AgentRole;
import com.exceptioncoder.llm.infrastructure.agent.tool.Tool;
import com.exceptioncoder.llm.infrastructure.agent.tool.ToolParam;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 项目画像语义检索工具 -- 在 Qdrant project_profile collection 中跨项目检索画像维度。
 *
 * <p>支持三种过滤模式：
 * <ul>
 *   <li>仅 query：纯语义检索，返回所有项目中最相关的维度</li>
 *   <li>query + projectName：指定项目内语义检索</li>
 *   <li>query + dimension：跨项目但限定维度（如"哪些项目用了 Kafka"只搜 TECH_STACK 和 CONFIG）</li>
 * </ul>
 *
 * <p><b>归属智能体：</b>开发计划智能体（devplan）
 * <br><b>归属 Agent：</b>需求分析专家（devplan-requirement-analyzer）、方案架构师（devplan-solution-architect）
 * <br><b>调用阶段：</b>第二阶段 — 需求分析 / 第三阶段 — 方案设计
 * <br><b>业务场景：</b>在需求分析和方案设计时，通过语义检索获取项目的架构规范、技术栈、
 * API 约定等画像维度信息，确保方案设计符合项目已有架构风格。支持跨项目检索，
 * 当需要参考其他项目的实现方式时（如"其他项目如何接入 Qdrant"），可指定维度过滤。
 *
 * @author zhangkai
 * @since 2026-04-08
 */
@Slf4j
@Component
public class ProfileSearchTool {

    private final VectorStore profileVectorStore;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProfileSearchTool(@Qualifier("profileVectorStore") VectorStore profileVectorStore) {
        this.profileVectorStore = profileVectorStore;
    }

    @Tool(name = "devplan_profile_search",
            description = "跨项目语义检索已索引的项目画像，支持按项目名和维度过滤",
            tags = {"devplan", "search"}, roles = {AgentRole.REQUIREMENT_ANALYZER, AgentRole.SOLUTION_ARCHITECT})
    public String search(
            @ToolParam(value = "query", description = "语义检索查询文本") String query,
            @ToolParam(value = "projectName", description = "按项目名过滤（精确匹配），不填则跨所有项目", required = false, defaultValue = "\"\"") String projectName,
            @ToolParam(value = "dimensions", description = "按维度过滤，逗号分隔如TECH_STACK,CONFIG，不填则搜全部维度", required = false, defaultValue = "\"\"") String dimensions,
            @ToolParam(value = "topK", description = "返回结果数量", required = false, defaultValue = "\"5\"") String topK
    ) {
        try {
            int k = parseIntSafe(topK, 5);

            SearchRequest.Builder builder = SearchRequest.builder()
                    .query(query)
                    .topK(k);

            // 构建过滤表达式
            FilterExpressionBuilder fb = new FilterExpressionBuilder();
            FilterExpressionBuilder.Op filterOp = null;

            boolean hasProjectFilter = projectName != null && !projectName.isBlank();
            boolean hasDimensionFilter = dimensions != null && !dimensions.isBlank();

            if (hasProjectFilter && hasDimensionFilter) {
                List<String> dimList = parseDimensions(dimensions);
                filterOp = fb.and(
                        fb.eq("projectName", projectName),
                        fb.in("dimension", dimList.toArray(new String[0]))
                );
            } else if (hasProjectFilter) {
                filterOp = fb.eq("projectName", projectName);
            } else if (hasDimensionFilter) {
                List<String> dimList = parseDimensions(dimensions);
                filterOp = fb.in("dimension", dimList.toArray(new String[0]));
            }

            if (filterOp != null) {
                builder.filterExpression(filterOp.build());
            }

            List<Document> results = profileVectorStore.similaritySearch(builder.build());

            List<Map<String, Object>> output = new ArrayList<>();
            for (Document doc : results) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("projectName", doc.getMetadata().getOrDefault("projectName", ""));
                item.put("projectPath", doc.getMetadata().getOrDefault("projectPath", ""));
                item.put("dimension", doc.getMetadata().getOrDefault("dimension", ""));
                item.put("dimensionLabel", doc.getMetadata().getOrDefault("dimensionLabel", ""));
                item.put("content", truncate(doc.getText(), 800));
                output.add(item);
            }

            return objectMapper.writeValueAsString(output);
        } catch (Exception e) {
            log.warn("ProfileSearchTool 执行失败（降级为空结果）: {}", e.getMessage());
            return "[]";
        }
    }

    private List<String> parseDimensions(String dimensions) {
        List<String> result = new ArrayList<>();
        for (String dim : dimensions.split(",")) {
            String trimmed = dim.trim().toUpperCase();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private int parseIntSafe(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
