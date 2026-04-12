<template>
  <el-drawer
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    title=""
    direction="rtl"
    size="400px"
    :show-close="true"
    :with-header="false"
    class="tool-panel-drawer"
  >
    <div class="p-6 space-y-6 h-full overflow-y-auto no-scrollbar">
      <!-- Agent 信息头 -->
      <div>
        <div class="flex items-center gap-3 mb-3">
          <div class="w-10 h-10 neo-concave rounded-xl flex items-center justify-center text-primary">
            <Bot :size="20" />
          </div>
          <div class="flex-1 min-w-0">
            <h3 class="text-lg font-black tracking-tight truncate">{{ agent?.name }}</h3>
            <p class="text-[10px] font-bold text-foreground/30 uppercase tracking-widest">{{ agent?.id }}</p>
          </div>
        </div>
        <p class="text-xs text-foreground/50 mb-3">{{ agent?.description || '暂无描述' }}</p>
        <div class="flex flex-wrap gap-2">
          <span class="px-2 py-1 neo-concave rounded-lg text-[10px] font-bold text-foreground/50">
            {{ agent?.llmProvider || 'N/A' }}
          </span>
          <span class="px-2 py-1 neo-concave rounded-lg text-[10px] font-mono font-bold text-primary/70">
            {{ agent?.llmModel || 'N/A' }}
          </span>
          <span class="px-2 py-1 neo-concave rounded-lg text-[10px] font-bold text-foreground/40">
            Max {{ agent?.maxIterations || 10 }} iters
          </span>
          <span class="px-2 py-1 neo-concave rounded-lg text-[10px] font-bold text-foreground/40">
            Timeout {{ agent?.timeoutSeconds || 120 }}s
          </span>
        </div>
      </div>

      <!-- Tool 列表 -->
      <div>
        <p class="text-[10px] font-bold text-foreground/30 uppercase tracking-[0.2em] mb-3">
          TOOLS ({{ tools.length }})
        </p>

        <div v-if="tools.length === 0" class="neo-concave rounded-2xl p-6 text-center">
          <Wrench :size="24" class="mx-auto mb-2 text-foreground/20" />
          <p class="text-xs text-foreground/40">暂无绑定的 Tool</p>
        </div>

        <div v-else class="space-y-3">
          <div
            v-for="tool in tools" :key="tool.id"
            class="neo-concave rounded-2xl p-4 space-y-2"
          >
            <div class="flex items-center justify-between">
              <p class="font-bold text-sm">{{ tool.name }}</p>
              <span
                class="px-2 py-0.5 rounded-lg text-[9px] font-black uppercase tracking-widest"
                :class="toolTypeClass(tool.type)"
              >
                {{ tool.type }}
              </span>
            </div>
            <p class="text-[11px] text-foreground/50 leading-relaxed">{{ tool.description || '暂无描述' }}</p>

            <!-- inputSchema 折叠 -->
            <details v-if="tool.inputSchema" class="mt-1">
              <summary class="text-[10px] font-bold text-primary/60 cursor-pointer select-none">
                INPUT SCHEMA
              </summary>
              <pre class="mt-2 p-3 neo-concave rounded-xl text-[10px] font-mono text-foreground/60 overflow-x-auto leading-relaxed whitespace-pre-wrap break-all">{{ formatSchema(tool.inputSchema) }}</pre>
            </details>

            <div class="flex items-center gap-2 pt-1">
              <span v-if="tool.isAsync" class="px-1.5 py-0.5 neo-concave rounded text-[9px] font-bold text-orange-500/60">
                ASYNC
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { computed } from 'vue'
import { Bot, Wrench } from 'lucide-vue-next'

const props = defineProps({
  agent: { type: Object, default: null },
  tools: { type: Array, default: () => [] },
  visible: { type: Boolean, default: false }
})

defineEmits(['update:visible'])

const toolTypeColors = {
  FUNCTION: 'text-blue-500/70 bg-blue-500/10',
  CODE_INTERPRETER: 'text-purple-500/70 bg-purple-500/10',
  RETRIEVER: 'text-green-500/70 bg-green-500/10',
  WEB_SEARCH: 'text-orange-500/70 bg-orange-500/10',
  CALCULATOR: 'text-yellow-600/70 bg-yellow-500/10',
  CUSTOM: 'text-foreground/50 bg-foreground/5'
}

const toolTypeClass = (type) => toolTypeColors[type] || toolTypeColors.CUSTOM

const formatSchema = (schema) => {
  try {
    return JSON.stringify(JSON.parse(schema), null, 2)
  } catch {
    return schema
  }
}
</script>

<style scoped>
@reference "@/styles/index.css";
</style>
