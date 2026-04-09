<template>
  <div class="max-w-7xl mx-auto space-y-10">
    
    <!-- 1. 顶部控制台：动作中心 -->
    <header class="flex items-center justify-between px-6">
      <div class="flex items-center gap-5">
        <div class="w-14 h-14 neo-convex rounded-[1.5rem] flex items-center justify-center text-primary shadow-xl">
          <Wrench :size="28" />
        </div>
        <div>
          <h1 class="text-2xl font-black tracking-tighter italic">TOOL MODULES</h1>
          <p class="text-[10px] font-bold text-foreground/30 uppercase tracking-[0.3em]">Agent Function Extensions</p>
        </div>
      </div>
      
      <div class="flex items-center gap-4">
        <div class="neo-concave rounded-2xl px-4 py-2 flex items-center gap-3">
          <Search :size="14" class="text-foreground/20" />
          <input placeholder="SEARCH TOOLS..." class="bg-transparent border-none focus:outline-none text-[10px] font-bold tracking-widest w-40" />
        </div>
        <button 
          @click="showCreateDialog = true"
          class="h-12 px-6 neo-convex rounded-2xl flex items-center gap-3 text-primary font-black tracking-widest active:scale-95 transition-all shadow-lg"
        >
          <Plus :size="18" />
          <span>REGISTER NEW TOOL</span>
        </button>
      </div>
    </header>

    <!-- 2. 物理数据舱 (The Table Hub) -->
    <section class="neo-convex rounded-[3.5rem] overflow-hidden border border-white/40 shadow-2xl">
      <!-- 表头槽位 (Header Slot) -->
      <div class="grid grid-cols-12 gap-4 px-10 py-6 neo-concave bg-foreground/[0.02]">
        <div class="col-span-3 text-[10px] font-black tracking-[0.2em] text-foreground/30 uppercase">Tool Identity</div>
        <div class="col-span-4 text-[10px] font-black tracking-[0.2em] text-foreground/30 uppercase">Description & Logic</div>
        <div class="col-span-2 text-[10px] font-black tracking-[0.2em] text-foreground/30 uppercase">Status</div>
        <div class="col-span-3 text-right text-[10px] font-black tracking-[0.2em] text-foreground/30 uppercase">Operations</div>
      </div>

      <!-- 数据条目流 (Data Stream) -->
      <div class="divide-y divide-foreground/5">
        <div 
          v-for="tool in tools" :key="tool.id"
          class="grid grid-cols-12 gap-4 px-10 py-8 items-center hover:bg-foreground/[0.01] transition-colors group"
          v-motion-slide-visible-bottom
        >
          <!-- 身份列 -->
          <div class="col-span-3 flex items-center gap-4">
            <div class="w-12 h-12 neo-convex rounded-2xl flex items-center justify-center shrink-0 group-hover:scale-110 transition-transform">
              <Code2 v-if="tool.type === 'FUNCTION'" :size="20" class="text-primary/60" />
              <Globe v-else :size="20" class="text-blue-400/60" />
            </div>
            <div class="overflow-hidden">
              <p class="text-sm font-black tracking-tight truncate">{{ tool.name }}</p>
              <p class="text-[9px] font-mono text-foreground/20 truncate uppercase tracking-tighter">{{ tool.id }}</p>
            </div>
          </div>

          <!-- 描述列 -->
          <div class="col-span-4">
            <p class="text-xs text-foreground/60 leading-relaxed line-clamp-2 pr-10 italic">
              {{ tool.description || 'No specialized description provided for this module.' }}
            </p>
          </div>

          <!-- 状态列 -->
          <div class="col-span-2">
            <div class="flex items-center gap-3">
              <div 
                class="w-2.5 h-2.5 rounded-full shadow-[0_0_10px_currentColor] transition-all"
                :class="tool.enabled ? 'text-green-500 bg-green-500' : 'text-foreground/10 bg-foreground/10 shadow-none'"
              ></div>
              <span class="text-[10px] font-black tracking-widest uppercase opacity-40">
                {{ tool.enabled ? 'Active' : 'Offline' }}
              </span>
            </div>
          </div>

          <!-- 操作列 -->
          <div class="col-span-3 flex justify-end gap-3 opacity-40 group-hover:opacity-100 transition-opacity">
            <button class="px-4 py-2 neo-convex rounded-xl text-[10px] font-black tracking-widest hover:text-primary transition-all active:scale-95">
              EDIT
            </button>
            <button class="px-4 py-2 neo-convex rounded-xl text-[10px] font-black tracking-widest hover:text-primary transition-all active:scale-95">
              DOCS
            </button>
            <button 
              @click="handleDelete(tool.id)"
              class="w-10 h-10 neo-convex rounded-xl flex items-center justify-center text-red-500/40 hover:text-red-500 active:scale-90 transition-all"
            >
              <Trash2 :size="16" />
            </button>
          </div>
        </div>
      </div>

      <!-- 空舱状态 -->
      <div v-if="tools.length === 0" class="flex flex-col items-center justify-center py-32 space-y-6 opacity-10">
        <BoxSelect :size="80" stroke-width="1" />
        <p class="text-sm font-black tracking-[0.5em] uppercase text-center">Data Hub Empty<br/><span class="text-[10px] tracking-widest opacity-50">Please register system modules</span></p>
      </div>
    </section>

    <!-- 底部统计 -->
    <footer class="flex items-center justify-between px-10">
      <div class="flex items-center gap-8">
        <div class="flex flex-col">
          <span class="text-[9px] font-black text-foreground/20 uppercase tracking-widest">Total Modules</span>
          <span class="text-lg font-black text-primary">{{ tools.length }}</span>
        </div>
        <div class="w-[1px] h-8 bg-foreground/5"></div>
        <div class="flex flex-col">
          <span class="text-[9px] font-black text-foreground/20 uppercase tracking-widest">System Load</span>
          <span class="text-lg font-black text-foreground/40 font-mono">0.02ms</span>
        </div>
      </div>
      
      <!-- 极简分页 (Physical Tabs) -->
      <div class="flex gap-2">
        <button class="w-10 h-10 neo-concave rounded-xl flex items-center justify-center text-primary font-bold">1</button>
        <button class="w-10 h-10 neo-convex rounded-xl flex items-center justify-center text-foreground/30">2</button>
      </div>
    </footer>

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { 
  Wrench, Plus, Search, Code2, Globe, 
  Trash2, BoxSelect, Activity 
} from 'lucide-vue-next'
import { toolAPI } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const tools = ref([])
const showCreateDialog = ref(false)

const loadTools = async () => {
  try {
    const res = await toolAPI.getAll()
    tools.value = res || []
  } catch (err) {
    console.error('Failed to load tools:', err)
  }
}

const handleDelete = (id) => {
  ElMessageBox.confirm('确定要拆卸该工具模块吗？这将影响关联的智能体。', '系统警告', {
    confirmButtonText: '确定拆卸',
    cancelButtonText: '取消',
    type: 'warning',
    customClass: 'neo-dialog' // 待后续全局对话框重构
  }).then(async () => {
    try {
      await toolAPI.deleteTool(id)
      loadTools()
      ElMessage.success('模块已成功离线')
    } catch (e) {
      ElMessage.error('离线操作失败')
    }
  })
}

onMounted(loadTools)
</script>

<style scoped>
@reference "@/styles/index.css";

/* 隐藏单元格默认边框，使用物理条目感 */
:deep(.el-table) {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: transparent;
}
</style>
