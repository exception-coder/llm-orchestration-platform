import React, { useState, useEffect, useMemo } from 'react'
import { BrowserRouter as Router, Routes, Route, Link, useLocation } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import {
  Menu, X, Sun, Moon, Bell, ChevronRight, Terminal,
  MessageSquare, LayoutGrid, FileText, Notebook,
  Settings, Image, Compass, Boxes, UserCheck,
  Bot, GitBranch, Wrench, GitCompare
} from 'lucide-react'
import { useResponsive } from '@/hooks/useResponsive'

import AgentList from '@/views/AgentList'
import GraphOrchestration from '@/views/GraphOrchestration'
import GraphVisualization from '@/views/GraphVisualization'
import Chat from '@/views/Chat'
import DocViewer from '@/views/DocViewer'
import PromptTest from '@/views/PromptTest'
import PromptComparison from '@/views/PromptComparison'
import ContentOptimization from '@/views/ContentOptimization'
import NoteCapture from '@/views/NoteCapture'
import ModelManagement from '@/views/ModelManagement'
import TemplateManagement from '@/views/TemplateManagement'
import ToolManagement from '@/views/ToolManagement'
import MarkdownToImage from '@/views/MarkdownToImage'
import Secretary from '@/views/Secretary'

// 路由占位组件
const Placeholder = ({ title }: { title: string }) => (
  <div className="flex flex-col items-center justify-center py-20 opacity-20">
    <h2 className="text-2xl font-black uppercase tracking-widest">{title} 建设中</h2>
  </div>
)

const AppContent: React.FC = () => {
  const { isMobile } = useResponsive()
  const location = useLocation()
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [isCollapsed, setIsCollapsed] = useState(false)
  const [currentTheme, setCurrentTheme] = useState(localStorage.getItem('app-theme') || '')

  const themes = [
    { id: '', label: '物理拟物', icon: Boxes },
    { id: 'apple', label: '苹果毛玻璃', icon: Sun },
    { id: 'google', label: '谷歌材质', icon: LayoutGrid },
    { id: 'cyber', label: '赛博终端', icon: Terminal }
  ]

  const toggleTheme = () => {
    const currentIndex = themes.findIndex(t => t.id === currentTheme)
    const nextIndex = (currentIndex + 1) % themes.length
    const theme = themes[nextIndex].id
    
    setCurrentTheme(theme)
    localStorage.setItem('app-theme', theme)
    applyTheme(theme)
  }

  const applyTheme = (theme: string) => {
    if (theme) {
      document.documentElement.setAttribute('data-theme', theme)
    } else {
      document.documentElement.removeAttribute('data-theme')
    }
  }

  useEffect(() => {
    applyTheme(currentTheme)
  }, [currentTheme])

  const menuGroups = [
    {
      label: '核心',
      items: [
        { name: '智能对话', path: '/chat', icon: MessageSquare },
        { name: '知识库', path: '/doc-viewer', icon: FileText },
        { name: '碎片记录', path: '/note-capture', icon: Notebook }
      ]
    },
    {
      label: '工程',
      items: [
        { name: 'Prompt 实验室', path: '/prompt-test', icon: LayoutGrid },
        { name: 'Prompt A/B 对比', path: '/prompt-comparison', icon: GitCompare },
        { name: '内容优化', path: '/content-optimization', icon: Compass },
        { name: '模型管理', path: '/model-management', icon: Settings },
        { name: '模板管理', path: '/template-management', icon: FileText }
      ]
    },
    {
      label: '智能体管理',
      items: [
        { name: '智能体', path: '/agent-list', icon: Bot },
        { name: 'Agent 编排', path: '/graph-orchestration', icon: GitBranch },
        { name: 'Tool 管理', path: '/tool-management', icon: Wrench }
      ]
    },
    {
      label: '工具',
      items: [
        { name: 'Markdown 绘图', path: '/markdown-to-image', icon: Image },
        { name: '个人助理', path: '/secretary', icon: UserCheck }
      ]
    }
  ]

  const pageTitle = useMemo(() => {
    const titles: Record<string, string> = {
      '/chat': '智能对话',
      '/doc-viewer': '知识库中心',
      '/note-capture': '碎片化笔记',
      '/prompt-test': 'Prompt 工程',
      '/model-management': '模型控制台',
      '/agent-list': '智能体',
      '/graph-orchestration': 'Agent 编排',
      '/tool-management': 'Tool 管理'
    }
    return titles[location.pathname] || '控制台'
  }, [location.pathname])

  return (
    <div data-theme={currentTheme} className="min-h-screen bg-background text-foreground transition-colors duration-500 font-sans">
      
      {/* 1. 响应式移动端菜单按钮 */}
      {isMobile && (
        <button 
          onClick={() => setSidebarOpen(!sidebarOpen)}
          className="fixed bottom-6 right-6 z-50 p-4 neo-convex rounded-full text-primary"
        >
          {!sidebarOpen ? <Menu size={24} /> : <X size={24} />}
        </button>
      )}

      <div className="flex h-screen overflow-hidden">
        
        {/* 2. 极致侧边栏 */}
        <aside 
          className={`relative z-40 flex flex-col transition-all duration-500 ease-[cubic-bezier(0.4,0,0.2,1)] border-r border-white/10 ${
            sidebarOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'
          } ${isCollapsed ? 'w-24' : 'w-72'}`}
        >
          {/* Logo & Toggle 区域 */}
          <div className={`h-24 flex items-center px-6 relative overflow-hidden ${isCollapsed ? 'justify-center' : 'justify-start gap-4'}`}>
            <button 
              onClick={() => setIsCollapsed(!isCollapsed)}
              className="w-12 h-12 neo-convex rounded-2xl flex items-center justify-center text-foreground/40 hover:text-primary transition-all active:scale-90 shrink-0"
            >
              <Menu size={20} className={isCollapsed ? 'text-primary' : ''} />
            </button>

            {!isCollapsed && (
              <motion.div 
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                className="flex items-center gap-3"
              >
                <div className="w-8 h-8 bg-primary rounded-xl flex items-center justify-center text-white font-black shadow-lg shadow-primary/30">L</div>
                <span className="font-black tracking-tighter text-xl whitespace-nowrap">LLM OS</span>
              </motion.div>
            )}
          </div>

          {/* 导航列表 */}
          <nav className="flex-1 overflow-y-auto px-6 py-8 space-y-10 no-scrollbar">
            {menuGroups.map((group) => (
              <div key={group.label} className="space-y-4">
                {!isCollapsed ? (
                  <p className="text-[9px] font-black uppercase tracking-[0.3em] text-foreground/20 px-2 flex items-center gap-3">
                    <span>{group.label}</span>
                    <span className="flex-1 h-[1px] bg-foreground/5"></span>
                  </p>
                ) : (
                  <div className="h-[1px] bg-foreground/5 mx-2"></div>
                )}

                <div className="space-y-2">
                  {group.items.map((item) => {
                    const isActive = location.pathname === item.path
                    const Icon = item.icon
                    return (
                      <Link key={item.path} to={item.path} className="block">
                        <div 
                          className={`flex items-center gap-4 rounded-2xl cursor-pointer transition-all duration-300 relative group ${
                            isActive ? 'neo-concave text-primary' : 'text-foreground/40 hover:text-foreground/70'
                          } ${isCollapsed ? 'h-16 w-16 mx-auto justify-center px-0' : 'px-5 py-4'}`}
                        >
                          <Icon 
                            size={isCollapsed ? 28 : 22} 
                            strokeWidth={isActive ? 2.5 : 2}
                            className="transition-transform duration-300 group-hover:scale-110"
                          />
                          {!isCollapsed && <span className="font-bold text-xs tracking-wide whitespace-nowrap">{item.name}</span>}
                          
                          {isActive && !isCollapsed && (
                            <div className="absolute left-1 w-1 h-4 bg-primary rounded-full shadow-[0_0_8px_var(--color-primary)]"></div>
                          )}

                          {isCollapsed && (
                            <div className="absolute left-20 px-4 py-2 bg-foreground text-background text-[10px] font-black rounded-xl opacity-0 translate-x-[-10px] group-hover:opacity-100 group-hover:translate-x-0 transition-all pointer-events-none whitespace-nowrap z-50 shadow-xl">
                              {item.name}
                            </div>
                          )}
                        </div>
                      </Link>
                    )
                  })}
                </div>
              </div>
            ))}
          </nav>

          {/* 主题切换脚部 */}
          <div className="p-4 border-t border-white/5 space-y-2">
            <button 
              onClick={toggleTheme} 
              className="w-full flex items-center gap-4 px-4 py-3 rounded-2xl neo-convex text-foreground/60 active:scale-95"
            >
              {currentTheme === 'glass' ? <Sun size={20} /> : <Moon size={20} />}
              {!isCollapsed && <span className="text-sm">切换材质</span>}
            </button>
          </div>
        </aside>

        {/* 3. 主内容区域 */}
        <main className="flex-1 flex flex-col relative overflow-hidden">
          <header className="h-20 flex items-center px-8 border-b border-white/5">
            <h1 className="text-xl font-bold tracking-tight">{pageTitle}</h1>
            <div className="flex-1"></div>
            <div className="flex items-center gap-4">
              <div className="w-10 h-10 neo-convex rounded-full flex items-center justify-center cursor-pointer">
                <Bell size={18} />
              </div>
              <div className="w-10 h-10 neo-convex rounded-full overflow-hidden border-2 border-primary/20 cursor-pointer">
                <img src="https://api.dicebear.com/7.x/avataaars/svg?seed=Lucky" alt="avatar" />
              </div>
            </div>
          </header>

          <div className="flex-1 overflow-y-auto p-8">
            <AnimatePresence mode="out-in">
              <motion.div
                key={location.pathname}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -10 }}
                transition={{ duration: 0.4, ease: [0.4, 0, 0.2, 1] }}
                className="h-full"
              >
                <Routes>
                  <Route path="/" element={<Placeholder title="首页" />} />
                  <Route path="/chat" element={<Chat />} />
                  <Route path="/doc-viewer" element={<DocViewer />} />
                  <Route path="/note-capture" element={<NoteCapture />} />
                  <Route path="/prompt-test" element={<PromptTest />} />
                  <Route path="/prompt-comparison" element={<PromptComparison />} />
                  <Route path="/content-optimization" element={<ContentOptimization />} />
                  <Route path="/model-management" element={<ModelManagement />} />
                  <Route path="/template-management" element={<TemplateManagement />} />
                  <Route path="/agent-list" element={<AgentList />} />
                  <Route path="/graph-orchestration" element={<GraphOrchestration />} />
                  <Route path="/graph-visualization/:id" element={<GraphVisualization />} />
                  <Route path="/tool-management" element={<ToolManagement />} />
                  <Route path="/markdown-to-image" element={<MarkdownToImage />} />
                  <Route path="/secretary" element={<Secretary />} />
                </Routes>
              </motion.div>
            </AnimatePresence>
          </div>
        </main>
      </div>
    </div>
  )
}

const App: React.FC = () => {
  return (
    <Router>
      <AppContent />
    </Router>
  )
}

export default App
