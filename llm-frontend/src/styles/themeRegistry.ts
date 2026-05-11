import {
  Boxes,
  FileText,
  LayoutGrid,
  Moon,
  Sparkles,
  Sun,
  Terminal,
  type LucideIcon
} from 'lucide-react'

export interface AppTheme {
  id: string
  label: string
  icon: LucideIcon
}

export const themes: AppTheme[] = [
  { id: '', label: '极致简约', icon: LayoutGrid },
  { id: 'ai-clean', label: 'AI 清透', icon: Sparkles },
  { id: 'paper', label: '报刊杂志', icon: FileText },
  { id: 'win95', label: '怀旧经典', icon: Terminal },
  { id: 'bauhaus', label: '几何主义', icon: Boxes },
  { id: 'aurora', label: '未来流光', icon: Sun },
  { id: 'dark', label: '暗黑专业', icon: Moon },
  { id: 'apple', label: '苹果毛玻璃', icon: Sun },
  { id: 'cyber', label: '赛博终端', icon: Terminal }
]

export const applyTheme = (theme: string) => {
  if (theme) {
    document.documentElement.setAttribute('data-theme', theme)
  } else {
    document.documentElement.removeAttribute('data-theme')
  }
}
