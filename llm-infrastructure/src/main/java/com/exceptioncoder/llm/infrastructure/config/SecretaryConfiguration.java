package com.exceptioncoder.llm.infrastructure.config;

import com.exceptioncoder.llm.domain.repository.SecretaryMemoryRepository;
import com.exceptioncoder.llm.domain.repository.SecretaryScheduleRepository;
import com.exceptioncoder.llm.domain.repository.SecretaryTodoRepository;
import com.exceptioncoder.llm.infrastructure.agent.tool.builtin.ScheduleTool;
import com.exceptioncoder.llm.infrastructure.agent.tool.builtin.TodoTool;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 秘书工具插拔配置
 * 通过 application.yml secretary.tools.xxx=true/false 控制工具是否注册
 */
@Configuration
public class SecretaryConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "secretary.tools", name = "schedule", havingValue = "true", matchIfMissing = true)
    public ScheduleTool scheduleTool(SecretaryScheduleRepository scheduleRepository) {
        return new ScheduleTool(scheduleRepository);
    }

    @Bean
    @ConditionalOnProperty(prefix = "secretary.tools", name = "todo", havingValue = "true", matchIfMissing = true)
    public TodoTool todoTool(SecretaryTodoRepository todoRepository) {
        return new TodoTool(todoRepository);
    }
}
