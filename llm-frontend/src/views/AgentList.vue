<template>
  <div class="max-w-6xl mx-auto space-y-12">

    <!-- 顶部 -->
    <header class="flex items-center justify-between px-6">
      <div class="flex items-center gap-4">
        <div class="w-12 h-12 neo-convex rounded-2xl flex items-center justify-center text-primary shadow-lg">
          <Bot :size="24" />
        </div>
        <div>
          <h1 class="text-2xl font-black tracking-tighter">智能体列表</h1>
          <p class="text-[10px] font-bold text-foreground/30 uppercase tracking-[0.2em]">Orchestration Hierarchy</p>
        </div>
      </div>
    </header>

    <!-- 层级列表：智能体 → Agent → Tools -->
    <section class="space-y-8">
      <div
        v-for="graph in graphs" :key="graph.id"
        class="neo-convex rounded-[2rem] overflow-hidden transition-all"
        v-motion-pop
      >
        <!-- 智能体（Graph）头部 -->
        <div
          class="flex items-center gap-4 px-8 py-6 cursor-pointer select-none group"
          @click="toggleGraph(graph.id)"
        >
          <ChevronRight
            :size="18"
            class="text-foreground/30 transition-transform duration-300 shrink-0"
            :class="{ 'rotate-90': expandedGraphs.has(graph.id) }"
          />
          <div class="w-10 h-10 neo-concave rounded-xl flex items-center justify-center text-primary shrink-0">
            <Workflow :size="20" />
          </div>
          <div class="flex-1 min-w-0">
            <h3 class="text-base font-black tracking-tight group-hover:text-primary transition-colors truncate">{{ graph.name }}</h3>
            <p class="text-[10px] text-foreground/40 truncate">{{ graph.description || graph.id }}</p>
          </div>
          <div class="flex items-center gap-4 shrink-0">
            <span class="text-[10px] font-bold text-foreground/30 uppercase tracking-widest">
              {{ graphAgentsMap[graph.id]?.length || 0 }} Agents
            </span>
            <button
              @click.stop="showGraphTools(graph)"
              class="px-3 py-1.5 neo-concave rounded-xl text-[10px] font-black tracking-widest text-foreground/40 hover:text-primary transition-all"
            >
              TOOLS
            </button>
          </div>
        </div>

        <!-- 展开区：Agent 子级 -->
        <transition name="expand">
          <div v-if="expandedGraphs.has(graph.id)" class="border-t border-foreground/5">
            <div v-if="!graphAgentsMap[graph.id]?.length" class="px-12 py-8 text-center text-foreground/20 text-xs">
              暂无关联 Agent
            </div>

            <div
              v-for="agent in graphAgentsMap[graph.id]" :key="agent.id"
              class="border-b border-foreground/5 last:border-b-0"
            >
              <!-- Agent 行 -->
              <div
                class="flex items-center gap-4 px-12 py-5 cursor-pointer select-none group/agent hover:bg-foreground/[0.02] transition-colors"
                @click="toggleAgent(agent.id)"
              >
                <ChevronRight
                  :size="14"
                  class="text-foreground/20 transition-transform duration-300 shrink-0"
                  :class="{ 'rotate-90': expandedAgents.has(agent.id) }"
                />
                <div class="w-8 h-8 neo-concave rounded-lg flex items-center justify-center shrink-0">
                  <Bot :size="16" class="text-foreground/50" />
                </div>
                <div class="flex-1 min-w-0">
                  <p class="text-sm font-bold tracking-tight group-hover/agent:text-primary transition-colors truncate">{{ agent.name }}</p>
                  <p class="text-[10px] text-foreground/30 truncate">{{ agent.llmProvider }} / {{ agent.llmModel }}</p>
                </div>
                <div class="flex items-center gap-3 shrink-0">
                  <!-- 调试按钮 -->
                  <button
                    @click.stop="openAgentDebug(agent)"
                    class="px-3 py-1 neo-concave rounded-lg text-[10px] font-black tracking-widest text-foreground/30 hover:text-primary transition-all"
                  >
                    DEBUG
                  </button>
                  <div
                    class="w-2 h-2 rounded-full"
                    :class="agent.enabled ? 'bg-green-500 shadow-[0_0_6px_theme(colors.green.500)]' : 'bg-foreground/10'"
                  ></div>
                  <span class="text-[10px] font-bold text-foreground/30 uppercase tracking-widest">
                    {{ agent.toolIds?.length || 0 }} Tools
                  </span>
                </div>
              </div>

              <!-- Agent 展开：Tool 详情 -->
              <transition name="expand">
                <div v-if="expandedAgents.has(agent.id)" class="bg-foreground/[0.02]">
                  <div class="px-16 py-4 grid grid-cols-4 gap-4">
                    <div class="neo-concave rounded-xl p-3 text-center">
                      <p class="text-[10px] font-bold text-foreground/30 uppercase">Provider</p>
                      <p class="text-xs font-mono text-foreground/60 mt-1">{{ agent.llmProvider || '-' }}</p>
                    </div>
                    <div class="neo-concave rounded-xl p-3 text-center">
                      <p class="text-[10px] font-bold text-foreground/30 uppercase">Model</p>
                      <p class="text-xs font-mono text-foreground/60 mt-1">{{ agent.llmModel || '-' }}</p>
                    </div>
                    <div class="neo-concave rounded-xl p-3 text-center">
                      <p class="text-[10px] font-bold text-foreground/30 uppercase">Max Iter</p>
                      <p class="text-xs font-mono text-foreground/60 mt-1">{{ agent.maxIterations || '-' }}</p>
                    </div>
                    <div class="neo-concave rounded-xl p-3 text-center">
                      <p class="text-[10px] font-bold text-foreground/30 uppercase">Timeout</p>
                      <p class="text-xs font-mono text-foreground/60 mt-1">{{ agent.timeoutSeconds ? agent.timeoutSeconds + 's' : '-' }}</p>
                    </div>
                  </div>

                  <!-- System Prompt 预览 -->
                  <div v-if="agent.systemPrompt" class="px-16 pb-4">
                    <p class="text-[10px] font-black text-foreground/20 uppercase tracking-[0.2em] mb-2">System Prompt</p>
                    <div class="neo-concave rounded-xl px-4 py-3 text-xs text-foreground/50 font-mono max-h-24 overflow-y-auto whitespace-pre-wrap">{{ agent.systemPrompt }}</div>
                  </div>

                  <!-- Tool 列表 -->
                  <div class="px-16 pb-5">
                    <p class="text-[10px] font-black text-foreground/20 uppercase tracking-[0.2em] mb-3">Tools</p>
                    <div v-if="agentToolsMap[agent.id]?.length" class="space-y-2">
                      <div
                        v-for="tool in agentToolsMap[agent.id]" :key="tool.id"
                        class="flex items-center gap-3 neo-concave rounded-xl px-4 py-3 cursor-pointer hover:ring-1 hover:ring-primary/20 transition-all"
                        @click.stop="openToolDebug(tool)"
                      >
                        <Wrench :size="14" class="text-primary/60 shrink-0" />
                        <div class="flex-1 min-w-0">
                          <p class="text-xs font-bold truncate">{{ tool.name }}</p>
                          <p class="text-[10px] text-foreground/30 truncate">{{ tool.description || tool.id }}</p>
                        </div>
                        <Play :size="12" class="text-foreground/20 shrink-0" />
                      </div>
                    </div>
                    <div v-else class="text-center text-foreground/20 text-xs py-4">
                      {{ loadingAgentTools.has(agent.id) ? '加载中...' : '暂无关联工具' }}
                    </div>
                  </div>
                </div>
              </transition>
            </div>
          </div>
        </transition>
      </div>
    </section>

    <!-- 空状态 -->
    <div v-if="graphs.length === 0 && !loading" class="flex flex-col items-center justify-center py-20 opacity-20">
      <Workflow :size="64" class="mb-4" />
      <p class="font-bold tracking-widest uppercase text-sm">No Orchestrations Found</p>
    </div>

    <!-- 智能体级 Tool 汇总弹窗 -->
    <el-dialog v-model="toolsDialogVisible" :title="toolsDialogTitle" width="640px" destroy-on-close>
      <div v-if="toolsDialogList.length" class="space-y-3 max-h-96 overflow-y-auto">
        <div
          v-for="tool in toolsDialogList" :key="tool.id"
          class="flex items-center gap-3 neo-concave rounded-xl px-5 py-4"
        >
          <Wrench :size="16" class="text-primary/60 shrink-0" />
          <div class="flex-1 min-w-0">
            <p class="text-sm font-bold truncate">{{ tool.name }}</p>
            <p class="text-[10px] text-foreground/40 truncate">{{ tool.description || tool.id }}</p>
          </div>
          <span class="px-2 py-0.5 text-[9px] font-bold text-foreground/30 neo-convex rounded-md uppercase shrink-0">
            {{ tool.type || 'function' }}
          </span>
        </div>
      </div>
      <div v-else class="text-center text-foreground/30 py-8">暂无关联工具</div>
    </el-dialog>

    <!-- Agent 调试弹窗 -->
    <el-dialog v-model="agentDebugVisible" :title="`调试 Agent：${debugAgent?.name || ''}`" width="720px" destroy-on-close>
      <div class="space-y-4">
        <el-form label-position="top">
          <el-form-item label="输入内容（User Input）">
            <el-input v-model="agentDebugInput" type="textarea" :rows="4" placeholder="输入要发送给 Agent 的指令..." />
          </el-form-item>
          <el-form-item label="上下文（Context JSON，可选）">
            <el-input v-model="agentDebugContext" type="textarea" :rows="2" placeholder='{"projectPath": "/path/to/project"}' />
          </el-form-item>
        </el-form>
        <!-- 执行流水结果 -->
        <div v-if="agentTraceResult" class="space-y-4">
          <!-- 流水概要 -->
          <div class="flex items-center gap-4">
            <p class="text-[10px] font-black text-foreground/30 uppercase tracking-[0.2em]">执行流水</p>
            <span class="font-mono text-[10px] text-primary/60">{{ agentTraceResult.traceId || agentTraceResult.executionId }}</span>
            <span class="px-2 py-0.5 text-[9px] font-bold rounded-md uppercase"
              :class="agentTraceResult.status === 'SUCCESS' ? 'bg-green-500/10 text-green-500' : 'bg-red-500/10 text-red-500'">
              {{ agentTraceResult.status }}
            </span>
            <span class="text-[10px] text-foreground/30">{{ agentTraceResult.elapsedMs }}ms / {{ agentTraceResult.iterations }} 轮</span>
          </div>

          <!-- Tool 调用步骤 -->
          <div v-if="agentTraceResult.toolCalls?.length" class="space-y-2">
            <p class="text-[10px] font-black text-foreground/20 uppercase tracking-[0.2em]">Tool 调用明细</p>
            <div v-for="(tc, idx) in agentTraceResult.toolCalls" :key="idx" class="neo-concave rounded-xl p-4 space-y-2">
              <div class="flex items-center gap-3">
                <span class="w-6 h-6 neo-convex rounded-lg flex items-center justify-center text-primary text-[10px] font-black shrink-0">{{ idx + 1 }}</span>
                <span class="font-bold text-xs">{{ tc.toolName || tc.toolId }}</span>
                <span :class="tc.success ? 'text-green-500' : 'text-red-500'" class="text-[10px] font-bold">{{ tc.success ? 'OK' : 'FAIL' }}</span>
                <span class="text-[10px] text-foreground/30 ml-auto">{{ tc.durationMs }}ms</span>
              </div>
              <details class="text-[11px]">
                <summary class="cursor-pointer text-foreground/40 hover:text-foreground/60">入参 / 出参</summary>
                <div class="mt-2 space-y-1">
                  <p class="text-[9px] font-bold text-foreground/20 uppercase">Input</p>
                  <pre class="bg-foreground/[0.03] rounded-lg p-2 text-[10px] font-mono text-foreground/50 overflow-auto max-h-32 whitespace-pre-wrap">{{ formatJson(tc.inputJson) }}</pre>
                  <p class="text-[9px] font-bold text-foreground/20 uppercase mt-2">Output</p>
                  <pre class="bg-foreground/[0.03] rounded-lg p-2 text-[10px] font-mono text-foreground/50 overflow-auto max-h-32 whitespace-pre-wrap">{{ formatJson(tc.output) }}</pre>
                </div>
              </details>
            </div>
          </div>

          <!-- 最终输出 -->
          <div class="space-y-2">
            <p class="text-[10px] font-black text-foreground/20 uppercase tracking-[0.2em]">最终输出</p>
            <pre class="neo-concave rounded-2xl p-4 text-xs font-mono text-foreground/70 overflow-auto max-h-48 whitespace-pre-wrap">{{ agentTraceResult.finalOutput }}</pre>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="agentDebugVisible = false">关闭</el-button>
        <el-button type="primary" @click="executeAgent" :loading="agentDebugLoading">执行</el-button>
      </template>
    </el-dialog>

    <!-- Tool 调试弹窗 -->
    <el-dialog v-model="toolDebugVisible" :title="`调试 Tool：${debugTool?.name || ''}`" width="720px" destroy-on-close>
      <div class="space-y-4">
        <!-- Schema 提示 -->
        <div v-if="debugTool?.inputSchema" class="space-y-2">
          <p class="text-[10px] font-black text-foreground/30 uppercase tracking-[0.2em]">Input Schema</p>
          <pre class="neo-concave rounded-xl p-3 text-[10px] font-mono text-foreground/40 max-h-28 overflow-auto whitespace-pre-wrap">{{ formatSchema(debugTool.inputSchema) }}</pre>
        </div>
        <el-form label-position="top">
          <el-form-item label="参数（JSON）">
            <el-input v-model="toolDebugParams" type="textarea" :rows="4" :placeholder="toolParamsPlaceholder" />
          </el-form-item>
        </el-form>
        <div v-if="toolDebugResult !== null" class="space-y-2">
          <div class="flex items-center gap-3">
            <p class="text-[10px] font-black text-foreground/30 uppercase tracking-[0.2em]">执行结果</p>
            <span v-if="toolTraceId" class="font-mono text-[10px] text-primary/60">{{ toolTraceId }}</span>
            <span v-if="toolDebugDuration" class="text-[10px] text-foreground/30 ml-auto">{{ toolDebugDuration }}ms</span>
          </div>
          <pre class="neo-concave rounded-2xl p-4 text-xs font-mono text-foreground/70 overflow-auto max-h-72 whitespace-pre-wrap">{{ toolDebugResult }}</pre>
        </div>
      </div>
      <template #footer>
        <el-button @click="toolDebugVisible = false">关闭</el-button>
        <el-button type="primary" @click="executeTool" :loading="toolDebugLoading">执行</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Bot, ChevronRight, Workflow, Wrench, Play } from 'lucide-vue-next'
import { graphAPI, agentAPI, toolAPI } from '@/api'
import { ElMessage } from 'element-plus'

const graphs = ref([])
const graphAgentsMap = reactive({})
const agentToolsMap = reactive({})
const loading = ref(false)

const expandedGraphs = ref(new Set())
const expandedAgents = ref(new Set())
const loadingAgentTools = ref(new Set())

const toolsDialogVisible = ref(false)
const toolsDialogTitle = ref('')
const toolsDialogList = ref([])

// ---- Agent 调试 ----
const agentDebugVisible = ref(false)
const debugAgent = ref(null)
const agentDebugInput = ref('')
const agentDebugContext = ref('')
const agentTraceResult = ref(null)
const agentDebugLoading = ref(false)

const openAgentDebug = (agent) => {
  debugAgent.value = agent
  agentDebugInput.value = ''
  agentDebugContext.value = ''
  agentTraceResult.value = null
  agentDebugVisible.value = true
}

const executeAgent = async () => {
  if (!agentDebugInput.value.trim()) {
    ElMessage.warning('请输入内容')
    return
  }
  agentDebugLoading.value = true
  agentTraceResult.value = null
  try {
    let context = {}
    if (agentDebugContext.value.trim()) {
      try { 
        context = JSON.parse(agentDebugContext.value) 
      } catch (e) {
        // 尝试自动修复 Windows 路径常见的单反斜杠问题
        try {
          const autoFixed = agentDebugContext.value.replace(/\\(?![\\"\/bfnrtu])/g, '\\\\')
          context = JSON.parse(autoFixed)
          ElMessage.info('已自动处理路径转义')
        } catch {
          ElMessage.error('Context JSON 格式错误，请检查反斜杠转义')
          agentDebugLoading.value = false
          return
        }
      }
    }
    const res = await agentAPI.execute(debugAgent.value.id, { input: agentDebugInput.value, context })
    agentTraceResult.value = res
  } catch (err) {
    agentTraceResult.value = { status: 'FAILED', finalOutput: '', errorMessage: err.response?.data?.message || err.message, toolCalls: [], elapsedMs: 0, iterations: 0 }
  } finally {
    agentDebugLoading.value = false
  }
}

const formatJson = (str) => {
  if (!str) return '-'
  try { return JSON.stringify(JSON.parse(str), null, 2) } catch { return str }
}

// ---- Tool 调试 ----
const toolDebugVisible = ref(false)
const debugTool = ref(null)
const toolDebugParams = ref('')
const toolDebugResult = ref(null)
const toolDebugLoading = ref(false)
const toolTraceId = ref(null)
const toolDebugDuration = ref(null)

const toolParamsPlaceholder = computed(() => {
  if (!debugTool.value?.inputSchema) return '{"key": "value"}'
  try {
    const schema = JSON.parse(debugTool.value.inputSchema)
    const sample = {}
    for (const [k, v] of Object.entries(schema)) {
      sample[k] = v.description || v.type || ''
    }
    return JSON.stringify(sample, null, 2)
  } catch { return '{"key": "value"}' }
})

const openToolDebug = (tool) => {
  debugTool.value = tool
  toolDebugParams.value = ''
  toolDebugResult.value = null
  toolTraceId.value = null
  toolDebugDuration.value = null
  toolDebugVisible.value = true
}

const executeTool = async () => {
  toolDebugLoading.value = true
  toolDebugResult.value = null
  toolTraceId.value = null
  toolDebugDuration.value = null
  try {
    let params = {}
    if (toolDebugParams.value.trim()) {
      try { 
        params = JSON.parse(toolDebugParams.value) 
      } catch (e) {
        try {
          const autoFixed = toolDebugParams.value.replace(/\\(?![\\"\/bfnrtu])/g, '\\\\')
          params = JSON.parse(autoFixed)
          ElMessage.info('已自动处理路径转义')
        } catch {
          ElMessage.error('参数 JSON 格式错误'); 
          toolDebugLoading.value = false; 
          return
        }
      }
    }
    const res = await toolAPI.execute(debugTool.value.id, params)
    // 新响应格式: { traceId, toolId, result, durationMs, success }
    toolTraceId.value = res.traceId
    toolDebugDuration.value = res.durationMs
    const raw = res.result || res
    toolDebugResult.value = typeof raw === 'string' ? raw : JSON.stringify(raw, null, 2)
    try { toolDebugResult.value = JSON.stringify(JSON.parse(toolDebugResult.value), null, 2) } catch {}
  } catch (err) {
    toolDebugResult.value = `错误: ${err.response?.data?.message || err.message}`
  } finally {
    toolDebugLoading.value = false
  }
}

const formatSchema = (schema) => {
  try { return JSON.stringify(JSON.parse(schema), null, 2) } catch { return schema }
}

// ---- 数据加载 ----

const loadData = async () => {
  loading.value = true
  try {
    const res = await graphAPI.getAll()
    graphs.value = res || []
    await Promise.all(graphs.value.map(g => loadGraphAgents(g.id)))
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
}

const loadGraphAgents = async (graphId) => {
  try {
    const res = await graphAPI.getAgents(graphId)
    graphAgentsMap[graphId] = res || []
  } catch {
    graphAgentsMap[graphId] = []
  }
}

const loadAgentTools = async (agentId) => {
  if (agentToolsMap[agentId]) return
  loadingAgentTools.value.add(agentId)
  try {
    const res = await agentAPI.getTools(agentId)
    agentToolsMap[agentId] = res || []
  } catch {
    agentToolsMap[agentId] = []
  } finally {
    loadingAgentTools.value.delete(agentId)
  }
}

const toggleGraph = (graphId) => {
  if (expandedGraphs.value.has(graphId)) {
    expandedGraphs.value.delete(graphId)
  } else {
    expandedGraphs.value.add(graphId)
  }
}

const toggleAgent = (agentId) => {
  if (expandedAgents.value.has(agentId)) {
    expandedAgents.value.delete(agentId)
  } else {
    expandedAgents.value.add(agentId)
    loadAgentTools(agentId)
  }
}

const showGraphTools = async (graph) => {
  toolsDialogTitle.value = `${graph.name} — 全部工具`
  const agents = graphAgentsMap[graph.id] || []
  await Promise.all(agents.map(a => loadAgentTools(a.id)))
  const seen = new Set()
  const allTools = []
  for (const a of agents) {
    for (const t of (agentToolsMap[a.id] || [])) {
      if (!seen.has(t.id)) {
        seen.add(t.id)
        allTools.push(t)
      }
    }
  }
  toolsDialogList.value = allTools
  toolsDialogVisible.value = true
}

onMounted(loadData)
</script>

<style scoped>
@reference "@/styles/index.css";

.expand-enter-active,
.expand-leave-active {
  transition: all 0.3s ease;
  overflow: hidden;
}
.expand-enter-from,
.expand-leave-to {
  opacity: 0;
  max-height: 0;
}
.expand-enter-to,
.expand-leave-from {
  opacity: 1;
  max-height: 2000px;
}
</style>
