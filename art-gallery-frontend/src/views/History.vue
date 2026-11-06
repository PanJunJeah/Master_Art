<template>
  <div class="history-container">
    <div class="page-header">
      <h2>{{ $t('user.viewHistory') || '浏览历史' }}</h2>
    </div>

    <!-- 筛选和排序 -->
    <div class="filter-section">
      <el-input
        v-model="searchQuery"
        :placeholder="$t('common.search') || '搜索历史'"
        class="search-input"
        clearable
        @input="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>

      <div class="filter-options">
        <el-select
          v-model="filters.type"
          :placeholder="$t('history.type') || '类型'"
          clearable
          @change="handleFilterChange"
        >
          <el-option label="艺术品" value="artwork" />
          <el-option label="展览" value="exhibition" />
        </el-select>

        <el-select
          v-model="sortBy"
          :placeholder="$t('history.sortBy') || '排序方式'"
          @change="handleSortChange"
        >
          <el-option label="浏览时间 (最新)" value="viewed_desc" />
          <el-option label="浏览时间 (最早)" value="viewed_asc" />
          <el-option label="名称 (A-Z)" value="title_asc" />
          <el-option label="名称 (Z-A)" value="title_desc" />
        </el-select>
      </div>
    </div>

    <!-- 历史记录列表 -->
    <div class="history-list" v-loading="loading">
      <el-empty v-if="historyItems.length === 0" :description="$t('history.noHistory') || '暂无浏览历史'" />

      <el-timeline v-else>
        <el-timeline-item
          v-for="(item, index) in historyItems"
          :key="index"
          :timestamp="formatDateTime(item.viewedAt)"
          :type="item.type === 'artwork' ? 'primary' : 'success'"
        >
          <el-card class="history-card">
            <div class="history-item">
              <div class="history-image">
                <img :src="item.imageUrl" :alt="item.title" @click="viewDetail(item)">
              </div>
              <div class="history-info">
                <div class="history-type-badge" :class="`type-${item.type}`">
                  {{ item.type === 'artwork' ? '艺术品' : '展览' }}
                </div>
                <h3 class="history-title" @click="viewDetail(item)">{{ item.title }}</h3>
                <p v-if="item.type === 'artwork'" class="history-artist" @click="viewArtistDetail(item.artistId)">
                  {{ item.artistName }}
                </p>
                <p v-if="item.type === 'exhibition'" class="history-date">
                  <el-icon><Calendar /></el-icon>
                  {{ formatDate(item.startDate) }} - {{ formatDate(item.endDate) }}
                </p>
                <p v-if="item.type === 'exhibition'" class="history-location">
                  <el-icon><Location /></el-icon>
                  {{ item.location }}
                </p>
              </div>
              <div class="history-actions">
                <el-button-group>
                  <el-button size="small" @click="viewDetail(item)">
                    <el-icon><View /></el-icon>
                  </el-button>
                  <el-button size="small" type="danger" @click="confirmRemove(item)">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </el-button-group>
              </div>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 30, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="totalItems"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 删除确认对话框 -->
    <el-dialog
      v-model="removeDialogVisible"
      :title="$t('common.confirm') || '确认'"
      width="30%"
    >
      <span>{{ $t('history.removeConfirm') || '确定要删除此浏览记录' }}?</span>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="removeDialogVisible = false">{{ $t('common.cancel') }}</el-button>
          <el-button type="danger" @click="removeHistoryItem">{{ $t('common.confirm') }}</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 清空历史确认对话框 -->
    <el-dialog
      v-model="clearDialogVisible"
      :title="$t('common.confirm') || '确认'"
      width="30%"
    >
      <span>{{ $t('history.clearConfirm') || '确定要清空所有浏览历史' }}?</span>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="clearDialogVisible = false">{{ $t('common.cancel') }}</el-button>
          <el-button type="danger" @click="clearHistory">{{ $t('common.confirm') }}</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, View, Delete, Calendar, Location } from '@element-plus/icons-vue'
import { format, parseISO } from 'date-fns'

const router = useRouter()

// 状态变量
const loading = ref(false)
const historyItems = ref([])
const totalItems = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const searchQuery = ref('')
const filters = reactive({
  type: ''
})
const sortBy = ref('viewed_desc')
const removeDialogVisible = ref(false)
const clearDialogVisible = ref(false)
const itemToRemove = ref(null)

// 获取浏览历史列表
const fetchHistory = async () => {
  loading.value = true
  try {
    // 这里应该调用API获取用户的浏览历史
    // 模拟数据
    const response = {
      data: [
        {
          id: 1,
          type: 'artwork',
          title: '星空',
          imageUrl: 'https://via.placeholder.com/100x100',
          artistId: 201,
          artistName: '文森特·梵高',
          viewedAt: '2023-11-28T15:30:00Z'
        },
        {
          id: 2,
          type: 'exhibition',
          title: '印象派大师作品展',
          imageUrl: 'https://via.placeholder.com/100x100',
          startDate: '2023-12-01',
          endDate: '2024-01-15',
          location: '北京国家博物馆',
          viewedAt: '2023-11-27T10:15:00Z'
        },
        {
          id: 3,
          type: 'artwork',
          title: '格尔尼卡',
          imageUrl: 'https://via.placeholder.com/100x100',
          artistId: 203,
          artistName: '巴勃罗·毕加索',
          viewedAt: '2023-11-26T14:45:00Z'
        },
        {
          id: 4,
          type: 'exhibition',
          title: '当代艺术展',
          imageUrl: 'https://via.placeholder.com/100x100',
          startDate: '2023-12-15',
          endDate: '2024-02-28',
          location: '上海当代艺术博物馆',
          viewedAt: '2023-11-25T09:20:00Z'
        },
        {
          id: 5,
          type: 'artwork',
          title: '蒙娜丽莎',
          imageUrl: 'https://via.placeholder.com/100x100',
          artistId: 205,
          artistName: '列奥纳多·达·芬奇',
          viewedAt: '2023-11-24T16:10:00Z'
        }
      ],
      total: 5
    }
    
    historyItems.value = response.data
    totalItems.value = response.total
  } catch (error) {
    console.error('获取浏览历史失败:', error)
    ElMessage.error('获取浏览历史失败，请稍后再试')
  } finally {
    loading.value = false
  }
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return ''
  try {
    return format(parseISO(dateString), 'yyyy-MM-dd')
  } catch (error) {
    return dateString
  }
}

// 格式化日期时间
const formatDateTime = (dateString) => {
  if (!dateString) return ''
  try {
    return format(parseISO(dateString), 'yyyy-MM-dd HH:mm')
  } catch (error) {
    return dateString
  }
}

// 处理搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchHistory()
}

// 处理筛选变化
const handleFilterChange = () => {
  currentPage.value = 1
  fetchHistory()
}

// 处理排序变化
const handleSortChange = () => {
  fetchHistory()
}

// 处理页码变化
const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchHistory()
}

// 处理每页数量变化
const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  fetchHistory()
}

// 查看详情
const viewDetail = (item) => {
  if (item.type === 'artwork') {
    router.push(`/artworks/${item.id}`)
  } else if (item.type === 'exhibition') {
    router.push(`/exhibitions/${item.id}`)
  }
}

// 查看艺术家详情
const viewArtistDetail = (id) => {
  router.push(`/artists/${id}`)
}

// 确认移除对话框
const confirmRemove = (item) => {
  itemToRemove.value = item
  removeDialogVisible.value = true
}

// 移除历史记录
const removeHistoryItem = async () => {
  loading.value = true
  try {
    // 这里应该调用API移除历史记录
    // await api.removeHistoryItem(itemToRemove.value.id, itemToRemove.value.type)
    
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 500))
    
    ElMessage.success('已删除浏览记录')
    removeDialogVisible.value = false
    
    // 更新列表
    historyItems.value = historyItems.value.filter(item => !(item.id === itemToRemove.value.id && item.type === itemToRemove.value.type))
    totalItems.value--
  } catch (error) {
    console.error('删除浏览记录失败:', error)
    ElMessage.error('删除失败，请稍后再试')
  } finally {
    loading.value = false
  }
}

// 确认清空历史对话框
const showClearHistoryDialog = () => {
  clearDialogVisible.value = true
}

// 清空历史记录
const clearHistory = async () => {
  loading.value = true
  try {
    // 这里应该调用API清空历史记录
    // await api.clearHistory()
    
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 500))
    
    ElMessage.success('已清空所有浏览记录')
    clearDialogVisible.value = false
    
    // 更新列表
    historyItems.value = []
    totalItems.value = 0
  } catch (error) {
    console.error('清空浏览记录失败:', error)
    ElMessage.error('清空失败，请稍后再试')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchHistory()
})
</script>

<style scoped>
.history-container {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.filter-section {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  margin-bottom: 20px;
}

.search-input {
  width: 250px;
}

.filter-options {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
}

.history-card {
  margin-bottom: 10px;
}

.history-item {
  display: flex;
  align-items: center;
}

.history-image {
  width: 100px;
  height: 100px;
  overflow: hidden;
  border-radius: 4px;
  margin-right: 15px;
  flex-shrink: 0;
}

.history-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  cursor: pointer;
  transition: transform 0.3s;
}

.history-image img:hover {
  transform: scale(1.05);
}

.history-info {
  flex: 1;
  position: relative;
}

.history-type-badge {
  position: absolute;
  top: 0;
  right: 0;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  color: white;
}

.type-artwork {
  background-color: #1f2a24;
}

.type-exhibition {
  background-color: #4caf50;
}

.history-title {
  margin: 0 0 10px 0;
  font-size: 16px;
  cursor: pointer;
  padding-right: 70px; /* 为徽章留出空间 */
}

.history-title:hover {
  color: #8a6a33;
}

.history-artist {
  margin: 0 0 5px 0;
  font-size: 14px;
  color: #8a6a33;
  cursor: pointer;
}

.history-artist:hover {
  text-decoration: underline;
}

.history-date,
.history-location {
  display: flex;
  align-items: center;
  margin: 0 0 5px 0;
  font-size: 14px;
  color: #666;
}

.history-date .el-icon,
.history-location .el-icon {
  margin-right: 5px;
  font-size: 16px;
}

.history-actions {
  margin-left: 15px;
}

.pagination-container {
  margin-top: 30px;
  display: flex;
  justify-content: center;
}

@media (max-width: 768px) {
  .history-item {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .history-image {
    margin-right: 0;
    margin-bottom: 10px;
    width: 100%;
    height: 150px;
  }
  
  .history-actions {
    margin-left: 0;
    margin-top: 10px;
    align-self: flex-end;
  }
}
</style>