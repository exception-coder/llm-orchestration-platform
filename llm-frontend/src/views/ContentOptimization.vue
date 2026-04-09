<template>
  <div class="max-w-7xl mx-auto space-y-12">
    
    <!-- 头部：加工厂状态 -->
    <header class="flex items-center gap-6 px-4">
      <div class="w-14 h-14 neo-convex rounded-2xl flex items-center justify-center text-primary shadow-lg">
        <Sparkles :size="28" />
      </div>
      <div>
        <h1 class="text-2xl font-black tracking-tighter">内容优化加工厂</h1>
        <p class="text-[10px] font-bold text-foreground/30 uppercase tracking-[0.2em]">Platform Specific Refinement Engine</p>
      </div>
    </header>

    <div class="grid grid-cols-1 xl:grid-cols-12 gap-10 items-stretch">
      
      <!-- 原文输入 (Input Slot) - 占据 5 列 -->
      <section class="xl:col-span-5 flex flex-col neo-convex rounded-[3.5rem] p-8 border border-white/40">
        <div class="flex items-center gap-3 mb-6 px-2">
          <div class="w-8 h-8 neo-concave rounded-xl flex items-center justify-center text-foreground/40">
            <FileText :size="16" />
          </div>
          <span class="text-[10px] font-black tracking-widest opacity-40 uppercase">Source Content</span>
        </div>
        
        <div class="flex-1 neo-concave rounded-[2.5rem] p-6">
          <textarea 
            v-model="sourceContent"
            placeholder="粘贴需要优化的原文..."
            class="w-full h-full bg-transparent border-none focus:outline-none text-sm leading-relaxed resize-none"
          ></textarea>
        </div>
      </section>

      <!-- 转换控制中心 (Action Panel) - 占据 2 列 -->
      <section class="xl:col-span-2 flex flex-col justify-center gap-6 py-10">
        <div class="text-center space-y-2 mb-4">
          <span class="text-[9px] font-black tracking-[0.3em] text-foreground/20 uppercase">Target Platform</span>
        </div>
        
        <button 
          v-for="p in platforms" :key="p.id"
          @click="optimize(p.id)"
          :disabled="loading"
          class="group relative h-20 neo-convex rounded-[2rem] flex flex-col items-center justify-center gap-2 transition-all active:scale-90"
          :class="[loading ? 'opacity-50 grayscale' : 'hover:text-primary']"
        >
          <component :is="p.icon" :size="20" class="group-hover:scale-110 transition-transform" />
          <span class="text-[9px] font-black tracking-widest uppercase">{{ p.label }}</span>
          
          <!-- 指示灯 -->
          <div v-if="currentPlatform === p.id && loading" class="absolute -right-2 top-1/2 -translate-y-1/2 w-2 h-2 bg-primary rounded-full shadow-[0_0_8px_var(--color-primary)]"></div>
        </button>
      </section>

      <!-- 优化成品 (Output Slot) - 占据 5 列 -->
      <section class="xl:col-span-5 flex flex-col neo-convex rounded-[3.5rem] p-8 border border-white/40">
        <div class="flex items-center justify-between mb-6 px-2">
          <div class="flex items-center gap-3">
            <div class="w-8 h-8 neo-concave rounded-xl flex items-center justify-center text-primary">
              <CheckCircle :size="16" />
            </div>
            <span class="text-[10px] font-black tracking-widest opacity-40 uppercase">Optimized Result</span>
          </div>
          <button class="p-2 neo-convex rounded-xl text-foreground/30 hover:text-primary transition-colors">
            <Copy :size="16" />
          </button>
        </div>

        <div class="flex-1 neo-concave rounded-[2.5rem] p-8 overflow-y-auto relative min-h-[400px]">
          <div v-if="loading" class="absolute inset-0 flex items-center justify-center bg-background/50 backdrop-blur-sm rounded-[2.5rem] z-20">
            <div class="flex flex-col items-center gap-4">
              <Loader2 :size="32" class="animate-spin text-primary" />
              <span class="text-[10px] font-black tracking-widest text-primary animate-pulse">OPTIMIZING...</span>
            </div>
          </div>
          <article 
            class="markdown-rendered prose prose-slate dark:prose-invert max-w-none"
            v-html="renderMarkdown(optimizedContent || '> 选择平台开始优化...')"
          ></article>
        </div>
      </section>
    </div>

  </div>
</template>

<script setup>
import { ref } from 'vue'
import { 
  Sparkles, FileText, CheckCircle, Copy, Loader2,
  Instagram, Twitter, Send, Github, Languages
} from 'lucide-vue-next'
import { contentOptimizationAPI } from '@/api'
import { useMarkdown } from '@/composables/useMarkdown'
import { ElMessage } from 'element-plus'

const { renderMarkdown } = useMarkdown()

const sourceContent = ref('')
const optimizedContent = ref('')
const loading = ref(false)
const currentPlatform = ref('')

const platforms = [
  { id: 'RED', label: '小红书', icon: Instagram },
  { id: 'TIKTOK', label: '抖音/TikTok', icon: Send },
  { id: 'TWITTER', label: 'Twitter/X', icon: Twitter },
  { id: 'GITHUB', label: 'GitHub Readme', icon: Github },
  { id: 'TRANSLATE', label: '智能润色/翻译', icon: Languages }
]

const optimize = async (platform) => {
  if (!sourceContent.value.trim()) return
  loading.value = true
  currentPlatform.value = platform
  try {
    const res = await contentOptimizationAPI.optimize({
      content: sourceContent.value,
      platform: platform
    })
    optimizedContent.value = res.content
  } catch (err) {
    ElMessage.error('优化任务执行失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
@reference "@/styles/index.css";
</style>
