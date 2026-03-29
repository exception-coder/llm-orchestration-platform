package com.exceptioncoder.llm.infrastructure.agent;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.exceptioncoder.llm.infrastructure.agent.tool.ToolExecutor;
import com.exceptioncoder.llm.infrastructure.agent.tool.ToolRegistryImpl;
import com.exceptioncoder.llm.infrastructure.config.LLMConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 文档目录结构 ReAct Agent
 * 驱动 LLM 自主调用 getLastDocStructure / saveDocStructure 完成目录更新
 */
@Slf4j
@Component
public class DocStructureAgent {

    private static final String SYSTEM_PROMPT = """
            你是文档目录结构分析专家。你有两个工具：
            1. getLastDocStructure() — 查询上一次解析的目录结构和版本号
            2. saveDocStructure(structure, diffSummary, readmeHash) — 保存新目录结构

            任务：根据提供的 README.md 内容，结合上一版本结果，生成最新目录结构并保存。

            要求：
            - 首先调用 getLastDocStructure 获取上一版本
            - 相同 path 的节点保留上一版本的字段值，保持一致性
            - 新增节点补充完整字段（path/name/type/category/description）
            - 生成简洁的 diffSummary 描述本次变更（如：「新增 2 个文档，删除 1 个文档」）
            - 结构确定后调用 saveDocStructure 保存
            - 无论如何都必须调用 saveDocStructure，即使无变化（diffSummary 填「无变化」）
            - structure 字段必须是合法的 JSON 数组字符串
            - 每个节点包含字段：name（语义名称）、path（文件相对路径）、type（FILE/DIRECTORY）、category（分类）、description（一句话描述）、children（子节点数组，FILE 节点为空数组）

            当你需要使用工具时，按照以下格式回复：
            ```json
            { "tool": "工具名称", "input": { "参数名": "参数值" } }
            ```
            不要在思考过程中使用工具，只有在确定需要调用时才使用。
            """;

    private static final List<String> TOOL_IDS = List.of("getLastDocStructure", "saveDocStructure");
    private static final int MAX_ITERATIONS = 8;

    private final ToolRegistryImpl toolRegistry;
    private final ToolExecutor toolExecutor;
    private final LLMConfiguration llmConfig;
    private final ObjectMapper objectMapper;

    public DocStructureAgent(ToolRegistryImpl toolRegistry,
                             ToolExecutor toolExecutor,
                             LLMConfiguration llmConfig) {
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
        this.llmConfig = llmConfig;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 执行目录结构更新
     *
     * @param readmeContent docs/README.md 的完整内容
     * @param readmeHash    README.md 内容的 SHA-256 hash
     * @return Agent 最终输出文本
     */
    public String execute(String readmeContent, String readmeHash) {
        DashScopeChatModel chatModel = buildChatModel();

        List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(SYSTEM_PROMPT));
        messages.add(new UserMessage(
                "以下是最新的 docs/README.md 内容：\n" + readmeContent
                + "\n\nREADME hash（保存时作为 readmeHash 参数传入）: " + readmeHash
                + "\n\n请分析目录结构并保存。"
        ));

        String finalOutput = null;

        for (int i = 1; i <= MAX_ITERATIONS; i++) {
            log.debug("DocStructureAgent 第 {} 轮迭代", i);
            try {
                Prompt prompt = new Prompt(messages);
                ChatResponse response = chatModel.call(prompt);

                if (response == null || response.getResult() == null) {
                    log.warn("LLM 返回空响应，终止迭代");
                    break;
                }

                String content = response.getResults().get(0).getOutput().getText();
                log.debug("LLM 输出: {}", content);

                ToolCallResult toolResult = parseAndExecuteTool(content);

                if (toolResult != null) {
                    messages.add(new AssistantMessage(content));
                    messages.add(new UserMessage("观察结果: " + toolResult.observation));
                    log.info("工具调用完成: tool={}, success={}", toolResult.toolName, toolResult.success);
                } else {
                    finalOutput = content;
                    log.info("DocStructureAgent 完成，共迭代 {} 次", i);
                    break;
                }
            } catch (Exception e) {
                log.error("DocStructureAgent 第 {} 轮迭代异常", i, e);
                break;
            }
        }

        if (finalOutput == null) {
            finalOutput = "Agent 执行完成（达到最大迭代次数）";
        }
        return finalOutput;
    }

    private ToolCallResult parseAndExecuteTool(String content) {
        int jsonStart = content.indexOf("{");
        int jsonEnd = content.lastIndexOf("}");
        if (jsonStart == -1 || jsonEnd == -1 || jsonEnd <= jsonStart) {
            return null;
        }
        try {
            String jsonStr = content.substring(jsonStart, jsonEnd + 1);
            Map<String, Object> callMap = objectMapper.readValue(jsonStr, Map.class);
            String toolName = (String) callMap.get("tool");
            if (toolName == null) return null;

            @SuppressWarnings("unchecked")
            Map<String, Object> input = (Map<String, Object>) callMap.get("input");
            if (input == null) input = Map.of();

            String toolId = findToolId(toolName);
            if (toolId == null) {
                return new ToolCallResult(toolName, "工具不存在: " + toolName, false);
            }

            String observation = toolExecutor.execute(toolId, input);
            return new ToolCallResult(toolName, observation, true);
        } catch (Exception e) {
            log.debug("解析工具调用失败", e);
            return null;
        }
    }

    private String findToolId(String toolName) {
        for (String id : TOOL_IDS) {
            var defOpt = toolRegistry.getDefinition(id);
            if (defOpt.isPresent() && defOpt.get().name().equals(toolName)) {
                return id;
            }
        }
        // 直接用 toolName 作为 id 兜底
        if (toolRegistry.getDefinition(toolName).isPresent()) {
            return toolName;
        }
        return null;
    }

    private DashScopeChatModel buildChatModel() {
        LLMConfiguration.AlibabaConfig config = llmConfig.getAlibaba();
        DashScopeChatOptions options = DashScopeChatOptions.builder()
                .withModel(config.getModel())
                .withTemperature(config.getTemperature())
                .withMaxToken(config.getMaxTokens())
                .build();
        DashScopeApi api = new DashScopeApi.Builder()
                .apiKey(config.getApiKey())
                .baseUrl(config.getBaseUrl())
                .build();
        return DashScopeChatModel.builder()
                .dashScopeApi(api)
                .defaultOptions(options)
                .build();
    }

    private record ToolCallResult(String toolName, String observation, boolean success) {}
}
