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
    <div className="max-w-6xl mx-auto space-y-12">
      {/* 顶部 */}
      <header className="flex items-center justify-between px-6">
        <div className="flex items-center gap-4">
          <div className="w-12 h-12 neo-convex rounded-2xl flex items-center justify-center text-primary shadow-lg">
            <Bot size={24} />
          </div>
          <div>
            <h1 className="text-2xl font-black tracking-tighter text-slate-900 dark:text-white">智能体列表</h1>
            <p className="text-[10px] font-bold text-foreground/30 uppercase tracking-[0.2em]">Orchestration Hierarchy</p>
          </div>
        </div>
      </header>

      {/* 层级列表 */}
      <section className="space-y-8">
        {graphs.map((graph) => (
          <motion.div
            key={graph.id}
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="neo-convex rounded-[2rem] overflow-hidden transition-all"
          >
            {/* Graph 头部 */}
            <div
              className="flex items-center gap-4 px-8 py-6 cursor-pointer select-none group"
              onClick={() => toggleGraph(graph.id)}
            >
              <ChevronRight
                size={18}
                className={`text-foreground/30 transition-transform duration-300 shrink-0 ${expandedGraphs.has(graph.id) ? 'rotate-90' : ''}`}
              />
              <div className="w-10 h-10 neo-concave rounded-xl flex items-center justify-center text-primary shrink-0">
                <Workflow size={20} />
              </div>
              <div className="flex-1 min-w-0">
                <h3 className="text-base font-black tracking-tight group-hover:text-primary transition-colors truncate">{graph.name}</h3>
                <p className="text-[10px] text-foreground/40 truncate">{graph.description || graph.id}</p>
              </div>
              <div className="flex items-center gap-4 shrink-0">
                <span className="text-[10px] font-bold text-foreground/30 uppercase tracking-widest">
                  {graphAgentsMap[graph.id]?.length || 0} Agents
                </span>
                <button className="px-3 py-1.5 neo-concave rounded-xl text-[10px] font-black tracking-widest text-foreground/40 hover:text-primary transition-all">
                  TOOLS
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
                  className="border-t border-foreground/5 overflow-hidden"
                >
                  {(!graphAgentsMap[graph.id] || graphAgentsMap[graph.id].length === 0) ? (
                    <div className="px-12 py-8 text-center text-foreground/20 text-xs">暂无关联 Agent</div>
                  ) : (
                    graphAgentsMap[graph.id].map((agent) => (
                      <div key={agent.id} className="border-b border-foreground/5 last:border-b-0">
                        {/* Agent 行 */}
                        <div
                          className="flex items-center gap-4 px-12 py-5 cursor-pointer select-none group/agent hover:bg-foreground/[0.02] transition-colors"
                          onClick={() => toggleAgent(agent.id)}
                        >
                          <ChevronRight
                            size={14}
                            className={`text-foreground/20 transition-transform duration-300 shrink-0 ${expandedAgents.has(agent.id) ? 'rotate-90' : ''}`}
                          />
                          <div className="w-8 h-8 neo-concave rounded-lg flex items-center justify-center shrink-0">
                            <Bot size={16} className="text-foreground/50" />
                          </div>
                          <div className="flex-1 min-w-0">
                            <p className="text-sm font-bold tracking-tight group-hover/agent:text-primary transition-colors truncate">{agent.name}</p>
                            <p className="text-[10px] text-foreground/30 truncate">{agent.llmProvider} / {agent.llmModel}</p>
                          </div>
                          <div className="flex items-center gap-3 shrink-0">
                            <button className="px-3 py-1 neo-concave rounded-lg text-[10px] font-black tracking-widest text-foreground/30 hover:text-primary transition-all">
                              DEBUG
                            </button>
                            <div className={`w-2 h-2 rounded-full ${agent.enabled ? 'bg-green-500 shadow-[0_0_6px_theme(colors.green.500)]' : 'bg-foreground/10'}`}></div>
                          </div>
                        </div>

                        {/* Agent 展开：Tool 详情 */}
                        <AnimatePresence>
                          {expandedAgents.has(agent.id) && (
                            <motion.div
                              initial={{ height: 0, opacity: 0 }}
                              animate={{ height: 'auto', opacity: 1 }}
                              exit={{ height: 0, opacity: 0 }}
                              className="bg-foreground/[0.02] overflow-hidden"
                            >
                              <div className="px-16 py-4 grid grid-cols-2 md:grid-cols-4 gap-4">
                                <div className="neo-concave rounded-xl p-3 text-center">
                                  <p className="text-[10px] font-bold text-foreground/30 uppercase">Provider</p>
                                  <p className="text-xs font-mono text-foreground/60 mt-1">{agent.llmProvider || '-'}</p>
                                </div>
                                <div className="neo-concave rounded-xl p-3 text-center">
                                  <p className="text-[10px] font-bold text-foreground/30 uppercase">Model</p>
                                  <p className="text-xs font-mono text-foreground/60 mt-1">{agent.llmModel || '-'}</p>
                                </div>
                                <div className="neo-concave rounded-xl p-3 text-center">
                                  <p className="text-[10px] font-bold text-foreground/30 uppercase">Max Iter</p>
                                  <p className="text-xs font-mono text-foreground/60 mt-1">{agent.maxIterations || '-'}</p>
                                </div>
                                <div className="neo-concave rounded-xl p-3 text-center">
                                  <p className="text-[10px] font-bold text-foreground/30 uppercase">Timeout</p>
                                  <p className="text-xs font-mono text-foreground/60 mt-1">{agent.timeoutSeconds ? agent.timeoutSeconds + 's' : '-'}</p>
                                </div>
                              </div>
                              
                              {/* Tool 列表 */}
                              <div className="px-16 pb-5">
                                <p className="text-[10px] font-black text-foreground/20 uppercase tracking-[0.2em] mb-3">Tools</p>
                                {agentToolsMap[agent.id]?.length > 0 ? (
                                  <div className="space-y-2">
                                    {agentToolsMap[agent.id].map((tool) => (
                                      <div key={tool.id} className="flex items-center gap-3 neo-concave rounded-xl px-4 py-3 cursor-pointer hover:ring-1 hover:ring-primary/20 transition-all">
                                        <Wrench size={14} className="text-primary/60 shrink-0" />
                                        <div className="flex-1 min-w-0">
                                          <p className="text-xs font-bold truncate">{tool.name}</p>
                                          <p className="text-[10px] text-foreground/30 truncate">{tool.description || tool.id}</p>
                                        </div>
                                        <Play size={12} className="text-foreground/20 shrink-0" />
                                      </div>
                                    ))}
                                  </div>
                                ) : (
                                  <div className="text-center text-foreground/20 text-xs py-4">暂无关联工具</div>
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
        <div className="flex flex-col items-center justify-center py-20 opacity-20">
          <Workflow size={64} className="mb-4" />
          <p className="font-bold tracking-widest uppercase text-sm">No Orchestrations Found</p>
        </div>
      )}
    </div>
  )
}

export default AgentList
