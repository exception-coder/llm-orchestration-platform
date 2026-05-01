import request from '@/utils/request'

// Prompt 测试相关 API
export const promptTestAPI = {
  test: (data: any) => request.post('/prompt-test', data),
  getModels: () => request.get('/prompt-test/models'),
  getTemplateVariables: (templateName: string) => 
    request.get(`/prompt-test/template-variables/${templateName}`)
}

// 内容优化相关 API
export const contentOptimizationAPI = {
  optimize: (data: any) => request.post('/content/optimize', data),
  getPlatforms: () => request.get('/content/platforms'),
  getStyles: () => request.get('/content/styles'),
  getContentTypes: () => request.get('/content/content-types')
}

// 模板管理相关 API
export const templateAPI = {
  getAll: () => request.get('/prompt-templates'),
  getByName: (name: string) => request.get(`/prompt-templates/${name}`),
  getByCategory: (category: string) => request.get(`/prompt-templates/category/${category}`),
  save: (data: any) => request.post('/prompt-templates', data),
  delete: (name: string) => request.delete(`/prompt-templates/${name}`)
}

// 模型配置管理相关 API
export const modelConfigAPI = {
  getAll: () => request.get('/model-config'),
  getByProvider: (provider: string) => request.get(`/model-config/provider/${provider}`),
  getByCode: (modelCode: string) => request.get(`/model-config/${modelCode}`),
  save: (data: any) => request.post('/model-config', data),
  delete: (modelCode: string) => request.delete(`/model-config/${modelCode}`)
}

// 对话相关 API
export const chatAPI = {
  sendMessage: (data: any) => request.post('/chat', data)
}

// 智能碎片记录 API
export const noteAPI = {
  capture: (data: any) => request.post('/notes', data),
  saveEncrypted: (data: any) => request.post('/notes/encrypted', data),
  getAll: (params?: any) => request.get('/notes', { params }),
  getByCategory: (categoryId: string | number) => request.get('/notes', { params: { categoryId } }),
  getById: (id: string | number) => request.get(`/notes/${id}`),
  search: (keyword: string) => request.get('/notes/search', { params: { keyword } }),
  delete: (id: string | number) => request.delete(`/notes/${id}`),
  getCategories: () => request.get('/notes/categories'),
  deleteCategory: (id: string | number) => request.delete(`/notes/categories/${id}`)
}

export const docViewerAPI = {
  getTree: () => request.get('/docs/tree'),
  getContent: (path: string) => request.get('/docs/content', { params: { path } }),
  search: (keyword: string, topK = 5) => request.get('/docs/search', { params: { keyword, topK } }),
  indexDocs: () => request.post('/docs/index')
}

// Agent 管理 API
export const agentAPI = {
  getAll: () => request.get('/agents'),
  getById: (id: string | number) => request.get(`/agents/${id}`),
  save: (data: any) => request.post('/agents', data),
  delete: (id: string | number) => request.delete(`/agents/${id}`),
  execute: (id: string | number, data: any) => request.post(`/agents/${id}/execute`, data),
  getTools: (id: string | number) => request.get(`/agents/${id}/tools`),
  getTrace: (traceId: string) => request.get(`/agents/traces/${traceId}`),
  getAgentTraces: (agentId: string | number, limit = 20) => request.get(`/agents/${agentId}/traces`, { params: { limit } }),
  getRecentTraces: (limit = 20) => request.get('/agents/traces', { params: { limit } })
}

// Graph 编排 API
export const graphAPI = {
  getAll: () => request.get('/graphs'),
  getById: (id: string | number) => request.get(`/graphs/${id}`),
  save: (data: any) => request.post('/graphs', data),
  delete: (id: string | number) => request.delete(`/graphs/${id}`),
  execute: (id: string | number, data: any) => request.post(`/graphs/${id}/execute`, data),
  getAgents: (id: string | number) => request.get(`/graphs/${id}/agents`),
  getCallChain: (id: string | number) => request.get(`/graphs/${id}/call-chain`)
}

// Tool 管理 API
export const toolAPI = {
  getAll: () => request.get('/tools'),
  getById: (id: string | number) => request.get(`/tools/${id}`),
  execute: (id: string | number, params: any) => request.post(`/tools/${id}/execute`, params),
  delete: (id: string | number) => request.delete(`/tools/${id}`)
}

export const secretaryAPI = {
  chat: (data: any) => request.post('/secretary/chat', data),
  getTools: () => request.get('/secretary/tools'),
  getMemory: () => request.get('/secretary/memory'),
  saveMemory: (type: string, content: any) => request.post('/secretary/memory', { type, content }),
  clearMemory: () => request.delete('/secretary/memory')
}
