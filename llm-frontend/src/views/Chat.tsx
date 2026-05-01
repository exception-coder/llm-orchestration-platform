import React, { useState, useEffect, useRef, useCallback } from 'react'
import { Settings, Trash2, Send, Loader2 } from 'lucide-react'
import { motion, AnimatePresence } from 'framer-motion'
import { promptTestAPI } from '@/api'
import { useMarkdown } from '@/hooks/useMarkdown'
import { useSSEStream } from '@/hooks/useSSEStream'

interface Message {
  role: 'user' | 'assistant'
  content: string
  tokenUsage?: any
}

const Chat: React.FC = () => {
  const { renderMarkdown } = useMarkdown()
  const { fetchSSE } = useSSEStream()

  const [config, setConfig] = useState({
    conversationId: 'conv-' + Date.now(),
    model: '',
    provider: ''
  })

  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [showRawText, setShowRawText] = useState(false)
  const [messages, setMessages] = useState<Message[]>([])
  const messageListRef = useRef<HTMLDivElement>(null)

  const loadModels = useCallback(async () => {
    try {
      const res: any = await promptTestAPI.getModels()
      if (res && res.length > 0) {
        setConfig(prev => ({
          ...prev,
          model: res[0].modelId,
          provider: res[0].provider || 'OpenAI'
        }))
      }
    } catch (err) {
      console.error('Failed to load models:', err)
    }
  }, [])

  const scrollToBottom = () => {
    if (messageListRef.current) {
      messageListRef.current.scrollTop = messageListRef.current.scrollHeight
    }
  }

  useEffect(() => {
    scrollToBottom()
  }, [messages])

  const handleSend = async () => {
    if (!input.trim() || loading) return
    
    const userContent = input.trim()
    const newMessages: Message[] = [...messages, { role: 'user', content: userContent }]
    setMessages(newMessages)
    setInput('')
    setLoading(true)

    // AI 占位
    const aiMessage: Message = { role: 'assistant', content: '' }
    setMessages(prev => [...prev, aiMessage])

    try {
      let currentAiContent = ''
      await fetchSSE('/chat/stream', {
        conversationId: config.conversationId,
        message: userContent,
        model: config.model,
        provider: config.provider
      }, {
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
        },
        onTokenUsage: (usage) => {
          setMessages(prev => {
            const next = [...prev]
            const last = next[next.length - 1]
            if (last && last.role === 'assistant') {
              last.tokenUsage = usage
            }
            return next
          })
        }
      })
    } catch (err) {
      console.error('Chat error:', err)
      setLoading(false)
    }
  }

  useEffect(() => {
    loadModels()
  }, [loadModels])

  return (
    <div className="flex flex-col h-full max-w-5xl mx-auto space-y-6">
      
      {/* 1. 顶部配置区域 */}
      <header className="p-6 neo-convex rounded-3xl flex flex-wrap items-center gap-6">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 neo-concave rounded-2xl flex items-center justify-center text-primary">
            <Settings size={20} />
          </div>
          <div>
            <p className="text-[10px] font-bold uppercase tracking-widest text-foreground/40">当前配置</p>
            <p className="text-sm font-semibold truncate">{config.model || '加载中...'} / {config.provider}</p>
          </div>
        </div>
        
        <div className="flex-1"></div>
        
        <div className="flex items-center gap-2">
          <button 
            onClick={() => setShowRawText(!showRawText)}
            className={`px-4 py-2 rounded-xl text-xs font-bold transition-all ${
              showRawText ? 'neo-concave text-primary' : 'neo-convex text-foreground/60'
            }`}
          >
            RAW 模式
          </button>
          <button 
            onClick={() => setMessages([])}
            className="p-2 rounded-xl neo-convex text-red-500/60 hover:text-red-500 transition-all active:scale-95"
          >
            <Trash2 size={18} />
          </button>
        </div>
      </header>

      {/* 2. 聊天记录流 */}
      <div 
        ref={messageListRef}
        className="flex-1 overflow-y-auto pr-4 space-y-8 scroll-smooth no-scrollbar"
      >
        <AnimatePresence initial={false}>
          {messages.map((msg, index) => (
            <motion.div 
              key={index}
              initial={{ opacity: 0, y: 20, scale: 0.95 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              className={`flex w-full ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}
            >
              <div className="max-w-[85%] group relative">
                {/* 消息头部 */}
                <div className={`flex items-center gap-2 mb-2 px-2 ${msg.role === 'user' ? 'flex-row-reverse' : ''}`}>
                  <span className="text-[10px] font-black uppercase tracking-tighter opacity-30">
                    {msg.role === 'user' ? 'YOU' : 'AI ASSISTANT'}
                  </span>
                </div>

                {/* 消息内容 */}
                <div 
                  className={`p-5 rounded-[2rem] text-sm leading-relaxed ${
                    msg.role === 'user' 
                      ? 'bg-primary text-white shadow-xl shadow-primary/20 rounded-tr-sm' 
                      : 'neo-convex border border-white/40 text-foreground/80 rounded-tl-sm'
                  }`}
                >
                  {showRawText ? (
                    <div className="whitespace-pre-wrap font-mono">{msg.content}</div>
                  ) : (
                    <article 
                      className="markdown-rendered prose prose-slate dark:prose-invert max-w-none"
                      dangerouslySetInnerHTML={{ __html: renderMarkdown(msg.content) }}
                    />
                  )}
                </div>

                {/* Token 使用统计 */}
                {msg.tokenUsage && (
                  <div className="mt-2 px-4 py-1 inline-block neo-concave rounded-full text-[9px] font-bold text-foreground/40">
                    ⚡ {msg.tokenUsage.totalTokens} Tokens
                  </div>
                )}
              </div>
            </motion.div>
          ))}
        </AnimatePresence>
        
        {/* 加载状态 */}
        {loading && (
          <div className="flex justify-start">
            <div className="neo-convex p-4 rounded-full flex gap-1">
              <div className="w-2 h-2 bg-primary rounded-full animate-bounce [animation-delay:-0.3s]"></div>
              <div className="w-2 h-2 bg-primary rounded-full animate-bounce [animation-delay:-0.15s]"></div>
              <div className="w-2 h-2 bg-primary rounded-full animate-bounce"></div>
            </div>
          </div>
        )}
      </div>

      {/* 3. 底部输入框 */}
      <footer className="p-4">
        <div className="relative neo-concave rounded-[2.5rem] p-2 flex items-center gap-2 group transition-all focus-within:ring-2 ring-primary/20">
          <textarea
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault()
                handleSend()
              }
            }}
            placeholder="输入消息，开启灵感对话..."
            className="flex-1 bg-transparent border-none focus:outline-none px-6 py-4 text-sm resize-none max-h-32 placeholder:text-foreground/20"
            rows={1}
          ></textarea>
          
          <button 
            onClick={handleSend}
            disabled={!input.trim() || loading}
            className={`w-12 h-12 flex items-center justify-center rounded-full transition-all ${
              input.trim() && !loading ? 'bg-primary text-white shadow-lg active:scale-90' : 'text-foreground/20 cursor-not-allowed'
            }`}
          >
            <Send size={20} />
          </button>
        </div>
      </footer>

    </div>
  )
}

export default Chat
