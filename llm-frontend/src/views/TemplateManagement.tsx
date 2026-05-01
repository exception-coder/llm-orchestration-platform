import React, { useState, useEffect, useMemo, useCallback } from 'react'
import { 
  Plus, Search, Edit, Check, X, 
  Trash2, BoxSelect, Info, Wand2, Tag 
} from 'lucide-react'
import { motion, AnimatePresence } from 'framer-motion'
import { templateAPI } from '@/api'
import { useResponsive } from '@/hooks/useResponsive'

const TemplateManagement: React.FC = () => {
  const { isMobile } = useResponsive()

  const [templates, setTemplates] = useState<any[]>([])
  const [selectedTemplate, setSelectedTemplate] = useState<any>(null)
  const [searchText, setSearchText] = useState('')
  const [isEditing, setIsEditing] = useState(false)
  const [isCreating, setIsCreating] = useState(false)
  const [saving, setSaving] = useState(false)

  const [editForm, setEditForm] = useState({
    templateName: '',
    category: '',
    description: '',
    templateContent: '',
    variableExamples: ''
  })

  const loadTemplates = useCallback(async () => {
    try {
      const res: any = await templateAPI.getAll()
      setTemplates(res || [])
    } catch (error) {
      console.error('Failed to load templates:', error)
    }
  }, [])

  useEffect(() => {
    loadTemplates()
  }, [loadTemplates])

  const filteredTemplates = useMemo(() => {
    if (!searchText) return templates
    return templates.filter(t =>
      t.templateName.toLowerCase().includes(searchText.toLowerCase())
    )
  }, [templates, searchText])

  const handleSelectTemplate = (t: any) => {
    setSelectedTemplate(t)
    setIsEditing(false)
    setIsCreating(false)
    setEditForm({ ...t })
  }

  const handleCreate = () => {
    setSelectedTemplate(null)
    setIsEditing(true)
    setIsCreating(true)
    setEditForm({
      templateName: '',
      category: 'content',
      description: '',
      templateContent: '',
      variableExamples: ''
    })
  }

  const handleSave = async () => {
    if (!editForm.templateName || !editForm.templateContent) return

    if (editForm.variableExamples) {
      try {
        JSON.parse(editForm.variableExamples)
      } catch {
        // TODO: Toast error
        return
      }
    }

    setSaving(true)
    try {
      await templateAPI.save(editForm)
      await loadTemplates()
      setIsEditing(false)
      setIsCreating(false)
      // Update selected
      const saved = templates.find(t => t.templateName === editForm.templateName)
      if (saved) setSelectedTemplate(saved)
    } catch (error) {
      console.error('Save failed', error)
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (t: any) => {
    if (window.confirm(`确定要删除模板 "${t.templateName}" 吗？`)) {
      try {
        await templateAPI.delete(t.templateName)
        await loadTemplates()
        if (selectedTemplate?.templateName === t.templateName) {
          setSelectedTemplate(null)
        }
      } catch (error) {
        console.error('Delete failed', error)
      }
    }
  }

  const formatJSON = () => {
    try {
      const parsed = JSON.parse(editForm.variableExamples)
      setEditForm({ ...editForm, variableExamples: JSON.stringify(parsed, null, 2) })
    } catch {
      // TODO: Toast error
    }
  }

  return (
    <div className="max-w-7xl mx-auto flex flex-col lg:flex-row gap-8 h-full overflow-hidden">
      
      {/* 左侧：模板列表轨道 */}
      <aside className={`w-full lg:w-96 flex flex-col shrink-0 transition-all ${isMobile && (selectedTemplate || isCreating) ? 'hidden' : 'flex'}`}>
        <div className="flex items-center justify-between px-4 mb-6">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 neo-convex rounded-xl flex items-center justify-center text-primary">
              <Tag size={20} />
            </div>
            <h2 className="font-black tracking-tight text-lg">模板库</h2>
          </div>
          <button 
            onClick={handleCreate}
            className="w-10 h-10 neo-convex rounded-xl flex items-center justify-center text-primary active:scale-90 transition-all"
          >
            <Plus size={20} />
          </button>
        </div>

        <div className="px-4 mb-6">
          <div className="neo-concave rounded-2xl flex items-center gap-3 px-4 py-3 group focus-within:ring-2 ring-primary/20 transition-all">
            <Search size={16} className="text-foreground/20" />
            <input 
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
              placeholder="搜索模板..."
              className="bg-transparent border-none focus:outline-none text-sm w-full font-bold placeholder:text-foreground/10"
            />
          </div>
        </div>

        <div className="flex-1 neo-concave rounded-[3rem] p-4 overflow-y-auto no-scrollbar space-y-3">
          {filteredTemplates.map((t) => (
            <div 
              key={t.templateName}
              onClick={() => handleSelectTemplate(t)}
              className={`p-5 rounded-[2rem] cursor-pointer transition-all flex items-center justify-between group ${
                selectedTemplate?.templateName === t.templateName ? 'neo-convex text-primary z-10' : 'hover:bg-foreground/5 text-foreground/60'
              }`}
            >
              <div className="flex-1 min-w-0 pr-4">
                <p className="text-sm font-black truncate">{t.templateName}</p>
                <div className="flex items-center gap-2 mt-1">
                  <span className="text-[9px] font-bold uppercase tracking-widest opacity-40">{t.category}</span>
                </div>
              </div>
              <button 
                onClick={(e) => { e.stopPropagation(); handleDelete(t); }}
                className="p-2 rounded-xl neo-convex text-red-500/20 hover:text-red-500 opacity-0 group-hover:opacity-100 transition-all"
              >
                <Trash2 size={14} />
              </button>
            </div>
          ))}
          {filteredTemplates.length === 0 && (
            <div className="py-20 text-center opacity-20">
              <BoxSelect size={48} className="mx-auto mb-4" />
              <p className="text-xs font-black tracking-widest uppercase">No Templates</p>
            </div>
          )}
        </div>
      </aside>

      {/* 右侧：编辑/详情面板 */}
      <main className={`flex-1 flex flex-col min-w-0 ${isMobile && !selectedTemplate && !isCreating ? 'hidden' : 'flex'}`}>
        <AnimatePresence mode="wait">
          {!selectedTemplate && !isCreating ? (
            <motion.div 
              key="empty" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
              className="flex-1 flex items-center justify-center"
            >
              <div className="neo-convex p-16 rounded-[4rem] text-center space-y-6">
                <div className="w-20 h-20 neo-concave rounded-[2rem] mx-auto flex items-center justify-center text-foreground/10">
                  <Edit size={32} />
                </div>
                <p className="text-sm font-bold text-foreground/30 uppercase tracking-[0.3em]">SELECT A TEMPLATE</p>
              </div>
            </motion.div>
          ) : (
            <motion.div 
              key="editor" initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -20 }}
              className="flex-1 flex flex-col neo-convex rounded-[4rem] overflow-hidden border border-white/40"
            >
              <header className="h-20 flex items-center px-10 border-b border-foreground/5 bg-white/5 backdrop-blur-xl">
                {isMobile && (
                  <button onClick={() => setSelectedTemplate(null)} className="mr-4 w-8 h-8 neo-convex rounded-full flex items-center justify-center text-foreground/40 active:scale-90">
                    <X size={16} />
                  </button>
                )}
                <h3 className="text-lg font-black tracking-tight flex-1 truncate">
                  {isCreating ? 'NEW TEMPLATE' : (isEditing ? 'EDITING...' : 'TEMPLATE VIEW')}
                </h3>
                <div className="flex items-center gap-3">
                  {!isEditing ? (
                    <button onClick={() => setIsEditing(true)} className="px-6 py-2.5 neo-convex rounded-2xl text-primary text-[11px] font-black tracking-widest active:scale-95 transition-all">EDIT</button>
                  ) : (
                    <>
                      <button onClick={() => { setIsEditing(false); if (isCreating) setSelectedTemplate(null); }} className="px-6 py-2.5 neo-convex rounded-2xl text-foreground/40 text-[11px] font-black tracking-widest active:scale-95 transition-all">CANCEL</button>
                      <button onClick={handleSave} disabled={saving} className="px-6 py-2.5 neo-convex rounded-2xl text-primary text-[11px] font-black tracking-widest active:scale-95 transition-all shadow-lg disabled:opacity-50">
                        {saving ? 'SAVING...' : 'SAVE'}
                      </button>
                    </>
                  )}
                </div>
              </header>

              <div className="flex-1 overflow-y-auto p-10 space-y-8 no-scrollbar">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                  <div className="space-y-2">
                    <label className="text-[10px] font-black tracking-widest text-foreground/30 uppercase px-4">Identifier</label>
                    <div className="neo-concave rounded-[1.5rem] p-1">
                      <input 
                        value={editForm.templateName}
                        onChange={(e) => setEditForm({...editForm, templateName: e.target.value})}
                        disabled={!isCreating}
                        placeholder="e.g. content-optimization"
                        className="w-full bg-transparent border-none focus:outline-none px-5 py-4 text-sm font-bold disabled:opacity-50"
                      />
                    </div>
                  </div>
                  <div className="space-y-2">
                    <label className="text-[10px] font-black tracking-widest text-foreground/30 uppercase px-4">Category</label>
                    <div className="neo-concave rounded-[1.5rem] p-1">
                      <select 
                        value={editForm.category}
                        onChange={(e) => setEditForm({...editForm, category: e.target.value})}
                        disabled={!isEditing}
                        className="w-full bg-transparent border-none focus:outline-none px-5 py-4 text-sm font-bold appearance-none cursor-pointer disabled:opacity-50"
                      >
                        <option value="content">Content Creation</option>
                        <option value="chat">Interactive Chat</option>
                        <option value="development">Dev Assistance</option>
                        <option value="analysis">Data Analysis</option>
                        <option value="translation">Translation</option>
                      </select>
                    </div>
                  </div>
                </div>

                <div className="space-y-2">
                  <label className="text-[10px] font-black tracking-widest text-foreground/30 uppercase px-4">Description</label>
                  <div className="neo-concave rounded-[1.5rem] p-1">
                    <textarea 
                      value={editForm.description}
                      onChange={(e) => setEditForm({...editForm, description: e.target.value})}
                      disabled={!isEditing}
                      rows={2}
                      placeholder="Brief overview of this template's purpose..."
                      className="w-full bg-transparent border-none focus:outline-none px-5 py-4 text-sm resize-none disabled:opacity-50"
                    />
                  </div>
                </div>

                <div className="space-y-4">
                  <div className="flex items-center justify-between px-4">
                    <label className="text-[10px] font-black tracking-widest text-foreground/30 uppercase">Template Payload</label>
                    <div className="flex items-center gap-2 text-[9px] font-bold text-primary/40 uppercase tracking-widest">
                      <Info size={10} />
                      Use {'{variable}'} as placeholders
                    </div>
                  </div>
                  <div className="neo-concave rounded-[2.5rem] p-1">
                    <textarea 
                      value={editForm.templateContent}
                      onChange={(e) => setEditForm({...editForm, templateContent: e.target.value})}
                      disabled={!isEditing}
                      rows={12}
                      className="w-full bg-transparent border-none focus:outline-none px-6 py-6 text-sm font-mono leading-relaxed resize-none disabled:opacity-50"
                    />
                  </div>
                </div>

                <div className="space-y-4">
                  <div className="flex items-center justify-between px-4">
                    <label className="text-[10px] font-black tracking-widest text-foreground/30 uppercase">Variable Examples (JSON)</label>
                    {isEditing && (
                      <button onClick={formatJSON} className="flex items-center gap-2 text-[9px] font-black text-primary hover:opacity-80 transition-all uppercase tracking-widest">
                        <Wand2 size={12} />
                        Format JSON
                      </button>
                    )}
                  </div>
                  <div className="neo-concave rounded-[2rem] p-1">
                    <textarea 
                      value={editForm.variableExamples}
                      onChange={(e) => setEditForm({...editForm, variableExamples: e.target.value})}
                      disabled={!isEditing}
                      rows={8}
                      placeholder='{"name": "John Doe", "role": "Expert"}'
                      className="w-full bg-transparent border-none focus:outline-none px-6 py-6 text-sm font-mono leading-relaxed resize-none disabled:opacity-50"
                    />
                  </div>
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </main>

    </div>
  )
}

export default TemplateManagement
