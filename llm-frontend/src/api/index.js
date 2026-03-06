import request from '@/utils/request'

// Prompt 测试相关 API
export const promptTestAPI = {
  // 执行测试
  test: (data) => request.post('/prompt-test', data),
  
  // 获取可用模型列表
  getModels: () => request.get('/prompt-test/models'),
  
  // 获取模板变量示例
  getTemplateVariables: (templateName) => 
    request.get(`/prompt-test/template-variables/${templateName}`)
}

// 内容优化相关 API
export const contentOptimizationAPI = {
  // 优化内容
  optimize: (data) => request.post('/content/optimize', data),
  
  // 获取平台列表
  getPlatforms: () => request.get('/content/platforms'),
  
  // 获取风格列表
  getStyles: () => request.get('/content/styles'),
  
  // 获取内容类型列表
  getContentTypes: () => request.get('/content/content-types')
}

// 模板管理相关 API
export const templateAPI = {
  // 获取所有模板
  getAll: () => request.get('/prompt-templates'),
  
  // 获取指定模板
  getByName: (name) => request.get(`/prompt-templates/${name}`),
  
  // 根据分类获取模板
  getByCategory: (category) => request.get(`/prompt-templates/category/${category}`),
  
  // 创建或更新模板
  save: (data) => request.post('/prompt-templates', data),
  
  // 删除模板
  delete: (name) => request.delete(`/prompt-templates/${name}`)
}

// 模型配置管理相关 API
export const modelConfigAPI = {
  // 获取所有模型
  getAll: () => request.get('/model-config'),
  
  // 根据提供商获取模型
  getByProvider: (provider) => request.get(`/model-config/provider/${provider}`),
  
  // 获取指定模型
  getByCode: (modelCode) => request.get(`/model-config/${modelCode}`),
  
  // 创建或更新模型
  save: (data) => request.post('/model-config', data),
  
  // 删除模型
  delete: (modelCode) => request.delete(`/model-config/${modelCode}`)
}

// 对话相关 API
export const chatAPI = {
  // 发送消息
  sendMessage: (data) => request.post('/chat', data)
}

