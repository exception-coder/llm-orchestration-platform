<template>
  <div class="h-full flex flex-col lg:flex-row gap-8 overflow-hidden">
    
    <!-- 1. 物理目录侧边栏 (Neomorphic Sidebar) -->
    <aside 
      class="w-full lg:w-80 flex flex-col shrink-0 transition-all duration-500"
      :class="[isMobile && currentDoc ? 'hidden' : 'flex']"
    >
      <!-- 搜索槽位 (Deep Inset) -->
      <div class="p-6 neo-convex rounded-[2.5rem] mb-6">
        <div class="relative neo-concave rounded-2xl p-1 flex items-center group transition-all focus-within:ring-2 ring-primary/20">
          <div class="pl-4 text-foreground/30"><Search :size="16" /></div>
          <input 
            v-model="searchKeyword"
            placeholder="语义检索文档..."
            @keyup.enter="handleSearch"
            class="w-full bg-transparent border-none focus:outline-none px-3 py-3 text-xs placeholder:text-foreground/20"
          />
          <button 
            @click="handleSearch"
            :disabled="searchLoading"
            class="p-2 mr-1 rounded-xl neo-convex text-primary active:scale-90 transition-all"
          >
            <ArrowRight v-if="!searchLoading" :size="16" />
            <Loader2 v-else :size="16" class="animate-spin" />
          </button>
        </div>
      </div>

      <!-- 目录内容区 (Concave Track) -->
      <div class="flex-1 neo-concave rounded-[2.5rem] p-4 overflow-y-auto overflow-x-hidden space-y-2">
        
        <!-- 搜索结果模式 -->
        <template v-if="searchResults.length > 0">
          <div class="px-4 py-2 flex items-center justify-between">
            <span class="text-[10px] font-black tracking-widest text-primary opacity-60">SEARCH HITS</span>
            <button @click="clearSearch" class="text-[10px] font-bold text-foreground/40 hover:text-primary transition-colors">CLEAR</button>
          </div>
          <div 
            v-for="hit in searchResults" 
            :key="hit.path"
            @click="loadDoc(hit.path)"
            class="p-4 rounded-2xl hover:bg-foreground/5 cursor-pointer transition-all border border-transparent hover:border-white/10 group"
          >
            <div class="flex items-center gap-3">
              <div class="w-8 h-8 neo-convex rounded-lg flex items-center justify-center text-primary group-hover:scale-110 transition-transform">
                <FileText :size="16" />
              </div>
              <div class="flex-1 overflow-hidden">
                <p class="text-xs font-bold truncate">{{ hit.name }}</p>
                <p class="text-[10px] text-foreground/40 truncate mt-1">{{ hit.content }}</p>
              </div>
            </div>
          </div>
        </template>

        <!-- 目录树模式 -->
        <template v-else>
          <div v-if="treeLoading" class="p-4 space-y-4">
            <div v-for="i in 5" :key="i" class="h-10 w-full neo-convex rounded-xl animate-pulse opacity-50"></div>
          </div>
          <div v-else class="space-y-1">
            <TreeItem 
              v-for="item in treeData" 
              :key="item.path" 
              :item="item" 
              @select="loadDoc"
              :current-path="currentDoc?.path"
            />
          </div>
        </template>
      </div>
    </aside>

    <!-- 2. 文档展示核心 (Convex Display Panel) -->
    <main 
      class="flex-1 flex flex-col min-w-0 transition-all duration-500"
      :class="[isMobile && !currentDoc ? 'hidden' : 'flex']"
    >
      <!-- 未选择状态 -->
      <div v-if="!currentDoc" class="flex-1 flex items-center justify-center">
        <div class="neo-convex p-16 rounded-[4rem] text-center space-y-6 max-w-sm" v-motion-pop>
          <div class="w-24 h-24 neo-concave rounded-[2rem] mx-auto flex items-center justify-center text-foreground/10">
            <Library :size="48" />
          </div>
          <div>
            <h3 class="text-xl font-black tracking-tight text-foreground/60">知识库中心</h3>
            <p class="text-sm text-foreground/30 mt-2 leading-relaxed">请从左侧轨道中选择一个文档进行深度阅读</p>
          </div>
        </div>
      </div>

      <!-- 文档内容状态 -->
      <div v-else class="flex-1 flex flex-col neo-convex rounded-[3rem] overflow-hidden" v-motion-slide-visible-right>
        <!-- 工具栏 (Glass Header) -->
        <header class="h-16 flex items-center px-8 border-b border-white/5 bg-white/5 backdrop-blur-md">
          <button 
            v-if="isMobile" 
            @click="currentDoc = null"
            class="mr-4 w-8 h-8 neo-convex rounded-full flex items-center justify-center text-foreground/60 active:scale-90"
          >
            <ChevronLeft :size="18" />
          </button>
          <span class="text-[10px] font-black tracking-[0.2em] text-foreground/30 truncate flex-1">
            {{ currentDoc.path }}
          </span>
          <div class="flex items-center gap-2">
            <button class="w-8 h-8 neo-convex rounded-full flex items-center justify-center text-foreground/40 hover:text-primary transition-colors">
              <Share2 :size="14" />
            </button>
            <button @click="currentDoc = null" class="w-8 h-8 neo-convex rounded-full flex items-center justify-center text-red-500/40 hover:text-red-500 transition-colors">
              <X :size="14" />
            </button>
          </div>
        </header>

        <!-- Markdown 内容区 -->
        <div class="flex-1 overflow-y-auto p-12 scroll-smooth">
          <div v-if="contentLoading" class="space-y-6">
            <div class="h-12 w-2/3 neo-concave rounded-2xl animate-pulse"></div>
            <div class="space-y-3">
              <div v-for="i in 10" :key="i" :class="['h-4 neo-concave rounded-full animate-pulse opacity-40', i % 3 === 0 ? 'w-full' : 'w-5/6']"></div>
            </div>
          </div>
          <article 
            v-else
            ref="markdownRef"
            class="markdown-rendered prose prose-slate dark:prose-invert max-w-none"
            v-html="renderMarkdown(currentDoc.content)"
          ></article>
        </div>
      </div>
    </main>

  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import { 
  Search, FileText, Folder, FolderOpen, 
  Library, ChevronLeft, X, Share2, ArrowRight, Loader2, ChevronRight
} from 'lucide-vue-next'
import { docViewerAPI } from '@/api'
import { useMarkdown } from '@/composables/useMarkdown'
import { useResponsive } from '@/composables/useResponsive'
import { ElMessage } from 'element-plus'

const { renderMarkdown } = useMarkdown()
const { isMobile } = useResponsive()

// ---- 子组件：树形项目 (为了逻辑纯净建议抽离，这里先内联处理逻辑) ----
const TreeItem = {
  name: 'TreeItem',
  props: ['item', 'currentPath', 'depth'],
  emits: ['select'],
  setup(props, { emit }) {
    const isOpen = ref(true)
    const toggle = () => { if (props.item.type === 'DIRECTORY') isOpen.value = !isOpen.value }
    const select = () => { if (props.item.type === 'FILE') emit('select', props.item.path) }
    
    return { isOpen, toggle, select }
  },
  template: `
    <div class="select-none">
      <div 
        @click="item.type === 'DIRECTORY' ? toggle() : select()"
        class="flex items-center gap-3 px-4 py-2 rounded-2xl cursor-pointer transition-all duration-200 group"
        :class="[
          currentPath === item.path ? 'neo-convex text-primary font-bold z-10' : 'hover:bg-foreground/5 text-foreground/60'
        ]"
        :style="{ marginLeft: (depth || 0) * 12 + 'px' }"
      >
        <div class="shrink-0 transition-transform group-hover:scale-110">
          <Folder v-if="item.type === 'DIRECTORY' && !isOpen" :size="16" class="opacity-40" />
          <FolderOpen v-else-if="item.type === 'DIRECTORY' && isOpen" :size="16" class="text-primary/60" />
          <FileText v-else :size="16" :class="currentPath === item.path ? 'text-primary' : 'opacity-40'" />
        </div>
        <span class="text-xs truncate flex-1">{{ item.name }}</span>
        <ChevronRight v-if="item.type === 'DIRECTORY'" :size="12" class="transition-transform opacity-20" :class="{ 'rotate-90': isOpen }" />
      </div>
      <div v-if="isOpen && item.children" class="mt-1">
        <TreeItem 
          v-for="child in item.children" 
          :key="child.path" 
          :item="child" 
          :depth="(depth || 0) + 1"
          :current-path="currentPath"
          @select="$emit('select', $event)"
        />
      </div>
    </div>
  `
}

// ---- 主逻辑 ----
const treeData = ref([])
const treeLoading = ref(true)
const currentDoc = ref(null)
const contentLoading = ref(false)
const searchKeyword = ref('')
const searchResults = ref([])
const searchLoading = ref(false)
const markdownRef = ref(null)

const loadTree = async () => {
  try {
    const res = await docViewerAPI.getTree()
    treeData.value = res.items || []
  } catch (err) {
    ElMessage.error('目录获取失败')
  } finally {
    treeLoading.value = false
  }
}

const loadDoc = async (path) => {
  contentLoading.value = true
  try {
    const doc = await docViewerAPI.getContent(path)
    currentDoc.value = doc
    searchResults.value = [] // 选中即清空搜索
    await nextTick()
    renderMermaid()
  } catch (err) {
    ElMessage.error('文档读取失败')
  } finally {
    contentLoading.value = false
  }
}

const handleSearch = async () => {
  if (!searchKeyword.value.trim()) return
  searchLoading.value = true
  try {
    const res = await docViewerAPI.search(searchKeyword.value)
    searchResults.value = res.hits || []
  } catch (err) {
    ElMessage.error('搜索服务异常')
  } finally {
    searchLoading.value = false
  }
}

const clearSearch = () => {
  searchKeyword.value = ''
  searchResults.value = []
}

// Mermaid 处理 (保持逻辑一致)
let mermaid = null
const renderMermaid = async () => {
  if (!markdownRef.value) return
  if (!mermaid) {
    try {
      const mod = await Function('return import("mermaid")')()
      mermaid = mod.default
      mermaid.initialize({ startOnLoad: false, theme: 'default' })
    } catch { return }
  }
  const blocks = markdownRef.value.querySelectorAll('code.language-mermaid')
  for (const block of blocks) {
    const id = `mermaid-${Math.random().toString(36).substr(2, 9)}`
    const { svg } = await mermaid.render(id, block.textContent)
    const container = document.createElement('div')
    container.className = 'my-8 flex justify-center bg-white/5 p-8 rounded-[2rem] neo-concave'
    container.innerHTML = svg
    block.parentElement.replaceWith(container)
  }
}

onMounted(loadTree)
</script>

<style scoped>
@reference "@/styles/index.css";

/* 深度适配 Markdown 物理质感 */
:deep(.markdown-rendered h1) { @apply text-3xl font-black tracking-tight mb-8 pb-4 border-b border-foreground/5; }
:deep(.markdown-rendered h2) { @apply text-xl font-bold mt-12 mb-6 text-primary; }
:deep(.markdown-rendered p) { @apply text-foreground/70 leading-relaxed mb-6; }
:deep(.markdown-rendered blockquote) { @apply border-l-4 border-primary/20 bg-primary/5 px-6 py-4 rounded-r-2xl italic text-foreground/60 my-8; }
:deep(.markdown-rendered pre) { @apply shadow-neo-inset bg-card p-8 rounded-[2rem] my-8 overflow-x-auto border border-white/5; }
:deep(.markdown-rendered code:not(pre code)) { @apply bg-primary/10 text-primary px-2 py-0.5 rounded-lg font-mono text-xs; }
:deep(.markdown-rendered table) { @apply w-full shadow-neo-inset bg-card rounded-[2rem] overflow-hidden my-8 border-collapse; }
:deep(.markdown-rendered th) { @apply bg-foreground/5 p-4 text-left text-[10px] font-black uppercase tracking-widest opacity-40; }
:deep(.markdown-rendered td) { @apply p-4 text-sm border-t border-foreground/5; }

/* 隐藏原生的滚动条以保持拟物感 */
::-webkit-scrollbar { width: 0px; background: transparent; }
</style>
