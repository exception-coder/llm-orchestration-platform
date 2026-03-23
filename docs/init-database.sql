-- 创建数据库
CREATE DATABASE IF NOT EXISTS llm_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE llm_platform;

-- 创建 Prompt 模板表
CREATE TABLE IF NOT EXISTS prompt_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    template_name VARCHAR(100) NOT NULL UNIQUE COMMENT '模板名称',
    category VARCHAR(50) COMMENT '模板分类',
    template_content TEXT NOT NULL COMMENT '模板内容',
    description VARCHAR(500) COMMENT '模板描述',
    variables TEXT COMMENT '模板变量（JSON格式）',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    version INT NOT NULL DEFAULT 1 COMMENT '版本号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by VARCHAR(50) COMMENT '创建人',
    updated_by VARCHAR(50) COMMENT '更新人',
    INDEX idx_template_name (template_name),
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Prompt模板表';

-- 插入内容优化模板
INSERT INTO prompt_template (template_name, category, template_content, description, variables) VALUES 
('content-optimization', 'content', 
'你是一位专业的内容创作优化专家，擅长为不同社交媒体平台优化文本内容。

## 任务目标
将用户提供的原始内容优化改写，使其更适合在指定平台发布。

## 平台特点
{platformCharacteristics}

## 内容风格
{styleDescription}

## 原始内容
{originalContent}

## 内容类型
{contentType}

## 目标受众
{targetAudience}

## 额外要求
{additionalRequirements}

## 优化要求
1. 保持原文核心信息和观点不变
2. 根据平台特点调整表达方式和结构
3. 运用符合风格的语言和修辞手法
4. 适当添加emoji表情（如果适合平台）
5. 优化段落结构，提升可读性
6. 添加吸引眼球的开头和引发互动的结尾

## 输出格式
请按以下JSON格式输出（不要包含markdown代码块标记）：
{
  "optimizedContent": "优化后的完整内容",
  "suggestedTitles": ["标题选项1", "标题选项2", "标题选项3"],
  "suggestedTags": ["标签1", "标签2", "标签3", "标签4", "标签5"],
  "optimizationNotes": "本次优化的关键调整说明"
}',
'内容优化模板，用于社交媒体平台内容改写',
'["platformCharacteristics", "styleDescription", "originalContent", "contentType", "targetAudience", "additionalRequirements"]'
);

-- 插入其他示例模板
INSERT INTO prompt_template (template_name, category, template_content, description, variables) VALUES 
('simple-chat', 'chat', 
'你是一个友好的AI助手。请用简洁、清晰的语言回答用户的问题。

用户问题：{userQuestion}

请提供有帮助的回答。',
'简单对话模板',
'["userQuestion"]'
),
('code-review', 'development',
'你是一位资深的代码审查专家。请审查以下代码并提供改进建议。

编程语言：{language}

代码内容：
{code}

请从以下方面进行审查：
1. 代码质量和可读性
2. 潜在的bug和安全问题
3. 性能优化建议
4. 最佳实践建议

请以JSON格式输出：
{
  "summary": "总体评价",
  "issues": ["问题1", "问题2"],
  "suggestions": ["建议1", "建议2"],
  "rating": "评分（1-10）"
}',
'代码审查模板',
'["language", "code"]'
);

-- =============================================
-- 智能碎片记录模块
-- =============================================

-- 创建记录类目表
CREATE TABLE IF NOT EXISTS note_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '类目名称',
    description VARCHAR(200) COMMENT '类目描述',
    icon VARCHAR(20) COMMENT 'emoji图标',
    sort_order INT DEFAULT 0 COMMENT '排序顺序',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='记录类目表';

-- 创建记录表
CREATE TABLE IF NOT EXISTS note (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    category_id BIGINT NOT NULL COMMENT '类目ID',
    title VARCHAR(200) COMMENT '标题',
    raw_input TEXT COMMENT '用户原始输入',
    content TEXT COMMENT '整理后的内容（加密时存密文）',
    summary VARCHAR(500) COMMENT 'AI生成的一句话摘要',
    is_encrypted TINYINT NOT NULL DEFAULT 0 COMMENT '是否加密：0-否，1-是',
    is_voice TINYINT NOT NULL DEFAULT 0 COMMENT '是否来自语音：0-否，1-是',
    tags VARCHAR(500) COMMENT '标签（JSON数组格式）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (category_id) REFERENCES note_category(id) ON DELETE CASCADE,
    INDEX idx_category_id (category_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='记录表';


-- ==========================================
-- Agent 运行时表（2026-03-23 新增）
-- ==========================================

-- Agent 定义表
CREATE TABLE IF NOT EXISTS agent_definition (
    id VARCHAR(100) PRIMARY KEY COMMENT 'Agent ID（业务主键）',
    name VARCHAR(200) NOT NULL COMMENT 'Agent 名称',
    description VARCHAR(500) COMMENT 'Agent 描述',
    system_prompt TEXT COMMENT 'System Prompt',
    tool_ids TEXT COMMENT '工具 ID 列表（JSON 数组）',
    llm_provider VARCHAR(100) COMMENT '指定 LLM Provider',
    llm_model VARCHAR(100) COMMENT '指定模型',
    max_iterations INT DEFAULT 10 COMMENT 'ReAct 最大迭代次数',
    timeout_seconds INT DEFAULT 120 COMMENT '执行超时秒数',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_agent_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 定义表';

-- Graph 定义表
CREATE TABLE IF NOT EXISTS graph_definition (
    id VARCHAR(100) PRIMARY KEY COMMENT 'Graph ID（业务主键）',
    name VARCHAR(200) NOT NULL COMMENT 'Graph 名称',
    description VARCHAR(500) COMMENT 'Graph 描述',
    nodes TEXT COMMENT '节点列表（JSON 数组）',
    edges TEXT COMMENT '边列表（JSON 数组）',
    entry_node_id VARCHAR(100) COMMENT '入口节点 ID',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_graph_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Graph 流程定义表';

-- Agent 示例数据：计算器助手
INSERT IGNORE INTO agent_definition (id, name, description, system_prompt, tool_ids, llm_model, max_iterations)
VALUES (
    'calculator-agent',
    '计算器助手',
    '能够执行数学计算的智能助手',
    '你是一个数学助手，擅长帮助用户进行各类数学计算。当用户提出计算需求时，使用 calculator 工具执行计算。',
    '["calculator"]',
    'qwen-plus',
    5
);

-- Graph 示例数据：碎片记录处理流程
INSERT IGNORE INTO graph_definition (id, name, description, nodes, edges, entry_node_id)
VALUES (
    'note-capture-graph',
    '碎片记录处理流程',
    '将用户输入的碎片文字经过 AI 分类后存储到对应类目',
    '[{"id":"classify","type":"LLM","name":"AI分类","config":{"prompt":"请分析以下用户输入，判断是否敏感（账号密码等），并分类：${rawInput}","model":"qwen-plus"}},{"id":"store","type":"TOOL","name":"存储记录","config":{"tool":"note_store","input":{"content":"${classify_output}"}}}]',
    '[{"from":"classify","to":"store","condition":null}]',
    'classify'
);
