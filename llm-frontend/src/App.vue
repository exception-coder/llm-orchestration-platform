<template>
  <div :data-theme="currentTheme" class="min-h-screen bg-background text-foreground transition-colors duration-500 font-sans">
    
    <!-- 1. 响应式移动端菜单按钮 -->
    <button 
      v-if="isMobile"
      @click="sidebarOpen = !sidebarOpen"
      class="fixed bottom-6 right-6 z-50 p-4 neo-convex rounded-full text-primary"
    >
      <Menu v-if="!sidebarOpen" :size="24" />
      <X v-else :size="24" />
    </button>

    <div class="flex h-screen overflow-hidden">
      
      <!-- 2. 极致侧边栏 (Neomorphic / Glass Surface) -->
      <aside 
        class="relative z-40 flex flex-col transition-all duration-500 ease-[cubic-bezier(0.4,0,0.2,1)] border-r border-white/10"
        :class="[
          sidebarOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0',
          isCollapsed ? 'w-24' : 'w-72'
        ]"
      >
        <!-- Logo & Toggle 区域 (Internal) -->
        <div class="h-24 flex items-center px-6 relative overflow-hidden" :class="isCollapsed ? 'justify-center' : 'justify-start gap-4'">
          <!-- 内部折叠切换按钮 -->
          <button 
            @click="isCollapsed = !isCollapsed"
            class="w-12 h-12 neo-convex rounded-2xl flex items-center justify-center text-foreground/40 hover:text-primary transition-all active:scale-90 shrink-0"
          >
            <Menu :size="20" :class="{ 'text-primary': isCollapsed }" />
          </button>

          <!-- Logo 与文字 (仅在展开时显示) -->
          <div 
            v-if="!isCollapsed" 
            class="flex items-center gap-3 animate-in fade-in slide-in-from-left-4 duration-500"
          >
            <div class="w-8 h-8 bg-primary rounded-xl flex items-center justify-center text-white font-black shadow-lg shadow-primary/30">L</div>
            <span class="font-black tracking-tighter text-xl whitespace-nowrap">LLM OS</span>
          </div>
        </div>

        <!-- 导航列表 -->
        <nav class="flex-1 overflow-y-auto px-6 py-8 space-y-10 no-scrollbar">
          <div v-for="group in menuGroups" :key="group.label" class="space-y-4">
            <p 
              v-if="!isCollapsed" 
              class="text-[9px] font-black uppercase tracking-[0.3em] text-foreground/20 px-2 flex items-center gap-3"
            >
              <span>{{ group.label }}</span>
              <span class="flex-1 h-[1px] bg-foreground/5"></span>
            </p>
            <div v-else class="h-[1px] bg-foreground/5 mx-2"></div>

            <div class="space-y-2">
              <router-link
                v-for="item in group.items"
                :key="item.path"
                :to="item.path"
                v-slot="{ isActive }"
                class="block"
              >
                <div 
                  class="flex items-center gap-4 rounded-2xl cursor-pointer transition-all duration-300 relative group"
                  :class="[
                    isActive ? 'neo-concave text-primary' : 'text-foreground/40 hover:text-foreground/70',
                    isCollapsed ? 'h-16 w-16 mx-auto justify-center px-0' : 'px-5 py-4'
                  ]"
                >
                  <component 
                    :is="item.icon" 
                    :size="isCollapsed ? 28 : 22" 
                    :stroke-width="isActive ? 2.5 : 2"
                    class="transition-transform duration-300 group-hover:scale-110"
                  />
                  <span v-if="!isCollapsed" class="font-bold text-xs tracking-wide whitespace-nowrap">{{ item.name }}</span>
                  
                  <!-- 活动状态指示灯 (非折叠模式) -->
                  <div v-if="isActive && !isCollapsed" class="absolute left-1 w-1 h-4 bg-primary rounded-full shadow-[0_0_8px_var(--color-primary)]"></div>

                  <!-- 悬浮提示 (仅在折叠模式显示) -->
                  <div 
                    v-if="isCollapsed"
                    class="absolute left-20 px-4 py-2 bg-foreground text-background text-[10px] font-black rounded-xl opacity-0 translate-x-[-10px] group-hover:opacity-100 group-hover:translate-x-0 transition-all pointer-events-none whitespace-nowrap z-50 shadow-xl"
                  >
                    {{ item.name }}
                  </div>
                </div>
              </router-link>
            </div>
          </div>
        </nav>

        <!-- 主题切换脚部 -->
        <div class="p-4 border-t border-white/5 space-y-2">
          <button 
            @click="toggleTheme" 
            class="w-full flex items-center gap-4 px-4 py-3 rounded-2xl neo-convex text-foreground/60 active:scale-95"
          >
            <Sun v-if="currentTheme === 'glass'" :size="20" />
            <Moon v-else :size="20" />
            <span v-if="!isCollapsed" class="text-sm">切换材质</span>
          </button>
        </div>
      </aside>

      <!-- 3. 主内容区域 -->
      <main class="flex-1 flex flex-col relative overflow-hidden">
        <!-- 头部 -->
        <header class="h-20 flex items-center px-8 border-b border-white/5">
          <h1 class="text-xl font-bold tracking-tight">{{ pageTitle }}</h1>
          <div class="flex-1"></div>
          <!-- 头部操作区 -->
          <div class="flex items-center gap-4">
            <div class="w-10 h-10 neo-convex rounded-full flex items-center justify-center cursor-pointer">
              <Bell :size="18" />
            </div>
            <div class="w-10 h-10 neo-convex rounded-full overflow-hidden border-2 border-primary/20 cursor-pointer">
              <img src="https://api.dicebear.com/7.x/avataaars/svg?seed=Lucky" alt="avatar" />
            </div>
          </div>
        </header>

        <!-- 视图渲染 -->
        <div class="flex-1 overflow-y-auto p-8">
          <router-view v-slot="{ Component }">
            <transition name="page" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </div>
      </main>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useResponsive } from '@/composables/useResponsive'
import {
  Menu, X, Sun, Moon, Bell, ChevronRight, Terminal,
  MessageSquare, LayoutGrid, FileText, Notebook,
  Settings, Image, Compass, Boxes, UserCheck,
  Bot, GitBranch, Wrench
} from 'lucide-vue-next'

const { isMobile } = useResponsive()
const route = useRoute()
const sidebarOpen = ref(false)
const isCollapsed = ref(false)
const currentTheme = ref(localStorage.getItem('app-theme') || '')

const themes = [
  { id: '', label: '物理拟物', icon: Boxes },
  { id: 'apple', label: '苹果毛玻璃', icon: Sun },
  { id: 'google', label: '谷歌材质', icon: LayoutGrid },
  { id: 'cyber', label: '赛博终端', icon: Terminal }
]

const toggleTheme = () => {
  const currentIndex = themes.findIndex(t => t.id === currentTheme.value)
  const nextIndex = (currentIndex + 1) % themes.length
  const theme = themes[nextIndex].id
  
  currentTheme.value = theme
  localStorage.setItem('app-theme', theme)
  applyTheme(theme)
}

function applyTheme(theme) {
  if (theme) {
    document.documentElement.setAttribute('data-theme', theme)
  } else {
    document.documentElement.removeAttribute('data-theme')
  }
}

const themeLabel = computed(() => {
  return themes.find(t => t.id === currentTheme.value)?.label || '切换材质'
})

onMounted(() => {
  applyTheme(currentTheme.value)
})

const pageTitle = computed(() => {
  const titles = {
    '/chat': '智能对话',
    '/doc-viewer': '知识库中心',
    '/note-capture': '碎片化笔记',
    '/prompt-test': 'Prompt 工程',
    '/model-management': '模型控制台',
    '/agent-list': '智能体',
    '/graph-orchestration': 'Agent 编排',
    '/tool-management': 'Tool 管理'
  }
  return titles[route.path] || '控制台'
})

const menuGroups = [
  {
    label: '核心',
    items: [
      { name: '智能对话', path: '/chat', icon: MessageSquare },
      { name: '知识库', path: '/doc-viewer', icon: FileText },
      { name: '碎片记录', path: '/note-capture', icon: Notebook }
    ]
  },
  {
    label: '工程',
    items: [
      { name: 'Prompt 实验室', path: '/prompt-test', icon: LayoutGrid },
      { name: '内容优化', path: '/content-optimization', icon: Compass },
      { name: '模型管理', path: '/model-management', icon: Settings }
    ]
  },
  {
    label: '智能体管理',
    items: [
      { name: '智能体', path: '/agent-list', icon: Bot },
      { name: 'Agent 编排', path: '/graph-orchestration', icon: GitBranch },
      { name: 'Tool 管理', path: '/tool-management', icon: Wrench }
    ]
  },
  {
    label: '工具',
    items: [
      { name: 'Markdown 绘图', path: '/markdown-to-image', icon: Image },
      { name: '个人助理', path: '/secretary', icon: UserCheck }
    ]
  }
]
</script>

<style>
/* 页面切换动画 */
.page-enter-active,
.page-leave-active {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.page-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.page-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
