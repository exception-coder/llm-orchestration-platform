<template>
  <div class="max-w-6xl mx-auto space-y-10">
    
    <!-- 1. 实验参数区 (Physical Control Panel) -->
    <header class="neo-convex p-8 rounded-[3rem] grid grid-cols-1 md:grid-cols-3 gap-8 items-end">
      <div class="space-y-4">
        <label class="text-[10px] font-black tracking-widest text-foreground/40 uppercase px-2">Select Model</label>
        <div class="neo-concave rounded-2xl p-1">
          <select v-model="selectedModel" class="w-full bg-transparent border-none focus:outline-none p-3 text-sm font-bold text-primary appearance-none">
            <option v-for="m in models" :key="m.id" :value="m.modelId">{{ m.name }}</option>
          </select>
        </div>
      </div>

      <div class="space-y-4">
        <label class="text-[10px] font-black tracking-widest text-foreground/40 uppercase px-2">Temperature</label>
        <div class="neo-concave rounded-2xl p-4 flex items-center gap-4">
          <input type="range" v-model="temperature" min="0" max="2" step="0.1" class="flex-1 accent-primary" />
          <span class="text-xs font-mono font-bold text-primary w-8 text-right">{{ temperature }}</span>
        </div>
      </div>

      <button 
        @click="runTest"
        :disabled="loading"
        class="h-14 neo-convex rounded-2xl flex items-center justify-center gap-3 text-primary font-black tracking-widest active:scale-95 transition-all"
      >
        <Play :size="18" :class="{ 'animate-pulse': loading }" />
        <span>EXECUTE TEST</span>
      </button>
    </header>

    <!-- 2. 编辑与输出区 (Dual Panel) -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-10 h-[600px]">
      
      <!-- 输入槽 (Input Slot) -->
      <section class="flex flex-col neo-convex rounded-[3rem] overflow-hidden border border-white/40">
        <div class="p-6 border-b border-foreground/5 flex items-center gap-3">
          <div class="w-8 h-8 neo-concave rounded-xl flex items-center justify-center text-foreground/40">
            <Terminal :size="16" />
          </div>
          <span class="text-[10px] font-black tracking-widest opacity-40 uppercase">System Prompt & Input</span>
        </div>
        <div class="flex-1 neo-concave m-6 rounded-[2rem] p-6">
          <textarea 
            v-model="promptInput"
            placeholder="在此处输入 Prompt 逻辑..."
            class="w-full h-full bg-transparent border-none focus:outline-none text-sm leading-relaxed resize-none font-mono"
          ></textarea>
        </div>
      </section>

      <!-- 输出台 (Output Platform) -->
      <section class="flex flex-col neo-convex rounded-[3rem] overflow-hidden border border-white/40">
        <div class="p-6 border-b border-foreground/5 flex items-center justify-between">
          <div class="flex items-center gap-3">
            <div class="w-8 h-8 neo-concave rounded-xl flex items-center justify-center text-primary">
              <Zap :size="16" />
            </div>
            <span class="text-[10px] font-black tracking-widest opacity-40 uppercase">AI Response</span>
          </div>
          <button class="p-2 neo-convex rounded-xl text-foreground/30 hover:text-primary transition-colors">
            <Copy :size="16" />
          </button>
        </div>
        
        <div class="flex-1 overflow-y-auto p-8 relative">
          <div v-if="loading" class="absolute inset-0 flex items-center justify-center bg-background/20 backdrop-blur-sm z-10">
            <Loader2 :size="32" class="animate-spin text-primary opacity-40" />
          </div>
          
          <article 
            class="markdown-rendered prose prose-slate dark:prose-invert max-w-none"
            v-html="renderMarkdown(testResult || '> 等待指令执行...')"
          ></article>
        </div>

        <footer v-if="tokenUsage" class="p-6 border-t border-foreground/5 bg-foreground/[0.02]">
          <div class="flex items-center gap-6">
            <div class="flex items-center gap-2 text-[10px] font-bold text-foreground/30 uppercase tracking-widest">
              <Activity :size="12" />
              Usage Statistics
            </div>
            <div class="flex-1 h-[1px] bg-foreground/5"></div>
            <span class="text-[10px] font-mono text-primary font-bold">TOKENS: {{ tokenUsage.totalTokens }}</span>
          </div>
        </footer>
      </section>
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Play, Terminal, Zap, Copy, Loader2, Activity } from 'lucide-vue-next'
import { modelConfigAPI, promptTestAPI } from '@/api'
import { useMarkdown } from '@/composables/useMarkdown'
import { ElMessage } from 'element-plus'

const { renderMarkdown } = useMarkdown()

const selectedModel = ref('')
const temperature = ref(0.7)
const promptInput = ref('')
const testResult = ref('')
const loading = ref(false)
const tokenUsage = ref(null)
const models = ref([])

const loadModels = async () => {
  const res = await modelConfigAPI.getModels()
  models.value = res || []
  if (models.value.length > 0) selectedModel.value = models.value[0].modelId
}

const runTest = async () => {
  if (!promptInput.value.trim()) return
  loading.value = true
  testResult.value = ''
  try {
    const res = await promptTestAPI.test({
      model: selectedModel.value,
      prompt: promptInput.value,
      temperature: temperature.value
    })
    testResult.value = res.content
    tokenUsage.value = res.tokenUsage
  } catch (err) {
    ElMessage.error('测试执行失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadModels)
</script>

<style scoped>
@reference "@/styles/index.css";
</style>
