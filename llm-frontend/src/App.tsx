import React, { useState, useEffect, useMemo } from 'react'
import { BrowserRouter as Router, Routes, Route, Link, useLocation } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import {
  Menu, X, Bell, Search, ChevronsLeft, ChevronsRight, Sparkles, Check
} from 'lucide-react'
import { useResponsive } from '@/hooks/useResponsive'
import { applyTheme, themes } from '@/styles/themeRegistry'
import { defaultPageMeta, menuGroups, pageMetaMap } from '@/app/navigation'

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

  const selectTheme = (theme: string) => {
    setCurrentTheme(theme)
    localStorage.setItem('app-theme', theme)
    applyTheme(theme)
  }

  useEffect(() => {
    applyTheme(currentTheme)
  }, [currentTheme])

  const pageMeta = useMemo(() => {
    return pageMetaMap[location.pathname] || defaultPageMeta
  }, [location.pathname])

  return (
    <div className="min-h-screen bg-background text-foreground transition-colors duration-500 font-sans selection:bg-primary/10 selection:text-primary app-shell">
      {isMobile && (
        <button 
          onClick={() => setSidebarOpen(!sidebarOpen)}
          className="fixed bottom-6 right-6 z-50 p-4 app-surface rounded-full text-primary shadow-xl active:scale-90 transition-transform"
          aria-label={sidebarOpen ? '关闭导航菜单' : '打开导航菜单'}
        >
          {!sidebarOpen ? <Menu size={24} /> : <X size={24} />}
        </button>
      )}

      <div className="flex h-screen overflow-hidden">
        <aside 
          className={`app-sidebar relative z-40 flex flex-col transition-all duration-500 ease-[cubic-bezier(0.16,1,0.3,1)] ${
            sidebarOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'
          } ${isCollapsed ? 'w-24' : 'w-80'}`}
        >
          <div className={`h-24 flex items-center px-6 shrink-0 border-b border-border/70 ${isCollapsed ? 'justify-center' : 'justify-between'}`}>
            {!isCollapsed ? (
              <motion.div 
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                className="flex items-center gap-3"
              >
                <div className="w-10 h-10 bg-primary rounded-2xl flex items-center justify-center text-primary-foreground font-black shadow-lg shadow-primary/20">L</div>
                <div>
                  <p className="font-bold tracking-tight text-lg leading-none">LLM OS</p>
                  <p className="text-xs text-muted-foreground mt-1">AI Workflow Studio</p>
                </div>
              </motion.div>
            ) : (
              <div className="w-10 h-10 bg-primary rounded-xl flex items-center justify-center text-primary-foreground font-black shadow-lg shadow-primary/20 shrink-0">L</div>
            )}
            
            <button 
              onClick={() => setIsCollapsed(!isCollapsed)}
              className="w-8 h-8 rounded-lg flex items-center justify-center text-muted-foreground hover:bg-muted hover:text-foreground transition-colors active:scale-95"
              aria-label={isCollapsed ? '展开侧边栏' : '收起侧边栏'}
            >
              {isCollapsed ? <ChevronsRight size={18} /> : <ChevronsLeft size={18} />}
            </button>
          </div>

          <nav className="flex-1 overflow-y-auto px-4 py-6 space-y-7 no-scrollbar">
            {menuGroups.map((group) => (
              <div key={group.label} className="space-y-2">
                {!isCollapsed && (
                  <p className="text-[10px] font-bold uppercase tracking-[0.18em] text-muted-foreground/50 px-4 mb-2">
                    {group.label}
                  </p>
                )}

                <div className="space-y-1">
                  {group.items.map((item) => {
                    const isActive = location.pathname === item.path
                    const Icon = item.icon
                    return (
                      <Link
                        key={item.path}
                        to={item.path}
                        className="block group"
                        onClick={() => isMobile && setSidebarOpen(false)}
                      >
                        <div 
                          className={`flex items-center gap-3 rounded-2xl cursor-pointer transition-all duration-200 relative ${
                            isActive 
                              ? 'bg-primary/12 text-primary font-semibold ring-1 ring-primary/20' 
                              : 'text-muted-foreground hover:bg-muted hover:text-foreground'
                          } ${isCollapsed ? 'h-12 w-12 mx-auto justify-center px-0' : 'px-4 py-3'}`}
                          aria-label={item.name}
                        >
                          <Icon 
                            size={isCollapsed ? 22 : 18} 
                            strokeWidth={isActive ? 2.5 : 2}
                            className="shrink-0 transition-transform group-hover:scale-110"
                          />
                          {!isCollapsed && (
                            <div className="min-w-0">
                              <p className="text-sm truncate">{item.name}</p>
                              <p className="text-[11px] text-muted-foreground truncate">{item.description}</p>
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

          <div className="p-4 border-t border-border/70 space-y-3">
            {!isCollapsed && (
              <p className="px-2 text-[10px] font-bold uppercase tracking-[0.18em] text-muted-foreground/50">
                Theme
              </p>
            )}
            <div className={isCollapsed ? 'flex flex-col items-center gap-2' : 'grid grid-cols-2 gap-2'}>
              {themes.map((theme) => {
                const ThemeIcon = theme.icon
                const isActive = currentTheme === theme.id
                return (
                  <button
                    key={theme.id || 'default'}
                    onClick={() => selectTheme(theme.id)}
                    title={theme.label}
                    aria-label={`切换到${theme.label}`}
                    className={`relative flex items-center rounded-xl border transition-all active:scale-95 ${
                      isCollapsed ? 'h-10 w-10 justify-center' : 'h-10 gap-2 px-3 text-left'
                    } ${
                      isActive
                        ? 'border-primary/30 bg-primary/10 text-primary shadow-sm'
                        : 'border-transparent text-muted-foreground hover:bg-muted hover:text-foreground'
                    }`}
                  >
                    <ThemeIcon size={16} className="shrink-0" />
                    {!isCollapsed && <span className="min-w-0 truncate text-xs font-semibold">{theme.label}</span>}
                    {isActive && !isCollapsed && <Check size={13} className="ml-auto shrink-0" />}
                  </button>
                )
              })}
            </div>
          </div>
        </aside>

        <main className="flex-1 flex flex-col relative overflow-hidden bg-background">
          <header className="app-header h-24 flex items-center justify-between px-6 md:px-8 sticky top-0 z-30">
            <div className="min-w-0">
              <div className="flex items-center gap-2 text-xs text-muted-foreground mb-1">
                <Sparkles size={14} />
                <span>Workspace</span>
              </div>
              <h1 className="text-xl md:text-2xl font-bold tracking-tight text-foreground/90 truncate">{pageMeta.title}</h1>
              <p className="hidden md:block text-sm text-muted-foreground mt-1 truncate">{pageMeta.subtitle}</p>
            </div>

            <div className="flex items-center gap-3 shrink-0">
              <button className="hidden md:flex h-10 items-center gap-2 rounded-full px-4 app-recess text-muted-foreground hover:text-foreground transition-colors">
                <Search size={16} />
                <span className="text-xs font-medium">快速搜索</span>
              </button>

              <div className="hidden md:flex items-center gap-1 bg-muted rounded-full px-3 py-1.5 border border-border">
                <div className="w-1.5 h-1.5 rounded-full bg-green-500 animate-pulse"></div>
                <span className="text-[10px] font-bold text-muted-foreground uppercase tracking-tight">System Online</span>
              </div>
              
              <button className="w-10 h-10 rounded-full flex items-center justify-center text-muted-foreground hover:bg-muted hover:text-foreground transition-all border border-transparent hover:border-border active:scale-95">
                <Bell size={18} />
              </button>

              <div className="w-10 h-10 rounded-full overflow-hidden border border-border p-0.5 cursor-pointer hover:ring-2 hover:ring-primary/20 transition-all active:scale-95">
                <img className="w-full h-full rounded-full object-cover" src="https://api.dicebear.com/7.x/avataaars/svg?seed=Lucky" alt="avatar" />
              </div>
            </div>
          </header>

          <div className="flex-1 overflow-y-auto no-scrollbar">
            <div className="max-w-[1600px] mx-auto min-h-full">
              <AnimatePresence mode="out-in">
                <motion.div
                  key={location.pathname}
                  initial={{ opacity: 0, y: 8 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -8 }}
                  transition={{ duration: 0.3, ease: [0.16, 1, 0.3, 1] }}
                  className="p-5 md:p-8 lg:p-10"
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
