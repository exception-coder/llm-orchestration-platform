package com.exceptioncoder.llm.domain.executor;

import com.exceptioncoder.llm.domain.model.GraphDefinition;
import com.exceptioncoder.llm.domain.model.GraphExecutionResult;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * Graph 流程执行器接口
 * 负责 DAG 流程的解析和执行
 */
public interface GraphExecutor {

    /**
     * 同步执行 Graph
     */
    GraphExecutionResult execute(GraphExecutionRequest request);

    /**
     * 流式执行 Graph
     * 返回 LLM 输出的增量文本流
     */
    Flux<String> executeStream(GraphExecutionRequest request);

    /**
     * 加载 Graph 定义
     */
    GraphDefinition loadGraph(String graphId);

    /**
     * 校验 Graph 定义的合法性
     */
    void validateGraph(GraphDefinition graph);

    /**
     * Graph 执行请求
     */
    record GraphExecutionRequest(
            String executionId,
            String graphId,
            Map<String, Object> input,
            boolean stream
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String executionId;
            private String graphId;
            private Map<String, Object> input = Map.of();
            private boolean stream = false;

            public Builder executionId(String executionId) { this.executionId = executionId; return this; }
            public Builder graphId(String graphId) { this.graphId = graphId; return this; }
            public Builder input(Map<String, Object> input) { this.input = input; return this; }
            public Builder stream(boolean stream) { this.stream = stream; return this; }

            public GraphExecutionRequest build() {
                return new GraphExecutionRequest(executionId, graphId, input, stream);
            }
        }
    }
}
