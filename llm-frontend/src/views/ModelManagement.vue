<template>
  <div class="max-w-6xl mx-auto space-y-12">
    
    <!-- 顶部状态栏 -->
    <header class="flex items-center justify-between px-6">
      <div class="flex items-center gap-4">
        <div class="w-12 h-12 neo-convex rounded-2xl flex items-center justify-center text-primary shadow-lg">
          <Cpu :size="24" />
        </div>
        <div>
          <h1 class="text-2xl font-black tracking-tighter">模型控制台</h1>
          <p class="text-[10px] font-bold text-foreground/30 uppercase tracking-[0.2em]">Engine Status: Operational</p>
        </div>
      </div>
      <button 
        @click="showAddDialog = true"
        class="flex items-center gap-3 px-6 py-3 neo-convex rounded-2xl text-primary font-bold active:scale-95 transition-all"
      >
        <Plus :size="18" />
        <span>接入新模型</span>
      </button>
    </header>

    <!-- 模型网格 -->
    <section class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-10">
      <div 
        v-for="model in models" :key="model.id"
        class="neo-convex p-8 rounded-[3rem] group relative flex flex-col transition-all hover:scale-[1.02]"
        v-motion-pop
      >
        <!-- 材质装饰线 -->
        <div class="absolute top-0 left-1/2 -translate-x-1/2 w-24 h-1 bg-primary/10 rounded-b-full"></div>

        <div class="flex items-start justify-between mb-8">
          <div class="space-y-1">
            <h3 class="text-lg font-black tracking-tight group-hover:text-primary transition-colors">{{ model.name }}</h3>
            <p class="text-[10px] font-bold text-foreground/40 uppercase tracking-widest">{{ model.provider }}</p>
          </div>
          <div 
            class="w-10 h-10 neo-concave rounded-xl flex items-center justify-center"
            :class="model.enabled ? 'text-green-500' : 'text-foreground/10'"
          >
            <Activity :size="20" :class="{ 'animate-pulse': model.enabled }" />
          </div>
        </div>

        <!-- 详细参数 (Inset Panel) -->
        <div class="flex-1 neo-concave rounded-3xl p-5 space-y-4 mb-8">
          <div class="flex justify-between items-center text-[11px]">
            <span class="font-bold text-foreground/30 uppercase">Model ID</span>
            <span class="font-mono text-foreground/60">{{ model.modelId }}</span>
          </div>
          <div class="flex justify-between items-center text-[11px]">
            <span class="font-bold text-foreground/30 uppercase">Context Limit</span>
            <span class="font-mono text-foreground/60">{{ model.contextLength || '128k' }}</span>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="flex gap-3">
          <button class="flex-1 py-3 neo-convex rounded-xl text-[11px] font-black tracking-widest text-foreground/60 hover:text-primary active:scale-95 transition-all">
            CONFIG
          </button>
          <button 
            @click="deleteModel(model.id)"
            class="w-12 h-12 neo-convex rounded-xl flex items-center justify-center text-red-500/40 hover:text-red-500 active:scale-90 transition-all"
          >
            <Trash2 :size="18" />
          </button>
        </div>
      </div>
    </section>

    <!-- 空状态 -->
    <div v-if="models.length === 0" class="flex flex-col items-center justify-center py-20 opacity-20">
      <Boxes :size="64" class="mb-4" />
      <p class="font-bold tracking-widest uppercase text-sm">No Models Loaded</p>
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Cpu, Plus, Activity, Trash2, Boxes } from 'lucide-vue-next'
import { modelConfigAPI } from '@/api'
import { ElMessage } from 'element-plus'

const models = ref([])
const showAddDialog = ref(false)

const loadModels = async () => {
  try {
    const res = await modelConfigAPI.getAll()
    models.value = res || []
  } catch (err) {
    console.error(err)
  }
}

const deleteModel = async (id) => {
  try {
    await modelConfigAPI.deleteModel(id)
    loadModels()
  } catch (err) {
    ElMessage.error('删除失败')
  }
}

onMounted(loadModels)
</script>

<style scoped>
@reference "@/styles/index.css";
</style>
