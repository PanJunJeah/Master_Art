<template>
  <div class="artist-dashboard">
    <el-card class="welcome-card">
      <template #header>
        <div class="card-header">
          <h2>{{ $t('artist.welcomeTitle') || '欢迎使用艺术家中心' }}</h2>
        </div>
      </template>
      <div class="welcome-content">
        <p>{{ $t('artist.welcomeMessage') || '您已成功登录艺术家中心，可以管理您的作品和查看统计数据。' }}</p>
      </div>
    </el-card>

    <div class="dashboard-stats">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="stat-card">
            <div class="stat-icon">
              <el-icon><Picture /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.artworksCount || 0 }}</div>
              <div class="stat-label">{{ $t('artist.totalArtworks') || '作品总数' }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="stat-card">
            <div class="stat-icon">
              <el-icon><View /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.viewsCount || 0 }}</div>
              <div class="stat-label">{{ $t('artist.totalViews') || '总浏览量' }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="stat-card">
            <div class="stat-icon">
              <el-icon><Star /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.favoritesCount || 0 }}</div>
              <div class="stat-label">{{ $t('artist.totalFavorites') || '总收藏数' }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="stat-card">
            <div class="stat-icon">
              <el-icon><ChatDotRound /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.commentsCount || 0 }}</div>
              <div class="stat-label">{{ $t('artist.totalComments') || '总评论数' }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <el-card class="recent-artworks">
      <template #header>
        <div class="card-header">
          <h3>{{ $t('artist.recentArtworks') || '最近上传的作品' }}</h3>
          <router-link to="/ArtistArtworks" class="view-all">
            {{ $t('common.more') || '查看全部' }}
            <el-icon><ArrowRight /></el-icon>
          </router-link>
        </div>
      </template>
      <div v-if="recentArtworks.length > 0" class="artwork-list">
        <el-row :gutter="20">
          <el-col v-for="artwork in recentArtworks" :key="artwork.id" :xs="24" :sm="12" :md="8" :lg="6">
            <el-card class="artwork-card" shadow="hover" :body-style="{ padding: '0px' }">
              <div class="artwork-image">
                <img :src="artwork.imageUrl" :alt="artwork.title">
              </div>
              <div class="artwork-info">
                <h4>{{ artwork.title }}</h4>
                <p class="artwork-date">{{ artwork.creationYear }}</p>
                <div class="artwork-stats">
                  <span><el-icon><View /></el-icon> {{ artwork.views || 0 }}</span>
                  <span><el-icon><Star /></el-icon> {{ artwork.favorites || 0 }}</span>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
      <div v-else class="no-data">
        {{ $t('artist.noArtworks') || '暂无作品，点击"上传新作品"按钮创建' }}
        <router-link to="/artist/artworks/create" class="upload-btn">
          <el-button type="primary">
            <el-icon><Plus /></el-icon>
            {{ $t('artist.uploadNewArtwork') || '上传新作品' }}
          </el-button>
        </router-link>
      </div>
    </el-card>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Picture, View, Star, ChatDotRound, ArrowRight, Plus } from '@element-plus/icons-vue'
import { getArtistStats, getRecentArtworks } from '@/api/artist'

export default {
  name: 'ArtistDashboard',
  components: {
    Picture,
    View,
    Star,
    ChatDotRound,
    ArrowRight,
    Plus
  },
  setup() {
    const router = useRouter()
    const loading = ref(false)
    const stats = ref({
      artworksCount: 0,
      viewsCount: 0,
      favoritesCount: 0,
      commentsCount: 0
    })
    const recentArtworks = ref([])

    // 获取艺术家统计数据
    const fetchArtistStats = async () => {
      loading.value = true
      try {
        const response = await getArtistStats()
        if (response && response.data) {
          stats.value = response.data
        }
      } catch (error) {
        console.error('获取艺术家统计数据失败:', error)
        ElMessage.error('获取统计数据失败，请稍后重试')
      } finally {
        loading.value = false
      }
    }

    // 获取最近上传的作品
    const fetchRecentArtworks = async () => {
      try {
        const response = await getRecentArtworks({ limit: 4 })
        if (response && response.data) {
          recentArtworks.value = response.data
        }
      } catch (error) {
        console.error('获取最近作品失败:', error)
        ElMessage.error('获取最近作品失败，请稍后重试')
      }
    }

    // 查看作品详情
    const viewArtworkDetail = (artworkId) => {
      router.push(`/artworks/${artworkId}`)
    }

    onMounted(() => {
      fetchArtistStats()
      fetchRecentArtworks()
    })

    return {
      loading,
      stats,
      recentArtworks,
      viewArtworkDetail
    }
  }
}
</script>

<style scoped>
.artist-dashboard {
  padding: 20px;
}

.welcome-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.welcome-content {
  padding: 10px 0;
}

.dashboard-stats {
  margin-bottom: 20px;
}

.stat-card {
  height: 100%;
  display: flex;
  padding: 20px;
  margin-bottom: 20px;
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background-color: #f0f9ff;
  margin-right: 15px;
  font-size: 24px;
  color: #409eff;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 5px;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.recent-artworks {
  margin-bottom: 20px;
}

.view-all {
  display: flex;
  align-items: center;
  font-size: 14px;
  color: #409eff;
  text-decoration: none;
}

.view-all .el-icon {
  margin-left: 5px;
}

.artwork-list {
  margin-top: 15px;
}

.artwork-card {
  margin-bottom: 20px;
  transition: transform 0.3s;
  height: 100%;
}

.artwork-card:hover {
  transform: translateY(-5px);
}

.artwork-image {
  height: 180px;
  overflow: hidden;
}

.artwork-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.artwork-card:hover .artwork-image img {
  transform: scale(1.05);
}

.artwork-info {
  padding: 15px;
}

.artwork-info h4 {
  margin: 0 0 10px 0;
  font-size: 16px;
  font-weight: 500;
}

.artwork-date {
  font-size: 14px;
  color: #909399;
  margin-bottom: 10px;
}

.artwork-stats {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #606266;
}

.artwork-stats span {
  display: flex;
  align-items: center;
}

.artwork-stats .el-icon {
  margin-right: 5px;
}

.no-data {
  text-align: center;
  padding: 30px 0;
  color: #909399;
}

.upload-btn {
  display: block;
  margin: 15px auto 0;
  text-decoration: none;
}

@media (max-width: 768px) {
  .stat-card {
    padding: 15px;
  }
  
  .stat-icon {
    width: 40px;
    height: 40px;
    font-size: 20px;
  }
  
  .stat-value {
    font-size: 20px;
  }
  
  .artwork-image {
    height: 150px;
  }
}
</style>