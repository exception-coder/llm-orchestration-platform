import React, { useMemo } from 'react'
import { Handle, Position, NodeProps } from '@xyflow/react'
import { Bot, Cpu, GitBranch, CircleDot, Merge, Repeat, Send, Play } from 'lucide-react'

const nodeIconMap: Record<string, any> = {
  LLM: Bot,
  TOOL: Cpu,
  CONDITION: GitBranch,
  MERGE: Merge,
  PARALLEL: CircleDot,
  LOOP: Repeat,
  OUTPUT: Send
}

const AgentNode: React.FC<NodeProps> = ({ data }) => {
  const Icon = nodeIconMap[data.nodeType as string] || CircleDot
  const visibleTools = ((data.tools as any[]) || []).slice(0, 3)

  return (
    <div
      className={`min-w-[200px] max-w-[240px] rounded-2xl p-4 cursor-pointer transition-all duration-200 neo-convex hover:scale-[1.02] ${
        data.isEntry ? 'ring-2 ring-primary ring-offset-2 ring-offset-background' : ''
      }`}
    >
      {/* 顶部：图标 + 名称 */}
      <div className="flex items-center gap-3 mb-2">
        <div className="w-9 h-9 neo-concave rounded-xl flex items-center justify-center text-primary shrink-0">
          <Icon size={16} />
        </div>
        <div className="min-w-0 flex-1">
          <p className="font-bold text-sm truncate leading-tight">{data.label as string}</p>
          <p className="text-[10px] font-bold text-foreground/40 uppercase tracking-widest">{data.nodeType as string}</p>
        </div>
      </div>

      {/* 中部：Agent 信息 */}
      {data.agent && (
        <div className="mb-2">
          <p className="text-[11px] text-foreground/60 truncate">{(data.agent as any).name}</p>
          <div className="flex items-center gap-1.5 mt-1">
            <span className="px-1.5 py-0.5 neo-concave rounded text-[9px] font-mono text-foreground/50">
              {(data.agent as any).llmModel || 'N/A'}
            </span>
          </div>
        </div>
      )}

      {/* 底部：Tool 标签 */}
      {((data.tools as any[])?.length > 0) && (
        <div className="flex flex-wrap gap-1 mt-2">
          {visibleTools.map((tool: any) => (
            <span
              key={tool.id}
              className="px-1.5 py-0.5 neo-concave rounded text-[9px] font-mono text-foreground/40 truncate max-w-[80px]"
              title={tool.name}
            >
              {tool.name}
            </span>
          ))}
          {(data.tools as any[]).length > 3 && (
            <span className="px-1.5 py-0.5 neo-concave rounded text-[9px] font-bold text-primary/60">
              +{(data.tools as any[]).length - 3}
            </span>
          )}
        </div>
      )}

      {/* 入口标记 */}
      {data.isEntry && (
        <div className="absolute -top-2 -right-2 w-5 h-5 bg-primary rounded-full flex items-center justify-center">
          <Play size={10} className="text-white ml-0.5" />
        </div>
      )}

      {/* Connection Handles */}
      <Handle 
        type="target" 
        position={Position.Top} 
        className="!w-3 !h-3 !bg-primary/40 !border-2 !border-primary" 
      />
      <Handle 
        type="source" 
        position={Position.Bottom} 
        className="!w-3 !h-3 !bg-primary/40 !border-2 !border-primary" 
      />
    </div>
  )
}

export default AgentNode
