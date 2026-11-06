import api from './index'

/**
 * 真实预约 API —— 人工确认闸门的"真实写入侧"
 * 这些接口只由前端（人）调用；AI Agent 只能生成草稿，永远无法触达 confirm。
 */

/** 人工确认预约草稿 → 真实预约 + 原子扣减库存 */
export const confirmDraft = (draftId, userId) =>
  api.post('/api/reservations/confirm', { draftId, userId })

/** 取消草稿（不占库存） */
export const cancelDraft = (draftId, userId) =>
  api.post(`/api/reservations/drafts/${draftId}/cancel`, { userId })

/** 草稿详情 */
export const getDraft = (draftId) =>
  api.get(`/api/reservations/drafts/${draftId}`)

/** 我的草稿列表 */
export const myDrafts = (userId) =>
  api.get('/api/reservations/drafts/my', { params: { userId } })

/** 我的真实预约 */
export const myReservations = (userId) =>
  api.get('/api/reservations/my', { params: { userId } })

/** 取消已确认预约（回补库存） */
export const cancelReservation = (reservationId, userId) =>
  api.post(`/api/reservations/${reservationId}/cancel`, { userId })

/** 展览排期与余票（只读） */
export const getSessions = (exhibitionId) =>
  api.get(`/api/ticket-sessions/exhibition/${exhibitionId}`, { params: { onSaleOnly: true } })

/** 新展订阅 / 取消订阅（人直接操作；Agent 工具走同一服务） */
export const subscribeExhibition = (userId, exhibitionId) =>
  api.post('/api/subscriptions', { userId, exhibitionId })

export const unsubscribeExhibition = (userId, exhibitionId) =>
  api.post('/api/subscriptions/cancel', { userId, exhibitionId })

export const mySubscriptions = (userId) =>
  api.get('/api/subscriptions/my', { params: { userId } })

export default {
  confirmDraft, cancelDraft, getDraft, myDrafts, myReservations, cancelReservation,
  getSessions, subscribeExhibition, unsubscribeExhibition, mySubscriptions
}
