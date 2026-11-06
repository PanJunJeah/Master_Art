<template>
  <div class="artwork-detail-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="container">
        <h1 class="page-title">{{ artwork?.name }}</h1>
        <p class="page-description">{{ artwork?.artist?.name }} | {{ artwork?.creationYear }}</p>
        <el-button round class="ask-ai-btn" @click="askAi">🤖 让 AI 讲讲这幅画</el-button>
      </div>
    </div>

    <!-- 主要内容 -->
    <div class="container">
      <div class="artwork-detail">
        <!-- 左侧：艺术品图片 -->
        <div class="artwork-images">
          <img 
            :src="currentImage || 'https://via.placeholder.com/600x400?text=艺术品图片'" 
            class="main-image" 
            alt="artwork?.name"
            @click="openImageViewer(currentImage)"
          >
          <div class="image-gallery" v-if="artwork && artwork.images && artwork.images.length > 1">
            <img 
              v-for="(image, index) in artwork.images" 
              :key="index"
              :src="image" 
              class="gallery-thumbnail" 
              :class="{ active: currentImage === image }"
              @click="currentImage = image"
              :alt="`${artwork?.name} - 图片 ${index + 1}`"
            >
          </div>
          
          <!-- 3D模型查看器 -->
          <div class="model-viewer" v-if="artwork && artwork.modelUrl">
            <model-viewer
              :src="artwork.modelUrl"
              auto-rotate
              camera-controls
              shadow-intensity="1"
              :alt="artwork?.name"
             model-url="">
            </model-viewer>
          </div>
        </div>

        <!-- 右侧：艺术品信息 -->
        <div class="artwork-info">
          <h2 class="artwork-title">{{ artwork?.name }}</h2>
          
          <div class="artwork-meta">
            <div class="artwork-meta-item">
              <span class="meta-label">艺术家：</span>
              <router-link v-if="artwork && artwork.artist" :to="`/artists/${artwork.artist.id}`">{{ artwork.artist?.name || '未知' }}</router-link>
              <span v-else>未知</span>
            </div>
            <div class="artwork-meta-item">
              <span class="meta-label">创作年代：</span>
              <span>{{ artwork.creationYear || '未知' }}</span>
            </div>
            <div class="artwork-meta-item">
              <span class="meta-label">材质：</span>
              <span>{{ artwork.medium || '未知' }}</span>
            </div>
            <div class="artwork-meta-item">
              <span class="meta-label">尺寸：</span>
              <span>{{ artwork.dimensions || '未知' }}</span>
            </div>
            <div class="artwork-meta-item">
              <span class="meta-label">流派：</span>
              <span>{{ artwork.style || '未知' }}</span>
            </div>
            <div class="artwork-meta-item">
              <span class="meta-label">收藏地：</span>
              <span>{{ artwork.location || '未知' }}</span>
            </div>
          </div>
          
          <div class="artwork-description">
            <h3>作品描述</h3>
            <p>{{ artwork.description }}</p>
          </div>
          
          <!-- 社交分享 -->
          <div class="social-share">
            <h3>分享作品</h3>
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
      
      <!-- 讲解系统 -->
      <div class="audio-guide" v-if="artwork.audioGuideUrl">
        <h3>语音讲解</h3>
        <div class="audio-controls">
          <el-button type="primary" @click="toggleAudio">
            <el-icon>
              <component :is="isPlaying ? VideoPause : ArrowRight"/>
            </el-icon>
            {{ isPlaying ? '暂停' : '播放' }}
          </el-button>
          <el-slider v-model="audioProgress" class="audio-progress" @change="seekAudio" :disabled="!isPlaying"></el-slider>
          <span class="audio-time">{{ formatTime(currentTime) }} / {{ formatTime(duration) }}</span>
        </div>
      </div>
      
      <!-- 相关艺术品 -->
      <div class="related-artworks" v-if="relatedArtworks.length > 0">
        <h2 class="section-title">相关艺术品</h2>
        <div class="artwork-grid">
          <div v-for="relatedArtwork in relatedArtworks" :key="relatedArtwork.id" class="artwork-card">
            <div class="card-image-container">
              <img :src="relatedArtwork.mainImageUrl || 'https://via.placeholder.com/300x200?text=艺术品图片'" class="card-image" :alt="relatedArtwork.name">
            </div>
            <div class="card-content">
              <h3 class="card-title">{{ relatedArtwork.name }}</h3>
              <p class="card-description">{{ truncateText(relatedArtwork.description, 100) }}</p>
              <div class="card-meta">
                <span>{{ relatedArtwork.artist?.name || '未知艺术家' }}</span>
                <span>{{ relatedArtwork.creationYear || '未知年代' }}</span>
              </div>
              <router-link :to="`/artworks/${relatedArtwork.id}`" class="btn btn-primary card-btn">查看详情</router-link>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 图片查看器 -->
    <div class="image-viewer-overlay" v-if="showImageViewer" @click="closeImageViewer">
      <div class="image-viewer-content" @click.stop>
        <img :src="viewerImage" class="viewer-image" :alt="artwork?.name">
        <div class="viewer-close" @click="closeImageViewer">
          <el-icon><Close /></el-icon>
        </div>
        <div class="viewer-controls" v-if="artwork.images && artwork.images.length > 1">
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
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useArtworkStore } from '../stores/artwork'
import { useArtistStore } from '../stores/artist'
import { ElMessage } from 'element-plus'
import { Picture, Close, ArrowLeft, ArrowRight, VideoPause } from '@element-plus/icons-vue'
import { Icon } from '@iconify/vue'
import { Howl } from 'howler'
import '@google/model-viewer'
import request from "@/utils/request.js";
import ModelViewer from "@/components/ModelViewer.vue";

const route = useRoute()
const router = useRouter()
const artworkStore = useArtworkStore()
const artistStore = useArtistStore()

// 艺术品数据
const artwork = ref({})
const relatedArtworks = ref([])
const loading = ref(true)
const currentImage = ref('')

// 问 AI：跳转到智能策展助手并自动提问
const askAi = () => {
  const name = artwork.value?.name || '这幅作品'
  router.push({
    path: '/ai-art-guide',
    query: { q: `请给我讲讲《${name}》的创作背景、故事和艺术特点` }
  })
}

// 图片查看器
const showImageViewer = ref(false)
const viewerImage = ref('')
const currentImageIndex = ref(0)

// 音频讲解
const audioPlayer = ref(null)
const isPlaying = ref(false)
const audioProgress = ref(0)
const currentTime = ref(0)
const duration = ref(0)

// 获取艺术品详情
const fetchArtworkDetail = async (id) => {
  loading.value = true
  try {
    const res = await request({
      url: `/api/artworks/${id}`,
      method: "get",
      headers: { "Content-Type": "application/json" },
      params: {
        // userId: 2,
        // sessionId: aiResponse.sessionId
      }
    })
    // console.log(res.content)
    if (res) {
      artwork.value = res;
    } else {
      artwork.value = {};
    }
    loading.value = false
  } catch (error) {
    console.log(error)
    loading.value = false
  }
}



// 初始化音频播放器
const initAudioPlayer = (url) => {
  audioPlayer.value = new Howl({
    src: [url],
    html5: true,
    onplay: () => {
      isPlaying.value = true
      requestAnimationFrame(updateAudioProgress)
    },
    onpause: () => {
      isPlaying.value = false
    },
    onend: () => {
      isPlaying.value = false
      audioProgress.value = 100
    },
    onload: () => {
      duration.value = audioPlayer.value.duration()
    }
  })
}

// 更新音频进度
const updateAudioProgress = () => {
  if (audioPlayer.value && isPlaying.value) {
    currentTime.value = audioPlayer.value.seek()
    audioProgress.value = (currentTime.value / duration.value) * 100
    requestAnimationFrame(updateAudioProgress)
  }
}

// 播放/暂停音频
const toggleAudio = () => {
  if (!audioPlayer.value) return
  
  if (isPlaying.value) {
    audioPlayer.value.pause()
  } else {
    audioPlayer.value.play()
  }
}

// 跳转音频进度
const seekAudio = (value) => {
  if (!audioPlayer.value) return
  
  const seekTime = (value / 100) * duration.value
  audioPlayer.value.seek(seekTime)
  currentTime.value = seekTime
}

// 格式化时间
const formatTime = (seconds) => {
  if (!seconds) return '0:00'
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins}:${secs < 10 ? '0' + secs : secs}`
}

// 打开图片查看器
const openImageViewer = (image) => {
  viewerImage.value = image
  showImageViewer.value = true
  
  if (artwork.value.images) {
    currentImageIndex.value = artwork.value.images.findIndex(img => img === image)
    if (currentImageIndex.value === -1) currentImageIndex.value = 0
  }
}

// 关闭图片查看器
const closeImageViewer = () => {
  showImageViewer.value = false
}

// 上一张图片
const prevImage = () => {
  if (!artwork.value.images || artwork.value.images.length <= 1) return
  
  currentImageIndex.value = (currentImageIndex.value - 1 + artwork.value.images.length) % artwork.value.images.length
  viewerImage.value = artwork.value.images[currentImageIndex.value]
}

// 下一张图片
const nextImage = () => {
  if (!artwork.value.images || artwork.value.images.length <= 1) return
  
  currentImageIndex.value = (currentImageIndex.value + 1) % artwork.value.images.length
  viewerImage.value = artwork.value.images[currentImageIndex.value]
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
  const text = encodeURIComponent(`${artwork.value.name} - ${artwork.value.artist?.name || '未知艺术家'}`)
  window.open(`https://twitter.com/intent/tweet?url=${url}&text=${text}`, '_blank')
}

const shareOnPinterest = () => {
  const url = encodeURIComponent(window.location.href)
  const media = encodeURIComponent(artwork.value.mainImageUrl || '')
  const description = encodeURIComponent(`${artwork.value.name} - ${artwork.value.artist?.name || '未知艺术家'}`)
  window.open(`https://pinterest.com/pin/create/button/?url=${url}&media=${media}&description=${description}`, '_blank')
}


onMounted(() => {
  const id = route.params.id
  fetchArtworkDetail(id)

})
</script>

<style scoped>
.artwork-detail {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 30px;
  margin-bottom: 50px;
}

.artwork-images {
  position: relative;
}

.main-image {
  width: 100%;
  border-radius: 8px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  cursor: zoom-in;
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
  height: 80px;
  object-fit: cover;
  border-radius: 4px;
  cursor: pointer;
  transition: transform 0.3s ease, opacity 0.3s ease;
  opacity: 0.7;
}

.gallery-thumbnail:hover, .gallery-thumbnail.active {
  transform: translateY(-5px);
  opacity: 1;
}

.artwork-info {
  padding: 20px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.artwork-title {
  font-size: 24px;
  margin-bottom: 20px;
  color: #333;
}

.artwork-meta {
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.artwork-meta-item {
  display: flex;
  margin-bottom: 10px;
}

.meta-label {
  font-weight: bold;
  width: 100px;
  color: #666;
}

.artwork-description h3 {
  font-size: 18px;
  margin-bottom: 10px;
  color: #333;
}

.artwork-description p {
  line-height: 1.8;
  color: #555;
  margin-bottom: 20px;
}

/* 社交分享 */
.social-share {
  margin-top: 30px;
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

/* 讲解系统 */
.audio-guide {
  margin-top: 30px;
  padding: 20px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.audio-guide h3 {
  font-size: 18px;
  margin-bottom: 15px;
  color: #333;
}

.audio-controls {
  display: flex;
  align-items: center;
  gap: 15px;
}

.audio-progress {
  flex-grow: 1;
}

.audio-time {
  font-size: 14px;
  color: #666;
  min-width: 80px;
  text-align: right;
}

/* 相关艺术品 */
.related-artworks {
  margin-top: 50px;
}

.artwork-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 30px;
  margin-top: 30px;
}

/* 图片查看器 */
.image-viewer-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.9);
  z-index: 2000;
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
  object-fit: contain;
}

.viewer-close {
  position: absolute;
  top: -40px;
  right: 0;
  color: white;
  font-size: 30px;
  cursor: pointer;
  transition: color 0.3s ease;
}

.viewer-close:hover {
  color: #8a6a33;
}

.viewer-controls {
  position: absolute;
  bottom: -50px;
  left: 0;
  width: 100%;
  display: flex;
  justify-content: center;
  gap: 20px;
}

.viewer-control {
  color: white;
  background-color: rgba(255, 255, 255, 0.2);
  border: none;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.viewer-control:hover {
  background-color: rgba(255, 255, 255, 0.3);
}

/* 3D模型查看器 */
.model-viewer {
  width: 100%;
  height: 400px;
  background-color: #f0f0f0;
  border-radius: 8px;
  margin-top: 30px;
  overflow: hidden;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .artwork-detail {
    grid-template-columns: 1fr;
  }
  
  .artwork-images {
    margin-bottom: 20px;
  }
  
  .audio-controls {
    flex-direction: column;
    align-items: stretch;
  }
  
  .audio-time {
    text-align: center;
    margin-top: 10px;
  }
}
</style>