<template>
  <div class="artist-artworks-container">
    <div class="page-header">
      <h2>{{ $t('artist.myArtworks') || '我的作品' }}</h2>
      <el-button type="primary" @click="navigateToUpload">
        <el-icon><Plus /></el-icon>
        {{ $t('artist.uploadArtwork') || '上传新作品' }}
      </el-button>
    </div>

    <!-- 搜索和筛选 -->
    <div class="filter-section">
      <el-input
        v-model="searchQuery"
        :placeholder="$t('common.search') || '搜索作品'"
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
          v-model="filters.status"
          :placeholder="$t('artworks.status') || '状态'"
          clearable
          @change="handleFilterChange"
        >
          <el-option label="已发布" value="published" />
          <el-option label="草稿" value="draft" />
          <el-option label="审核中" value="pending" />
        </el-select>

        <el-select
          v-model="filters.style"
          :placeholder="$t('artworks.style') || '艺术流派'"
          clearable
          @change="handleFilterChange"
        >
          <el-option label="现代主义" value="modernism" />
          <el-option label="印象派" value="impressionism" />
          <el-option label="抽象艺术" value="abstract" />
          <el-option label="写实主义" value="realism" />
          <el-option label="其他" value="other" />
        </el-select>

        <el-select
          v-model="sortBy"
          :placeholder="$t('artworks.sortBy') || '排序方式'"
          @change="handleSortChange"
        >
          <el-option label="创建时间 (最新)" value="created_desc" />
          <el-option label="创建时间 (最早)" value="created_asc" />
          <el-option label="名称 (A-Z)" value="title_asc" />
          <el-option label="名称 (Z-A)" value="title_desc" />
          <el-option label="浏览量 (最高)" value="views_desc" />
        </el-select>
      </div>
    </div>

    <!-- 作品列表 -->
    <div class="artworks-list" v-loading="loading">
      <el-empty v-if="artworks.length === 0" :description="$t('common.noData') || '暂无作品'" />

      <el-row :gutter="20" v-else>
        <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="artwork in artworks" :key="artwork.id" class="artwork-item-col">
          <el-card class="artwork-card" :body-style="{ padding: '0px' }">
            <div class="artwork-image">
              <img :src="artwork.imageUrl" :alt="artwork.title" @click="viewArtworkDetail(artwork.id)">
              <div class="artwork-status" :class="`status-${artwork.status}`">
                {{ getStatusText(artwork.status) }}
              </div>
            </div>
            <div class="artwork-info">
              <h3 class="artwork-title" @click="viewArtworkDetail(artwork.id)">{{ artwork.title }}</h3>
              <p class="artwork-date">{{ artwork.creationYear }}</p>
              <p class="artwork-style">{{ artwork.style }}</p>
              <div class="artwork-stats">
                <span><el-icon><View /></el-icon> {{ artwork.views || 0 }}</span>
                <span><el-icon><Star /></el-icon> {{ artwork.favorites || 0 }}</span>
              </div>
            </div>
            <div class="artwork-actions">
              <el-button-group>
                <el-button size="small" @click="editArtwork(artwork.id)">
                  <el-icon><Edit /></el-icon>
                </el-button>
                <el-button size="small" @click="viewArtworkDetail(artwork.id)">
                  <el-icon><View /></el-icon>
                </el-button>
                <el-button size="small" type="danger" @click="confirmDelete(artwork)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </el-button-group>
            </div>
          </el-card>
        </el-col>
      </el-row>

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

    <!-- 删除确认对话框 -->
    <el-dialog
      v-model="deleteDialogVisible"
      :title="$t('common.confirm') || '确认'"
      width="30%"
    >
      <span>{{ $t('artist.deleteArtworkConfirm') || '确定要删除作品' }} "{{ artworkToDelete?.title }}"?</span>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="deleteDialogVisible = false">{{ $t('common.cancel') }}</el-button>
          <el-button type="danger" @click="deleteArtwork">{{ $t('common.delete') }}</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Edit, Delete, View, Star } from '@element-plus/icons-vue'
import { useArtworkStore } from '../../stores/artwork.js'

const router = useRouter()
const artworkStore = useArtworkStore()

// 状态变量
const loading = ref(false)
const artworks = ref([])
const totalArtworks = ref(0)
const currentPage = ref(1)
const pageSize = ref(12)
const searchQuery = ref('')
const filters = reactive({
  status: '',
  style: ''
})
const sortBy = ref('created_desc')
const deleteDialogVisible = ref(false)
const artworkToDelete = ref(null)

// 获取作品列表
const fetchArtworks = async () => {
  loading.value = true
  try {
    // 这里应该调用API获取艺术家的作品列表
    // 模拟数据
    const response = {
      data: [
        {
          id: 1,
          title: '春天的印象',
          imageUrl: 'https://via.placeholder.com/300x200',
          creationYear: '2023',
          style: '印象派',
          status: 'published',
          views: 120,
          favorites: 15
        },
        {
          id: 2,
          title: '城市夜景',
          imageUrl: 'https://via.placeholder.com/300x200',
          creationYear: '2022',
          style: '现代主义',
          status: 'published',
          views: 85,
          favorites: 7
        },
        {
          id: 3,
          title: '抽象构成',
          imageUrl: 'https://via.placeholder.com/300x200',
          creationYear: '2023',
          style: '抽象艺术',
          status: 'draft',
          views: 0,
          favorites: 0
        },
        {
          id: 4,
          title: '山水画',
          imageUrl: 'https://via.placeholder.com/300x200',
          creationYear: '2021',
          style: '写实主义',
          status: 'pending',
          views: 45,
          favorites: 3
        }
      ],
      total: 4
    }
    
    artworks.value = response.data
    totalArtworks.value = response.total
  } catch (error) {
    console.error('获取作品列表失败:', error)
    ElMessage.error('获取作品列表失败，请稍后再试')
  } finally {
    loading.value = false
  }
}

// 处理搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchArtworks()
}

// 处理筛选变化
const handleFilterChange = () => {
  currentPage.value = 1
  fetchArtworks()
}

// 处理排序变化
const handleSortChange = () => {
  fetchArtworks()
}

// 处理页码变化
const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchArtworks()
}

// 处理每页数量变化
const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  fetchArtworks()
}

// 查看作品详情
const viewArtworkDetail = (id) => {
  router.push(`/artworks/${id}`)
}

// 编辑作品
const editArtwork = (id) => {
  router.push(`/artist/edit-artwork/${id}`)
}

// 确认删除对话框
const confirmDelete = (artwork) => {
  artworkToDelete.value = artwork
  deleteDialogVisible.value = true
}

// 删除作品
const deleteArtwork = async () => {
  loading.value = true
  try {
    // 这里应该调用API删除作品
    // await artworkStore.deleteArtwork(artworkToDelete.value.id)
    ElMessage.success('作品删除成功')
    deleteDialogVisible.value = false
    fetchArtworks()
  } catch (error) {
    console.error('删除作品失败:', error)
    ElMessage.error('删除作品失败，请稍后再试')
  } finally {
    loading.value = false
  }
}

// 导航到上传页面
const navigateToUpload = () => {
  router.push('/artist/upload')
}

// 获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    published: '已发布',
    draft: '草稿',
    pending: '审核中'
  }
  return statusMap[status] || status
}

onMounted(() => {
  fetchArtworks()
})
</script>

<style scoped>
.artist-artworks-container {
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

.artwork-item-col {
  margin-bottom: 20px;
}

.artwork-card {
  height: 100%;
  transition: transform 0.3s;
}

.artwork-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
}

.artwork-image {
  position: relative;
  height: 200px;
  overflow: hidden;
}

.artwork-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  cursor: pointer;
  transition: transform 0.3s;
}

.artwork-image img:hover {
  transform: scale(1.05);
}

.artwork-status {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  color: white;
}

.status-published {
  background-color: #67c23a;
}

.status-draft {
  background-color: #909399;
}

.status-pending {
  background-color: #e6a23c;
}

.artwork-info {
  padding: 15px;
}

.artwork-title {
  margin: 0 0 5px 0;
  font-size: 16px;
  cursor: pointer;
}

.artwork-title:hover {
  color: #8a6a33;
}

.artwork-date, .artwork-style {
  margin: 0 0 5px 0;
  font-size: 14px;
  color: #666;
}

.artwork-stats {
  display: flex;
  gap: 15px;
  font-size: 14px;
  color: #666;
}

.artwork-actions {
  padding: 0 15px 15px;
  display: flex;
  justify-content: flex-end;
}

.pagination-container {
  margin-top: 30px;
  display: flex;
  justify-content: center;
}
</style>