<template>
  <div class="artworks-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="container">
        <h1 class="page-title">艺术品</h1>
        <p class="page-description">探索世界各地的艺术珍品，感受艺术的魅力</p>
      </div>
    </div>

    <!-- 主要内容 -->
    <div class="container">
      <!-- 过滤器 -->
      <div class="filter-section">
        <el-form :inline="true" class="filter-form">
          <el-form-item label="艺术流派">
            <el-select v-model="filters.style" placeholder="选择流派" clearable @change="applyFilters">
              <el-option v-for="item in styleOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="创作时期">
            <el-select v-model="filters.period" placeholder="选择时期" clearable @change="applyFilters">
              <el-option v-for="item in periodOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="艺术家">
            <el-select v-model="filters.artist" placeholder="选择艺术家" clearable @change="applyFilters">
              <el-option v-for="item in artistOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>

          <!-- 按钮容器 -->
          <div class="filter-buttons">
            <el-button type="primary" @click="applyFilters">筛选</el-button>
            <el-button @click="resetFilters">重置</el-button>
          </div>
        </el-form>
      </div>

      <!-- 视图切换 -->
      <div class="view-controls">
        <div class="view-mode">
          <el-radio-group v-model="viewMode" size="large">
            <el-radio-button label="grid">
              <el-icon><Grid /></el-icon> 网格视图
            </el-radio-button>
            <el-radio-button label="list">
              <el-icon><List /></el-icon> 列表视图
            </el-radio-button>
          </el-radio-group>
        </div>
        <div class="sort-control">
          <el-select v-model="sortBy" placeholder="排序方式" @change="applyFilters">
            <el-option label="创作时间 (新到旧)" value="year_desc" />
            <el-option label="创作时间 (旧到新)" value="year_asc" />
            <el-option label="人气 (高到低)" value="popularity_desc" />
            <el-option label="名称 (A-Z)" value="name_asc" />
          </el-select>
        </div>
      </div>

      <!-- 艺术品列表 - 网格视图 -->
      <div v-if="viewMode === 'grid'" class="artworks-grid" v-loading="loading">
        <el-row :gutter="30">
          <template v-if="artworks.length > 0">
            <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="artwork in artworks" :key="artwork.id">
              <el-card class="artwork-card" shadow="hover" :body-style="{ padding: '0px' }">
                <div class="card-image-container">
                  <img
                      :src="artwork.mainImageUrl || 'https://via.placeholder.com/300x300?text=艺术品图片'"
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
                    <span>{{ artwork.artist?.name || '未知艺术家' }}</span>
                    <span>{{ artwork.creationYear || '未知年代' }}</span>
                  </div>
                  <router-link :to="`/artworks/${artwork.id}`" class="btn btn-primary card-btn">查看详情</router-link>
                </div>
              </el-card>
            </el-col>
          </template>
          <el-col :span="24" v-else>
            <el-empty description="暂无符合条件的艺术品" />
          </el-col>
        </el-row>
      </div>

      <!-- 艺术品列表 - 列表视图 -->
      <div v-else class="artworks-list" v-loading="loading">
        <template v-if="artworks.length > 0">
          <el-card v-for="artwork in artworks" :key="artwork.id" class="list-item" shadow="hover">
            <div class="list-item-content">
              <div class="list-item-image" @click="openImagePreview(artwork.mainImageUrl)">
                <img
                    :src="artwork.mainImageUrl || 'https://via.placeholder.com/300x300?text=艺术品图片'"
                    :alt="artwork.name"
                >
                <div class="image-zoom-hint">
                  <el-icon><ZoomIn /></el-icon>
                </div>
              </div>
              <div class="list-item-info">
                <h3 class="list-item-title">{{ artwork.name }}</h3>
                <div class="list-item-artist">艺术家: {{ artwork.artist?.name || '未知艺术家' }}</div>
                <div class="list-item-details">
                  <div><strong>创作年代:</strong> {{ artwork.creationYear || '未知' }}</div>
                  <div><strong>材质:</strong> {{ artwork.medium || '未知' }}</div>
                  <div><strong>尺寸:</strong> {{ artwork.dimensions || '未知' }}</div>
                  <div><strong>流派:</strong> {{ artwork.style || '未知' }}</div>
                </div>
                <p class="list-item-description">{{ truncateText(artwork.description, 200) }}</p>
                <router-link :to="`/artworks/${artwork.id}`" class="btn btn-primary">查看详情</router-link>
              </div>
            </div>
          </el-card>
        </template>
        <el-empty v-else description="暂无符合条件的艺术品" />
      </div>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[12, 24, 36, 48]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="totalArtworks"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 图片预览 -->
    <el-image-viewer
        v-if="showImageViewer"
        :url-list="[previewImage]"
        :initial-index="0"
        @close="showImageViewer = false"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useArtworkStore } from '../stores/index.js'
import { Grid, List, ZoomIn } from '@element-plus/icons-vue'

const artworkStore = useArtworkStore()
const artworks = computed(() => artworkStore.artworks)
const totalArtworks = computed(() => artworkStore.totalArtworks)
const loading = computed(() => artworkStore.loading)

// 分页相关
const currentPage = ref(1)
const pageSize = ref(12)

// 视图模式
const viewMode = ref('grid')
const sortBy = ref('popularity_desc')

// 筛选条件
const filters = reactive({
  style: null,
  period: null,
  artist: null
})

// 图片预览
const showImageViewer = ref(false)
const previewImage = ref('')

// 筛选选项 - 从store获取
const styleOptions = computed(() => {
  return artworkStore.styleOptions.map(style => ({ value: style, label: style }))
})

const periodOptions = computed(() => {
  return artworkStore.periodOptions.map(period => ({ value: period, label: period }))
})

const artistOptions = computed(() => {
  return artworkStore.artistOptions
})

// 生成筛选选项
async function generateFilterOptions() {
  try {
    // 并行加载所有选项
    await Promise.all([
      artworkStore.fetchStyleOptions(),
      artworkStore.fetchPeriodOptions(),
      artworkStore.fetchArtistOptions()
    ])

    console.log('加载的艺术流派选项:', artworkStore.styleOptions)
    console.log('加载的创作时期选项:', artworkStore.periodOptions)
    console.log('加载的艺术家选项:', artworkStore.artistOptions)

  } catch (error) {
    console.error('获取筛选选项失败:', error)
  }
}

// 加载艺术品数据
async function loadArtworks() {
  try {
    // 构建请求参数
    const requestParams = {
      page: currentPage.value,
      pageSize: pageSize.value,
      sort: sortBy.value,
      style: filters.style,
      period: filters.period,
      artist: filters.artist
    };

    console.log('发送艺术品请求参数:', requestParams);

    // 调用 store 方法
    await artworkStore.fetchArtworks(requestParams);

    // 更新分页状态
    artworkStore.setPagination(currentPage.value, pageSize.value);

  } catch (error) {
    console.error('加载艺术品数据失败:', error);
    // 错误提示
    window.$message && window.$message.error('加载艺术品数据失败，请稍后重试');
  }
}

// 应用过滤器
function applyFilters() {
  currentPage.value = 1
  loadArtworks()
}

// 重置过滤器
function resetFilters() {
  Object.keys(filters).forEach(key => {
    filters[key] = null
  })
  sortBy.value = 'popularity_desc'
  currentPage.value = 1
  loadArtworks()
}

// 处理页码变化
function handleCurrentChange(val) {
  currentPage.value = val
  loadArtworks()
}

// 处理每页显示数量变化
function handleSizeChange(val) {
  pageSize.value = val
  currentPage.value = 1
  loadArtworks()
}

// 打开图片预览
function openImagePreview(imageUrl) {
  if (!imageUrl) return
  previewImage.value = imageUrl
  showImageViewer.value = true
}

// 工具函数：截断文本
function truncateText(text, length) {
  if (!text) return '暂无描述'
  return text.length > length ? text.substring(0, length) + '...' : text
}

// 初始化数据
onMounted(async () => {
  await generateFilterOptions()
  await loadArtworks()
})
</script>

<style scoped>
.artworks-page {
  padding-bottom: 60px;
}

.page-header {
  background: linear-gradient(135deg, #1f2a24 0%, #2c3a32 55%, #3a4638 100%);
  color: white;
  padding: 60px 0;
  margin-bottom: 40px;
  text-align: center;
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

/* 过滤器样式 */
.filter-section {
  background-color: white;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 30px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

/* 新增筛选器样式 */
.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
}

.el-form-item {
  margin-bottom: 0;
}

/* 筛选框宽度 */
.el-select {
  min-width: 200px; /* 设置最小宽度 */
}

/* 按钮容器 */
.filter-buttons {
  display: flex;
  gap: 10px;
}

/* 响应式调整 */
@media (max-width: 992px) {
  .el-select {
    min-width: 180px;
  }
}

@media (max-width: 768px) {
  .filter-form {
    flex-direction: column;
    align-items: flex-start;
  }

  .el-select {
    width: 100%;
    min-width: unset;
  }

  .filter-buttons {
    width: 100%;
    justify-content: flex-start;
    margin-top: 10px;
  }
}

/* 视图控制 */
.view-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

/* 网格视图样式 */
.artworks-grid {
  margin-bottom: 30px;
}

.artwork-card {
  margin-bottom: 30px;
  border-radius: 8px;
  overflow: hidden;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.artwork-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.15);
}

.card-image-container {
  height: 250px;
  overflow: hidden;
  position: relative;
  cursor: pointer;
}

.card-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.artwork-card:hover .card-image {
  transform: scale(1.05);
}

.image-zoom-hint {
  position: absolute;
  top: 10px;
  right: 10px;
  background-color: rgba(0, 0, 0, 0.5);
  color: white;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.card-image-container:hover .image-zoom-hint,
.list-item-image:hover .image-zoom-hint {
  opacity: 1;
}

.card-content {
  padding: 20px;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
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
  font-size: 14px;
  color: #666;
  margin-bottom: 15px;
  min-height: 60px;
}

.card-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #888;
  margin-bottom: 15px;
}

.card-btn {
  display: block;
  width: 100%;
  text-align: center;
}

/* 列表视图样式 */
.artworks-list {
  margin-bottom: 30px;
}

.list-item {
  margin-bottom: 20px;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.list-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.15);
}

.list-item-content {
  display: flex;
  align-items: flex-start;
}

.list-item-image {
  width: 200px;
  height: 200px;
  flex-shrink: 0;
  margin-right: 20px;
  overflow: hidden;
  position: relative;
  cursor: pointer;
}

.list-item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.list-item:hover .list-item-image img {
  transform: scale(1.05);
}

.list-item-info {
  flex: 1;
}

.list-item-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 5px;
  color: #333;
}

.list-item-artist {
  font-size: 16px;
  color: #666;
  margin-bottom: 10px;
}

.list-item-details {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  margin-bottom: 15px;
  font-size: 14px;
  color: #666;
}

.list-item-description {
  margin-bottom: 15px;
  font-size: 14px;
  color: #666;
  line-height: 1.5;
}

/* 分页容器 */
.pagination-container {
  text-align: center;
  margin-top: 30px;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .filter-form {
    display: flex;
    flex-direction: column;
  }

  .filter-form .el-form-item {
    margin-right: 0;
    margin-bottom: 15px;
  }

  .view-controls {
    flex-direction: column;
    align-items: flex-start;
  }

  .sort-control {
    margin-top: 15px;
    width: 100%;
  }

  .list-item-content {
    flex-direction: column;
  }

  .list-item-image {
    width: 100%;
    margin-right: 0;
    margin-bottom: 15px;
  }

  .list-item-details {
    grid-template-columns: 1fr;
  }

  .page-header {
    padding: 40px 0;
  }

  .page-title {
    font-size: 28px;
  }

  .page-description {
    font-size: 16px;
  }
}
</style>