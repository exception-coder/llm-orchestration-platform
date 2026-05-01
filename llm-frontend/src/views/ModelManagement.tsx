import React, { useState, useEffect, useCallback } from 'react'
import { Cpu, Plus, Activity, Trash2, Boxes } from 'lucide-react'
import { motion, AnimatePresence } from 'framer-motion'
import { modelConfigAPI } from '@/api'

const ModelManagement: React.FC = () => {
  const [models, setModels] = useState<any[]>([])
  const [showAddDialog, setShowAddDialog] = useState(false)

  const loadModels = useCallback(async () => {
    try {
      const res: any = await modelConfigAPI.getAll()
      setModels(res || [])
    } catch (err) {
      console.error(err)
    }
  }, [])

  const deleteModel = async (id: string | number) => {
    try {
      await modelConfigAPI.delete(id as string) // Adjusting to API definition
      loadModels()
    } catch (err) {
      console.error('Delete failed', err)
    }
  }

  useEffect(() => {
    loadModels()
  }, [loadModels])

  return (
    <div className="max-w-6xl mx-auto space-y-12">
      
      {/* 顶部状态栏 */}
      <header className="flex items-center justify-between px-6">
        <div className="flex items-center gap-4">
          <div className="w-12 h-12 neo-convex rounded-2xl flex items-center justify-center text-primary shadow-lg">
            <Cpu size={24} />
          </div>
          <div>
            <h1 className="text-2xl font-black tracking-tighter">模型控制台</h1>
            <p className="text-[10px] font-bold text-foreground/30 uppercase tracking-[0.2em]">Engine Status: Operational</p>
          </div>
        </div>
        <button 
          onClick={() => setShowAddDialog(true)}
          className="flex items-center gap-3 px-6 py-3 neo-convex rounded-2xl text-primary font-bold active:scale-95 transition-all"
        >
          <Plus size={18} />
          <span>接入新模型</span>
        </button>
      </header>

      {/* 模型网格 */}
      <section className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-10">
        <AnimatePresence>
          {models.map((model) => (
            <motion.div 
              key={model.id}
              layout
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.9 }}
              className="neo-convex p-8 rounded-[3rem] group relative flex flex-col transition-all hover:scale-[1.02]"
            >
              {/* 材质装饰线 */}
              <div className="absolute top-0 left-1/2 -translate-x-1/2 w-24 h-1 bg-primary/10 rounded-b-full"></div>

              <div className="flex items-start justify-between mb-8">
                <div className="space-y-1">
                  <h3 className="text-lg font-black tracking-tight group-hover:text-primary transition-colors">{model.name}</h3>
                  <p className="text-[10px] font-bold text-foreground/40 uppercase tracking-widest">{model.provider}</p>
                </div>
                <div 
                  className={`w-10 h-10 neo-concave rounded-xl flex items-center justify-center ${
                    model.enabled ? 'text-green-500' : 'text-foreground/10'
                  }`}
                >
                  <Activity size={20} className={model.enabled ? 'animate-pulse' : ''} />
                </div>
              </div>

              {/* 详细参数 (Inset Panel) */}
              <div className="flex-1 neo-concave rounded-3xl p-5 space-y-4 mb-8">
                <div className="flex justify-between items-center text-[11px]">
                  <span className="font-bold text-foreground/30 uppercase">Model ID</span>
                  <span className="font-mono text-foreground/60">{model.modelId}</span>
                </div>
                <div className="flex justify-between items-center text-[11px]">
                  <span className="font-bold text-foreground/30 uppercase">Context Limit</span>
                  <span className="font-mono text-foreground/60">{model.contextLength || '128k'}</span>
                </div>
              </div>

              {/* 操作按钮 */}
              <div className="flex gap-3">
                <button className="flex-1 py-3 neo-convex rounded-xl text-[11px] font-black tracking-widest text-foreground/60 hover:text-primary active:scale-95 transition-all">
                  CONFIG
                </button>
                <button 
                  onClick={() => deleteModel(model.modelCode || model.id)}
                  className="w-12 h-12 neo-convex rounded-xl flex items-center justify-center text-red-500/40 hover:text-red-500 active:scale-90 transition-all"
                >
                  <Trash2 size={18} />
                </button>
              </div>
            </motion.div>
          ))}
        </AnimatePresence>
      </section>

      {/* 空状态 */}
      {models.length === 0 && (
        <div className="flex flex-col items-center justify-center py-20 opacity-20">
          <Boxes size={64} className="mb-4" />
          <p className="font-bold tracking-widest uppercase text-sm">No Models Loaded</p>
        </div>
      )}

    </div>
  )
}

export default ModelManagement
