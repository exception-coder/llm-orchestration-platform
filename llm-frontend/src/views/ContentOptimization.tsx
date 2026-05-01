import React, { useState } from 'react'
import { 
  Sparkles, FileText, CheckCircle, Copy, Loader2,
  Instagram, Twitter, Send, Github, Languages
} from 'lucide-react'
import { contentOptimizationAPI } from '@/api'
import { useMarkdown } from '@/hooks/useMarkdown'

const ContentOptimization: React.FC = () => {
  const { renderMarkdown } = useMarkdown()

  const [sourceContent, setSourceContent] = useState('')
  const [optimizedContent, setOptimizedContent] = useState('')
  const [loading, setLoading] = useState(false)
  const [currentPlatform, setCurrentPlatform] = useState('')

  const platforms = [
    { id: 'RED', label: '小红书', icon: Instagram },
    { id: 'TIKTOK', label: '抖音/TikTok', icon: Send },
    { id: 'TWITTER', label: 'Twitter/X', icon: Twitter },
    { id: 'GITHUB', label: 'GitHub Readme', icon: Github },
    { id: 'TRANSLATE', label: '智能润色/翻译', icon: Languages }
  ]

  const optimize = async (platform: string) => {
    if (!sourceContent.trim()) return
    setLoading(true)
    setCurrentPlatform(platform)
    try {
      const res: any = await contentOptimizationAPI.optimize({
        content: sourceContent,
        platform: platform
      })
      setOptimizedContent(res.content)
    } catch (err) {
      console.error('Optimization failed', err)
      // TODO: Replace with toast
    } finally {
      setLoading(false)
    }
  }

  const handleCopy = () => {
    if (optimizedContent) {
      navigator.clipboard.writeText(optimizedContent)
      // TODO: Show success toast
    }
  }

  return (
    <div className="max-w-7xl mx-auto space-y-12">
      
      {/* 头部：加工厂状态 */}
      <header className="flex items-center gap-6 px-4">
        <div className="w-14 h-14 neo-convex rounded-2xl flex items-center justify-center text-primary shadow-lg">
          <Sparkles size={28} />
        </div>
        <div>
          <h1 className="text-2xl font-black tracking-tighter">内容优化加工厂</h1>
          <p className="text-[10px] font-bold text-foreground/30 uppercase tracking-[0.2em]">Platform Specific Refinement Engine</p>
        </div>
      </header>

      <div className="grid grid-cols-1 xl:grid-cols-12 gap-10 items-stretch">
        
        {/* 原文输入 (Input Slot) - 占据 5 列 */}
        <section className="xl:col-span-5 flex flex-col neo-convex rounded-[3.5rem] p-8 border border-white/40">
          <div className="flex items-center gap-3 mb-6 px-2">
            <div className="w-8 h-8 neo-concave rounded-xl flex items-center justify-center text-foreground/40">
              <FileText size={16} />
            </div>
            <span className="text-[10px] font-black tracking-widest opacity-40 uppercase">Source Content</span>
          </div>
          
          <div className="flex-1 neo-concave rounded-[2.5rem] p-6 min-h-[400px]">
            <textarea 
              value={sourceContent}
              onChange={(e) => setSourceContent(e.target.value)}
              placeholder="粘贴需要优化的原文..."
              className="w-full h-full bg-transparent border-none focus:outline-none text-sm leading-relaxed resize-none"
            ></textarea>
          </div>
        </section>

        {/* 转换控制中心 (Action Panel) - 占据 2 列 */}
        <section className="xl:col-span-2 flex flex-col justify-center gap-6 py-10">
          <div className="text-center space-y-2 mb-4">
            <span className="text-[9px] font-black tracking-[0.3em] text-foreground/20 uppercase">Target Platform</span>
          </div>
          
          {platforms.map((p) => {
            const Icon = p.icon
            return (
              <button 
                key={p.id}
                onClick={() => optimize(p.id)}
                disabled={loading}
                className={`group relative h-20 neo-convex rounded-[2rem] flex flex-col items-center justify-center gap-2 transition-all active:scale-90 ${
                  loading ? 'opacity-50 grayscale' : 'hover:text-primary'
                }`}
              >
                <Icon size={20} className="group-hover:scale-110 transition-transform" />
                <span className="text-[9px] font-black tracking-widest uppercase">{p.label}</span>
                
                {/* 指示灯 */}
                {currentPlatform === p.id && loading && (
                  <div className="absolute -right-2 top-1/2 -translate-y-1/2 w-2 h-2 bg-primary rounded-full shadow-[0_0_8px_var(--color-primary)]"></div>
                )}
              </button>
            )
          })}
        </section>

        {/* 优化成品 (Output Slot) - 占据 5 列 */}
        <section className="xl:col-span-5 flex flex-col neo-convex rounded-[3.5rem] p-8 border border-white/40">
          <div className="flex items-center justify-between mb-6 px-2">
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 neo-concave rounded-xl flex items-center justify-center text-primary">
                <CheckCircle size={16} />
              </div>
              <span className="text-[10px] font-black tracking-widest opacity-40 uppercase">Optimized Result</span>
            </div>
            <button 
              onClick={handleCopy}
              className="p-2 neo-convex rounded-xl text-foreground/30 hover:text-primary transition-colors"
            >
              <Copy size={16} />
            </button>
          </div>

          <div className="flex-1 neo-concave rounded-[2.5rem] p-8 overflow-y-auto relative min-h-[400px]">
            {loading && (
              <div className="absolute inset-0 flex items-center justify-center bg-background/50 backdrop-blur-sm rounded-[2.5rem] z-20">
                <div className="flex flex-col items-center gap-4">
                  <Loader2 size={32} className="animate-spin text-primary" />
                  <span className="text-[10px] font-black tracking-widest text-primary animate-pulse">OPTIMIZING...</span>
                </div>
              </div>
            )}
            <article 
              className="markdown-rendered prose prose-slate dark:prose-invert max-w-none"
              dangerouslySetInnerHTML={{ __html: renderMarkdown(optimizedContent || '> 选择平台开始优化...') }}
            ></article>
          </div>
        </section>
      </div>

    </div>
  )
}

export default ContentOptimization
