import React, { useState, useEffect, useCallback } from 'react'
import { 
  Fingerprint, Plus, Trash2, Copy, ExternalLink, 
  Quote, Search, Filter 
} from 'lucide-react'
import { motion, AnimatePresence } from 'framer-motion'
import { noteAPI } from '@/api'

const NoteCapture: React.FC = () => {
  const [newNote, setNewNote] = useState('')
  const [selectedCategory, setSelectedCategory] = useState('UNCATEGORIZED')
  const categories = ['TECH', 'IDEA', 'CODE', 'MEETING']
  const [notes, setNotes] = useState<any[]>([])
  const [saving, setSaving] = useState(false)

  const loadNotes = useCallback(async () => {
    try {
      const res: any = await noteAPI.getAll()
      setNotes(res || [])
    } catch (e) {
      console.error(e)
    }
  }, [])

  const saveNote = async () => {
    if (!newNote.trim()) return
    setSaving(true)
    try {
      await noteAPI.capture({
        content: newNote,
        category: selectedCategory
      })
      setNewNote('')
      loadNotes()
      // TODO: Success toast
    } catch (e) {
      console.error('Save failed', e)
    } finally {
      setSaving(false)
    }
  }

  const deleteNote = async (id: string | number) => {
    try {
      await noteAPI.delete(id)
      loadNotes()
    } catch (e) {
      console.error('Delete failed', e)
    }
  }

  const handleCopy = (content: string) => {
    navigator.clipboard.writeText(content)
    // TODO: Success toast
  }

  useEffect(() => {
    loadNotes()
  }, [loadNotes])

  return (
    <div className="max-w-4xl mx-auto space-y-10">
      
      {/* 1. 顶部输入保险箱 (Deep Inset Slot) */}
      <section className="neo-convex p-8 rounded-[3rem] space-y-6">
        <div className="flex items-center justify-between px-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 neo-concave rounded-2xl flex items-center justify-center text-primary">
              <Fingerprint size={20} />
            </div>
            <h2 className="font-black tracking-tight text-lg">安全碎片记录</h2>
          </div>
          {/* 物理状态灯 */}
          <div className="flex items-center gap-2 px-4 py-2 neo-concave rounded-full">
            <div className="w-2 h-2 rounded-full bg-green-500 shadow-[0_0_8px_rgba(34,197,94,0.6)] animate-pulse"></div>
            <span className="text-[10px] font-bold text-foreground/40 uppercase tracking-widest">AES-GCM 加密已就绪</span>
          </div>
        </div>

        <div className="neo-concave rounded-[2rem] p-4 group transition-all focus-within:ring-2 ring-primary/20">
          <textarea
            value={newNote}
            onChange={(e) => setNewNote(e.target.value)}
            placeholder="在此处记录任何灵感或代码片段...（客户端实时加密）"
            className="w-full bg-transparent border-none focus:outline-none p-4 text-sm leading-relaxed min-h-[120px] resize-none placeholder:text-foreground/20"
          ></textarea>
          
          <div className="flex items-center gap-3 mt-4 border-t border-foreground/5 pt-4">
            <div className="flex-1 flex gap-2 overflow-x-auto no-scrollbar pb-2 md:pb-0">
              {categories.map((cat) => (
                <button 
                  key={cat}
                  onClick={() => setSelectedCategory(cat)}
                  className={`px-4 py-2 rounded-xl text-[10px] font-black tracking-widest transition-all whitespace-nowrap ${
                    selectedCategory === cat ? 'neo-concave text-primary' : 'neo-convex text-foreground/40'
                  }`}
                >
                  {cat}
                </button>
              ))}
            </div>
            <button 
              onClick={saveNote}
              disabled={!newNote.trim() || saving}
              className={`w-12 h-12 flex items-center justify-center rounded-2xl transition-all ${
                newNote.trim() && !saving ? 'neo-convex text-primary active:scale-90' : 'text-foreground/10'
              }`}
            >
              <Plus size={24} />
            </button>
          </div>
        </div>
      </section>

      {/* 2. 笔记矩阵 (Physical Card Matrix) */}
      <section className="space-y-6">
        <div className="flex items-center justify-between px-4">
          <span className="text-[10px] font-black tracking-[0.3em] text-foreground/30 uppercase">Recent Fragments</span>
          <div className="flex gap-2">
            <button className="w-8 h-8 neo-convex rounded-full flex items-center justify-center text-foreground/40"><Search size={14} /></button>
            <button className="w-8 h-8 neo-convex rounded-full flex items-center justify-center text-foreground/40"><Filter size={14} /></button>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          <AnimatePresence>
            {notes.map((note) => (
              <motion.div 
                key={note.id}
                layout
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, scale: 0.95 }}
                className="neo-convex p-6 rounded-[2.5rem] group relative overflow-hidden transition-all hover:scale-[1.02]"
              >
                {/* 卡片装饰 */}
                <div className="absolute top-0 right-0 p-4 opacity-5 group-hover:opacity-10 transition-opacity pointer-events-none">
                  <Quote size={64} />
                </div>

                <div className="flex items-center gap-3 mb-4">
                  <div className="px-3 py-1 neo-concave rounded-full text-[9px] font-bold text-primary uppercase tracking-widest">
                    {note.category}
                  </div>
                  <span className="text-[9px] font-bold text-foreground/20">{note.createdAt}</span>
                </div>

                <p className="text-sm leading-relaxed text-foreground/70 mb-6 line-clamp-4 font-medium">
                  {note.content}
                </p>

                <div className="flex items-center justify-between border-t border-foreground/5 pt-4">
                  <div className="flex gap-2">
                    <button 
                      onClick={() => handleCopy(note.content)}
                      className="p-2 neo-convex rounded-xl text-foreground/30 hover:text-primary transition-colors"
                    >
                      <Copy size={14} />
                    </button>
                    <button className="p-2 neo-convex rounded-xl text-foreground/30 hover:text-primary transition-colors"><ExternalLink size={14} /></button>
                  </div>
                  <button 
                    onClick={() => deleteNote(note.id)}
                    className="p-2 neo-convex rounded-xl text-red-500/30 hover:text-red-500 transition-colors"
                  >
                    <Trash2 size={14} />
                  </button>
                </div>
              </motion.div>
            ))}
          </AnimatePresence>
        </div>
      </section>

    </div>
  )
}

export default NoteCapture
