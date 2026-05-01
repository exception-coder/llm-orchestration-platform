import React, { useState, useEffect, useCallback } from 'react'
import { Play, Terminal, Zap, Copy, Loader2, Activity } from 'lucide-react'
import { promptTestAPI } from '@/api'
import { useMarkdown } from '@/hooks/useMarkdown'

const PromptTest: React.FC = () => {
  const { renderMarkdown } = useMarkdown()

  const [selectedModel, setSelectedModel] = useState('')
  const [temperature, setTemperature] = useState(0.7)
  const [promptInput, setPromptInput] = useState('')
  const [testResult, setTestResult] = useState('')
  const [loading, setLoading] = useState(false)
  const [tokenUsage, setTokenUsage] = useState<any>(null)
  const [models, setModels] = useState<any[]>([])

  const loadModels = useCallback(async () => {
    try {
      const res: any = await promptTestAPI.getModels()
      setModels(res || [])
      if (res && res.length > 0) {
        setSelectedModel(res[0].modelId)
      }
    } catch (err) {
      console.error('Failed to load models', err)
    }
  }, [])

  const runTest = async () => {
    if (!promptInput.trim()) return
    setLoading(true)
    setTestResult('')
    try {
      const res: any = await promptTestAPI.test({
        model: selectedModel,
        prompt: promptInput,
        temperature: temperature
      })
      setTestResult(res.content)
      setTokenUsage(res.tokenUsage)
    } catch (err) {
      console.error('Test execution failed', err)
      // TODO: Replace with toast
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadModels()
  }, [loadModels])

  return (
    <div className="max-w-6xl mx-auto space-y-10">
      
      {/* 1. 实验参数区 (Physical Control Panel) */}
      <header className="neo-convex p-8 rounded-[3rem] grid grid-cols-1 md:grid-cols-3 gap-8 items-end">
        <div className="space-y-4">
          <label className="text-[10px] font-black tracking-widest text-foreground/40 uppercase px-2">Select Model</label>
          <div className="neo-concave rounded-2xl p-1">
            <select 
              value={selectedModel} 
              onChange={(e) => setSelectedModel(e.target.value)}
              className="w-full bg-transparent border-none focus:outline-none p-3 text-sm font-bold text-primary appearance-none cursor-pointer"
            >
              {models.map((m) => (
                <option key={m.id} value={m.modelId}>{m.name}</option>
              ))}
            </select>
          </div>
        </div>

        <div className="space-y-4">
          <label className="text-[10px] font-black tracking-widest text-foreground/40 uppercase px-2">Temperature</label>
          <div className="neo-concave rounded-2xl p-4 flex items-center gap-4">
            <input 
              type="range" 
              min="0" max="2" step="0.1" 
              value={temperature}
              onChange={(e) => setTemperature(parseFloat(e.target.value))}
              className="flex-1 accent-primary" 
            />
            <span className="text-xs font-mono font-bold text-primary w-8 text-right">{temperature}</span>
          </div>
        </div>

        <button 
          onClick={runTest}
          disabled={loading}
          className="h-14 neo-convex rounded-2xl flex items-center justify-center gap-3 text-primary font-black tracking-widest active:scale-95 transition-all disabled:opacity-50"
        >
          <Play size={18} className={loading ? 'animate-pulse' : ''} />
          <span>EXECUTE TEST</span>
        </button>
      </header>

      {/* 2. 编辑与输出区 (Dual Panel) */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-10 h-[600px]">
        
        {/* 输入槽 (Input Slot) */}
        <section className="flex flex-col neo-convex rounded-[3rem] overflow-hidden border border-white/40">
          <div className="p-6 border-b border-foreground/5 flex items-center gap-3">
            <div className="w-8 h-8 neo-concave rounded-xl flex items-center justify-center text-foreground/40">
              <Terminal size={16} />
            </div>
            <span className="text-[10px] font-black tracking-widest opacity-40 uppercase">System Prompt & Input</span>
          </div>
          <div className="flex-1 neo-concave m-6 rounded-[2rem] p-6">
            <textarea 
              value={promptInput}
              onChange={(e) => setPromptInput(e.target.value)}
              placeholder="在此处输入 Prompt 逻辑..."
              className="w-full h-full bg-transparent border-none focus:outline-none text-sm leading-relaxed resize-none font-mono"
            ></textarea>
          </div>
        </section>

        {/* 输出台 (Output Platform) */}
        <section className="flex flex-col neo-convex rounded-[3rem] overflow-hidden border border-white/40">
          <div className="p-6 border-b border-foreground/5 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 neo-concave rounded-xl flex items-center justify-center text-primary">
                <Zap size={16} />
              </div>
              <span className="text-[10px] font-black tracking-widest opacity-40 uppercase">AI Response</span>
            </div>
            <button className="p-2 neo-convex rounded-xl text-foreground/30 hover:text-primary transition-colors">
              <Copy size={16} />
            </button>
          </div>
          
          <div className="flex-1 overflow-y-auto p-8 relative">
            {loading && (
              <div className="absolute inset-0 flex items-center justify-center bg-background/20 backdrop-blur-sm z-10">
                <Loader2 size={32} className="animate-spin text-primary opacity-40" />
              </div>
            )}
            
            <article 
              className="markdown-rendered prose prose-slate dark:prose-invert max-w-none"
              dangerouslySetInnerHTML={{ __html: renderMarkdown(testResult || '> 等待指令执行...') }}
            ></article>
          </div>

          {tokenUsage && (
            <footer className="p-6 border-t border-foreground/5 bg-foreground/[0.02]">
              <div className="flex items-center gap-6">
                <div className="flex items-center gap-2 text-[10px] font-bold text-foreground/30 uppercase tracking-widest">
                  <Activity size={12} />
                  Usage Statistics
                </div>
                <div className="flex-1 h-[1px] bg-foreground/5"></div>
                <span className="text-[10px] font-mono text-primary font-bold">TOKENS: {tokenUsage.totalTokens}</span>
              </div>
            </footer>
          )}
        </section>
      </div>

    </div>
  )
}

export default PromptTest
