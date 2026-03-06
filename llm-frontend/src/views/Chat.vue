<template>
  <div class="chat">
    <el-card class="chat-card">
      <template #header>
        <div class="card-header">
          <span>对话测试</span>
          <div>
            <el-switch
              v-model="showRawText"
              active-text="原始文本"
              inactive-text="Markdown"
              style="margin-right: 12px"
            />
            <el-button size="small" @click="handleClear">
              <el-icon><Delete /></el-icon>
              清空对话
            </el-button>
          </div>
        </div>
      </template>

      <!-- 对话配置 -->
      <el-form :inline="!isMobile" :model="config" class="config-form" :label-position="isMobile ? 'top' : 'right'">
        <el-form-item label="对话ID">
          <el-input v-model="config.conversationId" placeholder="conversation-001" :style="{ width: isMobile ? '100%' : '200px' }" />
        </el-form-item>
        <el-form-item label="模型">
          <el-select v-model="config.model" placeholder="选择模型" :style="{ width: isMobile ? '100%' : '250px' }" filterable>
            <el-option
              v-for="model in availableModels"
              :key="model.code"
              :label="`${model.name} (${model.provider})`"
              :value="model.code"
            >
              <span style="float: left">{{ model.name }}</span>
              <span style="float: right; color: #8492a6; font-size: 13px" class="hidden-xs-only">{{ model.provider }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="提供商">
          <el-select v-model="config.provider" placeholder="自动选择" :style="{ width: isMobile ? '100%' : '150px' }" filterable>
            <el-option label="自动选择" value="" />
            <el-option
              v-for="provider in providers"
              :key="provider"
              :label="getProviderLabel(provider)"
              :value="provider"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="输出模式">
          <el-switch
            v-model="config.stream"
            active-text="流式"
            inactive-text="非流式"
          />
        </el-form-item>
      </el-form>

      <!-- 消息列表 -->
      <div class="message-list" ref="messageListRef">
        <div v-if="messages.length === 0" class="empty-messages">
          <el-empty description="暂无对话消息，开始聊天吧" />
        </div>
        
        <div
          v-for="(msg, index) in messages"
          :key="index"
          :class="['message-item', msg.role]"
        >
          <div class="message-avatar">
            <el-avatar v-if="msg.role === 'user'" :size="36">
              <el-icon><User /></el-icon>
            </el-avatar>
            <el-avatar v-else :size="36" style="background: #67c23a">
              <el-icon><Cpu /></el-icon>
            </el-avatar>
          </div>
          <div class="message-content">
            <div class="message-header">
              <span class="message-role">{{ msg.role === 'user' ? '用户' : 'AI助手' }}</span>
              <span class="message-time">{{ msg.time }}</span>
            </div>
            <!-- 如果是 AI 消息且正在加载且内容为空，显示"正在思考..." -->
            <div v-if="msg.role === 'assistant' && loading && !msg.content" class="message-text">
              <el-icon class="is-loading"><Loading /></el-icon>
              正在思考...
            </div>
            <!-- 根据开关显示原始文本或 Markdown 渲染 -->
            <div v-else-if="showRawText" class="message-text raw-text">{{ msg.content }}</div>
            <div v-else class="message-text" v-html="renderMarkdown(msg.content)"></div>
            <div v-if="msg.tokenUsage" class="message-meta">
              <el-tag size="small" type="info">
                Tokens: {{ msg.tokenUsage.totalTokens }}
              </el-tag>
            </div>
          </div>
        </div>
      </div>

      <!-- 输入框 -->
      <div class="input-area">
        <el-input
          v-model="inputMessage"
          type="textarea"
          :rows="3"
          placeholder="输入消息，按 Ctrl+Enter 发送"
          @keydown.ctrl.enter="handleSend"
        />
        <div class="input-actions">
          <el-button type="primary" @click="handleSend" :loading="loading" :disabled="!inputMessage.trim()">
            <el-icon><Promotion /></el-icon>
            发送
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { chatAPI, modelConfigAPI } from '@/api'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'

const isMobile = ref(false)

// 配置 marked
marked.setOptions({
  highlight: function(code, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(code, { language: lang }).value
      } catch (err) {
        console.error('Highlight error:', err)
      }
    }
    return hljs.highlightAuto(code).value
  },
  breaks: true,
  gfm: true
})

const config = ref({
  conversationId: 'conversation-001',
  model: '',
  provider: '',
  stream: true  // 默认使用流式输出
})

const messages = ref([])
const inputMessage = ref('')
const loading = ref(false)
const messageListRef = ref(null)
const availableModels = ref([])
const showRawText = ref(false) // 是否显示原始文本

// 动态提取提供商列表
const providers = computed(() => {
  const providerSet = new Set()
  availableModels.value.forEach(model => {
    if (model.provider) {
      providerSet.add(model.provider)
    }
  })
  return Array.from(providerSet).sort()
})

// 获取提供商显示名称
const getProviderLabel = (provider) => {
  const labels = {
    openai: 'OpenAI',
    deepseek: 'DeepSeek',
    ollama: 'Ollama'
  }
  return labels[provider] || provider.charAt(0).toUpperCase() + provider.slice(1)
}

// 渲染 Markdown
const renderMarkdown = (content) => {
  if (!content) return ''
  try {
    return marked.parse(content)
  } catch (error) {
    console.error('Markdown render error:', error)
    return content
  }
}

// 检测是否为移动端
const checkMobile = () => {
  isMobile.value = window.innerWidth <= 768
}

onMounted(async () => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
  await loadModels()
})

// 加载可用模型列表
const loadModels = async () => {
  try {
    const models = await modelConfigAPI.getAll()
    availableModels.value = models.map(m => ({
      code: m.modelCode,
      name: m.modelName,
      provider: m.provider,
      description: m.description
    }))
    
    // 设置默认模型（第一个模型）
    if (availableModels.value.length > 0 && !config.value.model) {
      config.value.model = availableModels.value[0].code
    }
  } catch (error) {
    console.error('加载模型列表失败:', error)
    ElMessage.error('加载模型列表失败')
  }
}

// 发送消息
const handleSend = async () => {
  if (!inputMessage.value.trim()) return

  const userMessage = inputMessage.value.trim()
  const timestamp = new Date().toLocaleTimeString()

  // 添加用户消息
  messages.value.push({
    role: 'user',
    content: userMessage,
    time: timestamp
  })

  inputMessage.value = ''
  loading.value = true

  // 滚动到底部
  await nextTick()
  scrollToBottom()

  // 根据配置选择流式或非流式
  if (config.value.stream) {
    await handleStreamSend(userMessage)
  } else {
    await handleNonStreamSend(userMessage)
  }
}

// 流式发送
const handleStreamSend = async (userMessage) => {
  // 创建 AI 消息占位符
  const aiMessageIndex = messages.value.length
  messages.value.push({
    role: 'assistant',
    content: '',
    time: new Date().toLocaleTimeString(),
    tokenUsage: null
  })

  try {
    // 构建请求 URL - 使用流式接口
    const baseURL = import.meta.env.VITE_API_BASE_URL || '/api/v1'
    const url = `${baseURL}/chat/stream`
    
    // 使用 fetch 进行流式请求
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        conversationId: config.value.conversationId,
        message: userMessage,
        model: config.value.model,
        provider: config.value.provider || undefined
      })
    })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let isFirstChunk = true
    let buffer = ''  // 添加缓冲区处理不完整的数据

    // 读取流式数据
    while (true) {
      const { done, value } = await reader.read()
      
      if (done) {
        console.log('Stream completed')
        break
      }

      // 解码数据并添加到缓冲区
      buffer += decoder.decode(value, { stream: true })
      
      // 按行分割
      const lines = buffer.split('\n')
      
      // 保留最后一个不完整的行
      buffer = lines.pop() || ''

      for (const line of lines) {
        console.log('Received line:', line)  // 添加调试日志
        
        // 兼容 "data:" 和 "data: " 两种格式
        if (line.startsWith('data:')) {
          const data = line.slice(5).trim()  // 移除 "data:" 并去除空格
          
          if (data === '[DONE]') {
            console.log('Received [DONE] signal')
            continue
          }

          if (!data) continue

          try {
            const parsed = JSON.parse(data)
            console.log('Parsed data:', parsed)
            
            if (parsed.content) {
              // 第一次收到内容时，关闭 loading 状态
              if (isFirstChunk) {
                loading.value = false
                isFirstChunk = false
              }
              
              // 追加内容到消息
              messages.value[aiMessageIndex].content += parsed.content
              
              // 滚动到底部
              await nextTick()
              scrollToBottom()
            }
            
            // 更新 token 使用情况
            if (parsed.tokenUsage) {
              messages.value[aiMessageIndex].tokenUsage = parsed.tokenUsage
            }
          } catch (e) {
            console.error('Parse error:', e, 'Data:', data)
          }
        }
      }
    }

  } catch (error) {
    console.error('流式发送失败:', error)
    ElMessage.error('发送失败，请重试')
    // 移除失败的消息
    messages.value.splice(aiMessageIndex, 1)
  } finally {
    loading.value = false
  }
}

// 非流式发送
const handleNonStreamSend = async (userMessage) => {
  // 创建 AI 消息占位符
  const aiMessageIndex = messages.value.length
  messages.value.push({
    role: 'assistant',
    content: '',
    time: new Date().toLocaleTimeString(),
    tokenUsage: null
  })

  try {
    // 构建请求 URL - 使用非流式接口
    const baseURL = import.meta.env.VITE_API_BASE_URL || '/api/v1'
    const url = `${baseURL}/chat`
    
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        conversationId: config.value.conversationId,
        message: userMessage,
        model: config.value.model,
        provider: config.value.provider || undefined
      })
    })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    // 读取完整响应
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
    }

    // 解析响应
    const lines = buffer.split('\n')
    for (const line of lines) {
      // 兼容 "data:" 和 "data: " 两种格式
      if (line.startsWith('data:')) {
        const data = line.slice(5).trim()  // 移除 "data:" 并去除空格
        if (data === '[DONE]') continue
        if (!data) continue

        try {
          const parsed = JSON.parse(data)
          if (parsed.content) {
            messages.value[aiMessageIndex].content = parsed.content
          }
          if (parsed.tokenUsage) {
            messages.value[aiMessageIndex].tokenUsage = parsed.tokenUsage
          }
        } catch (e) {
          console.error('Parse error:', e)
        }
      }
    }

    loading.value = false
    await nextTick()
    scrollToBottom()

  } catch (error) {
    console.error('非流式发送失败:', error)
    ElMessage.error('发送失败，请重试')
    messages.value.splice(aiMessageIndex, 1)
  } finally {
    loading.value = false
  }
}

// 清空对话
const handleClear = () => {
  messages.value = []
  ElMessage.success('已清空对话')
}

// 滚动到底部
const scrollToBottom = () => {
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}
</script>

<style scoped>
.chat {
  max-width: 1200px;
  margin: 0 auto;
}

.chat-card {
  height: calc(100vh - 180px);
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.config-form {
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px 0;
  min-height: 400px;
}

.empty-messages {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.message-item {
  display: flex;
  margin-bottom: 24px;
  animation: fadeIn 0.3s;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message-item.user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
  margin: 0 12px;
}

.message-content {
  max-width: 70%;
  background: #f5f7fa;
  padding: 12px 16px;
  border-radius: 8px;
}

.message-item.user .message-content {
  background: #409eff;
  color: #fff;
}

.message-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 12px;
  opacity: 0.8;
}

.message-text {
  line-height: 1.6;
  word-break: break-word;
}

.raw-text {
  white-space: pre-wrap;
  font-family: 'Courier New', monospace;
  font-size: 14px;
}

/* Markdown 样式 */
.message-text :deep(h1),
.message-text :deep(h2),
.message-text :deep(h3),
.message-text :deep(h4),
.message-text :deep(h5),
.message-text :deep(h6) {
  margin: 16px 0 8px 0;
  font-weight: 600;
}

.message-text :deep(h1) { font-size: 1.8em; }
.message-text :deep(h2) { font-size: 1.5em; }
.message-text :deep(h3) { font-size: 1.3em; }

.message-text :deep(p) {
  margin: 8px 0;
}

.message-text :deep(ul),
.message-text :deep(ol) {
  margin: 8px 0;
  padding-left: 24px;
}

.message-text :deep(li) {
  margin: 4px 0;
}

.message-text :deep(code) {
  background: rgba(0, 0, 0, 0.05);
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
  font-size: 0.9em;
}

.message-item.user .message-text :deep(code) {
  background: rgba(255, 255, 255, 0.2);
}

.message-text :deep(pre) {
  background: #282c34;
  color: #abb2bf;
  padding: 12px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 12px 0;
}

.message-text :deep(pre code) {
  background: none;
  padding: 0;
  color: inherit;
}

.message-text :deep(blockquote) {
  border-left: 4px solid #ddd;
  padding-left: 12px;
  margin: 12px 0;
  color: #666;
}

.message-item.user .message-text :deep(blockquote) {
  border-left-color: rgba(255, 255, 255, 0.5);
  color: rgba(255, 255, 255, 0.9);
}

.message-text :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 12px 0;
}

.message-text :deep(th),
.message-text :deep(td) {
  border: 1px solid #ddd;
  padding: 8px;
  text-align: left;
}

.message-text :deep(th) {
  background: #f5f7fa;
  font-weight: 600;
}

.message-text :deep(a) {
  color: #409eff;
  text-decoration: none;
}

.message-text :deep(a:hover) {
  text-decoration: underline;
}

.message-item.user .message-text :deep(a) {
  color: #fff;
  text-decoration: underline;
}

.message-meta {
  margin-top: 8px;
}

.input-area {
  border-top: 1px solid #f0f0f0;
  padding-top: 16px;
}

.input-actions {
  margin-top: 12px;
  text-align: right;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .config-form {
    padding-bottom: 12px;
  }

  .config-form :deep(.el-form-item) {
    display: block;
    margin-bottom: 12px;
  }

  .config-form :deep(.el-form-item__label) {
    display: block;
    text-align: left;
    margin-bottom: 4px;
  }

  .config-form :deep(.el-form-item__content) {
    margin-left: 0 !important;
  }

  .hidden-xs-only {
    display: none;
  }

  .message-content {
    max-width: 85%;
  }

  .message-avatar {
    margin: 0 8px;
  }

  .message-avatar :deep(.el-avatar) {
    width: 32px;
    height: 32px;
  }

  .input-area :deep(.el-textarea__inner) {
    font-size: 14px;
  }
}
</style>

