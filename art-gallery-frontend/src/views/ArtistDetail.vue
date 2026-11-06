<template>
  <div class="artist-detail-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="container">
        <h1 class="page-title">{{ artist?.name }}</h1>
        <p class="page-description">{{ formatLifespan(artist) }} | {{ artist?.nationality }}</p>
        <el-button round class="ask-ai-btn" @click="askAi">🤖 让 AI 介绍这位艺术家</el-button>
      </div>
    </div>

    <!-- 主要内容 -->
    <div class="container">
      <div class="artist-detail">
        <!-- 左侧：艺术家信息 -->
        <div class="artist-info">
          <div class="artist-portrait-container">
            <img 
              :src="artist?.portraitUrl || 'https://via.placeholder.com/400x500?text=艺术家肖像'" 
              class="artist-portrait" 
              :alt="artist?.name"
            >
          </div>
          
          <div class="artist-meta">
            <div class="artist-meta-item">
              <span class="meta-label">国籍：</span>
              <span>{{ artist?.nationality || '未知' }}</span>
            </div>
            <div class="artist-meta-item">
              <span class="meta-label">生卒年：</span>
              <span>{{ formatLifespan(artist) }}</span>
            </div>
            <div class="artist-meta-item">
              <span class="meta-label">主要流派：</span>
              <span>{{ artist?.mainStyle || '未知' }}</span>
            </div>
            <div class="artist-meta-item">
              <span class="meta-label">代表作品：</span>
              <span>{{ artist?.masterpiece || '未知' }}</span>
            </div>
          </div>
          
          <!-- 社交分享 -->
          <div class="social-share">
            <h3>分享艺术家</h3>
            <div class="share-buttons">
              <a href="#" class="share-button share-facebook" @click.prevent="shareOnFacebook">
                <el-icon><Share /></el-icon>
              </a>
              <a href="#" class="share-button share-twitter" @click.prevent="shareOnTwitter">
                <el-icon><MessageBox /></el-icon>
              </a>
              <a href="#" class="share-button share-pinterest" @click.prevent="shareOnPinterest">
                <el-icon><Picture /></el-icon>
              </a>
            </div>
          </div>
        </div>
        
        <!-- 右侧：艺术家生平 -->
        <div class="artist-biography">
          <h2>艺术家生平</h2>
          <div class="biography-content">
            <p>{{ artist?.biography }}</p>
          </div>
          
          <!-- 艺术家时间轴 -->
          <div class="artist-timeline">
            <h3>艺术生涯</h3>
            <el-timeline>
              <el-timeline-item 
                v-for="(event, index) in artist?.lifeEvents" 
                :key="index"
                :timestamp="event.year" 
                placement="top"
                :type="getTimelineItemType(index)"
              >
                <el-card>
                  <h4>{{ event.title }}</h4>
                  <p>{{ event.description }}</p>
                </el-card>
              </el-timeline-item>
            </el-timeline>
          </div>
        </div>
      </div>
      
      <!-- 艺术家作品 -->
      <div class="artist-artworks">
        <h2 class="section-title">代表作品</h2>
        
        <!-- 流派筛选 -->
        <div class="style-filter">
          <span class="filter-label">按流派筛选：</span>
          <el-radio-group v-model="selectedStyle" @change="filterArtworksByStyle">
            <el-radio-button label="all">全部</el-radio-button>
            <el-radio-button 
              v-for="style in artistStyles" 
              :key="style" 
              :label="style"
            >
              {{ style }}
            </el-radio-button>
          </el-radio-group>
        </div>
        
        <!-- 作品展示 -->
        <div class="artwork-grid" v-loading="loading">
          <template v-if="filteredArtworks.length > 0">
            <div v-for="artwork in filteredArtworks" :key="artwork.id" class="artwork-card">
              <div class="card-image-container">
                <img 
                  :src="artwork.mainImageUrl || 'https://via.placeholder.com/300x200?text=艺术品图片'" 
                  class="card-image" 
                  :alt="artwork.name"
                  @click="openImagePreview(artwork.mainImageUrl)"
                >
                <div class="image-zoom-hint">
                  <el-icon><ZoomIn /></el-icon>
                </div>
              </div>
              <div class="card-content">
                <h3 class="card-title">{{ artwork.name }}</h3>
                <div class="card-tags">
                  <el-tag size="small" type="info" v-if="artwork.style">{{ artwork.style }}</el-tag>
                  <el-tag size="small" type="success" v-if="artwork.medium">{{ artwork.medium }}</el-tag>
                </div>
                <p class="card-description">{{ truncateText(artwork.description, 100) }}</p>
                <div class="card-meta">
                  <span>{{ artwork.creationYear || '未知年代' }}</span>
                  <span>{{ artwork.location || '未知收藏地' }}</span>
                </div>
                <router-link :to="`/artworks/${artwork.id}`" class="btn btn-primary card-btn">查看详情</router-link>
              </div>
            </div>
          </template>
          <el-empty v-else description="暂无作品信息" />
        </div>
      </div>
      
      <!-- 相关艺术家 -->
      <div class="related-artists" v-if="relatedArtists.length > 0">
        <h2 class="section-title">相关艺术家</h2>
        <div class="artist-grid">
          <div v-for="relatedArtist in relatedArtists" :key="relatedArtist.id" class="artist-card">
            <div class="card-image-container">
              <img :src="relatedArtist.portraitUrl || 'https://via.placeholder.com/300x300?text=艺术家图片'" class="card-image" :alt="relatedArtist.name">
            </div>
            <div class="card-content">
              <h3 class="card-title">{{ relatedArtist.name }}</h3>
              <div class="card-tags">
                <el-tag size="small" type="info" v-if="relatedArtist.nationality">{{ relatedArtist.nationality }}</el-tag>
                <el-tag size="small" type="success" v-if="relatedArtist.mainStyle">{{ relatedArtist.mainStyle }}</el-tag>
              </div>
              <p class="card-description">{{ truncateText(relatedArtist.biography, 100) }}</p>
              <div class="card-meta">
                <span>{{ formatLifespan(relatedArtist) }}</span>
                <span>{{ relatedArtist.artworksCount || 0 }} 件作品</span>
              </div>
              <router-link :to="`/artists/${relatedArtist.id}`" class="btn btn-primary card-btn">查看详情</router-link>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 图片预览 -->
    <el-image-viewer
      v-if="showImageViewer"
      :url-list="[previewImage]"
      :initial-index="0"
      @close="closeImagePreview"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useArtistStore } from '../stores/artist'
import { useArtworkStore } from '../stores/artwork'
import { ElMessage, ElImageViewer } from 'element-plus'
import { Share, MessageBox, Picture, ZoomIn } from '@element-plus/icons-vue'
import request from "@/utils/request.js";

const route = useRoute()
const router = useRouter()
const artistStore = useArtistStore()
const artworkStore = useArtworkStore()

// 艺术家数据
const artist = ref({})
const artworks = ref([])
const relatedArtists = ref([])
const loading = ref(true)

// 问 AI：跳转到智能策展助手并自动提问
const askAi = () => {
  const name = artist.value?.name || '这位艺术家'
  router.push({
    path: '/ai-art-guide',
    query: { q: `请介绍一下艺术家「${name}」的生平、风格与代表作品` }
  })
}

// 筛选状态
const selectedStyle = ref('all')
const artistStyles = computed(() => {
  const styles = new Set()
  artworks.value.forEach(artwork => {
    if (artwork.style) styles.add(artwork.style)
  })
  return Array.from(styles)
})

// 筛选后的作品
const filteredArtworks = computed(() => {
  if (selectedStyle.value === 'all') return artworks.value
  return artworks.value.filter(artwork => artwork.style === selectedStyle.value)
})


// 获取艺术家详情
const fetchArtistDetail = async (id) => {
  loading.value = true
  try {
    const res = await request({
      url: `/api/artists/${id}`,
      method: "get",
      headers: { "Content-Type": "application/json" },
    })
    if (res) {
      artist.value = res;
    } else {
      artist.value = {}
      ElMessage.warning('未获取到艺术家详情')
    }
    loading.value = false
  } catch (error) {
    ElMessage.error('获取艺术家详情失败')
    artist.value = {}
  } finally {
    loading.value = false
  }
}

// 格式化生卒年
const formatLifespan = (artist) => {
  if (!artist) return '未知'
  let lifespan = ''
  if (artist.birthYear) lifespan += artist.birthYear
  lifespan += ' - '
  if (artist.deathYear) lifespan += artist.deathYear
  else if (artist.birthYear) lifespan += '至今'
  return lifespan || '未知'
}

// 获取时间轴项目类型
const getTimelineItemType = (index) => {
  const types = ['primary', 'success', 'warning', 'danger', 'info']
  return types[index % types.length]
}

// 筛选作品
const filterArtworksByStyle = () => {
  // 已通过计算属性实现
}

// 打开图片预览
const openImagePreview = (image) => {
  previewImage.value = image
  showImageViewer.value = true
}

// 关闭图片预览
const closeImagePreview = () => {
  showImageViewer.value = false
}

// 截断文本
const truncateText = (text, length) => {
  if (!text) return ''
  return text.length > length ? text.substring(0, length) + '...' : text
}

// 社交分享
const shareOnFacebook = () => {
  const url = encodeURIComponent(window.location.href)
  window.open(`https://www.facebook.com/sharer/sharer.php?u=${url}`, '_blank')
}

const shareOnTwitter = () => {
  const url = encodeURIComponent(window.location.href)
  const text = encodeURIComponent(`${artist.value.name} - 艺术家`)
  window.open(`https://twitter.com/intent/tweet?url=${url}&text=${text}`, '_blank')
}

const shareOnPinterest = () => {
  const url = encodeURIComponent(window.location.href)
  const media = encodeURIComponent(artist.value.portraitUrl || '')
  const description = encodeURIComponent(`${artist.value.name} - 艺术家`)
  window.open(`https://pinterest.com/pin/create/button/?url=${url}&media=${media}&description=${description}`, '_blank')
}



onMounted(() => {
  const id = route.params.id
  if (id) {
    fetchArtistDetail(id)
  }
})
</script>

<style scoped>
.artist-detail {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 30px;
  margin-bottom: 50px;
}

.artist-info {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.artist-portrait-container {
  width: 100%;
  overflow: hidden;
}

.artist-portrait {
  width: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.artist-portrait:hover {
  transform: scale(1.05);
}

.artist-meta {
  padding: 20px;
}

.artist-meta-item {
  display: flex;
  margin-bottom: 10px;
}

.meta-label {
  font-weight: bold;
  width: 100px;
  color: #666;
}

.social-share {
  padding: 0 20px 20px;
}

.social-share h3 {
  font-size: 18px;
  margin-bottom: 15px;
  color: #333;
}

.share-buttons {
  display: flex;
  gap: 15px;
}

.share-button {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  cursor: pointer;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.share-button:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
}

.artist-biography {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  padding: 20px;
}

.artist-biography h2 {
  font-size: 24px;
  margin-bottom: 20px;
  color: #333;
}

.biography-content {
  line-height: 1.8;
  color: #555;
  margin-bottom: 30px;
}

.artist-timeline {
  margin-top: 30px;
}

.artist-timeline h3 {
  font-size: 20px;
  margin-bottom: 20px;
  color: #333;
}

.artist-artworks {
  margin-top: 50px;
}

.style-filter {
  margin-bottom: 20px;
  background-color: white;
  padding: 15px;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 15px;
}

.filter-label {
  font-weight: bold;
  color: #666;
}

.artwork-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 30px;
  margin-top: 20px;
}

.related-artists {
  margin-top: 50px;
  margin-bottom: 50px;
}

.artist-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 30px;
  margin-top: 30px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .artist-detail {
    grid-template-columns: 1fr;
  }
  
  .style-filter {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>