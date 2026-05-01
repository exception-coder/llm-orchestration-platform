import React, { useState, useEffect, useCallback, useMemo } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  ReactFlow,
  Background,
  Controls,
  MiniMap,
  useNodesState,
  useEdgesState,
  addEdge,
  Connection,
  Edge,
  MarkerType,
  ReactFlowProvider,
  useReactFlow
} from '@xyflow/react'
import { ArrowLeft, GitBranch, Loader2 } from 'lucide-react'
import { graphAPI, agentAPI } from '@/api'
import { useGraphLayout } from '@/hooks/useGraphLayout'
import AgentNode from '@/components/graph/AgentNode'
import ConditionalEdge from '@/components/graph/ConditionalEdge'
import ToolPanel from '@/components/graph/ToolPanel'

import '@xyflow/react/dist/style.css'

const nodeTypes = {
  agentNode: AgentNode
}

const edgeTypes = {
  conditionalEdge: ConditionalEdge
}

const defaultEdgeOptions = {
  animated: true,
  style: { stroke: 'var(--color-primary)', strokeWidth: 2, opacity: 0.4 },
  markerEnd: {
    type: MarkerType.ArrowClosed,
    color: 'var(--color-primary)'
  }
}

const GraphVisualizationContent: React.FC = () => {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { fitView } = useReactFlow()

  const [graphData, setGraphData] = useState<any>(null)
  const [nodes, setNodes, onNodesChange] = useNodesState([])
  const [edges, setEdges, onEdgesChange] = useEdgesState([])
  const [loading, setLoading] = useState(false)
  const [loadError, setLoadError] = useState(false)
  
  const [selectedAgent, setSelectedAgent] = useState<any>(null)
  const [selectedTools, setSelectedTools] = useState<any[]>([])
  const [panelVisible, setPanelVisible] = useState(false)

  const mapToXYFlow = useCallback((graph: any, agentMap: any, toolsMap: any) => {
    const vfNodes = (graph.nodes || []).map((node: any) => {
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

    const vfEdges = (graph.edges || []).map((edge: any) => ({
      id: `${edge.from}-${edge.to}`,
      source: edge.from,
      target: edge.to,
      type: edge.condition ? 'conditionalEdge' : 'default',
      animated: !edge.condition,
      data: { condition: edge.condition || '' },
      markerEnd: { type: MarkerType.ArrowClosed, color: 'var(--color-primary)' }
    }))

    return { vfNodes, vfEdges }
  }, [])

  const loadGraphData = useCallback(async () => {
    if (!id) {
      navigate('/graph-orchestration')
      return
    }

    setLoading(true)
    setLoadError(false)

    try {
      const [graph, agents]: [any, any] = await Promise.all([
        graphAPI.getById(id),
        graphAPI.getAgents(id).catch(() => [])
      ])

      setGraphData(graph)

      const agentMap: any = {}
      const agentList = agents || []
      agentList.forEach((a: any) => { agentMap[a.id] = a })

      const toolsMap: any = {}
      await Promise.all(
        agentList.map(async (agent: any) => {
          try {
            const tools: any = await agentAPI.getTools(agent.id)
            toolsMap[agent.id] = tools || []
          } catch {
            toolsMap[agent.id] = []
          }
        })
      )

      const { vfNodes, vfEdges } = mapToXYFlow(graph, agentMap, toolsMap)
      const layoutedNodes = useGraphLayout(vfNodes, vfEdges)

      setNodes(layoutedNodes)
      setEdges(vfEdges)
      
      // Delay fitView to ensure nodes are rendered
      setTimeout(() => fitView({ padding: 0.2 }), 100)
    } catch (err) {
      console.error('Graph load failed', err)
      setLoadError(true)
    } finally {
      setLoading(false)
    }
  }, [id, navigate, mapToXYFlow, setNodes, setEdges, fitView])

  useEffect(() => {
    loadGraphData()
  }, [loadGraphData])

  const onNodeClick = (_: any, node: any) => {
    if (node.data.agentId && node.data.agent) {
      setSelectedAgent(node.data.agent)
      setSelectedTools(node.data.tools || [])
      setPanelVisible(true)
    }
  }

  return (
    <div className="h-[calc(100vh-8rem)] flex flex-col relative">
      {/* 顶部导航 */}
      <header className="flex items-center justify-between px-6 py-4 shrink-0">
        <div className="flex items-center gap-4">
          <button
            onClick={() => navigate('/graph-orchestration')}
            className="w-10 h-10 neo-convex rounded-xl flex items-center justify-center text-foreground/40 hover:text-primary active:scale-95 transition-all"
          >
            <ArrowLeft size={18} />
          </button>
          <div>
            <h1 className="text-xl font-black tracking-tight">{graphData?.name || 'Graph 可视化'}</h1>
            <p className="text-[10px] font-bold text-foreground/30 uppercase tracking-[0.2em]">
              {graphData?.id || '...'}
            </p>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={() => fitView({ padding: 0.2 })}
            className="px-4 py-2 neo-convex rounded-xl text-[11px] font-black tracking-widest text-foreground/60 hover:text-primary active:scale-95 transition-all"
          >
            FIT
          </button>
        </div>
      </header>

      {/* 画布区域 */}
      <div className="flex-1 relative neo-concave rounded-3xl mx-4 mb-4 overflow-hidden">
        {loading && (
          <div className="absolute inset-0 flex items-center justify-center z-10 bg-background/50 backdrop-blur-sm">
            <div className="text-center">
              <Loader2 size={32} className="animate-spin text-primary mx-auto mb-3" />
              <p className="text-xs font-bold text-foreground/40 uppercase tracking-widest">Loading Orchestration...</p>
            </div>
          </div>
        )}

        {!loading && (!graphData || nodes.length === 0) && (
          <div className="absolute inset-0 flex items-center justify-center z-10">
            <div className="text-center">
              <GitBranch size={48} className="mx-auto mb-3 text-foreground/15" />
              <p className="text-sm font-bold text-foreground/30">暂无节点数据</p>
              {loadError && (
                <button
                  onClick={loadGraphData}
                  className="mt-4 px-4 py-2 neo-convex rounded-xl text-[11px] font-black tracking-widest text-primary active:scale-95 transition-all"
                >
                  RETRY
                </button>
              )}
            </div>
          </div>
        )}

        <ReactFlow
          nodes={nodes}
          edges={edges}
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          nodeTypes={nodeTypes}
          edgeTypes={edgeTypes}
          defaultEdgeOptions={defaultEdgeOptions}
          onNodeClick={onNodeClick}
          minZoom={0.2}
          maxZoom={2}
          fitView
        >
          <Background gap={20} size={1} />
          <Controls position="bottom-right" className="!bg-transparent !border-none !shadow-none" />
          <MiniMap 
            position="bottom-left" 
            pannable 
            zoomable 
            className="!bg-card !rounded-2xl !border-white/10 !shadow-2xl"
          />
        </ReactFlow>
      </div>

      {/* Tool 侧边面板 */}
      <ToolPanel
        agent={selectedAgent}
        tools={selectedTools}
        visible={panelVisible}
        onClose={() => setPanelVisible(false)}
      />
    </div>
  )
}

const GraphVisualization: React.FC = () => (
  <ReactFlowProvider>
    <GraphVisualizationContent />
  </ReactFlowProvider>
)

export default GraphVisualization
