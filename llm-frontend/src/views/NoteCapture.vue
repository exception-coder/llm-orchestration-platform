<template>
  <div class="note-capture">
    <!-- 顶部输入区 -->
    <el-card class="input-card" shadow="never">
      <div class="input-area">
        <el-input
          v-model="inputText"
          type="textarea"
          :rows="3"
          placeholder="输入你想记录的内容，支持语音输入..."
          :disabled="loading"
          @keydown.ctrl.enter="handleCapture"
          @keydown.meta.enter="handleCapture"
        />
        <div class="input-actions">
          <div class="voice-btn-wrapper">
            <el-button
              :type="isRecording ? 'danger' : 'default'"
              :icon="isRecording ? 'VideoPause' : 'Microphone'"
              circle
              @click="toggleVoice"
              :disabled="loading"
              :title="isRecording ? '停止录音' : '开始语音输入'"
            />
            <span v-if="isRecording" class="recording-indicator">
              <span class="dot"></span> 录音中
            </span>
          </div>
          <el-button
            type="primary"
            @click="handleCapture"
            :loading="loading"
            :disabled="!inputText.trim()"
          >
            记录
          </el-button>
        </div>
      </div>
    </el-card>

    <div class="content-wrapper">
      <!-- 左侧类目导航 -->
      <el-card class="category-card" shadow="never" :body-style="{ padding: '12px' }">
        <div class="category-header">
          <span>类目</span>
          <el-button text size="small" @click="selectedCategory = null">
            全部
          </el-button>
        </div>
        <div class="category-list">
          <div
            v-for="cat in categories"
            :key="cat.id"
            :class="['category-item', { active: selectedCategory === cat.id }]"
            @click="selectCategory(cat.id)"
          >
            <span class="cat-icon">{{ cat.icon || '📂' }}</span>
            <span class="cat-name">{{ cat.name }}</span>
            <span class="cat-count">{{ cat.noteCount }}</span>
          </div>
          <el-empty v-if="categories.length === 0" description="暂无类目" :image-size="60" />
        </div>
      </el-card>

      <!-- 右侧记录列表 -->
      <div class="notes-list">
        <div class="notes-header">
          <span>{{ filteredNotes.length }} 条记录</span>
          <el-input
            v-model="searchKeyword"
            placeholder="搜索记录..."
            prefix-icon="Search"
            clearable
            style="width: 200px"
          />
        </div>

        <div v-if="loading" class="loading-state">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>AI 正在分析内容...</span>
        </div>

        <div v-else-if="filteredNotes.length === 0" class="empty-state">
          <el-empty :description="searchKeyword ? '没有找到匹配的记录' : '还没有记录，随口说点什么吧~'" />
        </div>

        <div v-else class="notes-grid">
          <el-card
            v-for="note in filteredNotes"
            :key="note.id"
            class="note-card"
            shadow="hover"
            @click="openNoteDetail(note)"
          >
            <div class="note-header">
              <span class="note-icon">{{ note.categoryIcon || '📝' }}</span>
              <span class="note-category">{{ note.categoryName }}</span>
              <span v-if="note.isEncrypted" class="encrypted-badge">🔒</span>
              <span v-if="note.isVoice" class="voice-badge">🎤</span>
            </div>
            <h4 class="note-title">{{ note.title }}</h4>
            <p class="note-summary">{{ note.summary }}</p>
            <div class="note-footer">
              <div class="note-tags">
                <el-tag v-for="tag in (note.tags || []).slice(0, 3)" :key="tag" size="small">
                  {{ tag }}
                </el-tag>
              </div>
              <span class="note-time">{{ formatTime(note.createdAt) }}</span>
            </div>
          </el-card>
        </div>
      </div>
    </div>

    <!-- 记录详情抽屉 -->
    <el-drawer v-model="drawerVisible" :title="currentNote?.title" size="50%" direction="rtl">
      <template v-if="currentNote">
        <div class="note-detail">
          <div class="detail-meta">
            <el-tag>{{ currentNote.categoryName }}</el-tag>
            <span v-if="currentNote.isEncrypted" class="encrypted-tip">🔒 已加密</span>
            <span v-if="currentNote.isVoice" class="voice-tip">🎤 语音输入</span>
            <span class="detail-time">{{ formatTime(currentNote.createdAt) }}</span>
          </div>

          <div v-if="currentNote.rawInput" class="detail-section">
            <h5>原始输入</h5>
            <el-input type="textarea" :rows="2" :model-value="currentNote.rawInput" readonly />
          </div>

          <div v-if="currentNote.summary" class="detail-section">
            <h5>摘要</h5>
            <p>{{ currentNote.summary }}</p>
          </div>

          <div class="detail-section">
            <h5>内容</h5>
            <div v-if="currentNote.isEncrypted" class="encrypted-content">
              <el-input
                v-model="decryptPassword"
                type="password"
                placeholder="输入解密密钥"
                show-password
              />
              <el-button type="primary" @click="handleDecrypt" :loading="decrypting">
                解密查看
              </el-button>
              <p v-if="decryptError" class="decrypt-error">{{ decryptError }}</p>
              <div v-if="decryptedContent" class="decrypted-content" v-html="renderMarkdown(decryptedContent)"></div>
            </div>
            <div v-else class="content-text" v-html="renderMarkdown(currentNote.content)"></div>
          </div>

          <div v-if="currentNote.tags?.length" class="detail-section">
            <h5>标签</h5>
            <el-tag v-for="tag in currentNote.tags" :key="tag">{{ tag }}</el-tag>
          </div>
        </div>

        <template #footer>
          <el-button type="danger" @click="handleDelete">删除</el-button>
        </template>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { marked } from 'marked'
import hljs from 'highlight.js'
import { noteAPI } from '@/api'
import { encrypt, decrypt } from '@/utils/crypto'

// 响应式数据
const inputText = ref('')
const loading = ref(false)
const categories = ref([])
const notes = ref([])
const selectedCategory = ref(null)
const searchKeyword = ref('')
const drawerVisible = ref(false)
const currentNote = ref(null)
const isRecording = ref(false)
const decryptPassword = ref('')
const decryptedContent = ref('')
const decryptError = ref('')
const decrypting = ref(false)

// 语音识别
let recognition = null

// 计算属性
const filteredNotes = computed(() => {
  let result = notes.value
  if (selectedCategory.value) {
    result = result.filter(n => n.categoryId === selectedCategory.value)
  }
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    result = result.filter(n =>
      n.title?.toLowerCase().includes(kw) ||
      n.summary?.toLowerCase().includes(kw) ||
      n.content?.toLowerCase().includes(kw)
    )
  }
  return result
})

// 初始化
onMounted(async () => {
  await loadCategories()
  await loadNotes()
  initVoiceRecognition()
})

onUnmounted(() => {
  if (recognition) {
    recognition.stop()
  }
})

// 加载类目
async function loadCategories() {
  try {
    const res = await noteAPI.getCategories()
    categories.value = res.data || []
  } catch (e) {
    console.error('加载类目失败', e)
  }
}

// 加载记录
async function loadNotes() {
  try {
    const res = await noteAPI.getAll()
    notes.value = res.data || []
  } catch (e) {
    console.error('加载记录失败', e)
  }
}

// 捕获记录
async function handleCapture() {
  if (!inputText.value.trim()) return

  loading.value = true
  try {
    const res = await noteAPI.capture({
      content: inputText.value,
      isVoice: isRecording.value
    })

    notes.value.unshift(res.data)
    await loadCategories()
    inputText.value = ''

    ElMessage.success('记录已保存')
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

// 选择类目
function selectCategory(id) {
  selectedCategory.value = selectedCategory.value === id ? null : id
}

// 打开记录详情
function openNoteDetail(note) {
  currentNote.value = note
  decryptPassword.value = ''
  decryptedContent.value = ''
  decryptError.value = ''
  drawerVisible.value = true
}

// 解密内容
async function handleDecrypt() {
  if (!decryptPassword.value) {
    decryptError.value = '请输入解密密钥'
    return
  }
  decrypting.value = true
  decryptError.value = ''
  try {
    decryptedContent.value = await decrypt(currentNote.value.content, decryptPassword.value)
  } catch (e) {
    decryptError.value = e.message || '解密失败'
    decryptedContent.value = ''
  } finally {
    decrypting.value = false
  }
}

// 删除记录
async function handleDelete() {
  try {
    await ElMessageBox.confirm('确定删除这条记录吗？', '确认删除', {
      type: 'warning'
    })
    await noteAPI.delete(currentNote.value.id)
    notes.value = notes.value.filter(n => n.id !== currentNote.value.id)
    await loadCategories()
    drawerVisible.value = false
    ElMessage.success('已删除')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 格式化时间
function formatTime(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now - date

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)} 天前`

  return date.toLocaleDateString('zh-CN')
}

// 渲染 Markdown
function renderMarkdown(text) {
  if (!text) return ''
  marked.setOptions({
    highlight: (code, lang) => {
      if (lang && hljs.getLanguage(lang)) {
        return hljs.highlight(code, { language: lang }).value
      }
      return hljs.highlightAuto(code).value
    },
    breaks: true,
    gfm: true
  })
  return marked.parse(text)
}

// 语音识别
function initVoiceRecognition() {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
  if (!SpeechRecognition) {
    console.warn('当前浏览器不支持语音识别')
    return
  }

  recognition = new SpeechRecognition()
  recognition.continuous = true
  recognition.interimResults = true
  recognition.lang = 'zh-CN'

  recognition.onresult = (event) => {
    let transcript = ''
    for (let i = 0; i < event.results.length; i++) {
      transcript += event.results[i][0].transcript
    }
    inputText.value = transcript
  }

  recognition.onerror = (event) => {
    console.error('语音识别错误', event.error)
    isRecording.value = false
    if (event.error !== 'no-speech') {
      ElMessage.warning('语音识别出错: ' + event.error)
    }
  }

  recognition.onend = () => {
    isRecording.value = false
  }
}

function toggleVoice() {
  if (!recognition) {
    ElMessage.warning('当前浏览器不支持语音识别')
    return
  }

  if (isRecording.value) {
    recognition.stop()
    isRecording.value = false
  } else {
    inputText.value = ''
    recognition.start()
    isRecording.value = true
  }
}
</script>

<style scoped>
.note-capture {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.input-card {
  flex-shrink: 0;
}

.input-area {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.voice-btn-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
}

.recording-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #f56c6c;
  font-size: 14px;
}

.recording-indicator .dot {
  width: 8px;
  height: 8px;
  background: #f56c6c;
  border-radius: 50%;
  animation: pulse 1s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.content-wrapper {
  flex: 1;
  display: flex;
  gap: 16px;
  min-height: 0;
}

.category-card {
  width: 200px;
  flex-shrink: 0;
  overflow-y: auto;
}

.category-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-weight: 500;
  color: #333;
}

.category-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.category-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}

.category-item:hover {
  background: #f5f7fa;
}

.category-item.active {
  background: #e6f7ff;
  color: #1890ff;
}

.cat-icon {
  font-size: 16px;
}

.cat-name {
  flex: 1;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cat-count {
  font-size: 12px;
  color: #999;
  background: #f0f0f0;
  padding: 2px 6px;
  border-radius: 10px;
}

.notes-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.notes-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  color: #666;
  font-size: 14px;
}

.loading-state,
.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #999;
}

.notes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
  overflow-y: auto;
  flex: 1;
}

.note-card {
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.note-card:hover {
  transform: translateY(-2px);
}

.note-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  font-size: 12px;
}

.note-icon {
  font-size: 14px;
}

.note-category {
  color: #1890ff;
}

.encrypted-badge,
.voice-badge {
  font-size: 12px;
}

.note-title {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 500;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.note-summary {
  margin: 0 0 12px;
  font-size: 13px;
  color: #666;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.note-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.note-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.note-time {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}

/* 抽屉样式 */
.note-detail {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.encrypted-tip,
.voice-tip {
  font-size: 13px;
  color: #666;
}

.detail-time {
  font-size: 13px;
  color: #999;
  margin-left: auto;
}

.detail-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-section h5 {
  margin: 0;
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.detail-section p {
  margin: 0;
  color: #666;
  line-height: 1.6;
}

.encrypted-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.decrypt-error {
  color: #f56c6c;
  font-size: 13px;
  margin: 0;
}

.decrypted-content {
  background: #f5f7fa;
  padding: 16px;
  border-radius: 8px;
  line-height: 1.8;
}

.content-text {
  background: #f5f7fa;
  padding: 16px;
  border-radius: 8px;
  line-height: 1.8;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .content-wrapper {
    flex-direction: column;
  }

  .category-card {
    width: 100%;
    max-height: 150px;
  }

  .notes-grid {
    grid-template-columns: 1fr;
  }
}
</style>
