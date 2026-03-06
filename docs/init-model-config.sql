-- LLM 模型配置表初始化脚本

-- 创建模型配置表
CREATE TABLE IF NOT EXISTS llm_model_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    model_code VARCHAR(100) NOT NULL UNIQUE COMMENT '模型代码',
    provider VARCHAR(50) NOT NULL COMMENT '提供商',
    model_name VARCHAR(100) NOT NULL COMMENT '模型名称',
    description VARCHAR(500) COMMENT '模型描述',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    sort_order INT DEFAULT 0 COMMENT '排序顺序',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by VARCHAR(50) COMMENT '创建人',
    updated_by VARCHAR(50) COMMENT '更新人',
    INDEX idx_model_code (model_code),
    INDEX idx_provider (provider)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LLM模型配置表';

-- 插入初始模型配置数据

-- OpenAI 模型
INSERT INTO llm_model_config (model_code, provider, model_name, description, enabled, sort_order, created_by) VALUES
('gpt-4', 'openai', 'GPT-4', '最强大的模型，适合复杂任务', TRUE, 1, 'system'),
('gpt-4-turbo', 'openai', 'GPT-4 Turbo', '更快的 GPT-4 版本', TRUE, 2, 'system'),
('gpt-4o', 'openai', 'GPT-4o', 'GPT-4 优化版本，速度更快', TRUE, 3, 'system'),
('gpt-3.5-turbo', 'openai', 'GPT-3.5 Turbo', '性价比高，适合大多数场景', TRUE, 4, 'system');

-- DeepSeek 模型
INSERT INTO llm_model_config (model_code, provider, model_name, description, enabled, sort_order, created_by) VALUES
('deepseek-chat', 'deepseek', 'DeepSeek Chat', 'DeepSeek 对话模型，性价比高', TRUE, 10, 'system'),
('deepseek-coder', 'deepseek', 'DeepSeek Coder', '专注代码生成和理解的模型', TRUE, 11, 'system');

-- Ollama 本地模型
INSERT INTO llm_model_config (model_code, provider, model_name, description, enabled, sort_order, created_by) VALUES
('llama2', 'ollama', 'Llama 2', 'Meta 开源模型', TRUE, 20, 'system'),
('llama3', 'ollama', 'Llama 3', 'Meta 最新开源模型', TRUE, 21, 'system'),
('llama3.1', 'ollama', 'Llama 3.1', 'Meta Llama 3.1 版本', TRUE, 22, 'system'),
('mistral', 'ollama', 'Mistral', '高性能开源模型', TRUE, 23, 'system'),
('qwen', 'ollama', '通义千问', '阿里云开源模型', TRUE, 24, 'system'),
('qwen2', 'ollama', '通义千问 2', '阿里云通义千问 2.0', TRUE, 25, 'system'),
('deepseek-coder-local', 'ollama', 'DeepSeek Coder (本地)', '本地运行的 DeepSeek Coder', TRUE, 26, 'system');

-- 更新 prompt_template 表，添加 variable_examples 字段
ALTER TABLE prompt_template 
ADD COLUMN IF NOT EXISTS variable_examples TEXT COMMENT '变量示例（JSON格式）' AFTER variables;

-- 更新现有模板的变量示例
UPDATE prompt_template 
SET variable_examples = '{
  "platformCharacteristics": "年轻女性为主，注重生活方式和美学",
  "styleDescription": "轻松随意，口语化表达",
  "originalContent": "今天试用了一款新的护肤品，效果还不错。",
  "contentType": "产品测评",
  "targetAudience": "18-35岁女性",
  "additionalRequirements": "无"
}'
WHERE template_name = 'content-optimization';

-- 提交事务
COMMIT;

