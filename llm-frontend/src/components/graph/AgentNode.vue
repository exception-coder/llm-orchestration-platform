<template>
  <div
    class="min-w-[200px] max-w-[240px] rounded-2xl p-4 cursor-pointer transition-all duration-200"
    :class="[
      data.isEntry ? 'ring-2 ring-primary ring-offset-2 ring-offset-background' : '',
      'neo-convex hover:scale-[1.02]'
    ]"
  >
    <!-- 顶部：图标 + 名称 -->
    <div class="flex items-center gap-3 mb-2">
      <div class="w-9 h-9 neo-concave rounded-xl flex items-center justify-center text-primary shrink-0">
        <component :is="nodeIcon" :size="16" />
      </div>
      <div class="min-w-0 flex-1">
        <p class="font-bold text-sm truncate leading-tight">{{ data.label }}</p>
        <p class="text-[10px] font-bold text-foreground/40 uppercase tracking-widest">{{ data.nodeType }}</p>
      </div>
    </div>

    <!-- 中部：Agent 信息 -->
    <div v-if="data.agent" class="mb-2">
      <p class="text-[11px] text-foreground/60 truncate">{{ data.agent.name }}</p>
      <div class="flex items-center gap-1.5 mt-1">
        <span class="px-1.5 py-0.5 neo-concave rounded text-[9px] font-mono text-foreground/50">
          {{ data.agent.llmModel || 'N/A' }}
        </span>
      </div>
    </div>

    <!-- 底部：Tool 标签 -->
    <div v-if="data.tools?.length" class="flex flex-wrap gap-1 mt-2">
      <span
        v-for="tool in visibleTools" :key="tool.id"
        class="px-1.5 py-0.5 neo-concave rounded text-[9px] font-mono text-foreground/40 truncate max-w-[80px]"
        :title="tool.name"
      >
        {{ tool.name }}
      </span>
      <span
        v-if="data.tools.length > 3"
        class="px-1.5 py-0.5 neo-concave rounded text-[9px] font-bold text-primary/60"
      >
        +{{ data.tools.length - 3 }}
      </span>
    </div>

    <!-- 入口标记 -->
    <div v-if="data.isEntry" class="absolute -top-2 -right-2 w-5 h-5 bg-primary rounded-full flex items-center justify-center">
      <Play :size="10" class="text-white ml-0.5" />
    </div>

    <!-- Vue Flow 连接点 -->
    <Handle type="target" :position="Position.Top" class="!w-3 !h-3 !bg-primary/40 !border-2 !border-primary" />
    <Handle type="source" :position="Position.Bottom" class="!w-3 !h-3 !bg-primary/40 !border-2 !border-primary" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Handle, Position } from '@vue-flow/core'
import { Bot, Cpu, GitBranch, CircleDot, Merge, Repeat, Send, Play } from 'lucide-vue-next'

const props = defineProps({
  data: {
    type: Object,
    required: true
  }
})

const nodeIconMap = {
  LLM: Bot,
  TOOL: Cpu,
  CONDITION: GitBranch,
  MERGE: Merge,
  PARALLEL: CircleDot,
  LOOP: Repeat,
  OUTPUT: Send
}

const nodeIcon = computed(() => nodeIconMap[props.data.nodeType] || CircleDot)

const visibleTools = computed(() => (props.data.tools || []).slice(0, 3))
</script>

<style scoped>
@reference "@/styles/index.css";
</style>
