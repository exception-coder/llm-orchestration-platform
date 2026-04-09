<template>
  <div class="flex flex-col h-full max-w-5xl mx-auto space-y-6">
    
    <!-- 1. 顶部配置区域 (Neomorph Card) -->
    <header class="p-6 neo-convex rounded-3xl flex flex-wrap items-center gap-6">
      <div class="flex items-center gap-3">
        <div class="w-10 h-10 neo-concave rounded-2xl flex items-center justify-center text-primary">
          <Settings :size="20" />
        </div>
        <div>
          <p class="text-[10px] font-bold uppercase tracking-widest text-foreground/40">当前配置</p>
          <p class="text-sm font-semibold truncate">{{ config.model }} / {{ config.provider }}</p>
        </div>
      </div>
      
      <div class="flex-1"></div>
      
      <div class="flex items-center gap-2">
        <button 
          @click="showRawText = !showRawText"
          class="px-4 py-2 rounded-xl text-xs font-bold transition-all"
          :class="showRawText ? 'neo-concave text-primary' : 'neo-convex text-foreground/60'"
        >
          RAW 模式
        </button>
        <button 
          @click="messages = []"
          class="p-2 rounded-xl neo-convex text-red-500/60 hover:text-red-500 transition-all active:scale-95"
        >
          <Trash2 :size="18" />
        </button>
      </div>
    </header>

    <!-- 2. 聊天记录流 (极致间距与动效) -->
    <div 
      ref="messageListRef"
      class="flex-1 overflow-y-auto pr-4 space-y-8 scroll-smooth"
    >
      <div 
        v-for="(msg, index) in messages" 
        :key="index"
        class="flex w-full"
        :class="msg.role === 'user' ? 'justify-end' : 'justify-start'"
      >
        <div 
          class="max-w-[85%] group relative"
          v-motion
          :initial="{ opacity: 0, y: 20, scale: 0.95 }"
          :enter="{ opacity: 1, y: 0, scale: 1 }"
        >
          <!-- 消息头部 -->
          <div 
            class="flex items-center gap-2 mb-2 px-2"
            :class="msg.role === 'user' ? 'flex-row-reverse' : ''"
          >
            <span class="text-[10px] font-black uppercase tracking-tighter opacity-30">
              {{ msg.role === 'user' ? 'YOU' : 'AI ASSISTANT' }}
            </span>
          </div>

          <!-- 消息内容 (物理材质差异) -->
          <div 
            class="p-5 rounded-[2rem] text-sm leading-relaxed"
            :class="[
              msg.role === 'user' 
                ? 'bg-primary text-white shadow-xl shadow-primary/20 rounded-tr-sm' 
                : 'neo-convex border border-white/40 text-foreground/80 rounded-tl-sm'
            ]"
          >
            <div v-if="showRawText" class="whitespace-pre-wrap font-mono">{{ msg.content }}</div>
            <div v-else class="markdown-rendered prose prose-slate dark:prose-invert max-w-none" v-html="renderMarkdown(msg.content)"></div>
          </div>

          <!-- Token 使用统计 -->
          <div 
            v-if="msg.tokenUsage" 
            class="mt-2 px-4 py-1 inline-block neo-concave rounded-full text-[9px] font-bold text-foreground/40"
          >
            ⚡ {{ msg.tokenUsage.totalTokens }} Tokens
          </div>
        </div>
      </div>
      
      <!-- 加载状态 -->
      <div v-if="loading" class="flex justify-start">
        <div class="neo-convex p-4 rounded-full flex gap-1">
          <div class="w-2 h-2 bg-primary rounded-full animate-bounce [animation-delay:-0.3s]"></div>
          <div class="w-2 h-2 bg-primary rounded-full animate-bounce [animation-delay:-0.15s]"></div>
          <div class="w-2 h-2 bg-primary rounded-full animate-bounce"></div>
        </div>
      </div>
    </div>

    <!-- 3. 底部输入框 (Neomorphic Inset) -->
    <footer class="p-4">
      <div class="relative neo-concave rounded-[2.5rem] p-2 flex items-center gap-2 group transition-all focus-within:ring-2 ring-primary/20">
        <textarea
          v-model="input"
          @keydown.enter.prevent="handleSend"
          placeholder="输入消息，开启灵感对话..."
          class="flex-1 bg-transparent border-none focus:outline-none px-6 py-4 text-sm resize-none max-h-32 placeholder:text-foreground/20"
          rows="1"
        ></textarea>
        
        <button 
          @click="handleSend"
          :disabled="!input.trim() || loading"
          class="w-12 h-12 flex items-center justify-center rounded-full transition-all"
          :class="input.trim() ? 'bg-primary text-white shadow-lg active:scale-90' : 'text-foreground/20 cursor-not-allowed'"
        >
          <Send :size="20" />
        </button>
      </div>
    </footer>

  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { Settings, Trash2, Send } from 'lucide-vue-next'
import { modelConfigAPI } from '@/api'
import { useMarkdown } from '@/composables/useMarkdown'
import { useSSEStream } from '@/composables/useSSEStream'

const { renderMarkdown } = useMarkdown()
const { fetchSSE } = useSSEStream()

const config = ref({
  conversationId: 'conv-' + Date.now(),
  model: '',
  provider: ''
})

const input = ref('')
const loading = ref(false)
const showRawText = ref(false)
const messages = ref([])
const messageListRef = ref(null)

const loadModels = async () => {
  try {
    const res = await modelConfigAPI.getModels()
    if (res.length > 0) {
      config.value.model = res[0].modelId
      config.value.provider = res[0].provider
    }
  } catch (err) {
    console.error('Failed to load models:', err)
  }
}

const handleSend = async () => {
  if (!input.value.trim() || loading.value) return
  
  const userContent = input.value.trim()
  messages.value.push({ role: 'user', content: userContent })
  input.value = ''
  loading.value = true

  // AI 占位
  const aiIndex = messages.value.length
  messages.value.push({ role: 'assistant', content: '' })

  try {
    let firstChunk = true
    await fetchSSE('/chat/stream', {
      conversationId: config.value.conversationId,
      message: userContent,
      model: config.value.model,
      provider: config.value.provider
    }, {
      onContent: (content) => {
        if (firstChunk) {
          loading.value = false
          firstChunk = false
        }
        messages.value[aiIndex].content += content
        scrollToBottom()
      },
      onTokenUsage: (usage) => {
        messages.value[aiIndex].tokenUsage = usage
      }
    })
  } catch (err) {
    console.error('Chat error:', err)
  } finally {
    loading.value = false
  }
}

const scrollToBottom = async () => {
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

onMounted(() => {
  loadModels()
})
</script>

<style scoped>
@reference "@/styles/index.css";

/* 针对 Markdown 渲染的 Tailwind 样式微调 */
:deep(.markdown-rendered) {
  @apply text-inherit;
}
:deep(.markdown-rendered pre) {
  @apply bg-black/5 rounded-2xl p-4 my-4 overflow-x-auto;
}
:deep(.markdown-rendered code) {
  @apply bg-primary/10 text-primary px-1 rounded;
}
</style>
