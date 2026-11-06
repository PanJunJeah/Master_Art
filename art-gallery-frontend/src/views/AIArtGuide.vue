<template>
  <div class="curator-page">
    <!-- ===== 顶部介绍区 ===== -->
    <section class="curator-hero">
      <div class="hero-inner">
        <div class="hero-badge">
          <span class="badge-dot"></span>
          MasterArt · AI Curator
        </div>
        <h1>AI 智能策展助手</h1>
        <p class="hero-sub">
          我是您的专属策展助手：检索艺术品与背景知识、查询展览排期与余票、生成预约草稿、登记新展订阅。
        </p>
        <div class="hero-capabilities">
          <span class="cap"><el-icon><Search /></el-icon> 作品检索</span>
          <span class="cap"><el-icon><Collection /></el-icon> 展览排期 · 余票</span>
          <span class="cap"><el-icon><DocumentChecked /></el-icon> 预约草稿</span>
          <span class="cap"><el-icon><Bell /></el-icon> 新展订阅</span>
        </div>
      </div>
      <div class="security-note">
        <el-icon class="lock-icon"><Lock /></el-icon>
        <span>
          安全提示：AI 只能生成<strong>预约草稿</strong>，没有权限直接扣款或锁座。
          真实预约需由您本人点击草稿卡片上的「确认预约」按钮完成。
        </span>
      </div>
    </section>

    <!-- ===== 对话区 ===== -->
    <section class="chat-card">
      <div class="chat-head">
        <div class="chat-title">
          <span class="curator-avatar">🖼️</span>
          <div>
            <b>策展助手</b>
            <small>正在为您服务 · 回答基于实时展馆数据</small>
          </div>
        </div>
        <div class="chat-head-right">
          <div class="chat-steps" v-if="lastSteps > 0">
            本轮工具调用 {{ lastSteps }} 步
          </div>
          <el-button text class="new-chat-btn" :disabled="streaming" @click="startNewChat">
            <el-icon><RefreshRight /></el-icon> 新对话
          </el-button>
        </div>
      </div>

      <div class="messages" ref="messagesContainer">
        <div v-for="m in messages" :key="m.id" :class="['msg-row', m.sender]">
          <div class="msg-avatar">
            <span v-if="m.sender === 'ai'">🖼️</span>
            <el-avatar v-else :size="36" :src="userAvatar" />
          </div>
          <div class="msg-content">
            <div class="msg-name">{{ m.sender === 'ai' ? 'AI 策展助手' : (userName || '您') }}</div>
            <div class="msg-text" v-html="renderMarkdown(m.text)"></div>

            <!-- 工具调用轨迹 -->
            <div v-if="m.toolCalls && m.toolCalls.length" class="tool-traces">
              <span v-for="(t, i) in m.toolCalls" :key="i" :class="['trace', t.success ? 'ok' : 'fail']">
                <el-icon><DataAnalysis v-if="t.success" /><Warning v-else /></el-icon>
                {{ toolLabel(t.name) }}
                <em>{{ t.message }}</em>
              </span>
            </div>

            <!-- 预约草稿卡片（人工确认闸门） -->
            <div v-if="m.drafts && m.drafts.length" class="draft-list">
              <div v-for="d in m.drafts" :key="d.draftId" :class="['draft-card', d.status.toLowerCase()]">
                <div class="draft-head">
                  <span class="draft-flag">🎫 预约草稿 · #{{ d.draftId }}</span>
                  <el-tag size="small" :type="statusType(d.status)" effect="light">{{ statusText(d.status) }}</el-tag>
                </div>
                <div class="draft-body">
                  <div class="draft-exhibition">{{ d.exhibitionName }}</div>
                  <div class="draft-meta">
                    <span><el-icon><Clock /></el-icon> {{ d.sessionTime }}</span>
                    <span><el-icon><Ticket /></el-icon> {{ d.ticketCount }} 张</span>
                    <span class="draft-price">¥ {{ d.totalPrice }}</span>
                  </div>
                  <div class="draft-expire">草稿有效期至 {{ formatExpire(d.expiresAt) }}，过期自动作废</div>
                </div>
                <div class="draft-actions" v-if="d.status === 'PENDING'">
                  <el-button type="primary" round :loading="confirmingId === d.draftId" @click="confirmDraftCard(d)">
                    确认预约
                  </el-button>
                  <el-button text @click="cancelDraftCard(d)">取消草稿</el-button>
                  <el-button text @click="goExhibition(d.exhibitionId)">查看展览 →</el-button>
                </div>
                <div class="draft-done success" v-else-if="d.status === 'CONFIRMED'">
                  <el-icon><CircleCheck /></el-icon> 已确认预约，票已为您保留
                </div>
                <div class="draft-done muted" v-else>
                  {{ statusText(d.status) === '已过期' ? '草稿已过期，可让助手重新生成' : '草稿已取消' }}
                </div>
              </div>
            </div>

            <!-- 订阅结果 -->
            <div v-if="m.subscriptions && m.subscriptions.length" class="sub-chips">
              <el-tag v-for="(s, i) in m.subscriptions" :key="i" type="success" effect="plain" round>
                <el-icon><Bell /></el-icon>&nbsp;{{ s }}
              </el-tag>
            </div>
          </div>
        </div>

        <!-- 流式输入中 -->
        <div v-if="streaming" class="msg-row ai">
          <div class="msg-avatar"><span>🖼️</span></div>
          <div class="msg-content">
            <div class="msg-name">AI 策展助手</div>
            <div class="typing-indicator"><span></span><span></span><span></span></div>
          </div>
        </div>
      </div>

      <div class="composer">
        <el-input
            v-model="userInput"
            type="textarea"
            :rows="2"
            resize="none"
            placeholder="试试：帮我找印象派的作品 · 周末有哪些展览还有余票 · 帮我预约《星空》主题展 2 张票"
            @keydown.enter.exact.prevent="sendMessage"
        />
        <div class="composer-row">
          <div class="quick-questions">
            <el-tag
                v-for="q in quickQuestions"
                :key="q"
                class="question-tag"
                effect="plain"
                @click="selectQuickQuestion(q)"
            >{{ q }}</el-tag>
          </div>
          <el-button type="primary" round :loading="streaming" :disabled="!userInput.trim()" @click="sendMessage">
            发送 <el-icon><Promotion /></el-icon>
          </el-button>
        </div>
      </div>
    </section>

    <!-- ===== 能力边界说明 ===== -->
    <section class="curator-foot">
      <h3>受控工具白名单 · 能力边界</h3>
      <div class="tool-grid">
        <div class="tool" v-for="t in whitelist" :key="t.name">
          <b>{{ t.name }}</b>
          <span :class="t.kind === '只读' ? 'kind read' : 'kind write'">{{ t.kind }}</span>
          <p>{{ t.desc }}</p>
        </div>
      </div>
      <p class="foot-note">
        预约流程：AI 生成草稿 → 您在前端确认 → 系统才完成真实预约与库存扣减（人工确认闸门 / 幂等键防重 / 单轮最多 6 步工具调用）。
      </p>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/index.js'
import { ElMessage } from 'element-plus'
import {
  Search, Collection, DocumentChecked, Bell, Lock, Clock, Ticket,
  DataAnalysis, Warning, CircleCheck, Promotion, RefreshRight
} from '@element-plus/icons-vue'
import { chatWithCurator, chatWithCuratorStream, clearConversation } from '@/api/agent.js'
import { confirmDraft, cancelDraft } from '@/api/reservation.js'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
const userAvatar = computed(() => userStore.currentUser?.avatar || defaultAvatar)

// 兼容 userStore 里 user 可能是 JSON 字符串的历史实现
const parsedUser = computed(() => {
  try {
    const u = userStore.currentUser
    if (!u) return null
    return typeof u === 'string' ? JSON.parse(u) : u
  } catch (e) {
    return null
  }
})
const currentUserId = computed(() => parsedUser.value?.id ?? parsedUser.value?.userId ?? null)
const userName = computed(() => parsedUser.value?.username ?? parsedUser.value?.realName ?? null)

let msgSeq = 1
const welcomeText = '您好，我是 **AI 智能策展助手** 🖼️\n\n我可以为您：\n- 检索艺术品与背景知识（如《星空》的创作故事）\n- 查询展览排期、场次与余票\n- 生成预约草稿（确认需由您本人在网页上完成）\n- 登记新展订阅提醒\n\n请问今天想了解什么？'
const messages = ref([{
  id: msgSeq++,
  sender: 'ai',
  text: welcomeText,
  toolCalls: [], drafts: [], subscriptions: []
}])
const userInput = ref('')
const streaming = ref(false)
const confirmingId = ref(null)
const lastSteps = ref(0)
const messagesContainer = ref(null)

const quickQuestions = [
  '帮我找印象派的作品',
  '最近有哪些展览？余票情况如何？',
  '给我讲讲《星空》的创作故事',
  '帮我预约周末场 2 张票',
  '订阅即将开展的新展'
]

const whitelist = [
  { name: 'searchArtworkKnowledge', kind: '只读', desc: 'RAG 检索艺术品背景知识，超时自动降级提示' },
  { name: 'searchArtworks', kind: '只读', desc: '按名称/流派/艺术家/年代多条件检索作品' },
  { name: 'queryExhibition', kind: '只读', desc: '查询展览排期、场次与余票' },
  { name: 'createReservationDraft', kind: '受限写', desc: '仅生成预约草稿（幂等键防重），不能扣款锁座' },
  { name: 'createSubscription', kind: '受限写', desc: '登记新展订阅提醒，幂等合并' }
]

const toolLabel = (name) => ({
  searchArtworkKnowledge: '知识检索',
  searchArtworks: '检索作品',
  queryExhibition: '查询展览',
  createReservationDraft: '生成预约草稿',
  createSubscription: '订阅新展'
}[name] || name)

const statusType = (s) => ({ PENDING: 'warning', CONFIRMED: 'success', EXPIRED: 'info', CANCELLED: 'danger' }[s] || 'info')
const statusText = (s) => ({ PENDING: '待确认', CONFIRMED: '已确认', EXPIRED: '已过期', CANCELLED: '已取消' }[s] || s)

const formatExpire = (v) => {
  if (!v) return '—'
  const d = new Date(v)
  if (isNaN(d)) return '—'
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

// 简易安全的 Markdown 渲染（先转义再渲染）
const renderMarkdown = (text) => {
  if (!text) return ''
  const esc = text
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  return esc
      .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
      .replace(/(^|\n)- (.+)/g, '$1<span class="md-li">• $2</span>')
      .replace(/(^|\n)(#{1,3}) (.+)/g, '$1<span class="md-h">$3</span>')
      .replace(/`([^`]+)`/g, '<code>$1</code>')
      .replace(/\n/g, '<br>')
}

const scrollToBottom = () => nextTick(() => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
})

watch(messages, scrollToBottom, { deep: true })

// 发送消息（优先 SSE 流式，失败自动降级为非流式）
const sendMessage = async () => {
  const text = userInput.value.trim()
  if (!text || streaming.value) return

  messages.value.push({ id: msgSeq++, sender: 'user', text, toolCalls: [], drafts: [], subscriptions: [] })
  userInput.value = ''
  streaming.value = true
  const aiMessage = { id: msgSeq++, sender: 'ai', text: '', toolCalls: [], drafts: [], subscriptions: [] }
  messages.value.push(aiMessage)

  const payload = { message: text, userId: currentUserId.value }
  let streamFailed = false

  try {
    await chatWithCuratorStream(payload, {
      onToken: (token) => {
        if (typeof token === 'string') aiMessage.text += token
        scrollToBottom()
      },
      onDone: (done) => applyDone(aiMessage, done),
      onError: (err) => {
        streamFailed = true
        aiMessage.text = typeof err === 'string' ? err : (err?.message || 'AI 服务暂时不可用，请稍后重试')
      }
    })
    if (streamFailed || !aiMessage.text) {
      const res = await chatWithCurator(payload)
      const data = res?.data || res
      applyDone(aiMessage, data)
    }
  } catch (e) {
    // SSE 失败 → 非流式兜底
    try {
      const res = await chatWithCurator(payload)
      const data = res?.data || res
      applyDone(aiMessage, data)
    } catch (e2) {
      aiMessage.text = 'AI 服务暂时不可用，请稍后重试。'
    }
  } finally {
    streaming.value = false
    scrollToBottom()
  }
}

const applyDone = (aiMessage, data) => {
  if (!data) return
  aiMessage.text = data.content || aiMessage.text
  aiMessage.toolCalls = data.toolCalls || []
  aiMessage.drafts = data.drafts || []
  aiMessage.subscriptions = data.subscriptions || []
  lastSteps.value = data.steps || 0
  scrollToBottom()
}

const selectQuickQuestion = (q) => {
  userInput.value = q
  sendMessage()
}

// ===== 人工确认闸门：前端直接调用真实预约接口 =====
const confirmDraftCard = async (draft) => {
  if (!currentUserId.value) {
    ElMessage.warning('请先登录后再确认预约')
    router.push('/login')
    return
  }
  confirmingId.value = draft.draftId
  try {
    const res = await confirmDraft(draft.draftId, currentUserId.value)
    const ok = res?.success !== false
    if (ok) {
      draft.status = 'CONFIRMED'
      ElMessage.success(res?.message || '预约成功！票已为您保留')
    } else {
      ElMessage.error(res?.message || '预约失败，请重试')
    }
  } catch (e) {
    const msg = typeof e === 'string' ? e : (e?.response?.data?.message || '预约失败，请重试')
    ElMessage.error(msg)
  } finally {
    confirmingId.value = null
  }
}

const cancelDraftCard = async (draft) => {
  try {
    const res = await cancelDraft(draft.draftId, currentUserId.value)
    if (res?.success !== false) {
      draft.status = 'CANCELLED'
      ElMessage.success(res?.message || '草稿已取消')
    } else {
      ElMessage.error(res?.message || '取消失败')
    }
  } catch (e) {
    ElMessage.error(typeof e === 'string' ? e : '取消失败，请重试')
  }
}

const goExhibition = (exhibitionId) => {
  if (exhibitionId) router.push(`/exhibitions/${exhibitionId}`)
}

// 新对话：清空服务端记忆 + 重置前端消息
const startNewChat = async () => {
  try {
    await clearConversation({ userId: currentUserId.value })
  } catch (e) {
    // 清空失败不阻塞前端重置
  }
  messages.value = [{ id: msgSeq++, sender: 'ai', text: welcomeText, toolCalls: [], drafts: [], subscriptions: [] }]
  lastSteps.value = 0
  userInput.value = ''
  ElMessage.success('已开启新对话')
  scrollToBottom()
}

onMounted(() => {
  scrollToBottom()
  // 支持 ?q= 自动提问（作品/艺术家详情页「问 AI」、展览场次「让 AI 生成草稿」入口）
  const q = route.query.q
  if (q && typeof q === 'string' && q.trim()) {
    userInput.value = q.trim()
    sendMessage()
  }
})
</script>

<style scoped>
/* ===== 页面基调：美术馆暖色系 ===== */
.curator-page {
  max-width: 1080px;
  margin: 0 auto;
  padding: 28px 20px 60px;
  font-family: 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
}

/* ===== 顶部英雄区 ===== */
.curator-hero {
  position: relative;
  overflow: hidden;
  border-radius: 20px;
  background:
      radial-gradient(1200px 400px at 85% -20%, rgba(201, 168, 106, 0.35), transparent 60%),
      linear-gradient(135deg, #1f2a24 0%, #2c3a32 55%, #3a4638 100%);
  color: #f5efe4;
  padding: 42px 44px 36px;
  box-shadow: 0 18px 50px rgba(31, 42, 36, 0.28);
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  letter-spacing: 1.5px;
  text-transform: uppercase;
  color: #d9c391;
  border: 1px solid rgba(217, 195, 145, 0.45);
  border-radius: 999px;
  padding: 6px 16px;
  margin-bottom: 18px;
}

.badge-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #d9c391;
  box-shadow: 0 0 12px #d9c391;
}

.curator-hero h1 {
  font-family: Georgia, 'Noto Serif SC', 'Songti SC', serif;
  font-size: 38px;
  letter-spacing: 2px;
  margin-bottom: 12px;
}

.hero-sub {
  max-width: 640px;
  line-height: 1.8;
  color: #cfd6cc;
  margin-bottom: 22px;
}

.hero-capabilities {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.cap {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 10px;
  padding: 8px 14px;
  font-size: 13px;
  backdrop-filter: blur(4px);
}

.security-note {
  margin-top: 26px;
  display: flex;
  gap: 10px;
  align-items: flex-start;
  background: rgba(217, 195, 145, 0.12);
  border: 1px dashed rgba(217, 195, 145, 0.5);
  border-radius: 12px;
  padding: 12px 16px;
  font-size: 13px;
  line-height: 1.7;
  color: #e8ddc4;
}

.security-note strong { color: #f2dfae; }

.lock-icon { margin-top: 3px; }

/* ===== 对话卡片 ===== */
.chat-card {
  margin-top: 24px;
  background: #fffdf9;
  border: 1px solid #e9e2d4;
  border-radius: 20px;
  box-shadow: 0 12px 40px rgba(60, 50, 30, 0.08);
  overflow: hidden;
}

.chat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-bottom: 1px solid #efe8da;
  background: #fdfaf3;
}

.chat-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.curator-avatar {
  width: 42px;
  height: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  border-radius: 12px;
  background: linear-gradient(135deg, #1f2a24, #3a4638);
  box-shadow: 0 4px 14px rgba(31, 42, 36, 0.3);
}

.chat-title b { display: block; color: #23201c; }
.chat-title small { color: #9a8f7d; font-size: 12px; }

.chat-steps {
  font-size: 12px;
  color: #8a7a5c;
  background: #f3ecdc;
  padding: 4px 12px;
  border-radius: 999px;
}

.chat-head-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.new-chat-btn {
  color: #8a6a33;
  font-size: 13px;
}

.new-chat-btn:hover {
  background: #f3ecdc;
}

/* ===== 消息区 ===== */
.messages {
  height: 520px;
  overflow-y: auto;
  padding: 24px;
  background:
      linear-gradient(rgba(246, 243, 238, 0.6), rgba(246, 243, 238, 0.6)),
      radial-gradient(circle at 20% 10%, rgba(201, 168, 106, 0.06), transparent 40%);
}

.msg-row {
  display: flex;
  gap: 12px;
  margin-bottom: 22px;
}

.msg-row.user { flex-direction: row-reverse; }

.msg-avatar { flex-shrink: 0; }
.msg-avatar span {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  border-radius: 50%;
  background: #1f2a24;
}

.msg-content {
  max-width: 78%;
  padding: 14px 18px;
  border-radius: 16px;
  background: #ffffff;
  border: 1px solid #ece5d7;
  box-shadow: 0 3px 12px rgba(60, 50, 30, 0.05);
}

.msg-row.user .msg-content {
  background: #1f2a24;
  color: #f2efe6;
  border-color: #1f2a24;
}

.msg-name {
  font-size: 12px;
  font-weight: 600;
  color: #9a8f7d;
  margin-bottom: 6px;
}

.msg-row.user .msg-name { text-align: right; color: #b9c1b2; }

.msg-text {
  line-height: 1.85;
  font-size: 15px;
  color: #332e26;
  word-break: break-word;
}

.msg-row.user .msg-text { color: #f2efe6; }

.msg-text :deep(strong) { color: #8a6a33; }
.msg-text :deep(code) {
  background: #f3ecdc;
  border-radius: 6px;
  padding: 2px 7px;
  font-size: 13px;
  color: #7a5c2e;
}
.msg-text :deep(.md-li) { display: block; margin: 2px 0; }
.msg-text :deep(.md-h) {
  display: block;
  font-weight: 700;
  font-size: 16px;
  margin: 6px 0 4px;
  color: #1f2a24;
}

/* 工具轨迹 */
.tool-traces {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed #eee5d4;
}

.trace {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 999px;
}

.trace.ok { background: #edf7ee; color: #3f7d4a; }
.trace.fail { background: #fdf0ee; color: #b4564a; }
.trace em { font-style: normal; opacity: 0.75; }

/* 草稿卡片 */
.draft-list { margin-top: 14px; display: flex; flex-direction: column; gap: 12px; }

.draft-card {
  border: 1px solid #e5d9bd;
  border-radius: 14px;
  background: linear-gradient(135deg, #fffdf6, #fbf6e9);
  padding: 14px 16px;
  box-shadow: 0 4px 16px rgba(140, 110, 60, 0.08);
}

.draft-card.confirmed { border-color: #bcd9c2; background: #f4faf5; }
.draft-card.cancelled, .draft-card.expired { opacity: 0.72; }

.draft-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.draft-flag { font-size: 13px; font-weight: 700; color: #8a6a33; }

.draft-exhibition {
  font-family: Georgia, 'Noto Serif SC', serif;
  font-size: 17px;
  color: #23201c;
  margin-bottom: 8px;
}

.draft-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  font-size: 13px;
  color: #6f6553;
  margin-bottom: 6px;
}

.draft-meta span { display: inline-flex; align-items: center; gap: 4px; }

.draft-price { font-weight: 700; color: #a3761f; }

.draft-expire { font-size: 12px; color: #a89a80; }

.draft-actions {
  margin-top: 12px;
  display: flex;
  gap: 4px;
  align-items: center;
}

.draft-done {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.draft-done.success { color: #3f7d4a; }
.draft-done.muted { color: #9a8f7d; }

/* 订阅 chips */
.sub-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

/* 输入中动画 */
.typing-indicator {
  display: flex;
  gap: 5px;
  padding: 6px 0;
}

.typing-indicator span {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #b7a888;
  animation: typing 1.2s infinite ease-in-out;
}

.typing-indicator span:nth-child(2) { animation-delay: 0.15s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.3s; }

@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.5; }
  30% { transform: translateY(-7px); opacity: 1; }
}

/* ===== 输入区 ===== */
.composer {
  padding: 18px 24px 20px;
  border-top: 1px solid #efe8da;
  background: #fdfaf3;
}

.composer :deep(.el-textarea__inner) {
  border-radius: 12px;
  border-color: #e2d9c6;
  background: #fffdf9;
  font-size: 14px;
  box-shadow: none;
}

.composer :deep(.el-textarea__inner:focus) {
  border-color: #b08d57;
  box-shadow: 0 0 0 2px rgba(176, 141, 87, 0.18);
}

.composer-row {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 14px;
  margin-top: 12px;
}

.quick-questions { display: flex; flex-wrap: wrap; gap: 8px; flex: 1; }

.question-tag {
  cursor: pointer;
  color: #7a6a4f;
  border-color: #e0d5bd;
  background: #faf6ec;
  transition: all 0.25s ease;
}

.question-tag:hover {
  background: #1f2a24;
  border-color: #1f2a24;
  color: #f5efe4;
  transform: translateY(-2px);
}

.composer-row .el-button--primary {
  background: #1f2a24;
  border-color: #1f2a24;
}

.composer-row .el-button--primary:hover {
  background: #8a6a33;
  border-color: #8a6a33;
}

/* ===== 底部能力边界 ===== */
.curator-foot {
  margin-top: 26px;
  background: #fffdf9;
  border: 1px solid #e9e2d4;
  border-radius: 18px;
  padding: 26px 28px;
}

.curator-foot h3 {
  font-family: Georgia, 'Noto Serif SC', serif;
  font-size: 18px;
  color: #23201c;
  margin-bottom: 16px;
}

.tool-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}

.tool {
  background: #faf6ec;
  border: 1px solid #eee5d1;
  border-radius: 12px;
  padding: 14px;
}

.tool b { display: block; font-size: 13px; color: #3a3529; margin-bottom: 6px; word-break: break-all; }

.kind {
  display: inline-block;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 999px;
  margin-bottom: 8px;
}

.kind.read { background: #e7f3ea; color: #3f7d4a; }
.kind.write { background: #fbf0dc; color: #a3761f; }

.tool p { font-size: 12px; color: #8d8171; line-height: 1.6; }

.foot-note {
  margin-top: 16px;
  font-size: 12px;
  color: #9a8f7d;
  line-height: 1.7;
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .curator-hero { padding: 26px 20px 22px; }
  .curator-hero h1 { font-size: 28px; }
  .messages { height: 400px; padding: 16px; }
  .msg-content { max-width: 88%; }
  .composer-row { flex-direction: column; align-items: stretch; }
}
</style>
