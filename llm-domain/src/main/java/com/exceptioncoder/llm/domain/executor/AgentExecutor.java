package com.exceptioncoder.llm.domain.executor;

import com.exceptioncoder.llm.domain.model.AgentExecutionResult;
import com.exceptioncoder.llm.domain.executor.AgentIterationListener;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * Agent 执行器接口
 * 定义 Agent 的执行行为
 */
public interface AgentExecutor {

    /**
     * 同步执行 Agent
     */
    AgentExecutionResult execute(AgentExecutionRequest request);

    /**
     * 同步执行 Agent，支持迭代级事件回调。
     *
     * <p>默认委托给无 listener 版本，实现类可覆盖以支持每轮迭代通知。</p>
     */
    default AgentExecutionResult execute(AgentExecutionRequest request, AgentIterationListener listener) {
        return execute(request);
    }

    /**
     * 流式执行 Agent
     * 返回 LLM 输出的增量文本流
     */
    Flux<String> executeStream(AgentExecutionRequest request);

    /**
     * 检查是否支持该 Agent
     */
    boolean supports(String agentId);

    /**
     * Agent 执行请求
     */
    record AgentExecutionRequest(
            String executionId,
            String agentId,
            String userInput,
            Map<String, Object> context,
            boolean stream
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String executionId;
            private String agentId;
            private String userInput;
            private Map<String, Object> context = Map.of();
            private boolean stream = false;

            public Builder executionId(String executionId) { this.executionId = executionId; return this; }
            public Builder agentId(String agentId) { this.agentId = agentId; return this; }
            public Builder userInput(String userInput) { this.userInput = userInput; return this; }
            public Builder context(Map<String, Object> context) { this.context = context; return this; }
            public Builder stream(boolean stream) { this.stream = stream; return this; }

            public AgentExecutionRequest build() {
                return new AgentExecutionRequest(executionId, agentId, userInput, context, stream);
            }
        }
    }
}
