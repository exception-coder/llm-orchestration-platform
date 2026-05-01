import React, { useState, useEffect, useCallback } from 'react'
import { 
  Wrench, Plus, Search, Code2, Globe, 
  Trash2, BoxSelect, Activity 
} from 'lucide-react'
import { motion, AnimatePresence } from 'framer-motion'
import { toolAPI } from '@/api'

const ToolManagement: React.FC = () => {
  const [tools, setTools] = useState<any[]>([])
  const [showCreateDialog, setShowCreateDialog] = useState(false)
  const [searchKeyword, setSearchKeyword] = useState('')

  const loadTools = useCallback(async () => {
    try {
      const res: any = await toolAPI.getAll()
      setTools(res || [])
    } catch (err) {
      console.error('Failed to load tools:', err)
    }
  }, [])

  const handleDelete = async (id: string | number) => {
    // TODO: Use a proper confirm dialog (Radix or custom)
    if (window.confirm('确定要拆卸该工具模块吗？这将影响关联的智能体。')) {
      try {
        await toolAPI.delete(id)
        loadTools()
        // TODO: Success toast
      } catch (e) {
        console.error('Delete failed', e)
      }
    }
  }

  useEffect(() => {
    loadTools()
  }, [loadTools])

  const filteredTools = tools.filter(tool => 
    tool.name.toLowerCase().includes(searchKeyword.toLowerCase()) ||
    tool.id.toString().toLowerCase().includes(searchKeyword.toLowerCase())
  )

  return (
    <div className="max-w-7xl mx-auto space-y-10">
      
      {/* 1. 顶部控制台：动作中心 */}
      <header className="flex items-center justify-between px-6 flex-wrap gap-6">
        <div className="flex items-center gap-5">
          <div className="w-14 h-14 neo-convex rounded-[1.5rem] flex items-center justify-center text-primary shadow-xl">
            <Wrench size={28} />
          </div>
          <div>
            <h1 className="text-2xl font-black tracking-tighter italic uppercase text-slate-900 dark:text-white">Tool Modules</h1>
            <p className="text-[10px] font-bold text-foreground/30 uppercase tracking-[0.3em]">Agent Function Extensions</p>
          </div>
        </div>
        
        <div className="flex items-center gap-4">
          <div className="neo-concave rounded-2xl px-4 py-2 flex items-center gap-3">
            <Search size={14} className="text-foreground/20" />
            <input 
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              placeholder="SEARCH TOOLS..." 
              className="bg-transparent border-none focus:outline-none text-[10px] font-bold tracking-widest w-40 placeholder:text-foreground/10" 
            />
          </div>
          <button 
            onClick={() => setShowCreateDialog(true)}
            className="h-12 px-6 neo-convex rounded-2xl flex items-center gap-3 text-primary font-black tracking-widest active:scale-95 transition-all shadow-lg"
          >
            <Plus size={18} />
            <span>REGISTER NEW TOOL</span>
          </button>
        </div>
      </header>

      {/* 2. 物理数据舱 (The Table Hub) */}
      <section className="neo-convex rounded-[3.5rem] overflow-hidden border border-white/40 shadow-2xl">
        {/* 表头槽位 (Header Slot) */}
        <div className="grid grid-cols-12 gap-4 px-10 py-6 neo-concave bg-foreground/[0.02]">
          <div className="col-span-3 text-[10px] font-black tracking-[0.2em] text-foreground/30 uppercase">Tool Identity</div>
          <div className="col-span-4 text-[10px] font-black tracking-[0.2em] text-foreground/30 uppercase">Description & Logic</div>
          <div className="col-span-2 text-[10px] font-black tracking-[0.2em] text-foreground/30 uppercase">Status</div>
          <div className="col-span-3 text-right text-[10px] font-black tracking-[0.2em] text-foreground/30 uppercase">Operations</div>
        </div>

        {/* 数据条目流 (Data Stream) */}
        <div className="divide-y divide-foreground/5">
          <AnimatePresence>
            {filteredTools.map((tool) => (
              <motion.div 
                key={tool.id}
                layout
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, x: -20 }}
                className="grid grid-cols-12 gap-4 px-10 py-8 items-center hover:bg-foreground/[0.01] transition-colors group"
              >
                {/* 身份列 */}
                <div className="col-span-3 flex items-center gap-4">
                  <div className="w-12 h-12 neo-convex rounded-2xl flex items-center justify-center shrink-0 group-hover:scale-110 transition-transform">
                    {tool.type === 'FUNCTION' ? (
                      <Code2 size={20} className="text-primary/60" />
                    ) : (
                      <Globe size={20} className="text-blue-400/60" />
                    )}
                  </div>
                  <div className="overflow-hidden">
                    <p className="text-sm font-black tracking-tight truncate">{tool.name}</p>
                    <p className="text-[9px] font-mono text-foreground/20 truncate uppercase tracking-tighter">{tool.id}</p>
                  </div>
                </div>

                {/* 描述列 */}
                <div className="col-span-4">
                  <p className="text-xs text-foreground/60 leading-relaxed line-clamp-2 pr-10 italic">
                    {tool.description || 'No specialized description provided for this module.'}
                  </p>
                </div>

                {/* 状态列 */}
                <div className="col-span-2">
                  <div className="flex items-center gap-3">
                    <div 
                      className={`w-2.5 h-2.5 rounded-full shadow-[0_0_10px_currentColor] transition-all ${
                        tool.enabled ? 'text-green-500 bg-green-500' : 'text-foreground/10 bg-foreground/10 shadow-none'
                      }`}
                    ></div>
                    <span className="text-[10px] font-black tracking-widest uppercase opacity-40">
                      {tool.enabled ? 'Active' : 'Offline'}
                    </span>
                  </div>
                </div>

                {/* 操作列 */}
                <div className="col-span-3 flex justify-end gap-3 opacity-40 group-hover:opacity-100 transition-opacity">
                  <button className="px-4 py-2 neo-convex rounded-xl text-[10px] font-black tracking-widest hover:text-primary transition-all active:scale-95">
                    EDIT
                  </button>
                  <button className="px-4 py-2 neo-convex rounded-xl text-[10px] font-black tracking-widest hover:text-primary transition-all active:scale-95">
                    DOCS
                  </button>
                  <button 
                    onClick={() => handleDelete(tool.id)}
                    className="w-10 h-10 neo-convex rounded-xl flex items-center justify-center text-red-500/40 hover:text-red-500 active:scale-90 transition-all"
                  >
                    <Trash2 size={16} />
                  </button>
                </div>
              </motion.div>
            ))}
          </AnimatePresence>
        </div>

        {/* 空舱状态 */}
        {filteredTools.length === 0 && (
          <div className="flex flex-col items-center justify-center py-32 space-y-6 opacity-10">
            <BoxSelect size={80} strokeWidth={1} />
            <p className="text-sm font-black tracking-[0.5em] uppercase text-center">Data Hub Empty<br/><span className="text-[10px] tracking-widest opacity-50">Please register system modules</span></p>
          </div>
        )}
      </section>

      {/* 底部统计 */}
      <footer className="flex items-center justify-between px-10 flex-wrap gap-6">
        <div className="flex items-center gap-8">
          <div className="flex flex-col">
            <span className="text-[9px] font-black text-foreground/20 uppercase tracking-widest">Total Modules</span>
            <span className="text-lg font-black text-primary">{tools.length}</span>
          </div>
          <div className="w-[1px] h-8 bg-foreground/5"></div>
          <div className="flex flex-col">
            <span className="text-[9px] font-black text-foreground/20 uppercase tracking-widest">System Load</span>
            <span className="text-lg font-black text-foreground/40 font-mono">0.02ms</span>
          </div>
        </div>
        
        {/* 极简分页 (Physical Tabs) */}
        <div className="flex gap-2">
          <button className="w-10 h-10 neo-concave rounded-xl flex items-center justify-center text-primary font-bold">1</button>
          <button className="w-10 h-10 neo-convex rounded-xl flex items-center justify-center text-foreground/30">2</button>
        </div>
      </footer>

    </div>
  )
}

export default ToolManagement
