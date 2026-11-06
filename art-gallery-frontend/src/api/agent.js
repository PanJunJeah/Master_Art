import api from './index'

/**
 * 智能策展 Agent API
 * - chatWithCurator：非流式（JSON，含草稿卡片等结构化产物）
 * - chatWithCuratorStream：SSE 流式（token 事件 + done 事件）
 */

export function chatWithCurator(payload) {
  return api.post('/api/agent/chat', payload)
}

/** 清空当前用户对话记忆（新对话） */
export function clearConversation(payload) {
  return api.post('/api/agent/chat/clear', payload)
}

/**
 * SSE 流式对话
 * @param payload {{ message: string, userId: number|null }}
 * @param callbacks {{ onToken, onDone, onError }}
 */
export function chatWithCuratorStream(payload, callbacks) {
  const { onToken, onDone, onError } = callbacks || {}
  return fetch('http://localhost:8080/api/agent/chat/stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  }).then(async (resp) => {
    if (!resp.ok || !resp.body) {
      const err = await resp.text().catch(() => '')
      throw new Error('流式请求失败：' + (resp.status || '网络错误') + ' ' + err)
    }
    const reader = resp.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''

    const handleEvent = (raw) => {
      let event = 'message'
      let data = ''
      for (const line of raw.split('\n')) {
        if (line.startsWith('event:')) event = line.slice(6).trim()
        else if (line.startsWith('data:')) data += line.slice(5).trim()
      }
      if (!data) return
      let payloadData = null
      try {
        payloadData = JSON.parse(data)
      } catch (e) {
        payloadData = data
      }
      if (event === 'token') onToken && onToken(payloadData)
      else if (event === 'done') onDone && onDone(payloadData)
      else if (event === 'error') onError && onError(payloadData && payloadData.message ? payloadData.message : payloadData)
    }

    for (;;) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      let idx
      while ((idx = buffer.indexOf('\n\n')) >= 0) {
        const rawEvent = buffer.slice(0, idx)
        buffer = buffer.slice(idx + 2)
        if (rawEvent.trim()) handleEvent(rawEvent)
      }
    }
    if (buffer.trim()) handleEvent(buffer)
  })
}

export default { chatWithCurator, chatWithCuratorStream, clearConversation }
