import React, { useState, useEffect, useMemo, useCallback } from 'react'
import { 
  Operation, MagicStick, RefreshLeft, Play, 
  Terminal, Zap, Check, AlertCircle, Loader2, Activity 
} from 'lucide-react'
import { motion, AnimatePresence } from 'framer-motion'
import { templateAPI, modelConfigAPI, promptTestAPI } from '@/api'
import { useResponsive } from '@/hooks/useResponsive'
import { useMarkdown } from '@/hooks/useMarkdown'
import { useSSEStream } from '@/hooks/useSSEStream'

const PromptComparison: React.FC = () => {
  const { isMobile } = useResponsive()
  const { renderMarkdown } = useMarkdown()
  const { fetchSSE } = useSSEStream()

  const [config, setConfig] = useState({
    templateName: '',
    model: '',
    stream: false,
    variables: ''
  })

  const [templates, setTemplates] = useState<any[]>([])
  const [availableModels, setAvailableModels] = useState<any[]>([])
  const [promptA, setPromptA] = useState('')
  const [promptB, setPromptB] = useState('')
  const [comparing, setComparing] = useState(false)

  const [resultA, setResultA] = useState<any>({ status: '', content: '', tokenUsage: null, duration: 0 })
  const [resultB, setResultB] = useState<any>({ status: '', content: '', tokenUsage: null, duration: 0 })

  const loadData = useCallback(async () => {
    try {
      const [tRes, mRes]: [any, any] = await Promise.all([
        templateAPI.getAll(),
        modelConfigAPI.getAll()
      ])
      setTemplates(tRes || [])
      const models = (mRes || []).map((m: any) => ({
        code: m.modelCode,
        name: m.modelName,
        provider: m.provider
      }))
      setAvailableModels(models)
      if (models.length > 0) {
        setConfig(prev => ({ ...prev, model: models[0].code }))
      }
    } catch (error) {
      console.error(error)
    }
  }, [])

  useEffect(() => {
    loadData()
  }, [loadData])

  const handleTemplateChange = async (templateName: string) => {
    try {
      const template: any = await templateAPI.getByName(templateName)
      setPromptA(template.templateContent)
      setPromptB(template.templateContent)
      setConfig(prev => ({ ...prev, templateName }))
      setResultA({ status: '', content: '', tokenUsage: null, duration: 0 })
      setResultB({ status: '', content: '', tokenUsage: null, duration: 0 })
    } catch (error) {
      console.error(error)
    }
  }

  const loadTemplateVariables = async () => {
    if (!config.templateName) return
    try {
      const variables: any = await promptTestAPI.getTemplateVariables(config.templateName)
      setConfig(prev => ({ ...prev, variables: JSON.stringify(variables, null, 2) }))
    } catch (error) {
      console.error(error)
    }
  }

  const executePrompt = async (version: 'A' | 'B', promptContent: string, variables: any) => {
    const setResult = version === 'A' ? setResultA : setResultB
    const startTime = Date.now()

    try {
      let renderedPrompt = promptContent
      for (const [key, value] of Object.entries(variables)) {
        renderedPrompt = renderedPrompt.replaceAll(`{${key}}`, value as string)
      }

      setResult({ status: 'loading', content: '', tokenUsage: null, duration: 0 })

      let currentContent = ''
      await fetchSSE(config.stream ? '/chat/stream' : '/chat', {
        conversationId: `comparison-${version}-${Date.now()}`,
        message: renderedPrompt,
        model: config.model
      }, {
        onContent: (content) => {
          currentContent += content
          setResult((prev: any) => ({ ...prev, content: currentContent }))
        },
        onTokenUsage: (tokenUsage) => {
          setResult((prev: any) => ({ ...prev, tokenUsage }))
        }
      })

      setResult((prev: any) => ({ 
        ...prev, 
        status: 'success', 
        duration: Date.now() - startTime 
      }))
    } catch (error: any) {
      setResult({ 
        status: 'error', 
        content: `执行失败: ${error.message}`,
        tokenUsage: null,
        duration: 0
      })
    }
  }

  const handleCompare = async () => {
    let variables
    try {
      variables = JSON.parse(config.variables)
    } catch {
      return
    }

    setComparing(true)
    await Promise.all([
      executePrompt('A', promptA, variables),
      executePrompt('B', promptB, variables)
    ])
    setComparing(false)
  }

  const canCompare = config.templateName && config.model && promptA && promptB && config.variables

  return (
    <div className="max-w-7xl mx-auto space-y-12">
      
      {/* 顶部控制台 */}
      <header className="flex items-center justify-between px-6 flex-wrap gap-6">
        <div className="flex items-center gap-4">
          <div className="w-12 h-12 neo-convex rounded-2xl flex items-center justify-center text-primary shadow-lg">
            <Operation size={24} />
          </div>
          <div>
            <h1 className="text-2xl font-black tracking-tighter uppercase italic">Prompt A/B Matrix</h1>
            <p className="text-[10px] font-bold text-foreground/30 uppercase tracking-[0.2em]">Parallel Prompt Evaluation</p>
          </div>
        </div>
        <button 
          onClick={handleCompare}
          disabled={!canCompare || comparing}
          className={`h-14 px-8 neo-convex rounded-2xl flex items-center gap-3 font-black tracking-widest transition-all ${
            canCompare && !comparing ? 'text-primary active:scale-95 shadow-xl' : 'text-foreground/10'
          }`}
        >
          {comparing ? <Loader2 size={18} className="animate-spin" /> : <Play size={18} />}
          <span>START COMPARISON</span>
        </button>
      </header>

      {/* 配置面板 */}
      <section className="grid grid-cols-1 xl:grid-cols-12 gap-8 items-stretch">
        <div className="xl:col-span-5 neo-convex rounded-[3rem] p-8 border border-white/40 space-y-6">
          <div className="flex items-center gap-3 px-2">
            <div className="w-8 h-8 neo-concave rounded-xl flex items-center justify-center text-foreground/30">
              <Terminal size={14} />
            </div>
            <span className="text-[10px] font-black tracking-widest text-foreground/40 uppercase">Global Configuration</span>
          </div>

          <div className="space-y-4">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="neo-concave rounded-2xl p-1">
                <select 
                  value={config.templateName} 
                  onChange={(e) => handleTemplateChange(e.target.value)}
                  className="w-full bg-transparent border-none focus:outline-none px-4 py-3 text-xs font-bold text-primary appearance-none cursor-pointer"
                >
                  <option value="">Select Template...</option>
                  {templates.map(t => <option key={t.templateName} value={t.templateName}>{t.templateName}</option>)}
                </select>
              </div>
              <div className="neo-concave rounded-2xl p-1">
                <select 
                  value={config.model} 
                  onChange={(e) => setConfig({ ...config, model: e.target.value })}
                  className="w-full bg-transparent border-none focus:outline-none px-4 py-3 text-xs font-bold text-primary appearance-none cursor-pointer"
                >
                  {availableModels.map(m => <option key={m.code} value={m.code}>{m.name}</option>)}
                </select>
              </div>
            </div>
            
            <div className="flex items-center justify-between px-4 neo-concave rounded-2xl py-3">
              <span className="text-[10px] font-black text-foreground/40 tracking-widest uppercase">Stream Mode</span>
              <button 
                onClick={() => setConfig({...config, stream: !config.stream})}
                className={`w-12 h-6 rounded-full transition-all relative ${config.stream ? 'bg-primary shadow-lg shadow-primary/20' : 'neo-concave'}`}
              >
                <div className={`absolute top-1 w-4 h-4 rounded-full bg-white transition-all ${config.stream ? 'right-1' : 'left-1 bg-foreground/20'}`} />
              </button>
            </div>
          </div>
        </div>

        <div className="xl:col-span-7 neo-convex rounded-[3rem] p-8 border border-white/40 flex flex-col">
          <div className="flex items-center justify-between px-2 mb-6">
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 neo-concave rounded-xl flex items-center justify-center text-foreground/30">
                <MagicStick size={14} />
              </div>
              <span className="text-[10px] font-black tracking-widest text-foreground/40 uppercase">Variable Injection (JSON)</span>
            </div>
            <button 
              onClick={loadTemplateVariables}
              className="text-[9px] font-black text-primary hover:opacity-80 transition-all uppercase tracking-widest"
            >
              Load Example
            </button>
          </div>
          <div className="flex-1 neo-concave rounded-[2rem] p-1">
            <textarea 
              value={config.variables}
              onChange={(e) => setConfig({ ...config, variables: e.target.value })}
              placeholder='{"key": "value"}'
              className="w-full h-full bg-transparent border-none focus:outline-none px-6 py-4 text-xs font-mono resize-none min-h-[120px]"
            />
          </div>
        </div>
      </section>

      {/* 内容对比区域 */}
      <section className="grid grid-cols-1 lg:grid-cols-2 gap-10">
        <div className="space-y-4">
          <div className="flex items-center justify-between px-6">
            <span className="text-[10px] font-black tracking-widest text-green-500 uppercase">Version A: Baseline</span>
            <Check size={14} className="text-green-500/40" />
          </div>
          <div className="neo-convex rounded-[3rem] p-1 overflow-hidden border border-white/40">
            <textarea 
              value={promptA} 
              readOnly 
              className="w-full h-80 bg-foreground/[0.02] border-none focus:outline-none px-8 py-8 text-sm font-mono leading-relaxed resize-none opacity-60"
              placeholder="Select template to load..."
            />
          </div>
        </div>

        <div className="space-y-4">
          <div className="flex items-center justify-between px-6">
            <span className="text-[10px] font-black tracking-widest text-primary uppercase">Version B: Challenger</span>
            <button 
              onClick={() => setPromptB(promptA)}
              className="text-[9px] font-black text-foreground/30 hover:text-primary transition-all uppercase tracking-widest flex items-center gap-1"
            >
              <RefreshLeft size={10} /> Reset
            </button>
          </div>
          <div className="neo-convex rounded-[3rem] p-1 overflow-hidden border border-white/40">
            <textarea 
              value={promptB} 
              onChange={(e) => setPromptB(e.target.value)}
              className="w-full h-80 bg-transparent border-none focus:outline-none px-8 py-8 text-sm font-mono leading-relaxed resize-none"
              placeholder="Modify prompt to test differences..."
            />
          </div>
        </div>
      </section>

      {/* 结果对比区域 */}
      <section className="grid grid-cols-1 lg:grid-cols-2 gap-10">
        {[ {res: resultA, label: 'A OUTPUT'}, {res: resultB, label: 'B OUTPUT'} ].map((item, idx) => (
          <div key={idx} className="flex flex-col neo-convex rounded-[3.5rem] overflow-hidden border border-white/40 min-h-[400px]">
            <div className="p-8 border-b border-foreground/5 flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className={`w-8 h-8 neo-concave rounded-xl flex items-center justify-center ${idx === 0 ? 'text-green-500' : 'text-primary'}`}>
                  <Zap size={14} />
                </div>
                <span className="text-[10px] font-black tracking-widest text-foreground/40 uppercase">{item.label}</span>
              </div>
              {item.res.status === 'loading' && <Loader2 size={16} className="animate-spin text-primary" />}
              {item.res.status === 'success' && <div className="w-2 h-2 rounded-full bg-green-500 shadow-[0_0_8px_rgba(34,197,94,0.6)]" />}
              {item.res.status === 'error' && <AlertCircle size={16} className="text-red-500" />}
            </div>

            <div className="flex-1 p-10 overflow-y-auto relative no-scrollbar">
              <article 
                className="markdown-rendered prose prose-slate dark:prose-invert max-w-none"
                dangerouslySetInnerHTML={{ __html: renderMarkdown(item.res.content || '> Waiting for execution...') }}
              />
            </div>

            {item.res.tokenUsage && (
              <footer className="p-6 bg-foreground/[0.01] border-t border-foreground/5 flex items-center gap-6">
                <div className="flex items-center gap-2 text-[9px] font-black text-foreground/20 uppercase tracking-widest">
                  <Activity size={10} /> Metrics
                </div>
                <div className="flex-1 h-[1px] bg-foreground/5" />
                <span className="text-[10px] font-mono text-primary font-bold">TOKENS: {item.res.tokenUsage.totalTokens}</span>
                <span className="text-[10px] font-mono text-foreground/40">{item.res.duration}ms</span>
              </footer>
            )}
          </div>
        ))}
      </section>

    </div>
  )
}

export default PromptComparison
