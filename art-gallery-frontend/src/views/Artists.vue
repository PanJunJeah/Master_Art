<template>
  <div class="artists-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="container">
        <h1 class="page-title">艺术家</h1>
        <p class="page-description">探索世界各地杰出艺术家的生平和作品</p>
      </div>
    </div>

    <!-- 主要内容 -->
    <div class="container">
      <!-- 过滤器 -->
      <div class="filter-section">
        <el-form :inline="true" class="filter-form">
          <el-form-item label="国籍">
            <el-select v-model="filters.nationality" placeholder="选择国籍" clearable @change="applyFilters">
              <el-option v-for="item in nationalityOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="流派">
            <el-select v-model="filters.style" placeholder="选择流派" clearable @change="applyFilters">
              <el-option v-for="item in styleOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="时期">
            <el-select v-model="filters.period" placeholder="选择时期" clearable @change="applyFilters">
              <el-option v-for="item in periodOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>

          <!-- 按钮容器 -->
          <div class="filter-buttons">
            <el-button type="primary" @click="applyFilters">筛选</el-button>
            <el-button @click="resetFilters">重置</el-button>
          </div>
        </el-form>
      </div>

      <!-- 时间轴导航 -->
      <div class="timeline-section">
        <h2 class="section-title">艺术史时间轴</h2>
        <el-timeline>
          <el-timeline-item v-for="period in timelinePeriods" :key="period.value" :timestamp="period.years" placement="top">
            <el-card>
              <h3>{{ period.label }}</h3>
              <p>{{ period.description }}</p>
              <el-button type="text" @click="selectPeriod(period.value)">查看此时期艺术家</el-button>
            </el-card>
          </el-timeline-item>
        </el-timeline>
      </div>

      <!-- 艺术家列表 -->
      <div class="artists-section">
        <h2 class="section-title">艺术家列表</h2>
        <el-row :gutter="30" v-loading="loading">
          <template v-if="artists.content && artists.content.length > 0">
            <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="artist in artists.content" :key="artist.id">
              <el-card class="artist-card" shadow="hover" :body-style="{ padding: '0px' }">
                <div class="card-image-container">
                  <!-- 修改图片源处理方式 -->
                  <img
                      :src="getImageUrl(artist.portraitUrl)"
                      class="card-image"
                      :alt="artist.name"
                      @error="handleImageError"
                  >
                </div>
                <div class="card-content">
                  <h3 class="card-title">{{ artist.name }}</h3>
                  <div class="card-tags">
                    <el-tag size="small" type="info" v-if="artist.nationality">{{ artist.nationality }}</el-tag>
                    <el-tag size="small" type="success" v-if="artist.mainStyle">{{ artist.mainStyle }}</el-tag>
                  </div>
                  <p class="card-description">{{ truncateText(artist.biography, 100) }}</p>
                  <div class="card-meta">
                    <span>{{ formatLifespan(artist) }}</span>
                    <span>{{ artist.artworksCount || 0 }} 件作品</span>
                  </div>
                  <router-link :to="`/artists/${artist.id}`" class="btn btn-primary card-btn">查看详情</router-link>
                </div>
              </el-card>
            </el-col>
          </template>
          <el-col :span="24" v-else>
            <el-empty description="暂无符合条件的艺术家" />
          </el-col>
        </el-row>

        <!-- 分页 -->
        <div class="pagination-container">
          <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :page-sizes="[12, 24, 36, 48]"
              layout="total, sizes, prev, pager, next, jumper"
              :total="totalArtists"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useArtistStore } from '../stores'
import request from "@/utils/request.js";
import defaultArtistImg from '@/assets/vue.svg'

const artistStore = useArtistStore()
const searchParams = ref({});
const loading = ref(true)
const artists = ref({content:[]})
// 分页相关
const currentPage = ref(1)
const pageSize = ref(12)
const totalArtists = ref(0)
// 添加默认图片处理
const defaultImage = ref(defaultArtistImg)
// 过滤条件
const filters = reactive({
  nationality: null,
  style: null,
  period: null,
  search: ''
})

// 国籍选项
const nationalityOptions = [
  { value: 'chinese', label: '中国' },
  { value: 'french', label: '法国' },
  { value: 'italian', label: '意大利' },
  { value: 'dutch', label: '荷兰' },
  { value: 'spanish', label: '西班牙' },
  { value: 'american', label: '美国' },
  { value: 'british', label: '英国' },
  { value: 'german', label: '德国' },
  { value: 'russian', label: '俄罗斯' },
  { value: 'japanese', label: '日本' }
]

// 流派选项
const styleOptions = [
  { value: 'renaissance', label: '文艺复兴' },
  { value: 'baroque', label: '巴洛克' },
  { value: 'rococo', label: '洛可可' },
  { value: 'neoclassicism', label: '新古典主义' },
  { value: 'romanticism', label: '浪漫主义' },
  { value: 'realism', label: '现实主义' },
  { value: 'impressionism', label: '印象派' },
  { value: 'post-impressionism', label: '后印象派' },
  { value: 'expressionism', label: '表现主义' },
  { value: 'cubism', label: '立体主义' },
  { value: 'surrealism', label: '超现实主义' },
  { value: 'abstract', label: '抽象主义' },
  { value: 'pop-art', label: '波普艺术' },
  { value: 'contemporary', label: '当代艺术' }
]

// 时期选项
const periodOptions = [
  { value: 'ancient', label: '古代' },
  { value: 'medieval', label: '中世纪' },
  { value: 'renaissance', label: '文艺复兴' },
  { value: '17th-century', label: '17世纪' },
  { value: '18th-century', label: '18世纪' },
  { value: '19th-century', label: '19世纪' },
  { value: 'early-20th-century', label: '20世纪初' },
  { value: 'mid-20th-century', label: '20世纪中期' },
  { value: 'late-20th-century', label: '20世纪晚期' },
  { value: 'contemporary', label: '当代' }
]

// 时间轴数据
const timelinePeriods = [
  {
    value: 'renaissance',
    label: '文艺复兴时期',
    years: '14-16世纪',
    description: '文艺复兴时期是欧洲艺术史上的重要阶段，以对古典艺术的复兴和人文主义思想的兴起为特征。'
  },
  {
    value: 'baroque',
    label: '巴洛克时期',
    years: '17世纪',
    description: '巴洛克艺术以其戏剧性、动感和华丽的风格著称，强调光影对比和情感表达。'
  },
  {
    value: 'neoclassicism',
    label: '新古典主义',
    years: '18世纪中期-19世纪初',
    description: '新古典主义回归古希腊罗马艺术的理性和秩序，强调线条的纯净和构图的平衡。'
  },
  {
    value: 'romanticism',
    label: '浪漫主义',
    years: '19世纪初-中期',
    description: '浪漫主义强调情感、想象力和个性，关注自然、历史和神话主题。'
  },
  {
    value: 'impressionism',
    label: '印象派',
    years: '19世纪后期',
    description: '印象派关注光线和色彩的瞬间效果，以松散的笔触捕捉日常生活场景。'
  },
  {
    value: 'modernism',
    label: '现代主义',
    years: '20世纪初-中期',
    description: '现代主义包括多种前卫艺术运动，如立体主义、表现主义和超现实主义等，打破传统艺术规则。'
  },
  {
    value: 'contemporary',
    label: '当代艺术',
    years: '20世纪后期至今',
    description: '当代艺术多元且实验性强，融合各种媒介和技术，反映全球化和数字时代的特点。'
  }
]



// 加载艺术家数据
// 加载艺术家数据（已修复筛选功能）
const getAllArtists = async () => {
  try {
    loading.value = true;

    // 构建请求参数（添加筛选条件）
    const params = {
      page: currentPage.value - 1, // 后端页码从0开始
      size: pageSize.value,
      nationality: filters.nationality,
      style: filters.style,
      period: filters.period
    };

    // 清理空值参数
    Object.keys(params).forEach(key => {
      if (params[key] === null || params[key] === undefined || params[key] === '') {
        delete params[key];
      }
    });

    const res = await request({
      url: "/api/artists/getAllArtists",
      method: "get",
      params  // 传递筛选参数
    });

    if (res && res.content) {
      artists.value = res;
      totalArtists.value = res.totalElements || 0; // 确保使用正确的总数字段
    } else {
      artists.value = { content: [] };
      totalArtists.value = 0;
    }
    loading.value = false;
  } catch (error) {
    console.error('加载艺术家数据失败:', error);
    loading.value = false;
  }
}

// 应用过滤器
function applyFilters() {
  currentPage.value = 1
  getAllArtists()
}

// 重置过滤器
function resetFilters() {
  Object.keys(filters).forEach(key => {
    filters[key] = null
  })
  currentPage.value = 1
 getAllArtists()
}

// 选择时期
function selectPeriod(period) {
  filters.period = period
  applyFilters()
}

// 处理页码变化
function handleCurrentChange(val) {
  currentPage.value = val
  getAllArtists()
}

// 处理每页显示数量变化
function handleSizeChange(val) {
  pageSize.value = val
  currentPage.value = 1
  getAllArtists()
}


// 处理图片URL
function getImageUrl(url) {
  if (!url) return defaultImage.value;

  // 如果已经是完整URL则直接返回
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url;
  }

  // 处理相对路径
  const baseUrl = import.meta.env.VITE_API_BASE || '';

  // 确保路径格式正确
  if (url.startsWith('/')) {
    return `${baseUrl}${url}`;
  } else {
    return `${baseUrl}/${url}`;
  }
}


// 处理图片加载错误
const handleImageError = (event) => {
  event.target.src = defaultImage.value
  event.target.onerror = null // 防止循环报错
}

// 工具函数：截断文本
function truncateText(text, length) {
  if (!text) return '暂无简介'
  return text.length > length ? text.substring(0, length) + '...' : text
}

// 工具函数：格式化艺术家生平
function formatLifespan(artist) {
  if (!artist.birthYear) return '未知年代'
  return `${artist.birthYear || '?'} - ${artist.deathYear || '至今'}`
}
// 初始化数据
onMounted(async () => {
  await getAllArtists()
})
</script>

<style scoped>
.artists-page {
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

.section-title {
  font-size: 24px;
  color: #333;
  margin-bottom: 25px;
  position: relative;
  padding-left: 15px;
}

.section-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  width: 4px;
  background-color: #1f2a24;
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

/* 时间轴样式 */
.timeline-section {
  margin-bottom: 40px;
}

.el-timeline-item__timestamp {
  font-size: 16px;
  font-weight: bold;
  color: #8a6a33;
}

/* 艺术家卡片样式 */
.artist-card {
  margin-bottom: 30px;
  border-radius: 8px;
  overflow: hidden;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.artist-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.15);
}

/* 修改图片容器样式 */
.card-image-container {
  height: 250px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f5f7fa; /* 添加背景色 */
  overflow: hidden;
}

.card-image {
  width: 100%;
  height: 100%;
  object-fit: cover; /* 确保图片比例正确 */
  transition: transform 0.5s ease;
}

.artist-card:hover .card-image {
  transform: scale(1.05);
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