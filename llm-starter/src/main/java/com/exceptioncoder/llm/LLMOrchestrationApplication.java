package com.exceptioncoder.llm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * LLM 编排平台启动类
 *
 * <p><b>排除 Spring AI AutoConfiguration 说明：</b>
 *
 * <p>1. {@code ChatClientAutoConfiguration} — 该配置要求注入单一 {@code ChatModel} Bean
 * 来构建 {@code ChatClient}，但本项目同时引入了 DashScope（通义千问）、Ollama 两个平台的
 * starter，各自注册了 {@code dashscopeChatModel} 和 {@code ollamaChatModel}，导致
 * Spring 无法选择。本项目通过自建的 {@code LLMProviderRouter} 按配置（llm.default-provider）
 * 或模型名称动态路由到具体 Provider，不依赖 Spring AI 的 {@code ChatClient} 抽象。
 *
 * <p>2. {@code ToolCallingAutoConfiguration} — 该配置在初始化 {@code toolExecutionExceptionProcessor}
 * 时尝试加载 {@code spring-security-oauth2-client} 的异常类做兼容处理，但本项目未引入
 * OAuth2 依赖导致 {@code ClassNotFoundException}。且本项目有完整的自建 Tool 体系
 * （{@code @Tool} 注解 + {@code ToolScanner} 自动扫描 + {@code ToolExecutor} 反射执行
 * + {@code DevPlanToolRegistry} 角色权限隔离），不使用 Spring AI 内置的 Tool Calling 机制。
 *
 * <p><b>自建 vs Spring AI AutoConfiguration 对比：</b>
 * <pre>
 * ┌──────────────┬─────────────────────────────┬─────────────────────────────┐
 * │              │ Spring AI AutoConfiguration  │ 自建路由/工具体系            │
 * ├──────────────┼─────────────────────────────┼─────────────────────────────┤
 * │ 多平台支持    │ 单一 ChatModel，多个冲突      │ Router 动态路由，天然多平台   │
 * │ 接入成本      │ 零配置，starter 开箱即用      │ 需编写 Router/Provider 代码  │
 * │ 降级容错      │ 不支持，Bean 创建失败即启动失败 │ Router 可实现自动降级切换     │
 * │ 模型级路由    │ 不支持，全局一个 ChatClient    │ 按场景/模型名选择不同 Provider│
 * │ Tool Calling │ 内置 FunctionCallback 机制    │ 自建 @Tool 注解 + 角色权限隔离│
 * │ 权限控制      │ 无，所有 Tool 全局可调         │ DevPlanToolRegistry 按角色隔离│
 * │ 版本耦合      │ 强耦合 Spring AI 版本变更      │ 核心逻辑与框架版本解耦       │
 * │ 适用场景      │ 单平台、快速原型、Demo         │ 多平台、企业级、需定制化      │
 * └──────────────┴─────────────────────────────┴─────────────────────────────┘
 * </pre>
 *
 * <p><b>结论：</b>starter 仍然引入，利用其注册底层 API 客户端 Bean（如 DashScopeChatModel、
 * OllamaChatModel），但排除上层编排类的 AutoConfiguration，由项目自建体系接管编排逻辑。
 * 这是「借力底层、掌控上层」的企业级最佳实践。
 */
@SpringBootApplication(exclude = {
        org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration.class,
        org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration.class
})
public class LLMOrchestrationApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(LLMOrchestrationApplication.class, args);
    }
}

