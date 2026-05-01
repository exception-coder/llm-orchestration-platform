import React, { useState, useEffect, useRef, useCallback } from 'react'
import { 
  Power, Mic, ArrowUp, Plus, 
  User, Bot, Zap, Loader2 
} from 'lucide-react'
import { motion, AnimatePresence } from 'framer-motion'
import { secretaryAPI } from '@/api'
import { useMarkdown } from '@/hooks/useMarkdown'
import { useSSEStream } from '@/hooks/useSSEStream'

interface Message {
  role: 'user' | 'assistant'
  content: string
}

const Secretary: React.FC = () => {
  const { renderMarkdown } = useMarkdown()
  const { fetchSSE } = useSSEStream()

  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [messages, setMessages] = useState<Message[]>([
    { role: 'assistant', content: '您好，我是 Lumina。您的任务和知识库已经同步完成，今天有什么我可以帮您的？' }
  ])
  const chatScrollRef = useRef<HTMLDivElement>(null)

  const scrollToBottom = () => {
    if (chatScrollRef.current) {
      chatScrollRef.current.scrollTop = chatScrollRef.current.scrollHeight
    }
  }

  useEffect(() => {
    scrollToBottom()
  }, [messages])

  const handleSend = async () => {
    if (!input.trim() || loading) return
    
    const userMsg = input.trim()
    setMessages(prev => [...prev, { role: 'user', content: userMsg }])
    setInput('')
    setLoading(true)
    
    setMessages(prev => [...prev, { role: 'assistant', content: '' }])
    
    try {
      let currentAiContent = ''
      await fetchSSE('/secretary/chat', { message: userMsg }, {
        onContent: (content) => {
          currentAiContent += content
          setMessages(prev => {
            const next = [...prev]
            const last = next[next.length - 1]
            if (last && last.role === 'assistant') {
              last.content = currentAiContent
            }
            return next
          })
          setLoading(false)
        }
      })
    } catch (err) {
      console.error(err)
      setLoading(false)
    }
  }

  return (
    <div className="h-full flex flex-col lg:flex-row gap-10 overflow-hidden max-w-7xl mx-auto">
      
      {/* 左侧：助理状态与记忆 */}
      <aside className="w-full lg:w-72 flex flex-col shrink-0 gap-8">
        {/* 助理身份卡 */}
        <div className="neo-convex p-8 rounded-[3rem] text-center space-y-4">
          <div className="w-24 h-24 neo-concave rounded-[2.5rem] mx-auto p-1 overflow-hidden">
            <img src="https://api.dicebear.com/7.x/bottts/svg?seed=Secretary" alt="AI Avatar" className="w-full h-full" />
          </div>
          <div>
            <h2 className="font-black tracking-tighter text-xl">Lumina</h2>
            <p className="text-[10px] font-bold text-primary uppercase tracking-widest">Advanced Secretary</p>
          </div>
        </div>

        {/* 记忆插槽 */}
        <div className="flex-1 neo-concave rounded-[3rem] p-6 space-y-6 overflow-y-auto no-scrollbar">
          <div className="flex items-center justify-between px-2">
            <span className="text-[9px] font-black text-foreground/30 tracking-widest uppercase">Memory Clusters</span>
            <Plus size={12} className="cursor-pointer hover:text-primary transition-colors" />
          </div>
          
          {[...Array(3)].map((_, i) => (
            <div key={i} className="neo-convex p-4 rounded-2xl space-y-2 group cursor-pointer active:scale-95 transition-all">
              <div className="flex items-center gap-2">
                <div className="w-2 h-2 rounded-full bg-primary/40 group-hover:bg-primary transition-colors"></div>
                <span className="text-[10px] font-bold opacity-60">Session #{1024 + i}</span>
              </div>
              <p className="text-[10px] text-foreground/30 line-clamp-2 leading-relaxed">关于 llm-orchestration-platform 的前端视觉重构讨论记录...</p>
            </div>
          ))}
        </div>
      </aside>

      {/* 右侧：主协作区 */}
      <main className="flex-1 flex flex-col min-w-0 neo-convex rounded-[4rem] overflow-hidden border border-white/40 shadow-2xl">
        {/* 顶栏：连接状态 */}
        <header className="h-20 flex items-center px-10 border-b border-foreground/5 bg-white/5 backdrop-blur-xl">
          <div className="flex items-center gap-4">
            <div className="w-3 h-3 rounded-full bg-green-500 shadow-[0_0_10px_rgba(34,197,94,0.5)]"></div>
            <span className="text-xs font-bold tracking-widest opacity-60 uppercase">Encrypted Collaboration Channel</span>
          </div>
          <div className="flex-1"></div>
          <button className="w-10 h-10 neo-convex rounded-full flex items-center justify-center text-foreground/30 hover:text-red-500 transition-colors">
            <Power size={18} />
          </button>
        </header>

        {/* 消息记录 */}
        <div ref={chatScrollRef} className="flex-1 overflow-y-auto p-10 space-y-10 scroll-smooth no-scrollbar">
          <AnimatePresence initial={false}>
            {messages.map((msg, idx) => (
              <motion.div 
                key={idx}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}
              >
                <div 
                  className={`max-w-[80%] p-6 rounded-[2.5rem] ${
                    msg.role === 'user' ? 'bg-primary text-white shadow-xl rounded-tr-sm' : 'neo-concave text-foreground/80 rounded-tl-sm'
                  }`}
                >
                  <article 
                    className="markdown-rendered prose prose-sm dark:prose-invert max-w-none"
                    dangerouslySetInnerHTML={{ __html: renderMarkdown(msg.content) }}
                  />
                </div>
              </motion.div>
            ))}
          </AnimatePresence>
          
          {loading && (
            <div className="flex justify-start">
              <div className="neo-concave px-6 py-3 rounded-full flex items-center gap-3">
                <div className="w-1.5 h-1.5 bg-primary rounded-full animate-bounce"></div>
                <div className="w-1.5 h-1.5 bg-primary rounded-full animate-bounce [animation-delay:-0.15s]"></div>
                <div className="w-1.5 h-1.5 bg-primary rounded-full animate-bounce [animation-delay:-0.3s]"></div>
              </div>
            </div>
          )}
        </div>

        {/* 输入槽 */}
        <footer className="p-8">
          <div className="neo-concave rounded-[3rem] p-2 flex items-center gap-3 group focus-within:ring-2 ring-primary/20 transition-all">
            <div className="w-12 h-12 neo-convex rounded-full flex items-center justify-center text-foreground/20 group-focus-within:text-primary transition-colors">
              <Mic size={20} />
            </div>
            <input 
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleSend()}
              placeholder="下达指令给您的个人秘书..."
              className="flex-1 bg-transparent border-none focus:outline-none px-2 py-4 text-sm font-medium placeholder:text-foreground/10"
            />
            <button 
              onClick={handleSend}
              disabled={!input.trim() || loading}
              className={`w-14 h-14 neo-convex rounded-[1.8rem] flex items-center justify-center transition-all ${
                input.trim() && !loading ? 'text-primary active:scale-90 shadow-lg' : 'text-foreground/10'
              }`}
            >
              <ArrowUp size={24} />
            </button>
          </div>
        </footer>
      </main>

    </div>
  )
}

export default Secretary
