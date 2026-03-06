# LLM Orchestration Platform

企业级 LLM 能力编排和管理平台

## 项目简介

基于 Spring AI + LangChain4j 构建的企业级 LLM 开发脚手架，支持多种 LLM 提供商的统一管理和编排。

## 技术栈

- **Spring Boot 3.2.2**
- **Spring AI 1.0.0-M4** - OpenAI 集成
- **LangChain4j 0.35.0** - Ollama 等本地模型集成
- **Java 17**
- **Maven**

## 架构设计

采用 DDD-lite 分层架构：

```
llm-orchestration-platform/
├── llm-domain/          # 领域层：核心业务模型和接口
├── llm-application/     # 应用层：用例编排
├── llm-infrastructure/  # 基础设施层：外部系统集成
├── llm-api/            # API层：REST接口
└── llm-starter/        # 启动模块
```

### 核心功能

1. **多 Provider 支持**
   - OpenAI (通过 Spring AI)
   - Ollama (通过 LangChain4j)
   - 可扩展其他提供商

2. **对话管理**
   - 对话历史存储
   - 上下文管理
   - 会话隔离

3. **智能路由**
   - 根据模型自动选择 Provider
   - 支持手动指定 Provider

4. **配置动态刷新**
   - 支持运行时配置更新
   - 无需重启服务

## 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.6+
- (可选) Ollama 本地服务

### 2. 配置

编辑 `llm-starter/src/main/resources/application.yml`：

```yaml
llm:
  openai:
    api-key: your-openai-api-key
    
  ollama:
    base-url: http://localhost:11434
```

### 3. 构建项目

```bash
cd llm-orchestration-platform
mvn clean install
```

### 4. 运行

```bash
cd llm-starter
mvn spring-boot:run
```

服务将在 `http://localhost:8080` 启动

## API 使用示例

### 1. 对话接口

```bash
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{
    "conversationId": "conv-001",
    "message": "你好，介绍一下自己",
    "provider": "openai",
    "model": "gpt-3.5-turbo"
  }'
```

### 2. 内容优化接口（新功能）

优化文本内容，适配不同社交媒体平台：

```bash
curl -X POST http://localhost:8080/api/v1/content/optimize \
  -H "Content-Type: application/json" \
  -d '{
    "originalContent": "今天试用了一款新的护肤品，效果还不错。",
    "platform": "XIAOHONGSHU",
    "style": "CASUAL",
    "contentType": "PRODUCT_REVIEW"
  }'
```

响应示例：

```json
{
  "optimizedContent": "姐妹们！今天要给你们分享一个宝藏护肤品💎\n\n用了一周下来，真的被惊艳到了！...",
  "suggestedTitles": [
    "💎宝藏护肤品分享！用完皮肤水润到发光",
    "姐妹们！这款护肤品真的绝了"
  ],
  "suggestedTags": ["#护肤分享", "#好物推荐", "#美妆测评"],
  "platform": "小红书",
  "style": "轻松随意"
}
```

**支持的平台**：小红书、抖音、TikTok、微博、微信公众号

**支持的风格**：专业严谨、轻松随意、幽默风趣、情感共鸣、励志激励、潮流时尚

详细使用指南请查看：[内容优化功能指南](docs/content-optimization-guide.md)

## 扩展指南

### 添加新的 LLM Provider

1. 在 `llm-infrastructure` 模块创建新的 Provider 实现
2. 实现 `LLMProvider` 接口
3. 添加 `@Component` 注解，自动注册

示例：

```java
@Component
public class CustomProvider implements LLMProvider {
    
    @Override
    public LLMResponse chat(LLMRequest request) {
        // 实现调用逻辑
    }
    
    @Override
    public String getProviderName() {
        return "custom";
    }
    
    @Override
    public boolean supports(String model) {
        return model.startsWith("custom-");
    }
}
```

### 添加 Chain 编排

1. 在 `llm-domain` 定义 `ChainExecutor` 接口
2. 在 `llm-application` 实现具体的 Chain 逻辑
3. 在 API 层暴露接口

## 项目特点

- ✅ **清晰的分层架构**：遵循 DDD 原则，职责分明
- ✅ **依赖方向正确**：基础设施层实现领域层接口
- ✅ **构造函数注入**：避免字段注入，提高可测试性
- ✅ **配置动态刷新**：支持 `@RefreshScope`
- ✅ **统一异常处理**：全局异常拦截
- ✅ **参数校验**：使用 Bean Validation
- ✅ **日志规范**：结构化日志输出

## 后续规划

- [ ] 添加 Redis 存储对话历史
- [ ] 实现 Chain 编排引擎
- [ ] 支持 Function Calling
- [ ] 添加 Prompt 模板管理
- [ ] 实现流式响应 API
- [ ] 添加监控和指标
- [ ] 集成更多 LLM 提供商

## License

MIT License

