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

