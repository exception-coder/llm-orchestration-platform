import React from 'react'
import { Bot, Wrench, X } from 'lucide-react'
import { motion, AnimatePresence } from 'framer-motion'

interface ToolPanelProps {
  agent: any
  tools: any[]
  visible: boolean
  onClose: () => void
}

const toolTypeColors: Record<string, string> = {
  FUNCTION: 'text-blue-500/70 bg-blue-500/10',
  CODE_INTERPRETER: 'text-purple-500/70 bg-purple-500/10',
  RETRIEVER: 'text-green-500/70 bg-green-500/10',
  WEB_SEARCH: 'text-orange-500/70 bg-orange-500/10',
  CALCULATOR: 'text-yellow-600/70 bg-yellow-500/10',
  CUSTOM: 'text-foreground/50 bg-foreground/5'
}

const ToolPanel: React.FC<ToolPanelProps> = ({ agent, tools, visible, onClose }) => {
  const formatSchema = (schema: any) => {
    if (typeof schema === 'object') return JSON.stringify(schema, null, 2)
    try {
      return JSON.stringify(JSON.parse(schema), null, 2)
    } catch {
      return schema
    }
  }

  return (
    <AnimatePresence>
      {visible && (
        <>
          {/* Overlay */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
            className="fixed inset-0 bg-black/20 backdrop-blur-sm z-[100]"
          />
          
          {/* Panel */}
          <motion.div
            initial={{ x: '100%' }}
            animate={{ x: 0 }}
            exit={{ x: '100%' }}
            transition={{ type: 'spring', damping: 25, stiffness: 200 }}
            className="fixed top-0 right-0 h-full w-full max-w-[400px] bg-background shadow-2xl z-[101] border-l border-white/10"
          >
            <div className="p-6 space-y-6 h-full overflow-y-auto no-scrollbar relative">
              <button 
                onClick={onClose}
                className="absolute top-6 right-6 p-2 neo-convex rounded-full text-foreground/40 hover:text-primary transition-all"
              >
                <X size={18} />
              </button>

              {/* Agent 信息头 */}
              <div className="pt-8">
                <div className="flex items-center gap-3 mb-3">
                  <div className="w-10 h-10 neo-concave rounded-xl flex items-center justify-center text-primary">
                    <Bot size={20} />
                  </div>
                  <div className="flex-1 min-w-0">
                    <h3 className="text-lg font-black tracking-tight truncate">{agent?.name}</h3>
                    <p className="text-[10px] font-bold text-foreground/30 uppercase tracking-widest">{agent?.id}</p>
                  </div>
                </div>
                <p className="text-xs text-foreground/50 mb-3">{agent?.description || '暂无描述'}</p>
                <div className="flex flex-wrap gap-2">
                  <span className="px-2 py-1 neo-concave rounded-lg text-[10px] font-bold text-foreground/50">
                    {agent?.llmProvider || 'N/A'}
                  </span>
                  <span className="px-2 py-1 neo-concave rounded-lg text-[10px] font-mono font-bold text-primary/70">
                    {agent?.llmModel || 'N/A'}
                  </span>
                  <span className="px-2 py-1 neo-concave rounded-lg text-[10px] font-bold text-foreground/40">
                    Max {agent?.maxIterations || 10} iters
                  </span>
                  <span className="px-2 py-1 neo-concave rounded-lg text-[10px] font-bold text-foreground/40">
                    Timeout {agent?.timeoutSeconds || 120}s
                  </span>
                </div>
              </div>

              {/* Tool 列表 */}
              <div>
                <p className="text-[10px] font-bold text-foreground/30 uppercase tracking-[0.2em] mb-3">
                  TOOLS ({tools.length})
                </p>

                {tools.length === 0 ? (
                  <div className="neo-concave rounded-2xl p-6 text-center">
                    <Wrench size={24} className="mx-auto mb-2 text-foreground/20" />
                    <p className="text-xs text-foreground/40">暂无绑定的 Tool</p>
                  </div>
                ) : (
                  <div className="space-y-3">
                    {tools.map((tool) => (
                      <div
                        key={tool.id}
                        className="neo-concave rounded-2xl p-4 space-y-2"
                      >
                        <div className="flex items-center justify-between">
                          <p className="font-bold text-sm">{tool.name}</p>
                          <span
                            className={`px-2 py-0.5 rounded-lg text-[9px] font-black uppercase tracking-widest ${
                              toolTypeColors[tool.type] || toolTypeColors.CUSTOM
                            }`}
                          >
                            {tool.type}
                          </span>
                        </div>
                        <p className="text-[11px] text-foreground/50 leading-relaxed">{tool.description || '暂无描述'}</p>

                        {/* inputSchema 折叠 */}
                        {tool.inputSchema && (
                          <details className="mt-1">
                            <summary className="text-[10px] font-bold text-primary/60 cursor-pointer select-none">
                              INPUT SCHEMA
                            </summary>
                            <pre className="mt-2 p-3 neo-concave rounded-xl text-[10px] font-mono text-foreground/60 overflow-x-auto leading-relaxed whitespace-pre-wrap break-all">
                              {formatSchema(tool.inputSchema)}
                            </pre>
                          </details>
                        )}

                        <div className="flex items-center gap-2 pt-1">
                          {tool.isAsync && (
                            <span className="px-1.5 py-0.5 neo-concave rounded text-[9px] font-bold text-orange-500/60">
                              ASYNC
                            </span>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          </motion.div>
        </>
      )}
    </AnimatePresence>
  )
}

export default ToolPanel
