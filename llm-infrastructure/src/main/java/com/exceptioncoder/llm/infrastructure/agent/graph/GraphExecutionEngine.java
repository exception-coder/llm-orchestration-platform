package com.exceptioncoder.llm.infrastructure.agent.graph;

import com.exceptioncoder.llm.domain.model.*;
import com.exceptioncoder.llm.domain.executor.AgentExecutor;
import com.exceptioncoder.llm.infrastructure.agent.tool.ToolExecutor;
import com.exceptioncoder.llm.infrastructure.config.LLMConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.alibaba.AlibabaChatModel;
import org.springframework.ai.alibaba.AlibabaChatOptions;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Graph DAG 执行引擎
 * 支持：顺序执行、条件分支、并行节点
 */
@Slf4j
public class GraphExecutionEngine {

    private final AgentExecutor agentExecutor;
    private final ToolExecutor toolExecutor;
    private final LLMConfiguration llmConfig;
    private final ObjectMapper objectMapper;
    private final ExecutorService parallelExecutor;

    public GraphExecutionEngine(
            AgentExecutor agentExecutor,
            ToolExecutor toolExecutor,
            LLMConfiguration llmConfig
    ) {
        this.agentExecutor = agentExecutor;
        this.toolExecutor = toolExecutor;
        this.llmConfig = llmConfig;
        this.objectMapper = new ObjectMapper();
        this.parallelExecutor = Executors.newFixedThreadPool(4);
    }

    /**
     * 执行 Graph
     * @param graph Graph 定义
     * @param input 初始输入变量
     * @return 执行结果
     */
    public GraphExecutionResult execute(
            GraphDefinition graph,
            Map<String, Object> input,
            String executionId
    ) {
        long startMs = System.currentTimeMillis();
        Map<String, Object> ctx = new HashMap<>(input);
        List<GraphExecutionResult.NodeExecutionResult> nodeResults = new ArrayList<>();

        try {
            // 校验 Graph
            validateGraph(graph);

            // 从入口节点开始执行
            String currentNodeId = graph.entryNodeId();
            String finalOutput = null;
            Set<String> visited = new HashSet<>();

            while (currentNodeId != null && !visited.contains(currentNodeId)) {
                visited.add(currentNodeId);
                GraphNode node = graph.findNode(currentNodeId);
                if (node == null) break;

                log.info("执行节点: id={}, type={}", node.id(), node.type());

                long nodeStart = System.currentTimeMillis();
                NodeOutput nodeOutput = executeNode(node, ctx);
                long nodeDuration = System.currentTimeMillis() - nodeStart;

                // 记录节点结果
                nodeResults.add(new GraphExecutionResult.NodeExecutionResult(
                        node.id(),
                        node.name() != null ? node.name() : node.id(),
                        nodeOutput.output,
                        nodeDuration,
                        nodeOutput.success,
                        nodeOutput.errorMessage
                ));

                if (!nodeOutput.success) {
                    return GraphExecutionResult.builder()
                            .executionId(executionId)
                            .graphId(graph.id())
                            .nodeResults(nodeResults)
                            .context(ctx)
                            .status(GraphExecutionResult.Status.FAILED)
                            .errorMessage("节点执行失败: " + node.id() + " - " + nodeOutput.errorMessage)
                            .elapsedMs(System.currentTimeMillis() - startMs)
                            .build();
                }

                // 更新上下文
                if (nodeOutput.output != null) {
                    ctx.put(node.id() + "_output", nodeOutput.output);
                    finalOutput = nodeOutput.output;
                }

                // 确定下一个节点
                currentNodeId = resolveNextNode(graph, node, ctx, nodeOutput);
            }

            return GraphExecutionResult.builder()
                    .executionId(executionId)
                    .graphId(graph.id())
                    .finalOutput(finalOutput)
                    .context(ctx)
                    .nodeResults(nodeResults)
                    .status(GraphExecutionResult.Status.SUCCESS)
                    .elapsedMs(System.currentTimeMillis() - startMs)
                    .build();

        } catch (Exception e) {
            log.error("Graph 执行失败: graphId={}", graph.id(), e);
            return GraphExecutionResult.builder()
                    .executionId(executionId)
                    .graphId(graph.id())
                    .nodeResults(nodeResults)
                    .context(ctx)
                    .status(GraphExecutionResult.Status.FAILED)
                    .errorMessage(e.getMessage())
                    .elapsedMs(System.currentTimeMillis() - startMs)
                    .build();
        }
    }

    /**
     * 执行单个节点
     */
    private NodeOutput executeNode(GraphNode node, Map<String, Object> ctx) {
        try {
            return switch (node.type()) {
                case LLM -> executeLlmNode(node, ctx);
                case TOOL -> executeToolNode(node, ctx);
                case CONDITION -> executeConditionNode(node, ctx);
                case PARALLEL -> executeParallelNode(node, ctx);
                case OUTPUT -> executeOutputNode(node, ctx);
                default -> new NodeOutput(null, true, null, null);
            };
        } catch (Exception e) {
            log.error("节点执行失败: nodeId={}", node.id(), e);
            return new NodeOutput(null, false, e.getMessage(), null);
        }
    }

    private NodeOutput executeLlmNode(GraphNode node, Map<String, Object> ctx) {
        String prompt = node.getString("prompt");
        String model = node.getString("model");
        if (prompt == null) {
            return new NodeOutput(null, false, "LLM 节点缺少 prompt 配置", null);
        }

        // 变量替换
        String resolvedPrompt = resolveVariables(prompt, ctx);

        LLMConfiguration.AlibabaConfig config = llmConfig.getAlibaba();
        AlibabaChatOptions options = AlibabaChatOptions.builder()
                .withModel(model != null ? model : config.getModel())
                .withTemperature(config.getTemperature())
                .withMaxTokens(config.getMaxTokens())
                .build();

        AlibabaChatModel chatModel = AlibabaChatModel.builder()
                .apiKey(config.getApiKey())
                .baseUrl(config.getBaseUrl())
                .options(options)
                .build();

        String systemPrompt = node.getString("system_prompt");
        List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
        if (systemPrompt != null) {
            messages.add(new SystemMessage(resolveVariables(systemPrompt, ctx)));
        }
        messages.add(new UserMessage(resolvedPrompt));

        var response = chatModel.call(new Prompt(messages));
        String output = response.getResult().getOutput().getContent();
        return new NodeOutput(output, true, null, null);
    }

    private NodeOutput executeToolNode(GraphNode node, Map<String, Object> ctx) {
        String toolId = node.getString("tool");
        if (toolId == null) {
            return new NodeOutput(null, false, "TOOL 节点缺少 tool 配置", null);
        }

        // 从 node config 中提取 input 参数，支持变量引用
        @SuppressWarnings("unchecked")
        Map<String, Object> inputConfig = (Map<String, Object>) node.config().get("input");
        Map<String, Object> input = resolveInputMap(inputConfig, ctx);

        String output = toolExecutor.execute(toolId, input);
        return new NodeOutput(output, true, null, null);
    }

    private NodeOutput executeConditionNode(GraphNode node, Map<String, Object> ctx) {
        // CONDITION 节点不产生输出，但通过 branch 决定下一个节点
        // 条件在 resolveNextNode 中处理
        return new NodeOutput(null, true, null, null);
    }

    private NodeOutput executeParallelNode(GraphNode node, Map<String, Object> ctx) {
        @SuppressWarnings("unchecked")
        List<String> subNodeIds = (List<String>) node.config().get("nodes");
        if (subNodeIds == null || subNodeIds.isEmpty()) {
            return new NodeOutput(null, true, null, null);
        }

        // 注意：parallel 节点执行的子节点需要从 graph 中查找，这里简化处理
        List<CompletableFuture<String>> futures = subNodeIds.stream()
                .map(subId -> CompletableFuture.supplyAsync(() -> {
                    Map<String, Object> subCtx = new HashMap<>(ctx);
                    return subCtx.getOrDefault(subId + "_output", "").toString();
                }, parallelExecutor))
                .collect(Collectors.toList());

        String combined = futures.stream()
                .map(f -> {
                    try { return f.get(); } catch (Exception e) { return ""; }
                })
                .collect(Collectors.joining("\n"));

        return new NodeOutput(combined, true, null, null);
    }

    private NodeOutput executeOutputNode(GraphNode node, Map<String, Object> ctx) {
        String outputKey = node.getString("from");
        if (outputKey != null) {
            Object value = ctx.get(outputKey);
            return new NodeOutput(value != null ? value.toString() : null, true, null, null);
        }
        // 返回最后一个节点的输出
        return new NodeOutput(null, true, null, null);
    }

    /**
     * 决定下一个执行节点
     */
    private String resolveNextNode(
            GraphDefinition graph,
            GraphNode node,
            Map<String, Object> ctx,
            NodeOutput nodeOutput
    ) {
        List<GraphEdge> outgoing = graph.getOutgoingEdges(node.id());
        if (outgoing.isEmpty()) return null;

        if (node.type() == NodeType.CONDITION) {
            // 条件节点：评估条件表达式，选择对应分支
            String conditionKey = node.getString("input");
            Object condValue = conditionKey != null ? resolveVar(conditionKey, ctx) : null;

            for (GraphEdge edge : outgoing) {
                if (edge.isUnconditional()) continue;
                if (matchesCondition(edge.condition(), condValue)) {
                    return edge.to();
                }
            }
            // 无匹配分支，走无条件边
            return outgoing.stream()
                    .filter(GraphEdge::isUnconditional)
                    .findFirst()
                    .map(GraphEdge::to)
                    .orElse(null);
        }

        // 普通节点：走无条件边（第一条）
        return outgoing.stream()
                .filter(GraphEdge::isUnconditional)
                .findFirst()
                .map(GraphEdge::to)
                .orElse(null);
    }

    private boolean matchesCondition(String condition, Object value) {
        if (condition == null || value == null) return false;
        String strValue = value.toString();
        // 支持: "true", "false", 字符串相等
        return condition.equalsIgnoreCase(strValue) ||
               condition.equals("true") && Boolean.parseBoolean(strValue) ||
               condition.equals("false") && !Boolean.parseBoolean(strValue);
    }

    private String resolveVariables(String template, Map<String, Object> ctx) {
        if (template == null) return null;
        for (Map.Entry<String, Object> entry : ctx.entrySet()) {
            if (entry.getValue() != null) {
                template = template.replace("${" + entry.getKey() + "}", entry.getValue().toString());
                template = template.replace("{{" + entry.getKey() + "}}", entry.getValue().toString());
            }
        }
        return template;
    }

    private Object resolveVar(String key, Map<String, Object> ctx) {
        // 支持点号访问: "classification_result.isSensitive"
        String[] parts = key.split("\\.");
        Object current = ctx.get(parts[0]);
        if (parts.length == 1 || current == null) return current;

        for (int i = 1; i < parts.length; i++) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(parts[i]);
            } else if (current instanceof String) {
                try {
                    Map<?, ?> map = objectMapper.readValue(current.toString(), Map.class);
                    current = map.get(parts[i]);
                } catch (Exception e) {
                    return null;
                }
            } else {
                return null;
            }
        }
        return current;
    }

    private Map<String, Object> resolveInputMap(Map<String, Object> inputConfig, Map<String, Object> ctx) {
        if (inputConfig == null) return Map.of();
        Map<String, Object> resolved = new HashMap<>();
        for (Map.Entry<String, Object> entry : inputConfig.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String str && str.startsWith("${") && str.endsWith("}")) {
                String varName = str.substring(2, str.length() - 1);
                resolved.put(entry.getKey(), resolveVar(varName, ctx));
            } else {
                resolved.put(entry.getKey(), value);
            }
        }
        return resolved;
    }

    public void validateGraph(GraphDefinition graph) {
        if (graph.entryNodeId() == null) {
            throw new IllegalArgumentException("Graph entryNodeId 不能为空");
        }
        if (graph.findNode(graph.entryNodeId()) == null) {
            throw new IllegalArgumentException("入口节点不存在: " + graph.entryNodeId());
        }
        // 验证所有边的 from/to 节点都存在
        for (GraphEdge edge : graph.edges()) {
            if (graph.findNode(edge.from()) == null) {
                throw new IllegalArgumentException("边的源节点不存在: " + edge.from());
            }
            if (graph.findNode(edge.to()) == null) {
                throw new IllegalArgumentException("边的目标节点不存在: " + edge.to());
            }
        }
    }

    private record NodeOutput(
            String output,
            boolean success,
            String errorMessage,
            String nextNodeId
    ) {}
}
