<template>
  <div class="artist-statistics-container">
    <div class="page-header">
      <h2>{{ $t('artist.statistics') || '统计数据' }}</h2>
    </div>

    <div class="statistics-content" v-loading="loading">
      <!-- 数据概览卡片 -->
      <el-row :gutter="20" class="data-overview">
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="data-card">
            <div class="data-icon views-icon">
              <el-icon><View /></el-icon>
            </div>
            <div class="data-info">
              <div class="data-value">{{ statistics.totalViews }}</div>
              <div class="data-label">{{ $t('artist.totalViews') || '总浏览量' }}</div>
            </div>
            <div class="data-trend" :class="statistics.viewsTrend > 0 ? 'trend-up' : 'trend-down'">
              <el-icon v-if="statistics.viewsTrend > 0"><ArrowUp /></el-icon>
              <el-icon v-else><ArrowDown /></el-icon>
              {{ Math.abs(statistics.viewsTrend) }}%
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="data-card">
            <div class="data-icon favorites-icon">
              <el-icon><Star /></el-icon>
            </div>
            <div class="data-info">
              <div class="data-value">{{ statistics.totalFavorites }}</div>
              <div class="data-label">{{ $t('artist.totalFavorites') || '总收藏数' }}</div>
            </div>
            <div class="data-trend" :class="statistics.favoritesTrend > 0 ? 'trend-up' : 'trend-down'">
              <el-icon v-if="statistics.favoritesTrend > 0"><ArrowUp /></el-icon>
              <el-icon v-else><ArrowDown /></el-icon>
              {{ Math.abs(statistics.favoritesTrend) }}%
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="data-card">
            <div class="data-icon artworks-icon">
              <el-icon><Picture /></el-icon>
            </div>
            <div class="data-info">
              <div class="data-value">{{ statistics.totalArtworks }}</div>
              <div class="data-label">{{ $t('artist.totalArtworks') || '作品总数' }}</div>
            </div>
            <div class="data-trend" :class="statistics.artworksTrend > 0 ? 'trend-up' : 'trend-down'">
              <el-icon v-if="statistics.artworksTrend > 0"><ArrowUp /></el-icon>
              <el-icon v-else><ArrowDown /></el-icon>
              {{ Math.abs(statistics.artworksTrend) }}%
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="data-card">
            <div class="data-icon comments-icon">
              <el-icon><ChatDotRound /></el-icon>
            </div>
            <div class="data-info">
              <div class="data-value">{{ statistics.totalComments }}</div>
              <div class="data-label">{{ $t('artist.totalComments') || '总评论数' }}</div>
            </div>
            <div class="data-trend" :class="statistics.commentsTrend > 0 ? 'trend-up' : 'trend-down'">
              <el-icon v-if="statistics.commentsTrend > 0"><ArrowUp /></el-icon>
              <el-icon v-else><ArrowDown /></el-icon>
              {{ Math.abs(statistics.commentsTrend) }}%
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 时间范围选择器 -->
      <div class="time-range-selector">
        <el-radio-group v-model="timeRange" @change="handleTimeRangeChange">
          <el-radio-button label="week">{{ $t('common.week') || '本周' }}</el-radio-button>
          <el-radio-button label="month">{{ $t('common.month') || '本月' }}</el-radio-button>
          <el-radio-button label="year">{{ $t('common.year') || '本年' }}</el-radio-button>
          <el-radio-button label="all">{{ $t('common.all') || '全部' }}</el-radio-button>
        </el-radio-group>

        <el-date-picker
          v-model="customDateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          @change="handleCustomDateChange"
        />
      </div>

      <!-- 图表区域 -->
      <el-row :gutter="20" class="charts-row">
        <el-col :xs="24" :lg="12">
          <el-card class="chart-card">
            <template #header>
              <div class="chart-header">
                <h3>{{ $t('artist.viewsOverTime') || '浏览量趋势' }}</h3>
              </div>
            </template>
            <div class="chart-container" ref="viewsChartRef"></div>
          </el-card>
        </el-col>

        <el-col :xs="24" :lg="12">
          <el-card class="chart-card">
            <template #header>
              <div class="chart-header">
                <h3>{{ $t('artist.favoritesOverTime') || '收藏数趋势' }}</h3>
              </div>
            </template>
            <div class="chart-container" ref="favoritesChartRef"></div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20" class="charts-row">
        <el-col :xs="24" :lg="12">
          <el-card class="chart-card">
            <template #header>
              <div class="chart-header">
                <h3>{{ $t('artist.popularArtworks') || '热门作品' }}</h3>
              </div>
            </template>
            <div class="chart-container" ref="popularArtworksChartRef"></div>
          </el-card>
        </el-col>

        <el-col :xs="24" :lg="12">
          <el-card class="chart-card">
            <template #header>
              <div class="chart-header">
                <h3>{{ $t('artist.audienceGeography') || '观众地域分布' }}</h3>
              </div>
            </template>
            <div class="chart-container" ref="geographyChartRef"></div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 热门作品列表 -->
      <el-card class="popular-artworks-card">
        <template #header>
          <div class="card-header">
            <h3>{{ $t('artist.topPerformingArtworks') || '表现最佳作品' }}</h3>
          </div>
        </template>

        <el-table :data="popularArtworks" style="width: 100%" v-loading="loading">
          <el-table-column width="80">
            <template #default="scope">
              <el-image 
                :src="scope.row.imageUrl" 
                :alt="scope.row.title"
                style="width: 60px; height: 60px; object-fit: cover;"
                @click="viewArtworkDetail(scope.row.id)"
              />
            </template>
          </el-table-column>
          <el-table-column prop="title" :label="$t('artworks.title') || '作品标题'">
            <template #default="scope">
              <el-link type="primary" @click="viewArtworkDetail(scope.row.id)">{{ scope.row.title }}</el-link>
            </template>
          </el-table-column>
          <el-table-column prop="views" :label="$t('artist.views') || '浏览量'" width="120" sortable />
          <el-table-column prop="favorites" :label="$t('artist.favorites') || '收藏数'" width="120" sortable />
          <el-table-column prop="comments" :label="$t('artist.comments') || '评论数'" width="120" sortable />
          <el-table-column prop="conversionRate" :label="$t('artist.conversionRate') || '转化率'" width="120">
            <template #default="scope">
              {{ scope.row.conversionRate }}%
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { View, Star, Picture, ChatDotRound, ArrowUp, ArrowDown } from '@element-plus/icons-vue'

const router = useRouter()
const loading = ref(false)
const timeRange = ref('month')
const customDateRange = ref(null)

// 图表引用
const viewsChartRef = ref(null)
const favoritesChartRef = ref(null)
const popularArtworksChartRef = ref(null)
const geographyChartRef = ref(null)

// 图表实例
let viewsChart = null
let favoritesChart = null
let popularArtworksChart = null
let geographyChart = null

// 统计数据
const statistics = reactive({
  totalViews: 0,
  viewsTrend: 0,
  totalFavorites: 0,
  favoritesTrend: 0,
  totalArtworks: 0,
  artworksTrend: 0,
  totalComments: 0,
  commentsTrend: 0
})

// 热门作品数据
const popularArtworks = ref([])

// 获取统计数据
const fetchStatistics = async () => {
  loading.value = true
  try {
    // 这里应该调用API获取统计数据
    // 模拟数据
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    statistics.totalViews = 12458
    statistics.viewsTrend = 15.2
    statistics.totalFavorites = 843
    statistics.favoritesTrend = 8.7
    statistics.totalArtworks = 27
    statistics.artworksTrend = 3.5
    statistics.totalComments = 356
    statistics.commentsTrend = -2.1
    
    popularArtworks.value = [
      {
        id: 1,
        title: '春天的印象',
        imageUrl: 'https://via.placeholder.com/60x60',
        views: 1245,
        favorites: 87,
        comments: 32,
        conversionRate: 7.0
      },
      {
        id: 2,
        title: '城市夜景',
        imageUrl: 'https://via.placeholder.com/60x60',
        views: 986,
        favorites: 65,
        comments: 28,
        conversionRate: 6.6
      },
      {
        id: 3,
        title: '抽象构成',
        imageUrl: 'https://via.placeholder.com/60x60',
        views: 754,
        favorites: 43,
        comments: 19,
        conversionRate: 5.7
      },
      {
        id: 4,
        title: '山水画',
        imageUrl: 'https://via.placeholder.com/60x60',
        views: 632,
        favorites: 38,
        comments: 15,
        conversionRate: 6.0
      },
      {
        id: 5,
        title: '静物写生',
        imageUrl: 'https://via.placeholder.com/60x60',
        views: 521,
        favorites: 29,
        comments: 12,
        conversionRate: 5.6
      }
    ]
    
    // 初始化图表
    nextTick(() => {
      initCharts()
    })
  } catch (error) {
    console.error('获取统计数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 初始化图表
const initCharts = () => {
  // 浏览量趋势图表
  if (viewsChartRef.value) {
    viewsChart = echarts.init(viewsChartRef.value)
    const viewsOption = {
      tooltip: {
        trigger: 'axis'
      },
      xAxis: {
        type: 'category',
        data: ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']
      },
      yAxis: {
        type: 'value'
      },
      series: [{
        data: [820, 932, 901, 934, 1290, 1330, 1320, 1450, 1500, 1600, 1700, 1800],
        type: 'line',
        smooth: true,
        areaStyle: {
          opacity: 0.3
        },
        itemStyle: {
          color: '#b08d57'
        },
        lineStyle: {
          width: 3
        }
      }]
    }
    viewsChart.setOption(viewsOption)
  }

  // 收藏数趋势图表
  if (favoritesChartRef.value) {
    favoritesChart = echarts.init(favoritesChartRef.value)
    const favoritesOption = {
      tooltip: {
        trigger: 'axis'
      },
      xAxis: {
        type: 'category',
        data: ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']
      },
      yAxis: {
        type: 'value'
      },
      series: [{
        data: [50, 65, 78, 86, 94, 105, 115, 130, 142, 158, 170, 185],
        type: 'line',
        smooth: true,
        areaStyle: {
          opacity: 0.3
        },
        itemStyle: {
          color: '#ff9800'
        },
        lineStyle: {
          width: 3
        }
      }]
    }
    favoritesChart.setOption(favoritesOption)
  }

  // 热门作品图表
  if (popularArtworksChartRef.value) {
    popularArtworksChart = echarts.init(popularArtworksChartRef.value)
    const popularArtworksOption = {
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow'
        }
      },
      legend: {
        data: ['浏览量', '收藏数']
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'value'
      },
      yAxis: {
        type: 'category',
        data: popularArtworks.value.map(item => item.title).reverse()
      },
      series: [
        {
          name: '浏览量',
          type: 'bar',
          data: popularArtworks.value.map(item => item.views).reverse(),
          itemStyle: {
            color: '#b08d57'
          }
        },
        {
          name: '收藏数',
          type: 'bar',
          data: popularArtworks.value.map(item => item.favorites).reverse(),
          itemStyle: {
            color: '#ff9800'
          }
        }
      ]
    }
    popularArtworksChart.setOption(popularArtworksOption)
  }

  // 观众地域分布图表
  if (geographyChartRef.value) {
    geographyChart = echarts.init(geographyChartRef.value)
    const geographyOption = {
      tooltip: {
        trigger: 'item',
        formatter: '{a} <br/>{b}: {c} ({d}%)'
      },
      legend: {
        orient: 'vertical',
        left: 10,
        data: ['北京', '上海', '广州', '深圳', '杭州', '成都', '其他']
      },
      series: [
        {
          name: '观众来源',
          type: 'pie',
          radius: ['50%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 10,
            borderColor: '#fff',
            borderWidth: 2
          },
          label: {
            show: false,
            position: 'center'
          },
          emphasis: {
            label: {
              show: true,
              fontSize: '18',
              fontWeight: 'bold'
            }
          },
          labelLine: {
            show: false
          },
          data: [
            { value: 1048, name: '北京' },
            { value: 735, name: '上海' },
            { value: 580, name: '广州' },
            { value: 484, name: '深圳' },
            { value: 300, name: '杭州' },
            { value: 235, name: '成都' },
            { value: 400, name: '其他' }
          ]
        }
      ]
    }
    geographyChart.setOption(geographyOption)
  }

  // 监听窗口大小变化，调整图表大小
  window.addEventListener('resize', handleResize)
}

// 处理窗口大小变化
const handleResize = () => {
  viewsChart?.resize()
  favoritesChart?.resize()
  popularArtworksChart?.resize()
  geographyChart?.resize()
}

// 处理时间范围变化
const handleTimeRangeChange = (value) => {
  // 清空自定义日期范围
  if (value !== 'custom') {
    customDateRange.value = null
  }
  // 重新获取数据
  fetchStatistics()
}

// 处理自定义日期变化
const handleCustomDateChange = (value) => {
  if (value) {
    timeRange.value = 'custom'
    // 重新获取数据
    fetchStatistics()
  }
}

// 查看作品详情
const viewArtworkDetail = (id) => {
  router.push(`/artworks/${id}`)
}

onMounted(() => {
  fetchStatistics()
})

// 组件卸载时移除事件监听
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  // 销毁图表实例
  viewsChart?.dispose()
  favoritesChart?.dispose()
  popularArtworksChart?.dispose()
  geographyChart?.dispose()
})
</script>

<style scoped>
.artist-statistics-container {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.data-overview {
  margin-bottom: 30px;
}

.data-card {
  height: 120px;
  display: flex;
  align-items: center;
  padding: 20px;
  margin-bottom: 20px;
  position: relative;
  overflow: hidden;
}

.data-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
}

.data-icon .el-icon {
  font-size: 30px;
  color: white;
}

.views-icon {
  background-color: #1f2a24;
}

.favorites-icon {
  background-color: #ff9800;
}

.artworks-icon {
  background-color: #4caf50;
}

.comments-icon {
  background-color: #9c27b0;
}

.data-info {
  flex: 1;
}

.data-value {
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 5px;
}

.data-label {
  font-size: 14px;
  color: #666;
}

.data-trend {
  position: absolute;
  top: 15px;
  right: 15px;
  font-size: 14px;
  display: flex;
  align-items: center;
}

.trend-up {
  color: #4caf50;
}

.trend-down {
  color: #f44336;
}

.time-range-selector {
  display: flex;
  justify-content: space-between;
  margin-bottom: 30px;
  flex-wrap: wrap;
  gap: 15px;
}

.charts-row {
  margin-bottom: 30px;
}

.chart-card {
  margin-bottom: 20px;
  height: 400px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
}

.chart-container {
  height: 320px;
}

.popular-artworks-card {
  margin-bottom: 30px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
}

@media (max-width: 768px) {
  .time-range-selector {
    flex-direction: column;
  }
  
  .chart-container {
    height: 250px;
  }
}
</style>