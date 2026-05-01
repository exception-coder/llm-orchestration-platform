import { useCallback } from 'react'

interface SSECallbacks {
  onContent?: (content: string) => void
  onTokenUsage?: (usage: any) => void
}

/**
 * SSE 流式请求 Hook
 * 统一封装 POST + SSE 解析逻辑
 */
export function useSSEStream() {
  const apiBaseURL = (import.meta.env.VITE_API_BASE_URL as string) || '/api/v1'

  /**
   * 发送 POST 请求并逐行解析 SSE 流
   * @param url API 路径，例如 '/chat/stream'
   * @param body 请求体
   * @param callbacks 回调函数
   */
  const fetchSSE = useCallback(async (
    url: string, 
    body: any, 
    { onContent, onTokenUsage }: SSECallbacks = {}
  ) => {
    const response = await fetch(`${apiBaseURL}${url}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    if (!response.body) {
      throw new Error('Response body is null')
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (!line.startsWith('data:')) continue
        const data = line.slice(5).trim()
        if (data === '[DONE]' || !data) continue

        try {
          const parsed = JSON.parse(data)
          if (parsed.content) onContent?.(parsed.content)
          if (parsed.tokenUsage) onTokenUsage?.(parsed.tokenUsage)
        } catch (e) {
          console.error('SSE parse error:', e, 'Data:', data)
        }
      }
    }
  }, [apiBaseURL])

  return { fetchSSE }
}
