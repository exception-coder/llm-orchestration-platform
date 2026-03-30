<template>
  <div class="secretary">
    <el-container class="secretary-container">
      <!-- 左侧工具面板 -->
      <el-aside width="260px" class="tool-panel">
        <div class="panel-section">
          <div class="panel-title">🔧 工具状态</div>
          <el-skeleton v-if="toolsLoading" :rows="4" animated />
          <div v-else>
            <div v-for="tool in tools" :key="tool.id" class="tool-item">
              <el-tag type="success" size="small">已启用</el-tag>
              <span class="tool-name">{{ tool.name }}</span>
              <el-tooltip :content="tool.description" placement="right">
                <el-icon class="tool-info"><InfoFilled /></el-icon>
              </el-tooltip>
            </div>
            <div v-if="tools.length === 0" class="no-tools">暂无已启用工具</div>
          </div>
        </div>

        <div class="panel-section">
          <div class="panel-title">🧠 长期记忆</div>
          <el-skeleton v-if="memoryLoading" :rows="3" animated />
          <div v-else>
            <div v-for="mem in memories" :key="mem.id" class="memory-item">
              <el-tag :type="memoryTagType(mem.type)" size="small">{{ mem.type }}</el-tag>
              <span class="memory-content">{{ mem.content }}</span>
            </div>
            <div v-if="memories.length === 0" class="no-memory">暂无长期记忆</div>
          </div>
          <el-button size="small" type="danger" plain @click="handleClearMemory" style="margin-top:8px;width:100%">
            清除记忆
          </el-button>
        </div>
      </el-aside>

      <!-- 右侧对话区 -->
      <el-main class="chat-main">
        <el-card class="chat-card">
          <template #header>
            <div class="card-header">
              <span>🤖 个人秘书</span>
              <el-button size="small" @click="handleClearChat">
                <el-icon><Delete /></el-icon>
                清空对话
              </el-button>
            </div>
          </template>

          <!-- 消息列表 -->
          <div class="message-list" ref="messageListRef">
            <div v-if="messages.length === 0" class="empty-messages">
              <el-empty description="你好！我是你的个人秘书，有什么可以帮你的？" />
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
                <el-avatar v-else :size="36" style="background: #409eff">
                  <el-icon><Service /></el-icon>
                </el-avatar>
              </div>
              <div class="message-content">
                <div class="message-meta">
                  <span class="role-label">{{ msg.role === 'user' ? '你' : '秘书' }}</span>
                  <span class="time">{{ msg.time }}</span>
                </div>
                <div class="message-bubble">
                  <span v-if="msg.role === 'assistant' && msg.loading" class="typing">
                    <span></span><span></span><span></span>
                  </span>
                  <span v-else>{{ msg.content }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 输入区 -->
          <div class="input-area">
            <el-input
              v-model="inputText"
              type="textarea"
              :rows="3"
              placeholder="输入消息，Ctrl+Enter 发送..."
              @keydown.ctrl.enter="handleSend"
              :disabled="loading"
            />
            <div class="input-actions">
              <span class="hint">Ctrl + Enter 发送</span>
              <el-button type="primary" @click="handleSend" :loading="loading" :disabled="!inputText.trim()">
                发送
              </el-button>
            </div>
          </div>
        </el-card>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { secretaryAPI } from '@/api/index.js'

const messages = ref([])
const inputText = ref('')
const loading = ref(false)
const tools = ref([])
const memories = ref([])
const toolsLoading = ref(true)
const memoryLoading = ref(true)
const messageListRef = ref(null)
const sessionId = ref(`session-${Date.now()}`)

onMounted(() => {
  loadTools()
  loadMemory()
})

async function loadTools() {
  try {
    tools.value = await secretaryAPI.getTools()
  } catch (e) {
    ElMessage.error('获取工具列表失败')
  } finally {
    toolsLoading.value = false
  }
}

async function loadMemory() {
  try {
    memories.value = await secretaryAPI.getMemory()
  } catch (e) {
    ElMessage.error('获取记忆失败')
  } finally {
    memoryLoading.value = false
  }
}

async function handleSend() {
  const text = inputText.value.trim()
  if (!text || loading.value) return

  messages.value.push({
    role: 'user',
    content: text,
    time: new Date().toLocaleTimeString()
  })
  inputText.value = ''
  loading.value = true
  scrollToBottom()

  const assistantMsg = {
    role: 'assistant',
    content: '',
    loading: true,
    time: new Date().toLocaleTimeString()
  }
  messages.value.push(assistantMsg)

  try {
    const baseURL = import.meta.env.VITE_API_BASE_URL || '/api/v1'
    const response = await fetch(`${baseURL}/secretary/chat/stream`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message: text, sessionId: sessionId.value })
    })

    if (!response.ok) throw new Error(`HTTP ${response.status}`)

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    assistantMsg.loading = false

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''
      for (const line of lines) {
        if (!line.startsWith('data:')) continue
        const data = line.slice(5).trim()
        if (data === '[DONE]') continue
        if (!data) continue
        try {
          const parsed = JSON.parse(data)
          if (parsed.content) {
            assistantMsg.content += parsed.content
            scrollToBottom()
          }
        } catch {}
      }
    }

    // 对话完成后刷新记忆
    loadMemory()
  } catch (e) {
    assistantMsg.loading = false
    assistantMsg.content = '请求失败：' + e.message
    ElMessage.error('对话请求失败')
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

function handleClearChat() {
  messages.value = []
  sessionId.value = `session-${Date.now()}`
}

async function handleClearMemory() {
  try {
    await ElMessageBox.confirm('确定清除所有长期记忆吗？', '提示', { type: 'warning' })
    await secretaryAPI.clearMemory()
    memories.value = []
    ElMessage.success('记忆已清除')
  } catch {}
}

function memoryTagType(type) {
  const map = { PREFERENCE: 'primary', SUMMARY: 'success', PROFILE: 'warning' }
  return map[type] || 'info'
}

function scrollToBottom() {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}
</script>

<style scoped>
.secretary {
  height: 100%;
  padding: 16px;
  box-sizing: border-box;
}
.secretary-container {
  height: 100%;
  gap: 16px;
}
.tool-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow-y: auto;
}
.panel-section {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  border: 1px solid #e4e7ed;
}
.panel-title {
  font-weight: 600;
  margin-bottom: 12px;
  color: #303133;
}
.tool-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  border-bottom: 1px solid #f0f0f0;
}
.tool-item:last-child { border-bottom: none; }
.tool-name { flex: 1; font-size: 13px; }
.tool-info { color: #909399; cursor: pointer; }
.no-tools, .no-memory { color: #909399; font-size: 13px; text-align: center; padding: 8px 0; }
.memory-item {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  padding: 6px 0;
  border-bottom: 1px solid #f0f0f0;
}
.memory-item:last-child { border-bottom: none; }
.memory-content { font-size: 12px; color: #606266; line-height: 1.4; flex: 1; }
.chat-main { padding: 0; }
.chat-card { height: 100%; display: flex; flex-direction: column; }
.chat-card :deep(.el-card__body) { flex: 1; display: flex; flex-direction: column; padding: 16px; overflow: hidden; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
  margin-bottom: 16px;
}
.empty-messages { display: flex; justify-content: center; align-items: center; height: 200px; }
.message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}
.message-item.user { flex-direction: row-reverse; }
.message-meta {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 4px;
}
.message-item.user .message-meta { flex-direction: row-reverse; }
.role-label { font-size: 12px; font-weight: 600; color: #303133; }
.time { font-size: 11px; color: #909399; }
.message-bubble {
  background: #f4f4f5;
  border-radius: 8px;
  padding: 10px 14px;
  max-width: 600px;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
.message-item.user .message-bubble {
  background: var(--el-color-primary-light-8);
}
.typing span {
  display: inline-block;
  width: 6px;
  height: 6px;
  background: #909399;
  border-radius: 50%;
  margin: 0 2px;
  animation: bounce 1.2s infinite;
}
.typing span:nth-child(2) { animation-delay: 0.2s; }
.typing span:nth-child(3) { animation-delay: 0.4s; }
@keyframes bounce {
  0%, 80%, 100% { transform: scale(0.6); }
  40% { transform: scale(1); }
}
.input-area { border-top: 1px solid #e4e7ed; padding-top: 12px; }
.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}
.hint { font-size: 12px; color: #909399; }
</style>
