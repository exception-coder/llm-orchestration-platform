import React, { useState, useEffect, useCallback, useRef } from 'react'
import { 
  Search, FileText, Folder, FolderOpen, 
  Library, ChevronLeft, X, Share2, ArrowRight, Loader2, ChevronRight
} from 'lucide-react'
import { motion, AnimatePresence } from 'framer-motion'
import { docViewerAPI } from '@/api'
import { useMarkdown } from '@/hooks/useMarkdown'
import { useResponsive } from '@/hooks/useResponsive'

// ---- TreeItem Component ----
interface ITreeItem {
  id?: string
  name: string
  path: string
  type: 'DIRECTORY' | 'FILE'
  children?: ITreeItem[]
}

const TreeItem: React.FC<{ 
  item: ITreeItem
  currentPath?: string
  depth?: number
  onSelect: (path: string) => void 
}> = ({ item, currentPath, depth = 0, onSelect }) => {
  const [isOpen, setIsOpen] = useState(true)
  
  const toggle = () => {
    if (item.type === 'DIRECTORY') setIsOpen(!isOpen)
  }
  
  const select = () => {
    if (item.type === 'FILE') onSelect(item.path)
  }

  const isActive = currentPath === item.path

  return (
    <div className="select-none">
      <div 
        onClick={item.type === 'DIRECTORY' ? toggle : select}
        className={`flex items-center gap-3 px-4 py-2 rounded-2xl cursor-pointer transition-all duration-200 group ${
          isActive ? 'neo-convex text-primary font-bold z-10' : 'hover:bg-foreground/5 text-foreground/60'
        }`}
        style={{ marginLeft: depth * 12 + 'px' }}
      >
        <div className="shrink-0 transition-transform group-hover:scale-110">
          {item.type === 'DIRECTORY' ? (
            !isOpen ? <Folder size={16} className="opacity-40" /> : <FolderOpen size={16} className="text-primary/60" />
          ) : (
            <FileText size={16} className={isActive ? 'text-primary' : 'opacity-40'} />
          )}
        </div>
        <span className="text-xs truncate flex-1">{item.name}</span>
        {item.type === 'DIRECTORY' && (
          <ChevronRight size={12} className={`transition-transform opacity-20 ${isOpen ? 'rotate-90' : ''}`} />
        )}
      </div>
      {isOpen && item.children && (
        <div className="mt-1">
          {item.children.map((child) => (
            <TreeItem 
              key={child.path} 
              item={child} 
              depth={depth + 1}
              currentPath={currentPath}
              onSelect={onSelect}
            />
          ))}
        </div>
      )}
    </div>
  )
}

// ---- Main Component ----
const DocViewer: React.FC = () => {
  const { renderMarkdown } = useMarkdown()
  const { isMobile } = useResponsive()

  const [treeData, setTreeData] = useState<ITreeItem[]>([])
  const [treeLoading, setTreeLoading] = useState(true)
  const [currentDoc, setCurrentDoc] = useState<any>(null)
  const [contentLoading, setContentLoading] = useState(false)
  const [searchKeyword, setSearchKeyword] = useState('')
  const [searchResults, setSearchResults] = useState<any[]>([])
  const [searchLoading, setSearchLoading] = useState(false)
  const markdownRef = useRef<HTMLDivElement>(null)

  const loadTree = useCallback(async () => {
    try {
      const res: any = await docViewerAPI.getTree()
      setTreeData(res.items || [])
    } catch (err) {
      console.error('Failed to get tree', err)
    } finally {
      setTreeLoading(false)
    }
  }, [])

  const loadDoc = async (path: string) => {
    setContentLoading(true)
    try {
      const doc: any = await docViewerAPI.getContent(path)
      setCurrentDoc(doc)
      setSearchResults([]) // Clear search on selection
    } catch (err) {
      console.error('Failed to read doc', err)
    } finally {
      setContentLoading(false)
    }
  }

  const handleSearch = async () => {
    if (!searchKeyword.trim()) return
    setSearchLoading(true)
    try {
      const res: any = await docViewerAPI.search(searchKeyword)
      setSearchResults(res.hits || [])
    } catch (err) {
      console.error('Search service error', err)
    } finally {
      setSearchLoading(false)
    }
  }

  const clearSearch = () => {
    setSearchKeyword('')
    setSearchResults([])
  }

  useEffect(() => {
    loadTree()
  }, [loadTree])

  return (
    <div className="h-full flex flex-col lg:flex-row gap-8 overflow-hidden">
      
      {/* 1. 物理目录侧边栏 */}
      <aside 
        className={`w-full lg:w-80 flex-col shrink-0 transition-all duration-500 ${
          isMobile && currentDoc ? 'hidden' : 'flex'
        }`}
      >
        {/* 搜索槽位 */}
        <div className="p-6 neo-convex rounded-[2.5rem] mb-6">
          <div className="relative neo-concave rounded-2xl p-1 flex items-center group transition-all focus-within:ring-2 ring-primary/20">
            <div className="pl-4 text-foreground/30"><Search size={16} /></div>
            <input 
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              placeholder="语义检索文档..."
              onKeyUp={(e) => e.key === 'Enter' && handleSearch()}
              className="w-full bg-transparent border-none focus:outline-none px-3 py-3 text-xs placeholder:text-foreground/20"
            />
            <button 
              onClick={handleSearch}
              disabled={searchLoading}
              className="p-2 mr-1 rounded-xl neo-convex text-primary active:scale-90 transition-all disabled:opacity-50"
            >
              {!searchLoading ? <ArrowRight size={16} /> : <Loader2 size={16} className="animate-spin" />}
            </button>
          </div>
        </div>

        {/* 目录内容区 */}
        <div className="flex-1 neo-concave rounded-[2.5rem] p-4 overflow-y-auto overflow-x-hidden space-y-2 no-scrollbar">
          
          {/* 搜索结果模式 */}
          {searchResults.length > 0 ? (
            <>
              <div className="px-4 py-2 flex items-center justify-between">
                <span className="text-[10px] font-black tracking-widest text-primary opacity-60">SEARCH HITS</span>
                <button onClick={clearSearch} className="text-[10px] font-bold text-foreground/40 hover:text-primary transition-colors">CLEAR</button>
              </div>
              {searchResults.map((hit) => (
                <div 
                  key={hit.path}
                  onClick={() => loadDoc(hit.path)}
                  className="p-4 rounded-2xl hover:bg-foreground/5 cursor-pointer transition-all border border-transparent hover:border-white/10 group"
                >
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 neo-convex rounded-lg flex items-center justify-center text-primary group-hover:scale-110 transition-transform">
                      <FileText size={16} />
                    </div>
                    <div className="flex-1 overflow-hidden">
                      <p className="text-xs font-bold truncate">{hit.name}</p>
                      <p className="text-[10px] text-foreground/40 truncate mt-1">{hit.content}</p>
                    </div>
                  </div>
                </div>
              ))}
            </>
          ) : (
            /* 目录树模式 */
            <>
              {treeLoading ? (
                <div className="p-4 space-y-4">
                  {[...Array(5)].map((_, i) => (
                    <div key={i} className="h-10 w-full neo-convex rounded-xl animate-pulse opacity-50"></div>
                  ))}
                </div>
              ) : (
                <div className="space-y-1">
                  {treeData.map((item) => (
                    <TreeItem 
                      key={item.path} 
                      item={item} 
                      onSelect={loadDoc}
                      currentPath={currentDoc?.path}
                    />
                  ))}
                </div>
              )}
            </>
          )}
        </div>
      </aside>

      {/* 2. 文档展示核心 */}
      <main 
        className={`flex-1 flex-col min-w-0 transition-all duration-500 ${
          isMobile && !currentDoc ? 'hidden' : 'flex'
        }`}
      >
        <AnimatePresence mode="wait">
          {!currentDoc ? (
            /* 未选择状态 */
            <motion.div 
              key="empty"
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.9 }}
              className="flex-1 flex items-center justify-center"
            >
              <div className="neo-convex p-16 rounded-[4rem] text-center space-y-6 max-w-sm">
                <div className="w-24 h-24 neo-concave rounded-[2rem] mx-auto flex items-center justify-center text-foreground/10">
                  <Library size={48} />
                </div>
                <div>
                  <h3 className="text-xl font-black tracking-tight text-foreground/60">知识库中心</h3>
                  <p className="text-sm text-foreground/30 mt-2 leading-relaxed">请从左侧轨道中选择一个文档进行深度阅读</p>
                </div>
              </div>
            </motion.div>
          ) : (
            /* 文档内容状态 */
            <motion.div 
              key="content"
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              className="flex-1 flex flex-col neo-convex rounded-[3rem] overflow-hidden"
            >
              {/* 工具栏 */}
              <header className="h-16 flex items-center px-8 border-b border-white/5 bg-white/5 backdrop-blur-md">
                {isMobile && (
                  <button 
                    onClick={() => setCurrentDoc(null)}
                    className="mr-4 w-8 h-8 neo-convex rounded-full flex items-center justify-center text-foreground/60 active:scale-90"
                  >
                    <ChevronLeft size={18} />
                  </button>
                )}
                <span className="text-[10px] font-black tracking-[0.2em] text-foreground/30 truncate flex-1">
                  {currentDoc.path}
                </span>
                <div className="flex items-center gap-2">
                  <button className="w-8 h-8 neo-convex rounded-full flex items-center justify-center text-foreground/40 hover:text-primary transition-colors">
                    <Share2 size={14} />
                  </button>
                  <button onClick={() => setCurrentDoc(null)} className="w-8 h-8 neo-convex rounded-full flex items-center justify-center text-red-500/40 hover:text-red-500 transition-colors">
                    <X size={14} />
                  </button>
                </div>
              </header>

              {/* Markdown 内容区 */}
              <div className="flex-1 overflow-y-auto p-12 scroll-smooth no-scrollbar">
                {contentLoading ? (
                  <div className="space-y-6">
                    <div className="h-12 w-2/3 neo-concave rounded-2xl animate-pulse"></div>
                    <div className="space-y-3">
                      {[...Array(10)].map((_, i) => (
                        <div key={i} className={`h-4 neo-concave rounded-full animate-pulse opacity-40 ${i % 3 === 0 ? 'w-full' : 'w-5/6'}`}></div>
                      ))}
                    </div>
                  </div>
                ) : (
                  <article 
                    ref={markdownRef}
                    className="markdown-rendered prose prose-slate dark:prose-invert max-w-none"
                    dangerouslySetInnerHTML={{ __html: renderMarkdown(currentDoc.content) }}
                  />
                )}
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </main>

    </div>
  )
}

export default DocViewer
