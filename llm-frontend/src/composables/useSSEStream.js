/**
 * SSE 流式请求 composable
 * 统一封装 POST + SSE 解析逻辑，替代各视图中重复的 fetch + ReadableStream 代码
 */
export function useSSEStream() {
  const apiBaseURL = import.meta.env.VITE_API_BASE_URL || '/api/v1'

  /**
   * 发送 POST 请求并逐行解析 SSE 流
   * @param {string} url   API 路径，例如 '/chat/stream'
   * @param {object} body  请求体
   * @param {object} callbacks
   * @param {function} [callbacks.onContent]    收到文本片段时回调
   * @param {function} [callbacks.onTokenUsage] 收到 token 统计时回调
   */
  async function fetchSSE(url, body, { onContent, onTokenUsage } = {}) {
    const response = await fetch(`${apiBaseURL}${url}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
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
  }

  return { fetchSSE }
}
