import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'
import { useCallback } from 'react'

// 配置 marked
const setupMarked = () => {
  const renderer = new marked.Renderer()
  
  marked.setOptions({
    highlight(code: string, lang: string) {
      if (lang && hljs.getLanguage(lang)) {
        try {
          return hljs.highlight(code, { language: lang }).value
        } catch {
          // fall through
        }
      }
      return hljs.highlightAuto(code).value
    },
    breaks: true,
    gfm: true
  } as any)
  
  return renderer
}

setupMarked()

export function useMarkdown() {
  const renderMarkdown = useCallback((content: string) => {
    if (!content) return ''
    try {
      return marked.parse(content)
    } catch (error) {
      console.error('Markdown render error:', error)
      return content
    }
  }, [])

  return { renderMarkdown }
}
