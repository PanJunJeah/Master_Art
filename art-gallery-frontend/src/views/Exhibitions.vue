<template>
  <div class="exhibitions-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="container">
        <h1 class="page-title">展览</h1>
        <p class="page-description">探索世界各地的精彩艺术展览，感受艺术的魅力</p>
      </div>
    </div>

    <!-- 主要内容 -->
    <div class="container">
      <!-- 过滤器 -->
      <div class="filter-section">
        <el-form :inline="true" class="filter-form">
          <!-- 展览状态 -->
          <el-form-item label="展览状态">
            <el-select v-model="filters.status" placeholder="选择状态" clearable @change="applyFilters">
              <el-option label="正在进行" value="ongoing" />
              <el-option label="即将开始" value="upcoming" />
              <el-option label="已结束" value="past" />
            </el-select>
          </el-form-item>

          <!-- 展览地点 -->
          <el-form-item label="展览地点">
            <el-select v-model="filters.location" placeholder="选择地点" clearable @change="applyFilters">
              <el-option
                  v-for="item in locationOptions"
                  :key="item"
                  :label="item"
                  :value="item"
              />
            </el-select>
          </el-form-item>

          <!-- 展览主题 -->
          <el-form-item label="展览主题">
            <el-select v-model="filters.theme" placeholder="选择主题" clearable @change="applyFilters">
              <el-option
                  v-for="item in themeOptions"
                  :key="item"
                  :label="item"
                  :value="item"
              />
            </el-select>
          </el-form-item>

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
            <el-option label="开始日期 (近到远)" value="start_desc" />
            <el-option label="开始日期 (远到近)" value="start_asc" />
            <el-option label="结束日期 (近到远)" value="end_desc" />
            <el-option label="人气 (高到低)" value="popularity_desc" />
            <el-option label="名称 (A-Z)" value="name_asc" />
          </el-select>
        </div>
      </div>

      <!-- 展览列表 - 网格视图 -->
      <div v-if="viewMode === 'grid'" class="exhibitions-grid" v-loading="loading">
        <el-row :gutter="30">
          <template v-if="exhibitions.length > 0">
            <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="exhibition in exhibitions" :key="exhibition.id">
              <el-card class="exhibition-card" shadow="hover" :body-style="{ padding: '0px' }">
                <div class="card-image-container">
                  <img
                    :src="exhibition.coverImageUrl || exhibition.posterUrl || (exhibition.parsedImageUrls && exhibition.parsedImageUrls.length > 0 ? exhibition.parsedImageUrls[0] : 'https://via.placeholder.com/300x300?text=展览图片')"
                    class="card-image"
                    :alt="exhibition.name"
                    @click="openImagePreview(exhibition.coverImageUrl || exhibition.posterUrl || (exhibition.parsedImageUrls && exhibition.parsedImageUrls.length > 0 ? exhibition.parsedImageUrls[0] : null))"
                  >
                  <div class="image-zoom-hint">
                    <el-icon><ZoomIn /></el-icon>
                  </div>
                </div>
                <div class="card-content">
                  <h3 class="card-title">{{ exhibition.name }}</h3>
                  <div class="card-tags">
                    <el-tag size="small" type="success" v-if="isOngoing(exhibition)">正在进行</el-tag>
                    <el-tag size="small" type="warning" v-else-if="isUpcoming(exhibition)">即将开始</el-tag>
                    <el-tag size="small" type="info" v-else>已结束</el-tag>
                    <el-tag size="small" type="primary" v-if="exhibition.theme">{{ exhibition.theme }}</el-tag>
                  </div>
                  <p class="card-description">{{ truncateText(exhibition.description, 100) }}</p>
                  <div class="card-meta">
                    <span>{{ formatDate(exhibition.startTime) }} - {{ formatDate(exhibition.endTime) }}</span>
                    <span>{{ exhibition.location || '未知地点' }}</span>
                  </div>
                  <router-link :to="`/exhibitions/${exhibition.id}`" class="btn btn-primary card-btn">查看详情</router-link>
                </div>
              </el-card>
            </el-col>
          </template>
          <el-col :span="24" v-else>
            <el-empty description="暂无符合条件的展览" />
          </el-col>
        </el-row>
      </div>

      <!-- 展览列表 - 列表视图 -->
      <div v-else class="exhibitions-list" v-loading="loading">
        <template v-if="exhibitions.length > 0">
          <el-card v-for="exhibition in exhibitions" :key="exhibition.id" class="list-item" shadow="hover">
            <div class="list-item-content">
              <div class="list-item-image" @click="openImagePreview(exhibition.coverImageUrl)">
                <img
                    :src="exhibition.coverImageUrl || exhibition.posterUrl || (exhibition.parsedImageUrls && exhibition.parsedImageUrls.length > 0 ? exhibition.parsedImageUrls[0] : 'https://via.placeholder.com/300x300?text=展览图片')"
                    :alt="exhibition.name"
                >
                <div class="image-zoom-hint">
                  <el-icon><ZoomIn /></el-icon>
                </div>
              </div>
              <div class="list-item-info">
                <h3 class="list-item-title">{{ exhibition.name }}</h3>
                <div class="list-item-status">
                  <el-tag size="small" type="success" v-if="isOngoing(exhibition)">正在进行</el-tag>
                  <el-tag size="small" type="warning" v-else-if="isUpcoming(exhibition)">即将开始</el-tag>
                  <el-tag size="small" type="info" v-else>已结束</el-tag>
                </div>
                <div class="list-item-details">
                  <div><strong>展览时间:</strong> {{ formatDate(exhibition.startTime) }} - {{ formatDate(exhibition.endTime) }}</div>
                  <div><strong>展览地点:</strong> {{ exhibition.location || '未知' }}</div>
                  <div><strong>主题:</strong> {{ exhibition.theme || '未知' }}</div>
                  <div><strong>策展人:</strong> {{ exhibition.curator || '未知' }}</div>
                </div>
                <p class="list-item-description">{{ truncateText(exhibition.description, 200) }}</p>
                <router-link :to="`/exhibitions/${exhibition.id}`" class="btn btn-primary">查看详情</router-link>
              </div>
            </div>
          </el-card>
        </template>
        <el-empty v-else description="暂无符合条件的展览" />
      </div>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[12, 24, 36, 48]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="totalExhibitions"
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
import { useExhibitionStore } from '../stores'
import { Grid, List, ZoomIn } from '@element-plus/icons-vue'
import request from "@/utils/request.js";

const exhibitionStore = useExhibitionStore()
const searchParams = ref({});
const exhibitions = computed(() => exhibitionStore.exhibitions)
const loading = computed(() => exhibitionStore.loading)

// 分页相关
const currentPage = ref(1)
const pageSize = ref(12)
const totalExhibitions = ref(0)

// 视图模式
const viewMode = ref('grid')
const sortBy = ref('start_desc')

// 筛选条件
const filters = reactive({
  status: '',
  location: '',
  theme: ''
})

// 图片预览
const showImageViewer = ref(false)
const previewImage = ref('')

// 筛选选项
const locationOptions = ref([])
const themeOptions = ref([])



// 加载展览数据
const loadExhibitions = async () => {
  try {
    // 构建查询参数
    const params = {
      page: currentPage.value - 1, // 后端分页从0开始
      size: pageSize.value,
    };

    // 处理排序参数
    if (sortBy.value) {
      const [sortField, sortDirection] = sortBy.value.split('_');
      params.sortBy = mapSortField(sortField);
      params.direction = sortDirection;
    }

    // 添加筛选参数
    if (filters.location) params.location = filters.location;
    if (filters.theme) params.theme = filters.theme;

    // 将状态字符串映射为整数
    if (filters.status) {
      const statusMap = {
        'ongoing': 2,    // 进行中
        'upcoming': 1,   // 即将开始
        'past': 3        // 已结束
      };
      params.status = statusMap[filters.status];
    }

    // 清理undefined参数
    Object.keys(params).forEach(key => {
      if (params[key] === undefined || params[key] === '') {
        delete params[key];
      }
    });

    console.log('发送筛选请求，参数:', JSON.stringify(params, null, 3));

    // 调用支持筛选的接口
    const exhibitions = await exhibitionStore.fetchExhibitionsWithFilter(params);

    // 更新总数
    totalExhibitions.value = exhibitionStore.totalExhibitions || 0;

  } catch (error) {
    console.error('加载展览失败:', error);

    // 添加详细的错误日志
    if (error.response) {
      console.error('API错误详情:', {
        status: error.response.status,
        data: error.response.data
      });
    }
  }
};
// 生成筛选选项
async function generateFilterOptions() {
  try {
    console.log('开始加载筛选选项...')

    // 并行加载地点和主题选项
    await Promise.all([
      exhibitionStore.fetchLocationOptions(),
      exhibitionStore.fetchThemeOptions()
    ])

    // 直接使用 store 中的数组
    locationOptions.value = exhibitionStore.locationOptions
    themeOptions.value = exhibitionStore.themeOptions

    console.log('加载的地点选项:', JSON.parse(JSON.stringify(locationOptions.value)))
    console.log('加载的主题选项:', JSON.parse(JSON.stringify(themeOptions.value)))

  } catch (error) {
    console.error('获取筛选选项失败:', error)
    locationOptions.value = []
    themeOptions.value = []
  }
}
// 应用筛选
function applyFilters() {
  currentPage.value = 1
  loadExhibitions()
}

// 重置筛选
function resetFilters() {
  Object.keys(filters).forEach(key => {
    filters[key] = ''
  })
  sortBy.value = 'start_desc'
  applyFilters()
}

// 处理页码变化
function handleCurrentChange(val) {
  currentPage.value = val
  loadExhibitions()
}

// 处理每页数量变化
function handleSizeChange(val) {
  pageSize.value = val
  currentPage.value = 1
  loadExhibitions()
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

// 工具函数：格式化日期
function formatDate(dateString) {
  if (!dateString) return '未知';
  try {
    // 处理ISO格式日期
    const date = new Date(dateString);
    return date.toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    });
  } catch (e) {
    console.error('日期格式错误:', dateString, e);
    return '日期错误';
  }
}
function mapStatusToBackend(status) {
  const map = {
    'ongoing': 'ONGOING',   // 匹配后端的 "ONGOING" 状态
    'upcoming': 'UPCOMING', // 匹配后端的 "UPCOMING" 状态
    'past': 'ENDED'         // 匹配后端的 "ENDED" 状态
  };
  return map[status] || null;
}
// 排序字段映射
function mapSortField(field) {
  const map = {
    'start': 'startTime',
    'end': 'endTime',
    'popularity': 'viewCount',
    'name': 'name'
  };
  return map[field] || 'startTime';
}
// 判断展览状态
function isOngoing(exhibition) {
  const now = new Date()
  const startDate = new Date(exhibition.startTime)
  const endDate = new Date(exhibition.endTime)
  return startDate <= now && now <= endDate
}

function isUpcoming(exhibition) {
  const now = new Date()
  const startDate = new Date(exhibition.startTime)
  return startDate > now
}
// 加载展览数据
onMounted(async () => {
  await generateFilterOptions()
  await loadExhibitions()

})
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

/* 筛选器样式 */
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
/* 过滤器 */
.filter-section {
  background-color: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 30px;
}

/* 视图控制 */
.view-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

/* 网格视图 */
.exhibitions-grid {
  margin-bottom: 30px;
}

/* 列表视图 */
.exhibitions-list {
  margin-bottom: 30px;
}

.list-item {
  margin-bottom: 20px;
}

.list-item-content {
  display: flex;
  gap: 20px;
}

.list-item-image {
  flex: 0 0 200px;
  height: 200px;
  overflow: hidden;
  position: relative;
  cursor: pointer;
}

.list-item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.list-item-image:hover img {
  transform: scale(1.05);
}

.list-item-info {
  flex: 1;
}

.list-item-title {
  font-size: 20px;
  margin-bottom: 10px;
}

.list-item-status {
  margin-bottom: 10px;
}

.list-item-details {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  margin-bottom: 15px;
}

.list-item-description {
  margin-bottom: 15px;
  color: #666;
}

/* 卡片样式 */
.exhibition-card {
  margin-bottom: 30px;
  border-radius: 8px;
  overflow: hidden;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.exhibition-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.15);
}

.card-image-container {
  height: 200px;
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

.exhibition-card:hover .card-image {
  transform: scale(1.05);
}

.image-zoom-hint {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background-color: rgba(0, 0, 0, 0.5);
  color: white;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.card-image-container:hover .image-zoom-hint {
  opacity: 1;
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

/* 分页 */
.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 40px;
  margin-bottom: 60px;
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

/* 响应式调整 */
@media (max-width: 768px) {
  .list-item-content {
    flex-direction: column;
  }
  
  .list-item-image {
    flex: 0 0 auto;
    width: 100%;
    margin-bottom: 15px;
  }
  
  .list-item-details {
    grid-template-columns: 1fr;
  }
  
  .view-controls {
    flex-direction: column;
    gap: 15px;
  }
}
</style>