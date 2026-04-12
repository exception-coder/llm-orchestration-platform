package com.exceptioncoder.llm.infrastructure.agent.executor;

import com.exceptioncoder.llm.domain.model.*;
import com.exceptioncoder.llm.domain.executor.AgentExecutor;
import com.exceptioncoder.llm.domain.executor.AgentIterationListener;
import com.exceptioncoder.llm.domain.repository.AgentDefinitionRepository;
import com.exceptioncoder.llm.domain.service.LLMProvider;
import com.exceptioncoder.llm.infrastructure.agent.tool.ToolExecutor;
import com.exceptioncoder.llm.infrastructure.agent.tool.ToolRegistryImpl;
import com.exceptioncoder.llm.infrastructure.provider.LLMProviderRouter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
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
    private final LLMProviderRouter providerRouter;
    private final ObjectMapper objectMapper;

    public AlibabaAgentExecutor(
            AgentDefinitionRepository agentRepository,
            ToolRegistryImpl toolRegistry,
            ToolExecutor toolExecutor,
            LLMProviderRouter providerRouter
    ) {
        this.agentRepository = agentRepository;
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
        this.providerRouter = providerRouter;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public AgentExecutionResult execute(AgentExecutionRequest request) {
        return execute(request, AgentIterationListener.NOOP);
    }

    @Override
    public AgentExecutionResult execute(AgentExecutionRequest request, AgentIterationListener listener) {
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

        String model = agent.llmModel();
        LLMProvider provider = model != null ? providerRouter.route(model) : providerRouter.getDefault();
        ChatModel chatModel = provider.getChatModel();
        List<org.springframework.ai.chat.messages.Message> messages = buildInitialMessages(agent, request);

        int maxIterations = agent.maxIterations();
        AtomicInteger iterations = new AtomicInteger(0);
        int consecutiveFailures = 0;
        boolean finished = false;
        String finalOutput = null;

        while (iterations.incrementAndGet() <= maxIterations && !finished) {
            int currentIteration = iterations.get();

            try {
                // 单次 LLM 调用，含重试
                String content = callWithRetry(chatModel, messages, currentIteration);

                // 重置连续失败计数
                consecutiveFailures = 0;

                // 尝试解析工具调用
                final ToolCallResult toolCallResult = content != null
                        ? parseAndExecuteTool(content, agent, toolExecutor, toolCalls)
                        : null;

                if (toolCallResult != null) {
                    // 有工具调用，追加 Observation
                    String thought = extractThought(content);
                    thoughtHistory.add("思考: " + thought);
                    messages.add(new AssistantMessage(content));
                    messages.add(new UserMessage("观察结果: " + toolCallResult.observation));

                    // 回调：迭代 + 工具结果
                    ToolCall lastToolCall = toolCalls.isEmpty() ? null : toolCalls.get(toolCalls.size() - 1);
                    String observation = toolCallResult.observation;
                    notifyListener(listener, () -> listener.onIteration(
                            request.executionId(), currentIteration, thought, lastToolCall));
                    if (lastToolCall != null) {
                        notifyListener(listener, () -> listener.onToolResult(
                                request.executionId(), currentIteration,
                                lastToolCall.toolName(), observation));
                    }
                } else {
                    // 无工具调用，LLM 直接回答
                    if (content != null) {
                        messages.add(new AssistantMessage(content));
                    }
                    finalOutput = content;
                    finished = true;

                    // 回调：最终迭代
                    notifyListener(listener, () -> listener.onIteration(
                            request.executionId(), currentIteration, content, null));
                }

                log.info("Agent 迭代完成: executionId={}, iteration={}, hasToolCall={}, elapsed={}ms",
                        request.executionId(), currentIteration, toolCallResult != null,
                        System.currentTimeMillis() - startMs);

            } catch (Exception e) {
                consecutiveFailures++;
                log.error("Agent 执行异常: executionId={}, iteration={}, consecutiveFailures={}",
                        request.executionId(), currentIteration, consecutiveFailures, e);

                // R5: 连续 3 轮失败触发熔断
                if (consecutiveFailures >= 3) {
                    log.error("连续 {} 轮迭代失败，触发熔断: executionId={}", consecutiveFailures, request.executionId());
                    return AgentExecutionResult.builder()
                            .executionId(request.executionId())
                            .agentId(request.agentId())
                            .finalOutput(finalOutput)
                            .toolCalls(toolCalls)
                            .thoughtHistory(thoughtHistory)
                            .iterations(currentIteration)
                            .status(AgentExecutionResult.Status.FAILED)
                            .errorMessage("连续 " + consecutiveFailures + " 轮迭代失败: " + e.getMessage())
                            .elapsedMs(System.currentTimeMillis() - startMs)
                            .build();
                }

                if (currentIteration >= maxIterations) {
                    return AgentExecutionResult.builder()
                            .executionId(request.executionId())
                            .agentId(request.agentId())
                            .finalOutput(finalOutput)
                            .toolCalls(toolCalls)
                            .thoughtHistory(thoughtHistory)
                            .iterations(currentIteration)
                            .status(AgentExecutionResult.Status.MAX_ITERATIONS_REACHED)
                            .errorMessage("达到最大迭代次数，最后异常: " + e.getMessage())
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

    /**
     * 单次 LLM 调用，含超时重试（最多重试 2 次）。
     * <p>
     * 使用流式（stream）模式替代同步 call：首个 token 快速返回，
     * 连接在整个生成过程中保持活跃，从根本上避免 ReadTimeoutException。
     * 内部将流式 token 收集为完整响应，对上层调用者透明。
     */
    private String callWithRetry(ChatModel chatModel,
                                  List<org.springframework.ai.chat.messages.Message> messages,
                                  int iteration) {
        int maxRetries = 2;
        Exception lastException = null;

        for (int retry = 0; retry <= maxRetries; retry++) {
            try {
                Prompt prompt = new Prompt(messages);
                // 流式调用：token 逐个返回，避免长时间无数据导致读超时
                String content = chatModel.stream(prompt)
                        .map(response -> {
                            var results = response.getResults();
                            if (results == null || results.isEmpty()) return "";
                            String text = results.get(0).getOutput().getText();
                            return text != null ? text : "";
                        })
                        .collectList()
                        .map(chunks -> String.join("", chunks))
                        .block();
                return content;
            } catch (Exception e) {
                lastException = e;
                if (retry < maxRetries) {
                    log.warn("LLM 流式调用失败，重试 {}/{}: iteration={}", retry + 1, maxRetries, iteration, e);
                }
            }
        }
        throw new RuntimeException("LLM 调用失败（重试 " + maxRetries + " 次后）: " + lastException.getMessage(), lastException); // NOSONAR lastException is always non-null here
    }

    /**
     * 安全地通知 listener，不让回调异常影响主流程。
     */
    private void notifyListener(AgentIterationListener listener, Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            log.debug("Listener 回调异常（不影响执行）", e);
        }
    }

    /**
     * 流式执行 Agent（ReAct 循环）。
     * <p>
     * 工具调用迭代：内部缓冲，向调用方发送进度事件。
     * 最终回答迭代：逐 token 流式输出。
     */
    @Override
    public Flux<String> executeStream(AgentExecutionRequest request) {
        return Flux.create(sink -> {
            try {
                var agentOpt = agentRepository.findById(request.agentId());
                if (agentOpt.isEmpty()) {
                    sink.error(new IllegalArgumentException("Agent 不存在: " + request.agentId()));
                    return;
                }

                AgentDefinition agent = agentOpt.get();
                List<ToolCall> toolCalls = new ArrayList<>();
                String model = agent.llmModel();
                LLMProvider provider = model != null ? providerRouter.route(model) : providerRouter.getDefault();
                ChatModel chatModel = provider.getChatModel();
                List<org.springframework.ai.chat.messages.Message> messages = buildInitialMessages(agent, request);

                int maxIterations = agent.maxIterations();
                boolean finished = false;

                for (int iteration = 1; iteration <= maxIterations && !finished; iteration++) {
                    Prompt prompt = new Prompt(messages);
                    StringBuilder contentBuilder = new StringBuilder();

                    // 流式收集本轮响应
                    chatModel.stream(prompt)
                            .doOnNext(response -> {
                                var results = response.getResults();
                                if (results != null && !results.isEmpty()) {
                                    String text = results.get(0).getOutput().getText();
                                    if (text != null) {
                                        contentBuilder.append(text);
                                    }
                                }
                            })
                            .blockLast();

                    String content = contentBuilder.toString();
                    ToolCallResult toolCallResult = content.isEmpty() ? null
                            : parseAndExecuteTool(content, agent, toolExecutor, toolCalls);

                    if (toolCallResult != null) {
                        // 工具调用迭代：追加 Observation，继续循环
                        messages.add(new AssistantMessage(content));
                        messages.add(new UserMessage("观察结果: " + toolCallResult.observation));
                    } else {
                        // 最终回答：逐 token 发送给调用方
                        sink.next(content);
                        finished = true;
                    }
                }

                if (!finished) {
                    sink.next("Agent 执行未产生最终输出（达到最大迭代次数）");
                }
                sink.complete();
            } catch (Exception e) {
                sink.error(e);
            }
        });
    }

    @Override
    public boolean supports(String agentId) {
        return agentRepository.existsById(agentId);
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
