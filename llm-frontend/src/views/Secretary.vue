<template>
  <div class="h-full flex flex-col lg:flex-row gap-10 overflow-hidden max-w-7xl mx-auto">
    
    <!-- 左侧：助理状态与记忆 (Physical Status Column) -->
    <aside class="w-full lg:w-72 flex flex-col shrink-0 gap-8">
      <!-- 助理身份卡 -->
      <div class="neo-convex p-8 rounded-[3rem] text-center space-y-4">
        <div class="w-24 h-24 neo-concave rounded-[2.5rem] mx-auto p-1 overflow-hidden">
          <img src="https://api.dicebear.com/7.x/bottts/svg?seed=Secretary" alt="AI Avatar" class="w-full h-full" />
        </div>
        <div>
          <h2 class="font-black tracking-tighter text-xl">Lumina</h2>
          <p class="text-[10px] font-bold text-primary uppercase tracking-widest">Advanced Secretary</p>
        </div>
      </div>

      <!-- 记忆插槽 (Memory Modules) -->
      <div class="flex-1 neo-concave rounded-[3rem] p-6 space-y-6 overflow-y-auto">
        <div class="flex items-center justify-between px-2">
          <span class="text-[9px] font-black text-foreground/30 tracking-widest uppercase">Memory Clusters</span>
          <Plus :size="12" class="cursor-pointer hover:text-primary transition-colors" />
        </div>
        
        <div v-for="i in 3" :key="i" class="neo-convex p-4 rounded-2xl space-y-2 group cursor-pointer active:scale-95 transition-all">
          <div class="flex items-center gap-2">
            <div class="w-2 h-2 rounded-full bg-primary/40 group-hover:bg-primary transition-colors"></div>
            <span class="text-[10px] font-bold opacity-60">Session #{{ 1024 + i }}</span>
          </div>
          <p class="text-[10px] text-foreground/30 line-clamp-2 leading-relaxed">关于 llm-orchestration-platform 的前端视觉重构讨论记录...</p>
        </div>
      </div>
    </aside>

    <!-- 右侧：主协作区 (Main Collaboration Hub) -->
    <main class="flex-1 flex flex-col min-w-0 neo-convex rounded-[4rem] overflow-hidden border border-white/40 shadow-2xl">
      <!-- 顶栏：连接状态 -->
      <header class="h-20 flex items-center px-10 border-b border-foreground/5 bg-white/5 backdrop-blur-xl">
        <div class="flex items-center gap-4">
          <div class="w-3 h-3 rounded-full bg-green-500 shadow-[0_0_10px_rgba(34,197,94,0.5)]"></div>
          <span class="text-xs font-bold tracking-widest opacity-60">ENCRYPTED COLLABORATION CHANNEL</span>
        </div>
        <div class="flex-1"></div>
        <button class="w-10 h-10 neo-convex rounded-full flex items-center justify-center text-foreground/30 hover:text-red-500 transition-colors">
          <Power :size="18" />
        </button>
      </header>

      <!-- 消息记录 -->
      <div ref="chatScrollRef" class="flex-1 overflow-y-auto p-10 space-y-10 scroll-smooth">
        <div 
          v-for="(msg, idx) in messages" :key="idx"
          class="flex" :class="msg.role === 'user' ? 'justify-end' : 'justify-start'"
        >
          <div 
            class="max-w-[80%] p-6 rounded-[2.5rem]"
            :class="msg.role === 'user' ? 'bg-primary text-white shadow-xl rounded-tr-sm' : 'neo-concave text-foreground/80 rounded-tl-sm'"
            v-motion-slide-visible-bottom
          >
            <div class="markdown-rendered prose prose-sm dark:prose-invert max-w-none" v-html="renderMarkdown(msg.content)"></div>
          </div>
        </div>
        
        <div v-if="loading" class="flex justify-start">
          <div class="neo-concave px-6 py-3 rounded-full flex items-center gap-3">
            <div class="w-1.5 h-1.5 bg-primary rounded-full animate-bounce"></div>
            <div class="w-1.5 h-1.5 bg-primary rounded-full animate-bounce [animation-delay:-0.15s]"></div>
            <div class="w-1.5 h-1.5 bg-primary rounded-full animate-bounce [animation-delay:-0.3s]"></div>
          </div>
        </div>
      </div>

      <!-- 输入槽 -->
      <footer class="p-8">
        <div class="neo-concave rounded-[3rem] p-2 flex items-center gap-3 group focus-within:ring-2 ring-primary/20 transition-all">
          <div class="w-12 h-12 neo-convex rounded-full flex items-center justify-center text-foreground/20 group-focus-within:text-primary transition-colors">
            <Mic :size="20" />
          </div>
          <input 
            v-model="input"
            @keyup.enter="handleSend"
            placeholder="下达指令给您的个人秘书..."
            class="flex-1 bg-transparent border-none focus:outline-none px-2 py-4 text-sm font-medium"
          />
          <button 
            @click="handleSend"
            :disabled="!input.trim() || loading"
            class="w-14 h-14 neo-convex rounded-[1.8rem] flex items-center justify-center transition-all"
            :class="input.trim() ? 'text-primary active:scale-90 shadow-lg' : 'text-foreground/10'"
          >
            <ArrowUp :size="24" />
          </button>
        </div>
      </footer>
    </main>

  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { 
  Power, Mic, ArrowUp, Plus, 
  User, Bot, Zap 
} from 'lucide-vue-next'
import { secretaryAPI } from '@/api'
import { useMarkdown } from '@/composables/useMarkdown'
import { useSSEStream } from '@/composables/useSSEStream'

const { renderMarkdown } = useMarkdown()
const { fetchSSE } = useSSEStream()

const input = ref('')
const loading = ref(false)
const messages = ref([
  { role: 'assistant', content: '您好，我是 Lumina。您的任务和知识库已经同步完成，今天有什么我可以帮您的？' }
])
const chatScrollRef = ref(null)

const scrollToBottom = async () => {
  await nextTick()
  if (chatScrollRef.value) {
    chatScrollRef.value.scrollTop = chatScrollRef.value.scrollHeight
  }
}

const handleSend = async () => {
  if (!input.value.trim() || loading.value) return
  
  const userMsg = input.value.trim()
  messages.value.push({ role: 'user', content: userMsg })
  input.value = ''
  loading.value = true
  
  const aiIdx = messages.value.length
  messages.value.push({ role: 'assistant', content: '' })
  
  try {
    let first = true
    await fetchSSE('/secretary/chat', { message: userMsg }, {
      onContent: (c) => {
        if (first) { loading.value = false; first = false; }
        messages.value[aiIdx].content += c
        scrollToBottom()
      }
    })
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
@reference "@/styles/index.css";
</style>
