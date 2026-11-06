<template>
  <div class="my-reservations-page">
    <div class="page-header">
      <div class="container">
        <h1 class="page-title">我的预约</h1>
        <p class="page-description">管理您的预约草稿、已确认预约与新展订阅</p>
      </div>
    </div>

    <div class="container">
      <div v-if="!userId" class="guest-tip">
        <el-empty description="请先登录后查看预约信息">
          <el-button type="primary" round @click="$router.push('/login')">去登录</el-button>
        </el-empty>
      </div>

      <el-tabs v-else v-model="activeTab" class="reservation-tabs">
        <!-- ===== 预约草稿（可续确认） ===== -->
        <el-tab-pane label="预约草稿" name="drafts">
          <div v-if="loadingDrafts" class="list-loading"><el-skeleton :rows="3" animated /></div>
          <template v-else>
            <div v-if="drafts.length" class="cards">
              <div v-for="d in drafts" :key="d.draftId" :class="['res-card', d.status.toLowerCase()]">
                <div class="card-top">
                  <span class="flag">🎫 草稿 #{{ d.draftId }}</span>
                  <el-tag size="small" :type="statusType(d.status)" effect="light">{{ statusText(d.status) }}</el-tag>
                </div>
                <div class="card-title">{{ d.exhibitionName }}</div>
                <div class="card-meta">
                  <span><el-icon><Clock /></el-icon> {{ d.sessionTime }}</span>
                  <span><el-icon><Ticket /></el-icon> {{ d.ticketCount }} 张</span>
                  <span class="price">¥ {{ d.totalPrice }}</span>
                </div>
                <div class="card-expire">有效期至 {{ formatExpire(d.expiresAt) }}</div>
                <div class="card-actions" v-if="d.status === 'PENDING'">
                  <el-button type="primary" round size="small" :loading="confirmingId === d.draftId"
                             @click="confirmOne(d)">确认预约</el-button>
                  <el-button round size="small" @click="cancelOne(d)">取消草稿</el-button>
                  <el-button text size="small" @click="$router.push(`/exhibitions/${d.exhibitionId}`)">查看展览 →</el-button>
                </div>
                <div class="card-state success" v-else-if="d.status === 'CONFIRMED'">
                  <el-icon><CircleCheck /></el-icon> 已确认预约
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无预约草稿，去和 AI 策展助手聊聊吧">
              <el-button type="primary" round @click="$router.push('/ai-art-guide')">去生成草稿</el-button>
            </el-empty>
          </template>
        </el-tab-pane>

        <!-- ===== 已确认预约 ===== -->
        <el-tab-pane label="已确认预约" name="reservations">
          <div v-if="loadingReservations" class="list-loading"><el-skeleton :rows="3" animated /></div>
          <template v-else>
            <div v-if="reservations.length" class="cards">
              <div v-for="r in reservations" :key="r.reservationId" class="res-card confirmed">
                <div class="card-top">
                  <span class="flag">🎟️ 预约 #{{ r.reservationId }}</span>
                  <el-tag size="small" :type="r.status === 'CANCELLED' ? 'info' : 'success'" effect="light">
                    {{ r.status === 'CANCELLED' ? '已取消' : '已确认' }}
                  </el-tag>
                </div>
                <div class="card-title">{{ r.exhibitionName }}</div>
                <div class="card-meta">
                  <span><el-icon><Clock /></el-icon> {{ r.sessionTime }}</span>
                  <span><el-icon><Ticket /></el-icon> {{ r.ticketCount }} 张</span>
                  <span class="price">¥ {{ r.totalPrice }}</span>
                </div>
                <div class="card-actions" v-if="r.status === 'CONFIRMED'">
                  <el-button round size="small" @click="cancelReservationOne(r)">取消预约（票额回补）</el-button>
                  <el-button text size="small" @click="$router.push(`/exhibitions/${r.exhibitionId}`)">查看展览 →</el-button>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无已确认的预约" />
          </template>
        </el-tab-pane>

        <!-- ===== 新展订阅 ===== -->
        <el-tab-pane label="新展订阅" name="subscriptions">
          <div v-if="loadingSubscriptions" class="list-loading"><el-skeleton :rows="3" animated /></div>
          <template v-else>
            <div v-if="subscriptions.length" class="cards">
              <div v-for="s in subscriptions" :key="s.subscriptionId" class="res-card">
                <div class="card-top">
                  <span class="flag">🔔 订阅提醒</span>
                  <el-tag size="small" type="success" effect="light">订阅中</el-tag>
                </div>
                <div class="card-title">{{ s.exhibitionName }}</div>
                <div class="card-meta">
                  <span v-if="s.exhibitionTheme"><el-icon><Collection /></el-icon> {{ s.exhibitionTheme }}</span>
                  <span v-if="s.location"><el-icon><Location /></el-icon> {{ s.location }}</span>
                  <span v-if="s.startTime"><el-icon><Clock /></el-icon> {{ s.startTime }}</span>
                </div>
                <div class="card-actions">
                  <el-button round size="small" @click="unsubscribeOne(s)">取消订阅</el-button>
                  <el-button text size="small" @click="$router.push(`/exhibitions/${s.exhibitionId}`)">查看展览 →</el-button>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无新展订阅" />
          </template>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/index.js'
import { ElMessage } from 'element-plus'
import { Clock, Ticket, CircleCheck, Collection, Location } from '@element-plus/icons-vue'
import {
  myDrafts, myReservations, mySubscriptions,
  confirmDraft, cancelDraft, cancelReservation, unsubscribeExhibition
} from '@/api/reservation.js'

const userStore = useUserStore()

const userId = computed(() => {
  try {
    const u = userStore.currentUser
    if (!u) return null
    const o = typeof u === 'string' ? JSON.parse(u) : u
    return o?.id ?? o?.userId ?? null
  } catch (e) {
    return null
  }
})

const activeTab = ref('drafts')
const drafts = ref([])
const reservations = ref([])
const subscriptions = ref([])
const loadingDrafts = ref(false)
const loadingReservations = ref(false)
const loadingSubscriptions = ref(false)
const confirmingId = ref(null)

const statusType = (s) => ({ PENDING: 'warning', CONFIRMED: 'success', EXPIRED: 'info', CANCELLED: 'danger' }[s] || 'info')
const statusText = (s) => ({ PENDING: '待确认', CONFIRMED: '已确认', EXPIRED: '已过期', CANCELLED: '已取消' }[s] || s)

const formatExpire = (v) => {
  if (!v) return '—'
  const d = new Date(v)
  if (isNaN(d)) return '—'
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const loadDrafts = async () => {
  loadingDrafts.value = true
  try {
    const res = await myDrafts(userId.value)
    drafts.value = res?.data || []
  } catch (e) {
    drafts.value = []
  } finally {
    loadingDrafts.value = false
  }
}

const loadReservations = async () => {
  loadingReservations.value = true
  try {
    const res = await myReservations(userId.value)
    reservations.value = res?.data || []
  } catch (e) {
    reservations.value = []
  } finally {
    loadingReservations.value = false
  }
}

const loadSubscriptions = async () => {
  loadingSubscriptions.value = true
  try {
    const res = await mySubscriptions(userId.value)
    subscriptions.value = res?.data || []
  } catch (e) {
    subscriptions.value = []
  } finally {
    loadingSubscriptions.value = false
  }
}

const confirmOne = async (d) => {
  confirmingId.value = d.draftId
  try {
    const res = await confirmDraft(d.draftId, userId.value)
    if (res?.success !== false) {
      ElMessage.success(res?.message || '预约成功！')
      await Promise.all([loadDrafts(), loadReservations()])
    } else {
      ElMessage.error(res?.message || '预约失败')
    }
  } catch (e) {
    ElMessage.error(typeof e === 'string' ? e : (e?.response?.data?.message || '预约失败，请重试'))
  } finally {
    confirmingId.value = null
  }
}

const cancelOne = async (d) => {
  try {
    const res = await cancelDraft(d.draftId, userId.value)
    if (res?.success !== false) {
      ElMessage.success('草稿已取消')
      loadDrafts()
    } else {
      ElMessage.error(res?.message || '取消失败')
    }
  } catch (e) {
    ElMessage.error(typeof e === 'string' ? e : '取消失败')
  }
}

const cancelReservationOne = async (r) => {
  try {
    const res = await cancelReservation(r.reservationId, userId.value)
    if (res?.success !== false) {
      ElMessage.success('预约已取消，票额已回补')
      loadReservations()
    } else {
      ElMessage.error(res?.message || '取消失败')
    }
  } catch (e) {
    ElMessage.error(typeof e === 'string' ? e : '取消失败')
  }
}

const unsubscribeOne = async (s) => {
  try {
    const res = await unsubscribeExhibition(userId.value, s.exhibitionId)
    if (res?.success !== false) {
      ElMessage.success(res?.message || '已取消订阅')
      loadSubscriptions()
    } else {
      ElMessage.error(res?.message || '操作失败')
    }
  } catch (e) {
    ElMessage.error(typeof e === 'string' ? e : '操作失败')
  }
}

onMounted(() => {
  if (userId.value) {
    loadDrafts()
    loadReservations()
    loadSubscriptions()
  }
})
</script>

<style scoped>
.my-reservations-page { min-height: 70vh; }

.page-header {
  background: linear-gradient(135deg, #1f2a24 0%, #2c3a32 55%, #3a4638 100%);
  color: #f5efe4;
  padding: 46px 0;
  text-align: center;
  margin-bottom: 30px;
}

.page-title {
  font-family: Georgia, 'Noto Serif SC', 'Songti SC', serif;
  font-size: 32px;
  letter-spacing: 2px;
}

.page-description { color: #cfd6cc; margin-top: 8px; }

.guest-tip { padding: 40px 0; }

.reservation-tabs :deep(.el-tabs__item.is-active) { color: #8a6a33; }
.reservation-tabs :deep(.el-tabs__active-bar) { background-color: #8a6a33; }
.reservation-tabs :deep(.el-tabs__item) { color: #6f6553; }

.list-loading { padding: 20px 0; }

.cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
  padding: 8px 0 30px;
}

.res-card {
  background: #fffdf9;
  border: 1px solid #e9e2d4;
  border-radius: 14px;
  padding: 16px 18px;
  box-shadow: 0 6px 20px rgba(60, 50, 30, 0.06);
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.res-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 28px rgba(60, 50, 30, 0.12);
}

.res-card.expired, .res-card.cancelled { opacity: 0.68; }

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.flag { font-size: 13px; font-weight: 700; color: #8a6a33; }

.card-title {
  font-family: Georgia, 'Noto Serif SC', serif;
  font-size: 17px;
  color: #23201c;
  margin-bottom: 10px;
}

.card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 13px;
  color: #6f6553;
  margin-bottom: 8px;
}

.card-meta span { display: inline-flex; align-items: center; gap: 4px; }

.price { font-weight: 700; color: #a3761f; }

.card-expire { font-size: 12px; color: #a89a80; margin-bottom: 10px; }

.card-actions { display: flex; gap: 6px; align-items: center; margin-top: 6px; }

.card-state {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  margin-top: 8px;
}

.card-state.success { color: #3f7d4a; }
</style>
