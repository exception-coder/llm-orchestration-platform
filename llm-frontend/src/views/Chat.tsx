import React, { useState, useEffect, useRef, useCallback } from 'react'
import { Settings, Trash2, Send, Loader2, MessageSquare } from 'lucide-react'
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
      <header className="px-6 py-4 app-header rounded-3xl flex flex-wrap items-center gap-6 shadow-sm">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 app-surface rounded-xl flex items-center justify-center text-muted-foreground">
            <Settings size={18} />
          </div>
          <div>
            <p className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">Active Configuration</p>
            <p className="text-sm font-bold text-foreground truncate">{config.model || '加载中...'} / {config.provider}</p>
          </div>
        </div>
        
        <div className="flex-1"></div>
        
        <div className="flex items-center gap-2">
          <button 
            onClick={() => setShowRawText(!showRawText)}
            className={`px-4 py-2 rounded-xl text-[10px] font-black tracking-widest uppercase transition-all border ${
              showRawText ? 'bg-primary/10 border-primary/20 text-primary' : 'bg-muted/50 border-border text-muted-foreground hover:bg-muted'
            }`}
          >
            RAW 模式
          </button>
          <button 
            onClick={() => setMessages([])}
            className="w-10 h-10 flex items-center justify-center rounded-xl bg-muted/50 border border-border text-muted-foreground hover:text-red-500 hover:bg-red-500/5 hover:border-red-500/20 transition-all active:scale-95"
          >
            <Trash2 size={18} />
          </button>
        </div>
      </header>

      {/* 2. 聊天记录流 */}
      <div 
        ref={messageListRef}
        className="flex-1 overflow-y-auto px-2 space-y-10 scroll-smooth no-scrollbar"
      >
        <AnimatePresence initial={false}>
          {messages.length === 0 ? (
            <div className="h-full flex flex-col items-center justify-center opacity-20 pointer-events-none py-20">
              <MessageSquare size={80} strokeWidth={1} className="mb-6" />
              <p className="font-bold tracking-[0.4em] uppercase text-sm">Waiting for Spark</p>
            </div>
          ) : (
            messages.map((msg, index) => (
              <motion.div 
                key={index}
                initial={{ opacity: 0, y: 12 }}
                animate={{ opacity: 1, y: 0 }}
                className={`flex w-full ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}
              >
                <div className={`max-w-[85%] flex flex-col ${msg.role === 'user' ? 'items-end' : 'items-start'}`}>
                  {/* 消息内容 */}
                  <div 
                    className={`p-6 text-sm leading-relaxed transition-all ${
                      msg.role === 'user' 
                        ? 'bg-primary text-primary-foreground font-medium rounded-3xl rounded-tr-sm shadow-sm' 
                        : 'app-surface text-foreground rounded-3xl rounded-tl-sm'
                    }`}
                  >
                    {showRawText ? (
                      <div className="whitespace-pre-wrap font-mono text-xs">{msg.content}</div>
                    ) : (
                      <article 
                        className="markdown-rendered prose prose-slate dark:prose-invert max-w-none prose-sm"
                        dangerouslySetInnerHTML={{ __html: renderMarkdown(msg.content) }}
                      />
                    )}
                  </div>

                  {/* 消息底部状态 */}
                  <div className={`flex items-center gap-3 mt-2 px-2 transition-opacity ${msg.role === 'user' ? 'flex-row-reverse' : ''}`}>
                    {msg.tokenUsage && (
                      <span className="text-[9px] font-bold text-muted-foreground/40 tracking-tight">
                        {msg.tokenUsage.totalTokens} tokens
                      </span>
                    )}
                    <span className="text-[9px] font-bold text-muted-foreground/20 uppercase tracking-widest">
                      {msg.role === 'user' ? 'You' : 'Assistant'}
                    </span>
                  </div>
                </div>
              </motion.div>
            ))
          )}
        </AnimatePresence>
        
        {/* 加载状态 */}
        {loading && (
          <div className="flex justify-start">
            <div className="app-surface px-4 py-3 rounded-2xl flex items-center gap-2">
              <Loader2 size={14} className="animate-spin text-primary" />
              <span className="text-[10px] font-bold text-muted-foreground uppercase tracking-widest">Thought in progress...</span>
            </div>
          </div>
        )}
      </div>

      {/* 3. 底部输入框 */}
      <footer className="relative pb-6 px-1">
        <div className="app-input flex items-center gap-2 focus-within:ring-4 ring-primary/5 transition-all">
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
            className="flex-1 bg-transparent border-none focus:outline-none px-4 py-2 text-sm resize-none max-h-48 placeholder:text-muted-foreground/40 no-scrollbar"
            rows={1}
          ></textarea>
          
          <button 
            onClick={handleSend}
            disabled={!input.trim() || loading}
            className={`w-12 h-12 flex items-center justify-center rounded-full transition-all ${
              input.trim() && !loading ? 'app-btn-primary shadow-lg shadow-primary/20 active:scale-90' : 'bg-muted text-muted-foreground/40 cursor-not-allowed'
            }`}
          >
            <Send size={18} />
          </button>
        </div>
        <p className="text-center text-[9px] font-medium text-muted-foreground/30 mt-4 uppercase tracking-[0.2em]">
          AI may generate inaccurate information. Please verify important details.
        </p>
      </footer>
    </div>
  )
}

export default Chat
