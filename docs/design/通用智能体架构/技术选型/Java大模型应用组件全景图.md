# Java 大模型应用开发组件全景图

> 一张图总览 Java 生态中构建企业级 AI 应用涉及的所有组件层次、候选技术、优缺点及分支选择。
>
> 最后更新：2026-04-05 v1

---

## 全景图

```mermaid
graph TD
    USER["用户请求"] --> GW

    subgraph GW["接入层"]
        direction LR
        REST["REST API - Spring Boot\n+ 生态成熟 全家桶\n- 无原生流式推送"]
        WS["WebSocket / SSE\n+ 真实时推送\n- 连接管理复杂"]
    end

    GW --> SAFE

    subgraph SAFE["安全过滤层"]
        direction LR
        L1["L1 DFA关键词过滤\n+ 微秒级 零成本\n- 无法识别变体"]
        L2["L2 正则Prompt注入检测\n+ 精准匹配已知模式\n- 新攻击需手动更新"]
        L3["L3 LLM语义审核\n+ 能识别隐晦恶意\n- 增加延迟和成本"]
        L1 --> L2 --> L3
    end

    SAFE --> AGENT

    subgraph AGENT["Agent 核心层"]
        direction TB

        subgraph LC4J_PATH["LangChain4j -- 推荐"]
            AIS["AiServices 声明式Agent\n+ 自动ReAct循环 @Tool零样板\n+ @MemoryId声明式多用户隔离\n+ 5行代码=完整Agent\n- 无Advisor拦截器链\n- 可观测性需自建"]
        end

        subgraph SAI_PATH["Spring AI -- 备选"]
            CC["ChatClient + Advisor链\n+ Advisor拦截器 日志/审计/限流\n+ Micrometer原生可观测\n+ 阿里云官方spring-ai-alibaba\n- 无内置Agent循环\n- 工具注册样板代码多\n- @MemoryId无等价物"]
        end
    end

    AGENT --> MODEL_ROUTE

    subgraph MODEL_ROUTE["模型路由层 -- 自建"]
        ROUTER["LLMProviderRouter\n+ 两大框架均无内置\n+ 支持动态切换/AB测试\n- 需自行维护"]
    end

    MODEL_ROUTE --> MODELS

    subgraph MODELS["模型接入层"]
        direction LR
        QWEN["通义千问 DashScope\n+ 阿里官方 中文强\n+ 免费额度充足\n- 海外延迟高"]
        DEEPSEEK["DeepSeek\n+ 性价比极高\n+ OpenAI协议兼容\n- 高峰期不稳定"]
        OLLAMA["Ollama 本地\n+ 数据不出境\n+ 零API成本\n- 需GPU 推理慢"]
        OPENAI["OpenAI GPT\n+ 能力天花板\n+ 生态最完善\n- 成本高 需翻墙"]
    end

    AGENT --> RAG_PIPE

    subgraph RAG_PIPE["RAG Pipeline"]
        direction LR

        subgraph DOC_LOAD["文档处理"]
            TIKA["Apache Tika\n+ 全格式PDF/Word/PPTX\n+ 原生Java OCR支持\n- 中文PDF表格弱"]
            LC_LOAD["LangChain4j Loader\n+ 轻量 TXT/MD/HTML\n+ 与AiServices集成好\n- 不支持Word/PPTX"]
        end

        subgraph DOC_SPLIT["文档切分"]
            REC_SPLIT["递归切分\n+ 通用性强 简单可靠\n- 可能截断语义"]
            SEM_SPLIT["语义切分\n+ 按语义边界切分\n- 需额外Embedding调用"]
        end

        subgraph EMBED["Embedding"]
            GTE["GTE-Qwen2 DashScope\n+ 中文MTEB前列\n+ 已接入零配置\n- 依赖阿里云"]
            BGE["BGE-M3 本地\n+ 中文最强 免费\n+ 数据不出境\n- 需GPU部署"]
            OAI_EMB["OpenAI Embedding\n+ 多语言通用\n- 成本高 需翻墙"]
        end

        subgraph VDB["向量数据库"]
            PGV["PGVector\n+ 复用PG零运维\n+ SQL混合检索\n- 百万级后性能降"]
            QDR["Qdrant\n+ 单二进制部署\n+ 百万级高性能\n- 额外运维组件"]
            MIL["Milvus\n+ 亿级分布式\n- 部署复杂 运维重"]
            CHR["Chroma\n+ 零配置快速验证\n- 不适合生产"]
        end

        DOC_LOAD --> DOC_SPLIT --> EMBED --> VDB
    end

    AGENT --> MEM_STORE

    subgraph MEM_STORE["记忆持久化层"]
        direction LR
        INMEM["InMemory\n+ 零配置 开发快\n- 重启丢失"]
        REDIS_MEM["Redis\n+ 毫秒级读写\n+ 自动过期\n- 非持久化"]
        MYSQL_MEM["MySQL\n+ 永久持久化\n+ 可审计查询\n- 读写延迟高"]
    end

    AGENT --> MCP_LAYER

    subgraph MCP_LAYER["MCP 工具扩展层"]
        direction LR
        MCP_SA["Spring AI MCP\n+ 协议完整 注解开发\n+ STDIO/SSE/HTTP全支持\n- 与LangChain4j需适配"]
        MCP_SDK["mcp-java-sdk Anthropic官方\n+ Anthropic官方维护\n- 需手动桥接Agent"]
        MCP_SA --- MCP_SDK
    end

    AGENT --> OBS_LAYER

    subgraph OBS_LAYER["可观测性层"]
        direction LR
        MICRO["Micrometer\n+ Spring原生零配置\n+ QPS/延迟/错误率\n- 无LLM专项指标"]
        LANGF["LangFuse\n+ Token成本自动统计\n+ RAG质量评分\n+ 可私有化部署\n- 需额外部署服务"]
        MICRO --- LANGF
    end

    OBS_LAYER --> EVAL_LAYER

    subgraph EVAL_LAYER["评估层"]
        direction LR
        SELF_EVAL["自建指标\n+ Java原生 CI集成\n- 需自定义所有指标"]
        RAGAS_E["RAGAS\n+ RAG专项评估权威\n+ Faithfulness/Relevance\n- Python服务"]
        SELF_EVAL --- RAGAS_E
    end

    MEM_STORE --> INFRA
    VDB --> INFRA

    subgraph INFRA["基础设施层"]
        direction LR
        PG_DB["PostgreSQL\n+ 关系+向量一体"]
        REDIS_I["Redis\n+ 缓存+记忆一体"]
        K8S["Docker/K8s\n+ 标准化部署"]
    end

    style LC4J_PATH fill:#e3f2fd
    style SAI_PATH fill:#f1f8e9
    style RAG_PIPE fill:#fff8e1
    style SAFE fill:#fce4ec
    style OBS_LAYER fill:#f3e5f5
    style MCP_LAYER fill:#e0f2f1
    style INFRA fill:#eceff1
```

---

## 图例说明

| 层次 | 职责 | 关键决策点 |
|------|------|-----------|
| 接入层 | REST / WebSocket / SSE | Spring Boot 标配 |
| 安全过滤层 | 输入输出过滤 | 自建 3 层过滤（DFA → 正则 → LLM） |
| Agent 核心层 | 推理循环 / 工具调用 / 记忆 / RAG | **LangChain4j AiServices（推荐）** vs Spring AI ChatClient |
| 模型路由层 | 多模型动态切换 | 自建（两大框架均无内置） |
| 模型接入层 | LLM API 调用 | 通义 / DeepSeek / Ollama / OpenAI |
| RAG Pipeline | 文档解析 → 切分 → 向量化 → 检索 | Tika + LangChain4j Splitter + GTE-Qwen2 + PGVector |
| 记忆持久化层 | 对话历史存储 | Redis（生产）/ MySQL（归档） |
| MCP 扩展层 | 外部工具集成 | Spring AI MCP + 适配器桥接 |
| 可观测性层 | 监控 / 追踪 / 成本 | Micrometer（基础）+ LangFuse（LLM 专项） |
| 评估层 | 质量指标 | 自建 + RAGAS 辅助 |
| 基础设施层 | 数据库 / 缓存 / 容器 | PostgreSQL + Redis + Docker |

---

## 变更记录

| 版本 | 日期 | 变更内容 |
|------|------|---------|
| v1 | 2026-04-05 | 初始版本，11 层全景 + 优缺点标注 |
