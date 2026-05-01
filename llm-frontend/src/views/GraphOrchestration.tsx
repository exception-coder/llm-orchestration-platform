import React, { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { GitBranch, Workflow, Plus, Trash2, X, ChevronRight, Activity } from 'lucide-react'
import { motion, AnimatePresence } from 'framer-motion'
import { graphAPI } from '@/api'

const GraphOrchestration: React.FC = () => {
  const navigate = useNavigate()

  const [graphs, setGraphs] = useState<any[]>([])
  const [graphAgents, setGraphAgents] = useState<Record<string, any[]>>({})
  const [callChain, setCallChain] = useState<any[]>([])
  const [loading, setLoading] = useState(false)
  const [dialogVisible, setDialogVisible] = useState(false)
  const [chainDialogVisible, setChainDialogVisible] = useState(false)
  const [isEdit, setIsEdit] = useState(false)
  const [saving, setSaving] = useState(false)

  const [form, setForm] = useState({ id: '', name: '', description: '', entryNodeId: '', nodes: [], edges: [] })

  const loadGraphs = useCallback(async () => {
    setLoading(true)
    try {
      const res: any = await graphAPI.getAll()
      setGraphs(res || [])
      // 加载每个 graph 关联的 agents
      for (const g of (res || [])) {
        try {
          const agents: any = await graphAPI.getAgents(g.id)
          setGraphAgents(prev => ({ ...prev, [g.id]: agents || [] }))
        } catch { /* ignore */ }
      }
    } catch (err) {
      console.error(err)
    } finally {
      setLoading(false)
    }
  }, [])

  const openCreate = () => {
    setIsEdit(false)
    setForm({ id: '', name: '', description: '', entryNodeId: '', nodes: [], edges: [] })
    setDialogVisible(true)
  }

  const editGraph = (graph: any) => {
    setIsEdit(true)
    setForm({ ...graph })
    setDialogVisible(true)
  }

  const saveGraph = async () => {
    setSaving(true)
    try {
      await graphAPI.save(form)
      setDialogVisible(false)
      loadGraphs()
    } catch (err) {
      console.error('Save failed', err)
    } finally {
      setSaving(false)
    }
  }

  const deleteGraph = async (id: string | number) => {
    if (window.confirm('确定删除该编排？')) {
      try {
        await graphAPI.delete(id)
        loadGraphs()
      } catch (err) {
        console.error('Delete failed', err)
      }
    }
  }

  const viewVisual = (graph: any) => {
    navigate(`/graph-visualization/${graph.id}`)
  }

  const viewCallChain = async (graph: any) => {
    try {
      const res: any = await graphAPI.getCallChain(graph.id)
      setCallChain(res || [])
      setChainDialogVisible(true)
    } catch (err) {
      console.error('Failed to get call chain', err)
    }
  }

  useEffect(() => {
    loadGraphs()
  }, [loadGraphs])

  return (
    <div className="max-w-6xl mx-auto space-y-12">

      {/* 顶部 */}
      <header className="flex items-center justify-between px-6">
        <div className="flex items-center gap-4">
          <div className="w-12 h-12 neo-convex rounded-2xl flex items-center justify-center text-primary shadow-lg">
            <GitBranch size={24} />
          </div>
          <div>
            <h1 className="text-2xl font-black tracking-tighter">Agent 编排</h1>
            <p className="text-[10px] font-bold text-foreground/30 uppercase tracking-[0.2em]">Graph Orchestration</p>
          </div>
        </div>
        <button
          onClick={openCreate}
          className="flex items-center gap-3 px-6 py-3 neo-convex rounded-2xl text-primary font-bold active:scale-95 transition-all"
        >
          <Plus size={18} />
          <span>新建编排</span>
        </button>
      </header>

      {/* Graph 列表 */}
      <section className="space-y-6">
        <AnimatePresence>
          {graphs.map((graph) => (
            <motion.div
              key={graph.id}
              layout
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95 }}
              className="neo-convex p-8 rounded-[2rem] transition-all hover:scale-[1.01]"
            >
              <div className="flex items-start justify-between mb-6 flex-wrap gap-4">
                <div className="flex items-center gap-4">
                  <div className="w-10 h-10 neo-concave rounded-xl flex items-center justify-center text-primary">
                    <Workflow size={20} />
                  </div>
                  <div>
                    <h3 className="text-lg font-black tracking-tight">{graph.name}</h3>
                    <p className="text-[10px] font-bold text-foreground/40 uppercase tracking-widest">{graph.id}</p>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <button
                    onClick={() => viewVisual(graph)}
                    className="px-4 py-2 neo-convex rounded-xl text-[11px] font-black tracking-widest text-foreground/60 hover:text-primary active:scale-95 transition-all"
                  >
                    VISUAL
                  </button>
                  <button
                    onClick={() => viewCallChain(graph)}
                    className="px-4 py-2 neo-convex rounded-xl text-[11px] font-black tracking-widest text-foreground/60 hover:text-primary active:scale-95 transition-all"
                  >
                    CHAIN
                  </button>
                  <button
                    onClick={() => editGraph(graph)}
                    className="px-4 py-2 neo-convex rounded-xl text-[11px] font-black tracking-widest text-foreground/60 hover:text-primary active:scale-95 transition-all"
                  >
                    EDIT
                  </button>
                  <button
                    onClick={() => deleteGraph(graph.id)}
                    className="w-10 h-10 neo-convex rounded-xl flex items-center justify-center text-red-500/40 hover:text-red-500 active:scale-90 transition-all"
                  >
                    <Trash2 size={16} />
                  </button>
                </div>
              </div>

              <p className="text-xs text-foreground/50 mb-6">{graph.description || '暂无描述'}</p>

              {/* 节点与边概览 */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div className="neo-concave rounded-2xl p-4 text-center">
                  <p className="text-2xl font-black text-primary">{graph.nodes?.length || 0}</p>
                  <p className="text-[10px] font-bold text-foreground/30 uppercase tracking-widest mt-1">Nodes</p>
                </div>
                <div className="neo-concave rounded-2xl p-4 text-center">
                  <p className="text-2xl font-black text-primary">{graph.edges?.length || 0}</p>
                  <p className="text-[10px] font-bold text-foreground/30 uppercase tracking-widest mt-1">Edges</p>
                </div>
                <div className="neo-concave rounded-2xl p-4 text-center">
                  <p className="text-sm font-mono font-bold text-foreground/60 truncate">{graph.entryNodeId || '-'}</p>
                  <p className="text-[10px] font-bold text-foreground/30 uppercase tracking-widest mt-1">Entry Node</p>
                </div>
              </div>

              {/* 关联 Agent 列表 */}
              {graphAgents[graph.id]?.length > 0 && (
                <div className="mt-6">
                  <p className="text-[10px] font-bold text-foreground/30 uppercase tracking-widest mb-3">Associated Agents</p>
                  <div className="flex flex-wrap gap-2">
                    {graphAgents[graph.id].map((a) => (
                      <span
                        key={a.id}
                        className="px-3 py-1 neo-concave rounded-xl text-[11px] font-bold text-foreground/60"
                      >{a.name}</span>
                    ))}
                  </div>
                </div>
              )}
            </motion.div>
          ))}
        </AnimatePresence>
      </section>

      {/* 空状态 */}
      {graphs.length === 0 && !loading && (
        <div className="flex flex-col items-center justify-center py-20 opacity-20">
          <GitBranch size={64} className="mb-4" />
          <p className="font-bold tracking-widest uppercase text-sm">No Graphs Found</p>
        </div>
      )}

      {/* 调用链弹窗 */}
      <AnimatePresence>
        {chainDialogVisible && (
          <div className="fixed inset-0 flex items-center justify-center z-50 p-4">
            <motion.div 
              initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
              onClick={() => setChainDialogVisible(false)}
              className="absolute inset-0 bg-black/40 backdrop-blur-sm"
            />
            <motion.div 
              initial={{ opacity: 0, scale: 0.9 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.9 }}
              className="relative w-full max-w-lg neo-convex rounded-[3rem] p-8 overflow-hidden bg-background"
            >
              <div className="flex items-center justify-between mb-8">
                <h2 className="text-xl font-black tracking-tight">执行调用链</h2>
                <button onClick={() => setChainDialogVisible(false)} className="p-2 neo-convex rounded-full text-foreground/20 hover:text-primary transition-all">
                  <X size={18} />
                </button>
              </div>
              
              <div className="space-y-4 max-h-[60vh] overflow-y-auto no-scrollbar">
                {callChain.length > 0 ? callChain.map((step, idx) => (
                  <div key={idx} className="flex items-center gap-4 neo-concave rounded-2xl p-5 group hover:ring-1 ring-primary/20 transition-all">
                    <div className="w-10 h-10 neo-convex rounded-xl flex items-center justify-center text-primary font-black text-sm shrink-0">
                      {idx + 1}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="font-bold text-sm truncate">{step.nodeName || step.nodeId}</p>
                      <p className="text-[10px] font-bold text-foreground/30 uppercase tracking-widest mt-0.5">{step.nodeType || '-'}</p>
                    </div>
                    <ChevronRight size={14} className="text-foreground/10 group-hover:text-primary transition-colors" />
                  </div>
                )) : (
                  <div className="text-center text-foreground/20 py-12">暂无调用链数据</div>
                )}
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* 新建/编辑弹窗 */}
      <AnimatePresence>
        {dialogVisible && (
          <div className="fixed inset-0 flex items-center justify-center z-50 p-4">
            <motion.div 
              initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
              onClick={() => setDialogVisible(false)}
              className="absolute inset-0 bg-black/40 backdrop-blur-sm"
            />
            <motion.div 
              initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, scale: 0.95 }}
              className="relative w-full max-w-lg neo-convex rounded-[3rem] p-10 overflow-hidden bg-background"
            >
              <div className="flex items-center justify-between mb-10">
                <h2 className="text-2xl font-black tracking-tighter italic uppercase">{isEdit ? 'Update Graph' : 'New Orchestration'}</h2>
                <button onClick={() => setDialogVisible(false)} className="p-2 neo-convex rounded-full text-foreground/20 hover:text-primary transition-all">
                  <X size={18} />
                </button>
              </div>

              <div className="space-y-6">
                {!isEdit && (
                  <div className="space-y-2">
                    <label className="text-[10px] font-black tracking-widest text-foreground/30 uppercase px-2">Identification ID</label>
                    <div className="neo-concave rounded-2xl p-1">
                      <input 
                        value={form.id} 
                        onChange={(e) => setForm({ ...form, id: e.target.value })}
                        placeholder="e.g. sales-pipeline"
                        className="w-full bg-transparent border-none focus:outline-none px-5 py-4 text-sm font-bold placeholder:text-foreground/10"
                      />
                    </div>
                  </div>
                )}

                <div className="space-y-2">
                  <label className="text-[10px] font-black tracking-widest text-foreground/30 uppercase px-2">Display Name</label>
                  <div className="neo-concave rounded-2xl p-1">
                    <input 
                      value={form.name} 
                      onChange={(e) => setForm({ ...form, name: e.target.value })}
                      placeholder="Enter orchestration name..."
                      className="w-full bg-transparent border-none focus:outline-none px-5 py-4 text-sm font-bold placeholder:text-foreground/10"
                    />
                  </div>
                </div>

                <div className="space-y-2">
                  <label className="text-[10px] font-black tracking-widest text-foreground/30 uppercase px-2">Orchestration Description</label>
                  <div className="neo-concave rounded-[2rem] p-1">
                    <textarea 
                      value={form.description} 
                      onChange={(e) => setForm({ ...form, description: e.target.value })}
                      placeholder="What is this flow responsible for?"
                      className="w-full bg-transparent border-none focus:outline-none px-5 py-4 text-sm resize-none h-24 placeholder:text-foreground/10"
                    />
                  </div>
                </div>

                <div className="space-y-2">
                  <label className="text-[10px] font-black tracking-widest text-foreground/30 uppercase px-2">Entry Node Reference</label>
                  <div className="neo-concave rounded-2xl p-1">
                    <input 
                      value={form.entryNodeId} 
                      onChange={(e) => setForm({ ...form, entryNodeId: e.target.value })}
                      placeholder="start-node-id"
                      className="w-full bg-transparent border-none focus:outline-none px-5 py-4 text-sm font-mono placeholder:text-foreground/10"
                    />
                  </div>
                </div>

                <div className="flex gap-4 pt-6">
                  <button 
                    onClick={() => setDialogVisible(false)}
                    className="flex-1 h-14 neo-convex rounded-2xl text-[11px] font-black tracking-widest text-foreground/40 hover:text-foreground/60 transition-all"
                  >
                    CANCEL
                  </button>
                  <button 
                    onClick={saveGraph}
                    disabled={saving}
                    className="flex-1 h-14 neo-convex rounded-2xl text-[11px] font-black tracking-widest text-primary shadow-xl active:scale-95 transition-all disabled:opacity-50"
                  >
                    {saving ? 'SAVING...' : 'CONFIRM SAVE'}
                  </button>
                </div>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

    </div>
  )
}

export default GraphOrchestration
