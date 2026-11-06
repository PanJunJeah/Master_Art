<template>
  <div class="favorites-container">
    <div class="page-header">
      <h2>{{ $t('user.favorites') || '我的收藏' }}</h2>
    </div>

    <!-- 筛选和排序 -->
    <div class="filter-section">
      <el-input
        v-model="searchQuery"
        :placeholder="$t('common.search') || '搜索收藏'"
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
          <el-option label="收藏时间 (最新)" value="collected_desc" />
          <el-option label="收藏时间 (最早)" value="collected_asc" />
          <el-option label="名称 (A-Z)" value="title_asc" />
          <el-option label="名称 (Z-A)" value="title_desc" />
          <el-option label="艺术家 (A-Z)" value="artist_asc" />
        </el-select>
      </div>
    </div>

    <!-- 收藏列表 -->
    <div class="favorites-list" v-loading="loading">
      <el-empty v-if="favorites.length === 0" :description="$t('favorites.noFavorites') || '暂无收藏艺术品'" />

      <el-row :gutter="20" v-else>
        <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="item in favorites" :key="item.id" class="favorite-item-col">
          <el-card class="favorite-card" :body-style="{ padding: '0px' }">
            <div class="favorite-image">
              <img :src="item.artwork.imageUrl" :alt="item.artwork.title" @click="viewArtworkDetail(item.artwork.id)">
              <div class="favorite-date">{{ formatDate(item.collectedAt) }}</div>
            </div>
            <div class="favorite-info">
              <h3 class="favorite-title" @click="viewArtworkDetail(item.artwork.id)">{{ item.artwork.title }}</h3>
              <p class="favorite-artist" @click="viewArtistDetail(item.artwork.artist.id)">
                {{ item.artwork.artist.name }}
              </p>
              <p class="favorite-style">{{ item.artwork.style }}</p>
            </div>
            <div class="favorite-actions">
              <el-button-group>
                <el-button size="small" @click="viewArtworkDetail(item.artwork.id)">
                  <el-icon><View /></el-icon>
                </el-button>
                <el-button size="small" type="danger" @click="confirmRemove(item)">
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
          :total="totalFavorites"
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
      <span>{{ $t('favorites.removeConfirm') || '确定要移除收藏' }} "{{ favoriteToRemove?.artwork?.title }}"?</span>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="removeDialogVisible = false">{{ $t('common.cancel') }}</el-button>
          <el-button type="danger" @click="removeFavorite">{{ $t('common.confirm') }}</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, View, Delete } from '@element-plus/icons-vue'
import { format } from 'date-fns'

const router = useRouter()

// 状态变量
const loading = ref(false)
const favorites = ref([])
const totalFavorites = ref(0)
const currentPage = ref(1)
const pageSize = ref(12)
const searchQuery = ref('')
const filters = reactive({
  style: ''
})
const sortBy = ref('collected_desc')
const removeDialogVisible = ref(false)
const favoriteToRemove = ref(null)

// 获取收藏列表
const fetchFavorites = async () => {
  loading.value = true
  try {
    // 这里应该调用API获取用户的收藏列表
    // 模拟数据
    const response = {
      data: [
        {
          id: 1,
          collectedAt: '2023-10-15T14:30:00Z',
          artwork: {
            id: 101,
            title: '春天的印象',
            imageUrl: 'https://via.placeholder.com/300x200',
            style: '印象派',
            artist: {
              id: 201,
              name: '克劳德·莫奈'
            }
          }
        },
        {
          id: 2,
          collectedAt: '2023-09-22T10:15:00Z',
          artwork: {
            id: 102,
            title: '星空',
            imageUrl: 'https://via.placeholder.com/300x200',
            style: '后印象派',
            artist: {
              id: 202,
              name: '文森特·梵高'
            }
          }
        },
        {
          id: 3,
          collectedAt: '2023-11-05T16:45:00Z',
          artwork: {
            id: 103,
            title: '格尔尼卡',
            imageUrl: 'https://via.placeholder.com/300x200',
            style: '现代主义',
            artist: {
              id: 203,
              name: '巴勃罗·毕加索'
            }
          }
        },
        {
          id: 4,
          collectedAt: '2023-08-30T09:20:00Z',
          artwork: {
            id: 104,
            title: '记忆的永恒',
            imageUrl: 'https://via.placeholder.com/300x200',
            style: '超现实主义',
            artist: {
              id: 204,
              name: '萨尔瓦多·达利'
            }
          }
        }
      ],
      total: 4
    }
    
    favorites.value = response.data
    totalFavorites.value = response.total
  } catch (error) {
    console.error('获取收藏列表失败:', error)
    ElMessage.error('获取收藏列表失败，请稍后再试')
  } finally {
    loading.value = false
  }
}

// 格式化日期
const formatDate = (dateString) => {
  try {
    return format(new Date(dateString), 'yyyy-MM-dd')
  } catch (error) {
    return dateString
  }
}

// 处理搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchFavorites()
}

// 处理筛选变化
const handleFilterChange = () => {
  currentPage.value = 1
  fetchFavorites()
}

// 处理排序变化
const handleSortChange = () => {
  fetchFavorites()
}

// 处理页码变化
const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchFavorites()
}

// 处理每页数量变化
const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  fetchFavorites()
}

// 查看艺术品详情
const viewArtworkDetail = (id) => {
  router.push(`/artworks/${id}`)
}

// 查看艺术家详情
const viewArtistDetail = (id) => {
  router.push(`/artists/${id}`)
}

// 确认移除对话框
const confirmRemove = (favorite) => {
  favoriteToRemove.value = favorite
  removeDialogVisible.value = true
}

// 移除收藏
const removeFavorite = async () => {
  loading.value = true
  try {
    // 这里应该调用API移除收藏
    // await api.removeFavorite(favoriteToRemove.value.id)
    
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 500))
    
    ElMessage.success('已从收藏中移除')
    removeDialogVisible.value = false
    
    // 更新列表
    favorites.value = favorites.value.filter(item => item.id !== favoriteToRemove.value.id)
    totalFavorites.value--
  } catch (error) {
    console.error('移除收藏失败:', error)
    ElMessage.error('移除收藏失败，请稍后再试')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchFavorites()
})
</script>

<style scoped>
.favorites-container {
  padding: 20px;
}

.page-header {
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

.favorite-item-col {
  margin-bottom: 20px;
}

.favorite-card {
  height: 100%;
  transition: transform 0.3s;
}

.favorite-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
}

.favorite-image {
  position: relative;
  height: 200px;
  overflow: hidden;
}

.favorite-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  cursor: pointer;
  transition: transform 0.3s;
}

.favorite-image img:hover {
  transform: scale(1.05);
}

.favorite-date {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  color: white;
  background-color: rgba(0, 0, 0, 0.6);
}

.favorite-info {
  padding: 15px;
}

.favorite-title {
  margin: 0 0 5px 0;
  font-size: 16px;
  cursor: pointer;
}

.favorite-title:hover {
  color: #8a6a33;
}

.favorite-artist {
  margin: 0 0 5px 0;
  font-size: 14px;
  color: #8a6a33;
  cursor: pointer;
}

.favorite-artist:hover {
  text-decoration: underline;
}

.favorite-style {
  margin: 0;
  font-size: 14px;
  color: #666;
}

.favorite-actions {
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