<template>
  <div class="admin-dashboard">
    <el-card class="welcome-card">
      <template #header>
        <div class="card-header">
          <h2>{{ $t('admin.welcomeTitle') || '欢迎使用后台管理系统' }}</h2>
        </div>
      </template>
      <div class="welcome-content">
        <p>{{ $t('admin.welcomeMessage') || '您已成功登录管理员后台，可以管理艺术品、展览和用户。' }}</p>
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
              <div class="stat-label">{{ $t('admin.totalArtworks') || '艺术品总数' }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="stat-card">
            <div class="stat-icon">
              <el-icon><Calendar /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.exhibitionsCount || 0 }}</div>
              <div class="stat-label">{{ $t('admin.totalExhibitions') || '展览总数' }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="stat-card">
            <div class="stat-icon">
              <el-icon><User /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.usersCount || 0 }}</div>
              <div class="stat-label">{{ $t('admin.totalUsers') || '用户总数' }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <el-card class="stat-card">
            <div class="stat-icon">
              <el-icon><View /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.visitsCount || 0 }}</div>
              <div class="stat-label">{{ $t('admin.totalVisits') || '访问总数' }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <div class="dashboard-charts">
      <el-row :gutter="20">
        <el-col :xs="24" :lg="12">
          <el-card class="chart-card">
            <template #header>
              <div class="card-header">
                <h3>{{ $t('admin.recentVisits') || '最近访问统计' }}</h3>
              </div>
            </template>
            <div class="chart-placeholder">
              <!-- 这里可以集成图表库，如ECharts -->
              <div class="placeholder-text">{{ $t('admin.chartPlaceholder') || '图表加载中...' }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :lg="12">
          <el-card class="chart-card">
            <template #header>
              <div class="card-header">
                <h3>{{ $t('admin.popularArtworks') || '热门艺术品' }}</h3>
              </div>
            </template>
            <div class="chart-placeholder">
              <!-- 这里可以集成图表库，如ECharts -->
              <div class="placeholder-text">{{ $t('admin.chartPlaceholder') || '图表加载中...' }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <el-card class="recent-activities">
      <template #header>
        <div class="card-header">
          <h3>{{ $t('admin.recentActivities') || '最近活动' }}</h3>
        </div>
      </template>
      <div v-if="activities.length > 0">
        <el-timeline>
          <el-timeline-item
            v-for="(activity, index) in activities"
            :key="index"
            :timestamp="activity.time"
            :type="activity.type"
          >
            {{ activity.content }}
          </el-timeline-item>
        </el-timeline>
      </div>
      <div v-else class="no-data">
        {{ $t('admin.noRecentActivities') || '暂无最近活动' }}
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Picture, Calendar, User, View } from '@element-plus/icons-vue'
import service from '@/utils/request'

// 统计数据
const stats = ref({
  artworksCount: 0,
  exhibitionsCount: 0,
  usersCount: 0,
  visitsCount: 0
})

// 最近活动
const activities = ref([])

// 获取仪表盘数据
const fetchDashboardData = async () => {
  try {
    // 这里可以调用后端API获取实际数据
    const response = await service.get('/api/admin/dashboard')
    stats.value = response.data.stats
    activities.value = response.data.activities
  } catch (error) {
    console.error('获取仪表盘数据失败:', error)
    // 使用模拟数据
    stats.value = {
      artworksCount: 120,
      exhibitionsCount: 15,
      usersCount: 350,
      visitsCount: 12500
    }
    
    activities.value = [
      { time: '2023-06-15 14:30', type: 'primary', content: '管理员更新了展览「现代艺术展」的信息' },
      { time: '2023-06-15 11:20', type: 'success', content: '新增艺术品「星空」已上传' },
      { time: '2023-06-14 16:45', type: 'info', content: '用户张三注册成为新会员' },
      { time: '2023-06-14 09:30', type: 'warning', content: '系统自动备份完成' }
    ]
  }
}

onMounted(() => {
  fetchDashboardData()
})
</script>

<style scoped>
.admin-dashboard {
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

.dashboard-stats {
  margin-bottom: 20px;
}

.stat-card {
  height: 120px;
  display: flex;
  align-items: center;
  padding: 20px;
  margin-bottom: 20px;
}

.stat-icon {
  font-size: 48px;
  color: #8a6a33;
  margin-right: 20px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

.dashboard-charts {
  margin-bottom: 20px;
}

.chart-card {
  margin-bottom: 20px;
  height: 350px;
}

.chart-placeholder {
  height: 280px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f9f9f9;
  border-radius: 4px;
}

.placeholder-text {
  color: #999;
  font-size: 16px;
}

.recent-activities {
  margin-bottom: 20px;
}

.no-data {
  text-align: center;
  padding: 30px;
  color: #999;
}

@media (max-width: 768px) {
  .stat-card {
    margin-bottom: 15px;
  }
  
  .chart-card {
    height: auto;
  }
  
  .chart-placeholder {
    height: 200px;
  }
}
</style>