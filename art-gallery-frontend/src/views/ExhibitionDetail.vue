<template>
  <div class="exhibition-detail-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="container">
        <h1 class="page-title">{{ exhibition.name }}</h1>
        <p class="page-description">{{ formatDate(exhibition.startTime) }} - {{ formatDate(exhibition.endTime) }}</p>
      </div>
    </div>

    <!-- 主要内容 -->
    <div class="container">
      <div class="exhibition-detail">
        <!-- 左侧：展览图片 -->
        <div class="exhibition-images">
          <img 
            :src="currentImage || 'https://via.placeholder.com/600x400?text=展览图片'" 
            class="main-image" 
            alt="exhibition.name"
            @click="openImageViewer(currentImage)"
          >
          <div class="image-gallery" v-if="exhibitionImages && exhibitionImages.length > 1">
            <img 
              v-for="(image, index) in exhibitionImages" 
              :key="index"
              :src="image" 
              class="gallery-thumbnail" 
              :class="{ active: currentImage === image }"
              @click="currentImage = image"
              :alt="`${exhibition.name} - 图片 ${index + 1}`"
            >
          </div>
        </div>

        <!-- 右侧：展览信息 -->
        <div class="exhibition-info">
          <h2 class="exhibition-title">{{ exhibition.name }}</h2>
          
          <div class="exhibition-meta">
            <div class="exhibition-meta-item">
              <span class="meta-label">展览时间：</span>
              <span>{{ formatDate(exhibition.startTime) }} - {{ formatDate(exhibition.endTime) }}</span>
            </div>
            <div class="exhibition-meta-item">
              <span class="meta-label">展览地点：</span>
              <span>{{ exhibition.location || '未知' }}</span>
            </div>
            <div class="exhibition-meta-item">
              <span class="meta-label">策展人：</span>
              <span>{{ exhibition.curator || '未知' }}</span>
            </div>
            <div class="exhibition-meta-item">
              <span class="meta-label">主题：</span>
              <span>{{ exhibition.theme || '未知' }}</span>
            </div>
            <div class="exhibition-meta-item">
              <span class="meta-label">状态：</span>
              <span>
                <el-tag size="small" type="success" v-if="isOngoing(exhibition)">正在进行</el-tag>
                <el-tag size="small" type="warning" v-else-if="isUpcoming(exhibition)">即将开始</el-tag>
                <el-tag size="small" type="info" v-else>已结束</el-tag>
              </span>
            </div>
          </div>
          
          <div class="exhibition-description">
            <h3>展览介绍</h3>
            <p>{{ exhibition.description }}</p>
          </div>
          
          <!-- 社交分享 -->
          <div class="social-share">
            <h3>分享展览</h3>
            <div class="share-buttons">
              <a href="#" class="share-button share-facebook" @click.prevent="shareOnFacebook">
                <Icon icon="mdi:facebook" />
              </a>
              <a href="#" class="share-button share-twitter" @click.prevent="shareOnTwitter">
                <Icon icon="mdi:twitter" />
              </a>
              <a href="#" class="share-button share-pinterest" @click.prevent="shareOnPinterest">
                <Icon icon="mdi:pinterest" />
              </a>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 展览艺术品 -->
      <div class="exhibition-artworks" v-if="exhibitionArtworks.length > 0">
        <h2 class="section-title">展览艺术品</h2>
        <div class="artwork-grid">
          <div v-for="artwork in exhibitionArtworks" :key="artwork.id" class="artwork-card">
            <div class="card-image-container">
              <img :src="artwork.mainImageUrl || 'https://via.placeholder.com/300x200?text=艺术品图片'" class="card-image" :alt="artwork.name">
            </div>
            <div class="card-content">
              <h3 class="card-title">{{ artwork.name }}</h3>
              <div class="card-tags">
                <el-tag size="small" type="info" v-if="artwork.style">{{ artwork.style }}</el-tag>
                <el-tag size="small" type="success" v-if="artwork.medium">{{ artwork.medium }}</el-tag>
              </div>
              <p class="card-description">{{ truncateText(artwork.description, 100) }}</p>
              <div class="card-meta">
                <span>{{ artwork.artist?.name || '未知艺术家' }}</span>
                <span>{{ artwork.creationYear || '未知年代' }}</span>
              </div>
              <router-link :to="`/artworks/${artwork.id}`" class="btn btn-primary card-btn">查看详情</router-link>
            </div>
          </div>
        </div>
      </div>
      

      <!-- 票务排期与订阅（数据来自 /api/ticket-sessions，只读；预约草稿由 AI 策展助手生成） -->
      <div class="ticket-section">
        <div class="ticket-head">
          <h2 class="section-title">🎟️ 排期与余票</h2>
          <el-button v-if="userId" plain round size="small" :loading="subscribing" @click="subscribe">
            <el-icon><Bell /></el-icon>&nbsp;订阅新展提醒
          </el-button>
        </div>
        <div v-if="sessions.length" class="session-grid">
          <div v-for="s in sessions" :key="s.sessionId" class="session-card" :class="{ soldout: s.remaining <= 0 }">
            <div class="session-time">📅 {{ s.sessionTime }}</div>
            <div class="session-meta">
              <span class="session-price">¥{{ s.price }}</span>
              <span class="session-remain" :class="{ low: s.remaining > 0 && s.remaining <= 10 }">
                余票 {{ s.remaining }} / {{ s.totalTickets }}
              </span>
            </div>
            <div class="session-tip">
              <span v-if="s.remaining <= 0">该场次已售罄</span>
              <el-button v-else round size="small" class="ai-book-btn" @click="askAiBook(s)">
                🤖 让 AI 生成预约草稿
              </el-button>
            </div>
          </div>
        </div>
        <el-empty v-else description="该展览暂未开放售票排期" :image-size="70" />
      </div>

    <!-- 图片查看器 -->
    <div class="image-viewer-overlay" v-if="showImageViewer" @click="closeImageViewer">
      <div class="image-viewer-content" @click.stop>
        <img :src="viewerImage" class="viewer-image" :alt="exhibition.name">
        <div class="viewer-close" @click="closeImageViewer">
          <el-icon><Close /></el-icon>
        </div>
        <div class="viewer-controls" v-if="exhibitionImages && exhibitionImages.length > 1">
          <button class="viewer-control" @click="prevImage">
            <el-icon><ArrowLeft /></el-icon>
          </button>
          <button class="viewer-control" @click="nextImage">
            <el-icon><ArrowRight /></el-icon>
          </button>
        </div>
      </div>
    </div>
  </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useExhibitionStore, useUserStore } from '../stores'
import { useArtworkStore } from '../stores/artwork'
import { ElMessage } from 'element-plus'
import { Picture, Close, ArrowLeft, ArrowRight, Bell } from '@element-plus/icons-vue'
import { Icon } from '@iconify/vue'
import request from "@/utils/request.js";
import { getSessions, subscribeExhibition } from '@/api/reservation.js'

const route = useRoute()
const router = useRouter()

// 工具函数：格式化日期
function formatDate(dateString) {
  if (!dateString) return '未知'
  return new Date(dateString).toLocaleDateString()
}
// 工具函数：截断文本
function truncateText(text, length) {
  if (!text) return '暂无描述'
  return text.length > length ? text.substring(0, length) + '...' : text
}
// 判断展览状态
function isOngoing(exhibition) {
  if (!exhibition || !exhibition.startTime || !exhibition.endTime) return false
  const now = new Date()
  const startDate = new Date(exhibition.startTime)
  const endDate = new Date(exhibition.endTime)
  return startDate <= now && now <= endDate
}
function isUpcoming(exhibition) {
  if (!exhibition || !exhibition.startTime) return false
  const now = new Date()
  const startDate = new Date(exhibition.startTime)
  return startDate > now
}

// 展览数据
const exhibition = ref({content: []})
const exhibitionArtworks = ref({content: []})
const loading = ref(true)
const currentImage = ref('')

// 图片查看器
const showImageViewer = ref(false)
const viewerImage = ref('')
const currentImageIndex = ref(0)

// 处理展览图片
const exhibitionImages = computed(() => {
  const images = []
  if (exhibition.value.posterUrl) {
    images.push(exhibition.value.posterUrl)
  }
  if (exhibition.value.posterUrls) {
    try {
      const parsedImages = JSON.parse(exhibition.value.imageUrls)
      if (Array.isArray(parsedImages)) {
        images.push(...parsedImages)
      }
    } catch (error) {
      console.error('解析展览图片失败:', error)
    }
  }
  return images
})

// 获取展览详情
const fetchExhibitionDetail = async (id) => {
  loading.value = true
  try {
    const res = await request({
      url: `/api/exhibitions/${id}`,
      method: "get",
      headers: { "Content-Type": "application/json" },
    })
    if (res) {
      exhibition.value = res;
    } else {
      exhibition.value = {}
      ElMessage.warning('未获取到展览详情')
    }
    loading.value = false
  } catch (error) {
    ElMessage.error('获取展览详情失败')
    exhibition.value = {}
  } finally {
    loading.value = false
  }
}
// 获取展览艺术品
const fetchExhibitionArtworks = async (id) => {
  try {
    const res = await request({
      url: `/api/artworks/${id}`,
      method: "get",
      headers: {"Content-Type": "application/json"},
    })
    if (res && res.content) {
      exhibitionArtworks.value = res.content
    } else {
      exhibitionArtworks.value = []
    }
  } catch (error) {
    exhibitionArtworks.value = []
  }
}

// 打开图片查看器
const openImageViewer = (image) => {
  if (!image) return
  viewerImage.value = image
  showImageViewer.value = true
  if (exhibitionImages.value.length > 0) {
    currentImageIndex.value = exhibitionImages.value.indexOf(image)
    if (currentImageIndex.value === -1) currentImageIndex.value = 0
  }
}
// 关闭图片查看器
const closeImageViewer = () => {
  showImageViewer.value = false
}
// 上一张图片
const prevImage = () => {
  if (!exhibitionImages.value || exhibitionImages.value.length <= 1) return
  currentImageIndex.value = (currentImageIndex.value - 1 + exhibitionImages.value.length) % exhibitionImages.value.length
  viewerImage.value = exhibitionImages.value[currentImageIndex.value]
}
// 下一张图片
const nextImage = () => {
  if (!exhibitionImages.value || exhibitionImages.value.length <= 1) return
  currentImageIndex.value = (currentImageIndex.value + 1) % exhibitionImages.value.length
  viewerImage.value = exhibitionImages.value[currentImageIndex.value]
}
// 社交分享
const shareOnFacebook = () => {
  const url = encodeURIComponent(window.location.href)
  const title = encodeURIComponent(exhibition.value.name)
  window.open(`https://www.facebook.com/sharer/sharer.php?u=${url}&t=${title}`, '_blank')
}
const shareOnTwitter = () => {
  const url = encodeURIComponent(window.location.href)
  const text = encodeURIComponent(`${exhibition.value.name} - 艺术品展览网站`)
  window.open(`https://twitter.com/intent/tweet?url=${url}&text=${text}`, '_blank')
}
const shareOnPinterest = () => {
  const url = encodeURIComponent(window.location.href)
  const description = encodeURIComponent(exhibition.value.name)
  const media = encodeURIComponent(exhibition.value.posterUrl || '')
  window.open(`https://pinterest.com/pin/create/button/?url=${url}&media=${media}&description=${description}`, '_blank')
}

// 初始化加载
onMounted(() => {
  const id = route.params.id
  if (id) {
    fetchExhibitionDetail(id)
    fetchExhibitionArtworks(id)
    fetchSessions(id)
  }
})

// ===== 票务排期（只读）与订阅 =====
const userStore = useUserStore()
const sessions = ref([])
const subscribing = ref(false)

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

const fetchSessions = async (id) => {
  try {
    const res = await getSessions(id)
    sessions.value = res?.data || []
  } catch (e) {
    sessions.value = []
  }
}

const subscribe = async () => {
  if (!userId.value) {
    ElMessage.warning('请先登录后再订阅')
    router.push('/login')
    return
  }
  subscribing.value = true
  try {
    const res = await subscribeExhibition(userId.value, route.params.id)
    if (res?.success !== false) {
      ElMessage.success(res?.message || '订阅成功，开展时我们会通知您')
    } else {
      ElMessage.info(res?.message || '订阅失败')
    }
  } catch (e) {
    ElMessage.error(typeof e === 'string' ? e : '订阅失败，请重试')
  } finally {
    subscribing.value = false
  }
}

// 让 AI 生成预约草稿：带上展览与场次信息跳转策展助手
const askAiBook = (s) => {
  const name = exhibition.value?.name || ''
  router.push({
    path: '/ai-art-guide',
    query: { q: `我想预约《${name}》${s.sessionTime} 的场次 1 张票，请帮我生成预约草稿` }
  })
}
</script>

<style scoped>
/* 页面标题 */
.page-header {
  background: linear-gradient(135deg, #1f2a24 0%, #2c3a32 55%, #3a4638 100%);
  color: white;
  padding: 60px 0;
  text-align: center;
  margin-bottom: 40px;
}

.page-title {
  font-size: 36px;
  margin-bottom: 15px;
}

.page-description {
  font-size: 18px;
  max-width: 600px;
  margin: 0 auto;
}

/* 展览详情 */
.exhibition-detail {
  display: flex;
  gap: 40px;
  margin-bottom: 60px;
}

/* 左侧图片区域 */
.exhibition-images {
  flex: 0 0 50%;
}

.main-image {
  width: 100%;
  height: auto;
  border-radius: 8px;
  cursor: pointer;
  transition: transform 0.3s ease;
}

.main-image:hover {
  transform: scale(1.02);
}

.image-gallery {
  display: flex;
  gap: 10px;
  margin-top: 15px;
  overflow-x: auto;
  padding-bottom: 10px;
}

.gallery-thumbnail {
  width: 80px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
  cursor: pointer;
  opacity: 0.7;
  transition: opacity 0.3s ease;
}

.gallery-thumbnail:hover, .gallery-thumbnail.active {
  opacity: 1;
}

/* 右侧信息区域 */
.exhibition-info {
  flex: 1;
}

.exhibition-title {
  font-size: 28px;
  color: #333;
  margin-bottom: 20px;
}

.exhibition-meta {
  margin-bottom: 30px;
}

.exhibition-meta-item {
  margin-bottom: 10px;
  display: flex;
}

.meta-label {
  font-weight: bold;
  width: 100px;
  color: #666;
}

.exhibition-description {
  margin-bottom: 30px;
}

.exhibition-description h3 {
  font-size: 20px;
  color: #333;
  margin-bottom: 15px;
}

.exhibition-description p {
  line-height: 1.6;
  color: #555;
}

/* 社交分享 */
.social-share {
  margin-top: 30px;
}

.social-share h3 {
  font-size: 18px;
  margin-bottom: 15px;
}

.share-buttons {
  display: flex;
  gap: 15px;
}

.share-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  color: white;
  transition: transform 0.3s ease, background-color 0.3s ease;
}

.share-button:hover {
  transform: translateY(-3px);
}

.share-facebook {
  background-color: #3b5998;
}

.share-twitter {
  background-color: #1da1f2;
}

.share-pinterest {
  background-color: #bd081c;
}

/* 展览艺术品和相关展览 */
.exhibition-artworks,
.related-exhibitions {
  margin-bottom: 60px;
}

.section-title {
  font-size: 24px;
  color: #333;
  margin-bottom: 30px;
  text-align: center;
  position: relative;
}

.section-title::after {
  content: '';
  display: block;
  width: 50px;
  height: 3px;
  background-color: #1f2a24;
  margin: 15px auto 0;
}

.artwork-grid,
.exhibition-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 30px;
}

/* 卡片样式 */
.artwork-card,
.exhibition-card {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.artwork-card:hover,
.exhibition-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.15);
}

.card-image-container {
  height: 200px;
  overflow: hidden;
}

.card-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.artwork-card:hover .card-image,
.exhibition-card:hover .card-image {
  transform: scale(1.05);
}

.card-content {
  padding: 20px;
}

.card-title {
  font-size: 18px;
  margin-bottom: 10px;
  color: #333;
}

.card-tags {
  margin-bottom: 10px;
}

.card-tags .el-tag {
  margin-right: 5px;
  margin-bottom: 5px;
}

.card-description {
  color: #666;
  margin-bottom: 15px;
  line-height: 1.5;
}

.card-meta {
  display: flex;
  justify-content: space-between;
  color: #888;
  font-size: 14px;
  margin-bottom: 15px;
}

.card-btn {
  width: 100%;
  text-align: center;
}

/* 图片查看器 */
.image-viewer-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.9);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.image-viewer-content {
  position: relative;
  max-width: 90%;
  max-height: 90%;
}

.viewer-image {
  max-width: 100%;
  max-height: 90vh;
  display: block;
}

.viewer-close {
  position: absolute;
  top: -40px;
  right: 0;
  color: white;
  font-size: 24px;
  cursor: pointer;
}

.viewer-controls {
  position: absolute;
  bottom: 20px;
  left: 0;
  right: 0;
  display: flex;
  justify-content: center;
  gap: 20px;
}

.viewer-control {
  background-color: rgba(255, 255, 255, 0.2);
  border: none;
  color: white;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.viewer-control:hover {
  background-color: rgba(255, 255, 255, 0.4);
}

/* 按钮样式 */
.btn {
  display: inline-block;
  padding: 8px 16px;
  border-radius: 4px;
  text-decoration: none;
  transition: all 0.3s ease;
  font-weight: 500;
}

.btn-primary {
  background-color: #1f2a24;
  color: white;
}

.btn-primary:hover {
  background-color: #8a6a33;
}

/* 票务排期区 */
.ticket-section {
  margin: 40px 0;
  padding: 28px;
  background: #fffdf9;
  border: 1px solid #e9e2d4;
  border-radius: 16px;
  box-shadow: 0 8px 30px rgba(60, 50, 30, 0.06);
}

.ticket-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.session-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
  gap: 14px;
}

.session-card {
  background: linear-gradient(135deg, #fdfaf2, #f7f0df);
  border: 1px solid #e5d9bd;
  border-radius: 12px;
  padding: 16px;
  transition: all 0.25s ease;
}

.session-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 24px rgba(140, 110, 60, 0.12);
}

.session-card.soldout { opacity: 0.6; }

.session-time { font-weight: 600; color: #332e26; margin-bottom: 10px; }

.session-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.session-price { font-size: 20px; font-weight: 700; color: #a3761f; }

.session-remain { font-size: 13px; color: #6f6553; }
.session-remain.low { color: #c0503c; font-weight: 600; }

.session-tip { font-size: 12px; color: #9a8f7d; }

.ai-link { color: #8a6a33; font-weight: 600; text-decoration: underline; }

.ai-book-btn {
  background: #1f2a24;
  border-color: #1f2a24;
  color: #f5efe4;
}

.ai-book-btn:hover {
  background: #8a6a33;
  border-color: #8a6a33;
  color: #fff;
}

/* 响应式调整 */
@media (max-width: 992px) {
  .exhibition-detail {
    flex-direction: column;
  }
  
  .exhibition-images {
    flex: 0 0 auto;
    margin-bottom: 30px;
  }
}

@media (max-width: 768px) {
  .artwork-grid,
  .exhibition-grid {
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  }
}

@media (max-width: 576px) {
  .artwork-grid,
  .exhibition-grid {
    grid-template-columns: 1fr;
  }
}
</style>