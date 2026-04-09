import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'

marked.setOptions({
  highlight(code, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(code, { language: lang }).value
      } catch {
        // fall through to auto
      }
    }
    return hljs.highlightAuto(code).value
  },
  breaks: true,
  gfm: true
})

export function useMarkdown() {
  const renderMarkdown = (content) => {
    if (!content) return ''
    try {
      return marked.parse(content)
    } catch (error) {
      console.error('Markdown render error:', error)
      return content
    }
  }

  return { renderMarkdown }
}
