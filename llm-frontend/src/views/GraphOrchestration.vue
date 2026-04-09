<template>
  <div class="max-w-6xl mx-auto space-y-12">

    <!-- 顶部 -->
    <header class="flex items-center justify-between px-6">
      <div class="flex items-center gap-4">
        <div class="w-12 h-12 neo-convex rounded-2xl flex items-center justify-center text-primary shadow-lg">
          <GitBranch :size="24" />
        </div>
        <div>
          <h1 class="text-2xl font-black tracking-tighter">Agent 编排</h1>
          <p class="text-[10px] font-bold text-foreground/30 uppercase tracking-[0.2em]">Graph Orchestration</p>
        </div>
      </div>
      <button
        @click="openCreate"
        class="flex items-center gap-3 px-6 py-3 neo-convex rounded-2xl text-primary font-bold active:scale-95 transition-all"
      >
        <Plus :size="18" />
        <span>新建编排</span>
      </button>
    </header>

    <!-- Graph 列表 -->
    <section class="space-y-6">
      <div
        v-for="graph in graphs" :key="graph.id"
        class="neo-convex p-8 rounded-[2rem] transition-all hover:scale-[1.01]"
        v-motion-pop
      >
        <div class="flex items-start justify-between mb-6">
          <div class="flex items-center gap-4">
            <div class="w-10 h-10 neo-concave rounded-xl flex items-center justify-center text-primary">
              <Workflow :size="20" />
            </div>
            <div>
              <h3 class="text-lg font-black tracking-tight">{{ graph.name }}</h3>
              <p class="text-[10px] font-bold text-foreground/40 uppercase tracking-widest">{{ graph.id }}</p>
            </div>
          </div>
          <div class="flex items-center gap-3">
            <button
              @click="viewCallChain(graph)"
              class="px-4 py-2 neo-convex rounded-xl text-[11px] font-black tracking-widest text-foreground/60 hover:text-primary active:scale-95 transition-all"
            >
              CHAIN
            </button>
            <button
              @click="editGraph(graph)"
              class="px-4 py-2 neo-convex rounded-xl text-[11px] font-black tracking-widest text-foreground/60 hover:text-primary active:scale-95 transition-all"
            >
              EDIT
            </button>
            <button
              @click="deleteGraph(graph.id)"
              class="w-10 h-10 neo-convex rounded-xl flex items-center justify-center text-red-500/40 hover:text-red-500 active:scale-90 transition-all"
            >
              <Trash2 :size="16" />
            </button>
          </div>
        </div>

        <p class="text-xs text-foreground/50 mb-6">{{ graph.description || '暂无描述' }}</p>

        <!-- 节点与边概览 -->
        <div class="grid grid-cols-3 gap-4">
          <div class="neo-concave rounded-2xl p-4 text-center">
            <p class="text-2xl font-black text-primary">{{ graph.nodes?.length || 0 }}</p>
            <p class="text-[10px] font-bold text-foreground/30 uppercase tracking-widest mt-1">Nodes</p>
          </div>
          <div class="neo-concave rounded-2xl p-4 text-center">
            <p class="text-2xl font-black text-primary">{{ graph.edges?.length || 0 }}</p>
            <p class="text-[10px] font-bold text-foreground/30 uppercase tracking-widest mt-1">Edges</p>
          </div>
          <div class="neo-concave rounded-2xl p-4 text-center">
            <p class="text-sm font-mono font-bold text-foreground/60 truncate">{{ graph.entryNodeId || '-' }}</p>
            <p class="text-[10px] font-bold text-foreground/30 uppercase tracking-widest mt-1">Entry Node</p>
          </div>
        </div>

        <!-- 关联 Agent 列表 -->
        <div v-if="graphAgents[graph.id]?.length" class="mt-6">
          <p class="text-[10px] font-bold text-foreground/30 uppercase tracking-widest mb-3">Associated Agents</p>
          <div class="flex flex-wrap gap-2">
            <span
              v-for="a in graphAgents[graph.id]" :key="a.id"
              class="px-3 py-1 neo-concave rounded-xl text-[11px] font-bold text-foreground/60"
            >{{ a.name }}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- 空状态 -->
    <div v-if="graphs.length === 0 && !loading" class="flex flex-col items-center justify-center py-20 opacity-20">
      <GitBranch :size="64" class="mb-4" />
      <p class="font-bold tracking-widest uppercase text-sm">No Graphs Found</p>
    </div>

    <!-- 调用链弹窗 -->
    <el-dialog v-model="chainDialogVisible" title="调用链" width="600px" destroy-on-close>
      <div v-if="callChain.length" class="space-y-3">
        <div
          v-for="(step, idx) in callChain" :key="idx"
          class="flex items-center gap-4 neo-concave rounded-2xl p-4"
        >
          <div class="w-8 h-8 neo-convex rounded-lg flex items-center justify-center text-primary font-black text-sm shrink-0">
            {{ idx + 1 }}
          </div>
          <div class="flex-1 min-w-0">
            <p class="font-bold text-sm truncate">{{ step.nodeName || step.nodeId }}</p>
            <p class="text-[10px] text-foreground/40">{{ step.nodeType || '-' }}</p>
          </div>
        </div>
      </div>
      <div v-else class="text-center text-foreground/40 py-8">暂无调用链数据</div>
    </el-dialog>

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑编排' : '新建编排'" width="600px" destroy-on-close>
      <el-form :model="form" label-position="top" class="space-y-2">
        <el-form-item label="ID（唯一标识）" v-if="!isEdit">
          <el-input v-model="form.id" placeholder="my-graph" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="我的编排流程" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="入口节点 ID">
          <el-input v-model="form.entryNodeId" placeholder="start-node" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveGraph" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { GitBranch, Workflow, Plus, Trash2 } from 'lucide-vue-next'
import { graphAPI } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const graphs = ref([])
const graphAgents = ref({})
const callChain = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const chainDialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)

const defaultForm = () => ({ id: '', name: '', description: '', entryNodeId: '', nodes: [], edges: [] })
const form = ref(defaultForm())

const loadGraphs = async () => {
  loading.value = true
  try {
    const res = await graphAPI.getAll()
    graphs.value = res || []
    // 加载每个 graph 关联的 agents
    for (const g of graphs.value) {
      try {
        const agents = await graphAPI.getAgents(g.id)
        graphAgents.value[g.id] = agents || []
      } catch { /* ignore */ }
    }
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  isEdit.value = false
  form.value = defaultForm()
  dialogVisible.value = true
}

const editGraph = (graph) => {
  isEdit.value = true
  form.value = { ...graph }
  dialogVisible.value = true
}

const saveGraph = async () => {
  saving.value = true
  try {
    await graphAPI.save(form.value)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadGraphs()
  } catch (err) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const deleteGraph = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该编排？', '提示', { type: 'warning' })
    await graphAPI.delete(id)
    ElMessage.success('已删除')
    loadGraphs()
  } catch (err) {
    if (err !== 'cancel') ElMessage.error('删除失败')
  }
}

const viewCallChain = async (graph) => {
  try {
    const res = await graphAPI.getCallChain(graph.id)
    callChain.value = res || []
    chainDialogVisible.value = true
  } catch (err) {
    ElMessage.error('获取调用链失败')
  }
}

onMounted(loadGraphs)
</script>

<style scoped>
@reference "@/styles/index.css";
</style>
