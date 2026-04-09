<template>
  <div class="max-w-4xl mx-auto space-y-10">
    
    <!-- 1. 顶部输入保险箱 (Deep Inset Slot) -->
    <section class="neo-convex p-8 rounded-[3rem] space-y-6">
      <div class="flex items-center justify-between px-4">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 neo-concave rounded-2xl flex items-center justify-center text-primary">
            <Fingerprint :size="20" />
          </div>
          <h2 class="font-black tracking-tight text-lg">安全碎片记录</h2>
        </div>
        <!-- 物理状态灯 -->
        <div class="flex items-center gap-2 px-4 py-2 neo-concave rounded-full">
          <div class="w-2 h-2 rounded-full bg-green-500 shadow-[0_0_8px_rgba(34,197,94,0.6)] animate-pulse"></div>
          <span class="text-[10px] font-bold text-foreground/40 uppercase tracking-widest">AES-GCM 加密已就绪</span>
        </div>
      </div>

      <div class="neo-concave rounded-[2rem] p-4 group transition-all focus-within:ring-2 ring-primary/20">
        <textarea
          v-model="newNote"
          placeholder="在此处记录任何灵感或代码片段...（客户端实时加密）"
          class="w-full bg-transparent border-none focus:outline-none p-4 text-sm leading-relaxed min-h-[120px] resize-none placeholder:text-foreground/20"
        ></textarea>
        
        <div class="flex items-center gap-3 mt-4 border-t border-foreground/5 pt-4">
          <div class="flex-1 flex gap-2">
            <button 
              v-for="cat in categories" :key="cat"
              @click="selectedCategory = cat"
              class="px-4 py-2 rounded-xl text-[10px] font-black tracking-widest transition-all"
              :class="selectedCategory === cat ? 'neo-concave text-primary' : 'neo-convex text-foreground/40'"
            >
              {{ cat }}
            </button>
          </div>
          <button 
            @click="saveNote"
            :disabled="!newNote.trim() || saving"
            class="w-12 h-12 flex items-center justify-center rounded-2xl transition-all"
            :class="newNote.trim() ? 'neo-convex text-primary active:scale-90' : 'text-foreground/10'"
          >
            <Plus :size="24" />
          </button>
        </div>
      </div>
    </section>

    <!-- 2. 笔记矩阵 (Physical Card Matrix) -->
    <section class="space-y-6">
      <div class="flex items-center justify-between px-4">
        <span class="text-[10px] font-black tracking-[0.3em] text-foreground/30 uppercase">Recent Fragments</span>
        <div class="flex gap-2">
          <button class="w-8 h-8 neo-convex rounded-full flex items-center justify-center text-foreground/40"><Search :size="14" /></button>
          <button class="w-8 h-8 neo-convex rounded-full flex items-center justify-center text-foreground/40"><Filter :size="14" /></button>
        </div>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-8">
        <div 
          v-for="note in notes" :key="note.id"
          class="neo-convex p-6 rounded-[2.5rem] group relative overflow-hidden transition-all hover:scale-[1.02]"
          v-motion-slide-visible-bottom
        >
          <!-- 卡片装饰 -->
          <div class="absolute top-0 right-0 p-4 opacity-5 group-hover:opacity-10 transition-opacity">
            <Quote :size="64" />
          </div>

          <div class="flex items-center gap-3 mb-4">
            <div class="px-3 py-1 neo-concave rounded-full text-[9px] font-bold text-primary uppercase tracking-widest">
              {{ note.category }}
            </div>
            <span class="text-[9px] font-bold text-foreground/20">{{ note.createdAt }}</span>
          </div>

          <p class="text-sm leading-relaxed text-foreground/70 mb-6 line-clamp-4 font-medium">
            {{ note.content }}
          </p>

          <div class="flex items-center justify-between border-t border-foreground/5 pt-4">
            <div class="flex gap-2">
              <button class="p-2 neo-convex rounded-xl text-foreground/30 hover:text-primary transition-colors"><Copy :size="14" /></button>
              <button class="p-2 neo-convex rounded-xl text-foreground/30 hover:text-primary transition-colors"><ExternalLink :size="14" /></button>
            </div>
            <button @click="deleteNote(note.id)" class="p-2 neo-convex rounded-xl text-red-500/30 hover:text-red-500 transition-colors">
              <Trash2 :size="14" />
            </button>
          </div>
        </div>
      </div>
    </section>

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { 
  Fingerprint, Plus, Trash2, Copy, ExternalLink, 
  Quote, Search, Filter 
} from 'lucide-vue-next'
import { noteAPI } from '@/api'
import { ElMessage } from 'element-plus'

const newNote = ref('')
const selectedCategory = ref('UNCATEGORIZED')
const categories = ['TECH', 'IDEA', 'CODE', 'MEETING']
const notes = ref([])
const saving = ref(false)

const loadNotes = async () => {
  try {
    const res = await noteAPI.getAll()
    notes.value = res || []
  } catch (e) {
    console.error(e)
  }
}

const saveNote = async () => {
  if (!newNote.value.trim()) return
  saving.value = true
  try {
    await noteAPI.createNote({
      content: newNote.value,
      category: selectedCategory.value
    })
    newNote.value = ''
    loadNotes()
    ElMessage.success('碎片已安全加密存储')
  } catch (e) {
    ElMessage.error('存储失败')
  } finally {
    saving.value = false
  }
}

const deleteNote = async (id) => {
  try {
    await noteAPI.deleteNote(id)
    loadNotes()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

onMounted(loadNotes)
</script>

<style scoped>
@reference "@/styles/index.css";
</style>
