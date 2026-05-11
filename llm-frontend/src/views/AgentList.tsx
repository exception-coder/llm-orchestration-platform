import React, { useState, useEffect, useMemo, useCallback } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { Bot, ChevronRight, Workflow, Wrench, Play } from 'lucide-react'
import { graphAPI, agentAPI, toolAPI } from '@/api'
import { useResponsive } from '@/hooks/useResponsive'

const AgentList: React.FC = () => {
  const [graphs, setGraphs] = useState<any[]>([])
  const [graphAgentsMap, setGraphAgentsMap] = useState<Record<string, any[]>>({})
  const [agentToolsMap, setAgentToolsMap] = useState<Record<string, any[]>>({})
  const [loading, setLoading] = useState(false)

  const [expandedGraphs, setExpandedGraphs] = useState<Set<string>>(new Set())
  const [expandedAgents, setExpandedAgents] = useState<Set<string>>(new Set())

  // 加载基础数据
  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const res: any = await graphAPI.getAll()
      setGraphs(res || [])
      // 预加载所有 Graph 的 Agents
      if (res) {
        for (const g of res) {
          loadGraphAgents(g.id)
        }
      }
    } catch (err) {
      console.error(err)
    } finally {
      setLoading(false)
    }
  }, [])

  const loadGraphAgents = async (graphId: string) => {
    try {
      const res: any = await graphAPI.getAgents(graphId)
      setGraphAgentsMap(prev => ({ ...prev, [graphId]: res || [] }))
    } catch {
      setGraphAgentsMap(prev => ({ ...prev, [graphId]: [] }))
    }
  }

  const loadAgentTools = async (agentId: string) => {
    if (agentToolsMap[agentId]) return
    try {
      const res: any = await agentAPI.getTools(agentId)
      setAgentToolsMap(prev => ({ ...prev, [agentId]: res || [] }))
    } catch {
      setAgentToolsMap(prev => ({ ...prev, [agentId]: [] }))
    }
  }

  useEffect(() => {
    loadData()
  }, [loadData])

  const toggleGraph = (graphId: string) => {
    const next = new Set(expandedGraphs)
    if (next.has(graphId)) {
      next.delete(graphId)
    } else {
      next.add(graphId)
    }
    setExpandedGraphs(next)
  }

  const toggleAgent = (agentId: string) => {
    const next = new Set(expandedAgents)
    if (next.has(agentId)) {
      next.delete(agentId)
    } else {
      next.add(agentId)
      loadAgentTools(agentId)
    }
    setExpandedAgents(next)
  }

  return (
    <div className="max-w-6xl mx-auto space-y-10">
      {/* 顶部 */}
      <header className="flex items-center justify-between px-2">
        <div className="flex items-center gap-5">
          <div className="w-14 h-14 bg-primary/10 rounded-2xl flex items-center justify-center text-primary shadow-sm ring-1 ring-primary/20">
            <Bot size={28} />
          </div>
          <div>
            <h1 className="text-3xl font-bold tracking-tight text-foreground">智能体列表</h1>
            <p className="text-xs font-medium text-muted-foreground uppercase tracking-[0.2em] mt-1">Orchestration Hierarchy</p>
          </div>
        </div>
        <button className="px-5 py-2.5 bg-primary text-primary-foreground rounded-xl text-sm font-semibold shadow-lg shadow-primary/20 hover:opacity-90 active:scale-95 transition-all">
          新建编排
        </button>
      </header>

      {/* 层级列表 */}
      <section className="space-y-6">
        {graphs.map((graph) => (
          <motion.div
            key={graph.id}
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            className="app-surface rounded-3xl overflow-hidden border border-border"
          >
            {/* Graph 头部 */}
            <div
              className="flex items-center gap-4 px-8 py-6 cursor-pointer select-none group transition-colors hover:bg-muted/50"
              onClick={() => toggleGraph(graph.id)}
            >
              <div className={`p-1 rounded-md transition-colors ${expandedGraphs.has(graph.id) ? 'bg-primary/10 text-primary' : 'text-muted-foreground'}`}>
                <ChevronRight
                  size={18}
                  className={`transition-transform duration-300 ${expandedGraphs.has(graph.id) ? 'rotate-90' : ''}`}
                />
              </div>
              <div className="w-10 h-10 bg-muted rounded-xl flex items-center justify-center text-muted-foreground group-hover:bg-primary/10 group-hover:text-primary transition-colors">
                <Workflow size={20} />
              </div>
              <div className="flex-1 min-w-0">
                <h3 className="text-lg font-bold tracking-tight text-foreground group-hover:text-primary transition-colors truncate">{graph.name}</h3>
                <p className="text-xs text-muted-foreground mt-0.5 truncate">{graph.description || graph.id}</p>
              </div>
              <div className="flex items-center gap-6 shrink-0">
                <div className="hidden sm:flex flex-col items-end">
                  <span className="text-[10px] font-bold text-muted-foreground uppercase tracking-widest">Population</span>
                  <span className="text-sm font-bold text-foreground">{graphAgentsMap[graph.id]?.length || 0} Agents</span>
                </div>
                <button className="px-4 py-2 bg-muted text-muted-foreground rounded-lg text-[10px] font-black tracking-widest hover:bg-primary/10 hover:text-primary transition-all uppercase">
                  Manage
                </button>
              </div>
            </div>

            {/* 展开区：Agent 子级 */}
            <AnimatePresence>
              {expandedGraphs.has(graph.id) && (
                <motion.div
                  initial={{ height: 0, opacity: 0 }}
                  animate={{ height: 'auto', opacity: 1 }}
                  exit={{ height: 0, opacity: 0 }}
                  className="bg-muted/30 border-t border-border overflow-hidden"
                >
                  {(!graphAgentsMap[graph.id] || graphAgentsMap[graph.id].length === 0) ? (
                    <div className="px-12 py-10 text-center">
                      <p className="text-muted-foreground/40 text-sm font-medium">暂无关联 Agent</p>
                    </div>
                  ) : (
                    graphAgentsMap[graph.id].map((agent) => (
                      <div key={agent.id} className="border-b border-border last:border-b-0">
                        {/* Agent 行 */}
                        <div
                          className="flex items-center gap-4 px-12 py-5 cursor-pointer select-none group/agent hover:bg-muted/50 transition-colors"
                          onClick={() => toggleAgent(agent.id)}
                        >
                          <ChevronRight
                            size={14}
                            className={`text-muted-foreground/40 transition-transform duration-300 shrink-0 ${expandedAgents.has(agent.id) ? 'rotate-90' : ''}`}
                          />
                          <div className="w-9 h-9 bg-background rounded-lg flex items-center justify-center border border-border group-hover/agent:border-primary/30 transition-colors">
                            <Bot size={18} className="text-muted-foreground group-hover/agent:text-primary transition-colors" />
                          </div>
                          <div className="flex-1 min-w-0">
                            <p className="text-sm font-semibold text-foreground group-hover/agent:text-primary transition-colors truncate">{agent.name}</p>
                            <div className="flex items-center gap-2 mt-0.5">
                              <span className="text-[10px] font-medium text-muted-foreground px-1.5 py-0.5 bg-background border border-border rounded-md uppercase tracking-tight">{agent.llmProvider}</span>
                              <span className="text-[10px] font-mono text-muted-foreground/60">{agent.llmModel}</span>
                            </div>
                          </div>
                          <div className="flex items-center gap-4 shrink-0">
                            <button className="px-3 py-1.5 text-[10px] font-bold text-muted-foreground hover:text-primary transition-all uppercase tracking-tighter">
                              Config
                            </button>
                            <div className={`w-2.5 h-2.5 rounded-full ring-4 ring-offset-2 ${agent.enabled ? 'bg-green-500 ring-green-500/10' : 'bg-muted ring-transparent'}`}></div>
                          </div>
                        </div>

                        {/* Agent 展开：Tool 详情 */}
                        <AnimatePresence>
                          {expandedAgents.has(agent.id) && (
                            <motion.div
                              initial={{ height: 0, opacity: 0 }}
                              animate={{ height: 'auto', opacity: 1 }}
                              exit={{ height: 0, opacity: 0 }}
                              className="bg-background overflow-hidden"
                            >
                              <div className="px-16 py-8 grid grid-cols-2 md:grid-cols-4 gap-4">
                                {[
                                  { label: 'Provider', value: agent.llmProvider },
                                  { label: 'Model', value: agent.llmModel },
                                  { label: 'Max Iter', value: agent.maxIterations },
                                  { label: 'Timeout', value: agent.timeoutSeconds ? agent.timeoutSeconds + 's' : '-' }
                                ].map((stat) => (
                                  <div key={stat.label} className="p-4 rounded-2xl border border-border bg-muted/20">
                                    <p className="text-[10px] font-bold text-muted-foreground uppercase tracking-widest">{stat.label}</p>
                                    <p className="text-xs font-bold text-foreground mt-1 truncate">{stat.value || '-'}</p>
                                  </div>
                                ))}
                              </div>
                              
                              {/* Tool 列表 */}
                              <div className="px-16 pb-8">
                                <div className="flex items-center gap-3 mb-4">
                                  <p className="text-[10px] font-black text-muted-foreground uppercase tracking-[0.2em]">Associated Tools</p>
                                  <div className="flex-1 h-px bg-border"></div>
                                </div>
                                {agentToolsMap[agent.id]?.length > 0 ? (
                                  <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                                    {agentToolsMap[agent.id].map((tool) => (
                                      <div key={tool.id} className="flex items-center gap-4 p-4 rounded-2xl border border-border hover:border-primary/30 hover:shadow-sm transition-all group/tool cursor-pointer bg-muted/5">
                                        <div className="w-8 h-8 rounded-lg bg-background flex items-center justify-center border border-border group-hover/tool:text-primary transition-colors">
                                          <Wrench size={14} />
                                        </div>
                                        <div className="flex-1 min-w-0">
                                          <p className="text-xs font-bold text-foreground truncate">{tool.name}</p>
                                          <p className="text-[10px] text-muted-foreground truncate">{tool.description || tool.id}</p>
                                        </div>
                                        <Play size={14} className="text-muted-foreground opacity-0 group-hover/tool:opacity-100 transition-all transform translate-x-[-10px] group-hover:translate-x-0" />
                                      </div>
                                    ))}
                                  </div>
                                ) : (
                                  <div className="text-center py-6 border border-dashed border-border rounded-2xl bg-muted/5">
                                    <p className="text-xs text-muted-foreground/50">暂无关联工具</p>
                                  </div>
                                )}
                              </div>
                            </motion.div>
                          )}
                        </AnimatePresence>
                      </div>
                    ))
                  )}
                </motion.div>
              )}
            </AnimatePresence>
          </motion.div>
        ))}
      </section>

      {/* 空状态 */}
      {graphs.length === 0 && !loading && (
        <div className="flex flex-col items-center justify-center py-32 text-muted-foreground/20">
          <Workflow size={80} strokeWidth={1} className="mb-6 animate-pulse" />
          <p className="font-bold tracking-[0.3em] uppercase text-sm">No Orchestrations Found</p>
          <button onClick={loadData} className="mt-8 px-6 py-2 rounded-full border border-border text-xs font-bold hover:bg-muted transition-colors">
            Refresh Data
          </button>
        </div>
      )}
    </div>
  )
}

export default AgentList
