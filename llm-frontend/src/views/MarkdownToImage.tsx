import React, { useState, useRef } from 'react'
import { 
  Palette, Type, Download, Eye, 
  Loader2, QrCode 
} from 'lucide-react'
import { useMarkdown } from '@/hooks/useMarkdown'
import html2canvas from 'html2canvas'

const MarkdownToImage: React.FC = () => {
  const { renderMarkdown } = useMarkdown()

  const [markdownInput, setMarkdownInput] = useState('# Hello Visual World\n\n这是一段测试内容，它将被转换成**高质量**的图片。\n\n- 极致物理质感\n- 拟物化设计\n- 毛玻璃特效')
  const [currentTemplate, setCurrentTemplate] = useState('modern')
  const [exporting, setExporting] = useState(false)
  const previewRef = useRef<HTMLDivElement>(null)

  const templates = [
    { id: 'modern', name: '极简现代', color: 'bg-blue-500' },
    { id: 'dark', name: '深邃黑客', color: 'bg-zinc-800' },
    { id: 'paper', name: '复古纸张', color: 'bg-amber-100' },
    { id: 'glass', name: '毛玻璃', color: 'bg-white/40' }
  ]

  const getTemplateClass = (id: string) => {
    const base = 'p-12 rounded-[2.5rem] min-h-[500px] '
    const styles: Record<string, string> = {
      modern: 'bg-white text-slate-800 shadow-[0_20px_50px_rgba(0,0,0,0.1)]',
      dark: 'bg-zinc-950 text-zinc-100 shadow-[0_20px_50px_rgba(0,0,0,0.5)]',
      paper: 'bg-[#f4f1ea] text-[#43392b] border-2 border-[#e6e2d3] shadow-lg',
      glass: 'bg-white/60 backdrop-blur-xl text-slate-900 border border-white/40 shadow-2xl'
    }
    return base + (styles[id] || styles.modern)
  }

  const exportImage = async () => {
    if (!previewRef.current) return
    setExporting(true)
    try {
      const canvas = await html2canvas(previewRef.current, {
        backgroundColor: null,
        scale: 2, // 高清
        useCORS: true,
      })
      const link = document.createElement('a')
      link.download = `llm-export-${Date.now()}.png`
      link.href = canvas.toDataURL('image/png')
      link.click()
      // TODO: Success toast
    } catch (err) {
      console.error('Export failed', err)
    } finally {
      setExporting(false)
    }
  }

  return (
    <div className="h-full flex flex-col xl:flex-row gap-10 overflow-hidden max-w-7xl mx-auto">
      
      {/* 1. 输入控制区 */}
      <aside className="w-full xl:w-96 flex flex-col shrink-0 gap-8">
        {/* 模板选择器 */}
        <section className="neo-convex p-6 rounded-[2.5rem] space-y-4">
          <div className="flex items-center gap-2 px-2 mb-2">
            <Palette size={14} className="text-primary" />
            <span className="text-[10px] font-black tracking-widest text-foreground/40 uppercase">Select Style</span>
          </div>
          <div className="grid grid-cols-2 gap-3">
            {templates.map((t) => (
              <button 
                key={t.id}
                onClick={() => setCurrentTemplate(t.id)}
                className={`py-3 px-2 rounded-xl text-[10px] font-bold transition-all flex flex-col items-center gap-2 ${
                  currentTemplate === t.id ? 'neo-concave text-primary' : 'neo-convex text-foreground/40'
                }`}
              >
                <div className={`w-4 h-4 rounded-full shadow-sm ${t.color}`}></div>
                {t.name}
              </button>
            ))}
          </div>
        </section>

        {/* 内容输入槽 */}
        <section className="flex-1 neo-convex p-6 rounded-[3rem] flex flex-col min-h-[400px]">
          <div className="flex items-center justify-between mb-4 px-2">
            <div className="flex items-center gap-2">
              <Type size={14} className="text-foreground/30" />
              <span className="text-[10px] font-black tracking-widest text-foreground/40 uppercase">Markdown Input</span>
            </div>
            <button onClick={() => setMarkdownInput('')} className="text-[10px] font-bold text-red-500/40 hover:text-red-500">CLEAR</button>
          </div>
          <div className="flex-1 neo-concave rounded-[2rem] p-4">
            <textarea 
              value={markdownInput}
              onChange={(e) => setMarkdownInput(e.target.value)}
              placeholder="在此处输入或粘贴 Markdown..."
              className="w-full h-full bg-transparent border-none focus:outline-none text-xs leading-relaxed font-mono resize-none"
            ></textarea>
          </div>
        </section>

        {/* 导出按钮 */}
        <button 
          onClick={exportImage}
          disabled={!markdownInput.trim() || exporting}
          className="h-20 neo-convex rounded-[2rem] flex items-center justify-center gap-4 text-primary font-black tracking-[0.2em] active:scale-95 transition-all shadow-xl disabled:opacity-50"
        >
          {!exporting ? <Download size={24} /> : <Loader2 size={24} className="animate-spin" />}
          <span>{exporting ? 'PRINTING...' : 'EXPORT IMAGE'}</span>
        </button>
      </aside>

      {/* 2. 实时预览成品区 */}
      <main className="flex-1 flex flex-col min-w-0">
        <div className="flex items-center justify-between mb-6 px-4">
          <div className="flex items-center gap-4">
            <div className="w-10 h-10 neo-convex rounded-2xl flex items-center justify-center text-primary">
              <Eye size={20} />
            </div>
            <span className="text-[10px] font-black tracking-[0.3em] text-foreground/30 uppercase">Live Output Preview</span>
          </div>
        </div>

        {/* 预览容器 */}
        <div className="flex-1 neo-concave rounded-[4rem] p-10 overflow-y-auto relative bg-foreground/[0.02] no-scrollbar">
          <div 
            ref={previewRef}
            className={`mx-auto shadow-2xl transition-all duration-500 max-w-2xl ${getTemplateClass(currentTemplate)}`}
          >
            {/* 装饰性页头 */}
            <div className="h-2 w-24 bg-current opacity-20 rounded-full mb-8"></div>
            
            <article 
              className="markdown-rendered prose prose-sm max-w-none"
              dangerouslySetInnerHTML={{ 
                __html: renderMarkdown(markdownInput || '# 开始您的视觉创作\n\n在此输入 Markdown，右侧将实时生成精美的排版卡片。') 
              }}
            />

            {/* 装饰性页脚 */}
            <div className="mt-12 pt-8 border-t border-current/10 flex justify-between items-center opacity-40">
              <span className="text-[9px] font-black tracking-widest uppercase">Generated by LLM OS</span>
              <QrCode size={24} />
            </div>
          </div>
        </div>
      </main>

    </div>
  )
}

export default MarkdownToImage
