<template>
  <div class="h-[calc(100vh-4rem)] flex flex-col">

    <!-- 顶部导航 -->
    <header class="flex items-center justify-between px-6 py-4 shrink-0">
      <div class="flex items-center gap-4">
        <button
          @click="router.push({ name: 'GraphOrchestration' })"
          class="w-10 h-10 neo-convex rounded-xl flex items-center justify-center text-foreground/40 hover:text-primary active:scale-95 transition-all"
        >
          <ArrowLeft :size="18" />
        </button>
        <div>
          <h1 class="text-xl font-black tracking-tight">{{ graphData?.name || 'Graph 可视化' }}</h1>
          <p class="text-[10px] font-bold text-foreground/30 uppercase tracking-[0.2em]">
            {{ graphData?.id || '...' }}
          </p>
        </div>
      </div>
      <div class="flex items-center gap-3">
        <button
          @click="fitView"
          class="px-4 py-2 neo-convex rounded-xl text-[11px] font-black tracking-widest text-foreground/60 hover:text-primary active:scale-95 transition-all"
        >
          FIT
        </button>
      </div>
    </header>

    <!-- 画布区域 -->
    <div class="flex-1 relative neo-concave rounded-3xl mx-4 mb-4 overflow-hidden">

      <!-- 加载状态 -->
      <div v-if="loading" class="absolute inset-0 flex items-center justify-center z-10">
        <div class="text-center">
          <Loader2 :size="32" class="animate-spin text-primary mx-auto mb-3" />
          <p class="text-xs font-bold text-foreground/40">加载编排数据...</p>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else-if="!graphData || nodes.length === 0" class="absolute inset-0 flex items-center justify-center z-10">
        <div class="text-center">
          <GitBranch :size="48" class="mx-auto mb-3 text-foreground/15" />
          <p class="text-sm font-bold text-foreground/30">暂无节点数据</p>
          <button
            v-if="loadError"
            @click="loadGraphData"
            class="mt-4 px-4 py-2 neo-convex rounded-xl text-[11px] font-black tracking-widest text-primary active:scale-95 transition-all"
          >
            RETRY
          </button>
        </div>
      </div>

      <!-- Vue Flow -->
      <VueFlow
        v-else
        :nodes="nodes"
        :edges="edges"
        :node-types="nodeTypes"
        :edge-types="edgeTypes"
        :default-edge-options="defaultEdgeOptions"
        :fit-view-on-init="true"
        :nodes-draggable="true"
        :nodes-connectable="false"
        :zoom-on-scroll="true"
        :pan-on-drag="true"
        :min-zoom="0.2"
        :max-zoom="2"
        @node-click="onNodeClick"
        ref="vueFlowRef"
      >
        <Background :gap="20" :size="1" />
        <Controls position="bottom-right" />
        <MiniMap position="bottom-left" :pannable="true" :zoomable="true" />
      </VueFlow>
    </div>

    <!-- Tool 侧边面板 -->
    <ToolPanel
      :agent="selectedAgent"
      :tools="selectedTools"
      :visible="panelVisible"
      @update:visible="panelVisible = $event"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, markRaw } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { VueFlow, useVueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import { MiniMap } from '@vue-flow/minimap'
import { ArrowLeft, GitBranch, Loader2 } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { graphAPI, agentAPI } from '@/api'
import { useGraphLayout } from '@/composables/useGraphLayout'
import AgentNode from '@/components/graph/AgentNode.vue'
import ConditionalEdge from '@/components/graph/ConditionalEdge.vue'
import ToolPanel from '@/components/graph/ToolPanel.vue'

import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'

const route = useRoute()
const router = useRouter()

const graphData = ref(null)
const nodes = ref([])
const edges = ref([])
const loading = ref(false)
const loadError = ref(false)
const selectedAgent = ref(null)
const selectedTools = ref([])
const panelVisible = ref(false)

const nodeTypes = {
  agentNode: markRaw(AgentNode)
}

const edgeTypes = {
  conditionalEdge: markRaw(ConditionalEdge)
}

const defaultEdgeOptions = {
  animated: true,
  style: { stroke: 'var(--color-primary)', strokeWidth: 2, opacity: 0.4 }
}

const { fitView: vueFlowFitView } = useVueFlow()

const fitView = () => {
  vueFlowFitView({ padding: 0.2, duration: 300 })
}

const loadGraphData = async () => {
  const graphId = route.params.id
  if (!graphId) {
    router.replace({ name: 'GraphOrchestration' })
    return
  }

  loading.value = true
  loadError.value = false

  try {
    // 并行加载 Graph 定义和关联 Agent
    const [graph, agents] = await Promise.all([
      graphAPI.getById(graphId),
      graphAPI.getAgents(graphId).catch(() => [])
    ])

    graphData.value = graph

    // 构建 agentMap
    const agentMap = {}
    const agentList = agents || []
    agentList.forEach(a => { agentMap[a.id] = a })

    // 批量加载每个 Agent 的 Tool 列表
    const toolsMap = {}
    await Promise.all(
      agentList.map(async (agent) => {
        try {
          const tools = await agentAPI.getTools(agent.id)
          toolsMap[agent.id] = tools || []
        } catch {
          toolsMap[agent.id] = []
        }
      })
    )

    // 映射为 Vue Flow 格式
    const { vfNodes, vfEdges } = mapToVueFlow(graph, agentMap, toolsMap)

    // Dagre 自动布局
    const layoutedNodes = useGraphLayout(vfNodes, vfEdges)

    nodes.value = layoutedNodes
    edges.value = vfEdges
  } catch (err) {
    console.error('Graph 数据加载失败', err)
    ElMessage.error('Graph 数据加载失败')
    loadError.value = true
  } finally {
    loading.value = false
  }
}

const mapToVueFlow = (graph, agentMap, toolsMap) => {
  const vfNodes = (graph.nodes || []).map(node => {
    const agentId = node.config?.agentId || null
    const isLlmWithAgent = node.type === 'LLM' && agentId

    return {
      id: node.id,
      type: isLlmWithAgent ? 'agentNode' : 'default',
      position: { x: 0, y: 0 },
      data: {
        label: node.name || node.id,
        nodeType: node.type,
        isEntry: node.id === graph.entryNodeId,
        agentId,
        agent: agentMap[agentId] || null,
        tools: toolsMap[agentId] || []
      }
    }
  })

  const vfEdges = (graph.edges || []).map(edge => ({
    id: `${edge.from}-${edge.to}`,
    source: edge.from,
    target: edge.to,
    type: edge.condition ? 'conditionalEdge' : 'default',
    label: edge.condition && !edge.condition ? '' : undefined,
    animated: !edge.condition,
    data: { condition: edge.condition || '' },
    markerEnd: 'arrowclosed'
  }))

  return { vfNodes, vfEdges }
}

const onNodeClick = (event, node) => {
  if (node.data.agentId && node.data.agent) {
    selectedAgent.value = node.data.agent
    selectedTools.value = node.data.tools || []
    panelVisible.value = true
  }
}

onMounted(loadGraphData)
</script>

<style scoped>
@reference "@/styles/index.css";

/* Vue Flow 样式覆盖，融合 neomorphic 主题 */
:deep(.vue-flow__node-default) {
  background-color: var(--color-card);
  box-shadow: var(--shadow-neo-flat);
  border: 1px solid var(--glass-border, transparent);
  border-radius: 1rem;
  padding: 12px 16px;
  font-size: 13px;
  font-weight: 700;
  color: var(--color-foreground);
}

:deep(.vue-flow__node-default.selected) {
  box-shadow: 0 0 0 2px var(--color-primary);
}

:deep(.vue-flow__minimap) {
  background-color: var(--color-card);
  border-radius: 1rem;
  box-shadow: var(--shadow-neo-flat);
  overflow: hidden;
}

:deep(.vue-flow__controls) {
  background-color: transparent;
  border: none;
  box-shadow: none;
}

:deep(.vue-flow__controls-button) {
  background-color: var(--color-card);
  box-shadow: var(--shadow-neo-flat);
  border: 1px solid var(--glass-border, transparent);
  border-radius: 0.75rem;
  width: 36px;
  height: 36px;
  margin: 4px;
  color: var(--color-foreground);
  fill: var(--color-foreground);
  opacity: 0.6;
  transition: all 0.2s;
}

:deep(.vue-flow__controls-button:hover) {
  opacity: 1;
  color: var(--color-primary);
  fill: var(--color-primary);
}

:deep(.vue-flow__edge-path) {
  stroke: var(--color-primary);
  opacity: 0.4;
}

:deep(.vue-flow__background) {
  opacity: 0.3;
}
</style>
