package com.exceptioncoder.llm.infrastructure.agent.executor;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.exceptioncoder.llm.domain.model.*;
import com.exceptioncoder.llm.domain.executor.AgentExecutor;
import com.exceptioncoder.llm.domain.repository.AgentDefinitionRepository;
import com.exceptioncoder.llm.infrastructure.agent.tool.ToolExecutor;
import com.exceptioncoder.llm.infrastructure.agent.tool.ToolRegistryImpl;
import com.exceptioncoder.llm.infrastructure.config.LLMConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Agent 执行器实现
 * 基于 ReAct（Reasoning + Acting）循环：
 * 1. 用户输入 → 构建消息
 * 2. LLM 生成 Tool Call / Answer
 * 3. 执行工具 → 追加 Observation
 * 4. 循环直到结束
 */
@Slf4j
public class AlibabaAgentExecutor implements AgentExecutor {

    private final AgentDefinitionRepository agentRepository;
    private final ToolRegistryImpl toolRegistry;
    private final ToolExecutor toolExecutor;
    private final LLMConfiguration llmConfig;
    private final ObjectMapper objectMapper;

    public AlibabaAgentExecutor(
            AgentDefinitionRepository agentRepository,
            ToolRegistryImpl toolRegistry,
            ToolExecutor toolExecutor,
            LLMConfiguration llmConfig
    ) {
        this.agentRepository = agentRepository;
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
        this.llmConfig = llmConfig;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public AgentExecutionResult execute(AgentExecutionRequest request) {
        long startMs = System.currentTimeMillis();

        var agentOpt = agentRepository.findById(request.agentId());
        if (agentOpt.isEmpty()) {
            return AgentExecutionResult.builder()
                    .executionId(request.executionId())
                    .agentId(request.agentId())
                    .status(AgentExecutionResult.Status.FAILED)
                    .errorMessage("Agent 不存在: " + request.agentId())
                    .elapsedMs(System.currentTimeMillis() - startMs)
                    .build();
        }

        AgentDefinition agent = agentOpt.get();
        List<ToolCall> toolCalls = new ArrayList<>();
        List<String> thoughtHistory = new ArrayList<>();

        ChatModel chatModel = buildChatModel(agent);
        List<org.springframework.ai.chat.messages.Message> messages = buildInitialMessages(agent, request);

        int maxIterations = agent.maxIterations();
        AtomicInteger iterations = new AtomicInteger(0);
        boolean finished = false;
        String finalOutput = null;

        while (iterations.incrementAndGet() <= maxIterations && !finished) {
            try {
                Prompt prompt = new Prompt(messages);
                ChatResponse response = chatModel.call(prompt);

                response.getResult();

                String content = response.getResults().get(0).getOutput().getText();

                // 尝试解析工具调用
                ToolCallResult toolCallResult = null;
                if (content != null) {
                    toolCallResult = parseAndExecuteTool(content, agent, toolExecutor, toolCalls);
                }

                if (toolCallResult != null) {
                    // 有工具调用，追加 Observation
                    thoughtHistory.add("思考: " + extractThought(content));
                    messages.add(new AssistantMessage(content));
                    messages.add(new UserMessage("观察结果: " + toolCallResult.observation));
                } else {
                    // 无工具调用，LLM 直接回答
                    if (content != null) {
                        messages.add(new AssistantMessage(content));
                    }
                    finalOutput = content;
                    finished = true;
                }
            } catch (Exception e) {
                log.error("Agent 执行异常: iteration={}", iterations.get(), e);
                if (iterations.get() >= maxIterations) {
                    return AgentExecutionResult.builder()
                            .executionId(request.executionId())
                            .agentId(request.agentId())
                            .finalOutput(finalOutput)
                            .toolCalls(toolCalls)
                            .thoughtHistory(thoughtHistory)
                            .iterations(iterations.get())
                            .status(AgentExecutionResult.Status.MAX_ITERATIONS_REACHED)
                            .errorMessage("达到最大迭代次数")
                            .elapsedMs(System.currentTimeMillis() - startMs)
                            .build();
                }
            }
        }

        if (finalOutput == null) {
            finalOutput = "Agent 执行未产生最终输出（可能达到最大迭代次数）";
        }

        return AgentExecutionResult.builder()
                .executionId(request.executionId())
                .agentId(request.agentId())
                .finalOutput(finalOutput)
                .toolCalls(toolCalls)
                .thoughtHistory(thoughtHistory)
                .iterations(iterations.get())
                .status(finished ? AgentExecutionResult.Status.SUCCESS : AgentExecutionResult.Status.MAX_ITERATIONS_REACHED)
                .elapsedMs(System.currentTimeMillis() - startMs)
                .build();
    }

    @Override
    public Flux<String> executeStream(AgentExecutionRequest request) {
        return Flux.defer(() -> {
            AgentExecutionResult result = execute(request);
            return Flux.just(result.finalOutput());
        });
    }

    @Override
    public boolean supports(String agentId) {
        return agentRepository.existsById(agentId);
    }

    private ChatModel buildChatModel(AgentDefinition agent) {
        String provider = llmConfig.getDefaultProvider();
        if ("zhipu".equals(provider)) {
            return buildZhipuChatModel(agent);
        }
        return buildDashScopeChatModel(agent);
    }

    private DashScopeChatModel buildDashScopeChatModel(AgentDefinition agent) {
        LLMConfiguration.AlibabaConfig config = llmConfig.getAlibaba();
        String model = agent.llmModel() != null ? agent.llmModel() : config.getModel();
        DashScopeChatOptions options = DashScopeChatOptions.builder()
                .withModel(model)
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

    private OpenAiChatModel buildZhipuChatModel(AgentDefinition agent) {
        LLMConfiguration.ZhipuConfig config = llmConfig.getZhipu();
        String model = agent.llmModel() != null ? agent.llmModel() : config.getModel();
        OpenAiApi api = OpenAiApi.builder()
                .apiKey(config.getApiKey())
                .baseUrl(config.getBaseUrl())
                .completionsPath("/v4/chat/completions")
                .build();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model)
                .temperature(config.getTemperature())
                .maxTokens(config.getMaxTokens())
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(options)
                .build();
    }

    private List<org.springframework.ai.chat.messages.Message> buildInitialMessages(
            AgentDefinition agent,
            AgentExecutionRequest request
    ) {
        List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();

        // System Prompt
        String systemPrompt = buildSystemPrompt(agent);
        messages.add(new SystemMessage(systemPrompt));

        // 上下文
        if (request.context() != null && !request.context().isEmpty()) {
            String contextStr = request.context().entrySet().stream()
                    .map(e -> e.getKey() + ": " + e.getValue())
                    .collect(Collectors.joining("\n"));
            messages.add(new SystemMessage("上下文信息:\n" + contextStr));
        }

        // 用户输入
        messages.add(new UserMessage(request.userInput()));

        return messages;
    }

    private String buildSystemPrompt(AgentDefinition agent) {
        StringBuilder sb = new StringBuilder();
        sb.append(agent.systemPrompt() != null ? agent.systemPrompt() : "你是一个智能助手。");

        if (agent.toolIds() != null && !agent.toolIds().isEmpty()) {
            sb.append("\n\n你有以下工具可用:");
            for (String toolId : agent.toolIds()) {
                var defOpt = toolRegistry.getDefinition(toolId);
                defOpt.ifPresent(def -> {
                    sb.append("\n- ").append(def.name())
                       .append(": ").append(def.description());
                });
            }
            sb.append("\n\n当你需要使用工具时，按照以下格式回复:");
            sb.append("\n```json");
            sb.append("\n{ \"tool\": \"工具名称\", \"input\": { \"参数名\": \"参数值\" } }");
            sb.append("\n```");
            sb.append("\n不要在思考过程中使用工具，只有在确定需要调用时才使用。");
        }

        return sb.toString();
    }

    private ToolCallResult parseAndExecuteTool(
            String content,
            AgentDefinition agent,
            ToolExecutor executor,
            List<ToolCall> toolCalls
    ) {
        // 尝试从 LLM 输出中提取 JSON 工具调用
        // 格式: { "tool": "xxx", "input": {...} }
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

            // 查找工具 ID
            String toolId = findToolId(toolName, agent.toolIds());
            if (toolId == null) {
                return new ToolCallResult("工具不存在: " + toolName, false);
            }

            // 执行工具
            long start = System.currentTimeMillis();
            String output = executor.execute(toolId, input);
            long duration = System.currentTimeMillis() - start;

            toolCalls.add(ToolCall.success(toolName, toolId, objectMapper.writeValueAsString(input), output, duration));

            return new ToolCallResult(output, true);
        } catch (Exception e) {
            log.debug("解析工具调用失败: content={}", content, e);
            return null;
        }
    }

    private String findToolId(String toolName, List<String> toolIds) {
        if (toolIds == null) return null;
        for (String id : toolIds) {
            var defOpt = toolRegistry.getDefinition(id);
            if (defOpt.isPresent() && defOpt.get().name().equals(toolName)) {
                return id;
            }
        }
        return null;
    }

    private String extractThought(String content) {
        // 简单提取思考内容（去掉 JSON 部分）
        int jsonStart = content.indexOf("{");
        if (jsonStart > 0) {
            return content.substring(0, jsonStart).trim();
        }
        return content.length() > 200 ? content.substring(0, 200) + "..." : content;
    }

    private record ToolCallResult(String observation, boolean hasToolCall) {}
}
