<template>
  <div class="planned-exhibitions-container">
    <div class="page-header">
      <h2>{{ $t('user.plannedExhibitions') || '计划参观展览' }}</h2>
    </div>

    <!-- 筛选和排序 -->
    <div class="filter-section">
      <el-input
        v-model="searchQuery"
        :placeholder="$t('common.search') || '搜索展览'"
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
          :placeholder="$t('exhibitions.status') || '状态'"
          clearable
          @change="handleFilterChange"
        >
          <el-option label="即将参观" value="upcoming" />
          <el-option label="已参观" value="visited" />
          <el-option label="已过期" value="expired" />
        </el-select>

        <el-select
          v-model="sortBy"
          :placeholder="$t('exhibitions.sortBy') || '排序方式'"
          @change="handleSortChange"
        >
          <el-option label="计划日期 (最近)" value="date_asc" />
          <el-option label="计划日期 (最远)" value="date_desc" />
          <el-option label="添加时间 (最新)" value="added_desc" />
          <el-option label="添加时间 (最早)" value="added_asc" />
        </el-select>
      </div>
    </div>

    <!-- 展览列表 -->
    <div class="exhibitions-list" v-loading="loading">
      <el-empty v-if="plannedExhibitions.length === 0" :description="$t('exhibitions.noPlanned') || '暂无计划参观的展览'" />

      <el-row :gutter="20" v-else>
        <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="item in plannedExhibitions" :key="item.id" class="exhibition-item-col">
          <el-card class="exhibition-card" :body-style="{ padding: '0px' }">
            <div class="exhibition-image">
              <img :src="item.exhibition.imageUrl" :alt="item.exhibition.title" @click="viewExhibitionDetail(item.exhibition.id)">
              <div class="exhibition-status" :class="getStatusClass(item)">
                {{ getStatusText(item) }}
              </div>
            </div>
            <div class="exhibition-info">
              <h3 class="exhibition-title" @click="viewExhibitionDetail(item.exhibition.id)">{{ item.exhibition.title }}</h3>
              <p class="exhibition-date">
                <el-icon><Calendar /></el-icon>
                {{ formatDate(item.exhibition.startDate) }} - {{ formatDate(item.exhibition.endDate) }}
              </p>
              <p class="exhibition-location">
                <el-icon><Location /></el-icon>
                {{ item.exhibition.location }}
              </p>
              <p class="planned-date">
                <el-icon><Timer /></el-icon>
                {{ $t('exhibitions.plannedVisit') || '计划参观' }}: {{ formatDate(item.plannedDate) }}
              </p>
            </div>
            <div class="exhibition-actions">
              <el-button-group>
                <el-button size="small" @click="viewExhibitionDetail(item.exhibition.id)">
                  <el-icon><View /></el-icon>
                </el-button>
                <el-button size="small" @click="editPlannedDate(item)">
                  <el-icon><Edit /></el-icon>
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
          :total="totalPlannedExhibitions"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 编辑计划日期对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      :title="$t('exhibitions.editPlannedDate') || '修改计划参观日期'"
      width="30%"
    >
      <el-form :model="editForm" label-position="top">
        <el-form-item :label="$t('exhibitions.exhibition') || '展览'">
          <div>{{ editingItem?.exhibition?.title }}</div>
        </el-form-item>
        <el-form-item :label="$t('exhibitions.exhibitionPeriod') || '展览时间'">
          <div>{{ formatDate(editingItem?.exhibition?.startDate) }} - {{ formatDate(editingItem?.exhibition?.endDate) }}</div>
        </el-form-item>
        <el-form-item :label="$t('exhibitions.plannedDate') || '计划参观日期'" prop="plannedDate">
          <el-date-picker
            v-model="editForm.plannedDate"
            type="date"
            :placeholder="$t('exhibitions.selectDate') || '选择日期'"
            :disabled-date="disabledDate"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item :label="$t('exhibitions.notes') || '备注'" prop="notes">
          <el-input
            v-model="editForm.notes"
            type="textarea"
            :rows="3"
            :placeholder="$t('exhibitions.notesPlaceholder') || '添加备注信息...'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="editDialogVisible = false">{{ $t('common.cancel') }}</el-button>
          <el-button type="primary" @click="savePlannedDate">{{ $t('common.save') }}</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 删除确认对话框 -->
    <el-dialog
      v-model="removeDialogVisible"
      :title="$t('common.confirm') || '确认'"
      width="30%"
    >
      <span>{{ $t('exhibitions.removeConfirm') || '确定要移除计划参观' }} "{{ itemToRemove?.exhibition?.title }}"?</span>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="removeDialogVisible = false">{{ $t('common.cancel') }}</el-button>
          <el-button type="danger" @click="removePlannedExhibition">{{ $t('common.confirm') }}</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, View, Edit, Delete, Calendar, Location, Timer } from '@element-plus/icons-vue'
import { format, isAfter, isBefore, parseISO } from 'date-fns'
import response from "vue-3d-model";

const router = useRouter()

// 状态变量
const loading = ref(false)
const plannedExhibitions = ref([])
const totalPlannedExhibitions = ref(0)
const currentPage = ref(1)
const pageSize = ref(12)
const searchQuery = ref('')
const filters = reactive({
  status: ''
})
const sortBy = ref('date_asc')
const editDialogVisible = ref(false)
const removeDialogVisible = ref(false)
const editingItem = ref(null)
const itemToRemove = ref(null)
const editForm = reactive({
  plannedDate: null,
  notes: ''
})

// 获取计划参观展览列表
const fetchPlannedExhibitions = async () => {
  loading.value = true
  try {
    // 这里应该调用API获取用户的计划参观展览列表
    plannedExhibitions.value = response.data
    totalPlannedExhibitions.value = response.total
  } catch (error) {
    console.error('获取计划参观展览列表失败:', error)
    ElMessage.error('获取计划参观展览列表失败，请稍后再试')
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

// 获取展览状态类名
const getStatusClass = (item) => {
  const today = new Date()
  const plannedDate = parseISO(item.plannedDate)
  const endDate = parseISO(item.exhibition.endDate)
  
  if (isBefore(endDate, today)) {
    return 'status-expired'
  } else if (isBefore(plannedDate, today)) {
    return 'status-visited'
  } else {
    return 'status-upcoming'
  }
}

// 获取展览状态文本
const getStatusText = (item) => {
  const today = new Date()
  const plannedDate = parseISO(item.plannedDate)
  const endDate = parseISO(item.exhibition.endDate)
  
  if (isBefore(endDate, today)) {
    return '已过期'
  } else if (isBefore(plannedDate, today)) {
    return '已参观'
  } else {
    return '即将参观'
  }
}

// 禁用日期（不能选择展览日期范围之外的日期）
const disabledDate = (date) => {
  if (!editingItem.value || !editingItem.value.exhibition) return true
  
  const startDate = parseISO(editingItem.value.exhibition.startDate)
  const endDate = parseISO(editingItem.value.exhibition.endDate)
  
  return isBefore(date, startDate) || isAfter(date, endDate)
}

// 处理搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchPlannedExhibitions()
}

// 处理筛选变化
const handleFilterChange = () => {
  currentPage.value = 1
  fetchPlannedExhibitions()
}

// 处理排序变化
const handleSortChange = () => {
  fetchPlannedExhibitions()
}

// 处理页码变化
const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchPlannedExhibitions()
}

// 处理每页数量变化
const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  fetchPlannedExhibitions()
}

// 查看展览详情
const viewExhibitionDetail = (id) => {
  router.push(`/exhibitions/${id}`)
}

// 编辑计划日期
const editPlannedDate = (item) => {
  editingItem.value = item
  editForm.plannedDate = parseISO(item.plannedDate)
  editForm.notes = item.notes || ''
  editDialogVisible.value = true
}

// 保存计划日期
const savePlannedDate = async () => {
  if (!editForm.plannedDate) {
    ElMessage.warning('请选择计划参观日期')
    return
  }
  
  loading.value = true
  try {
    // 这里应该调用API保存计划参观日期
    // await api.updatePlannedExhibition(editingItem.value.id, editForm)
    
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 500))
    
    // 更新本地数据
    const index = plannedExhibitions.value.findIndex(item => item.id === editingItem.value.id)
    if (index !== -1) {
      plannedExhibitions.value[index].plannedDate = format(editForm.plannedDate, 'yyyy-MM-dd')
      plannedExhibitions.value[index].notes = editForm.notes
    }
    
    ElMessage.success('计划参观日期已更新')
    editDialogVisible.value = false
  } catch (error) {
    console.error('更新计划参观日期失败:', error)
    ElMessage.error('更新失败，请稍后再试')
  } finally {
    loading.value = false
  }
}

// 确认移除对话框
const confirmRemove = (item) => {
  itemToRemove.value = item
  removeDialogVisible.value = true
}

// 移除计划参观展览
const removePlannedExhibition = async () => {
  loading.value = true
  try {
    // 这里应该调用API移除计划参观展览
    // await api.removePlannedExhibition(itemToRemove.value.id)
    
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 500))
    
    ElMessage.success('已移除计划参观展览')
    removeDialogVisible.value = false
    
    // 更新列表
    plannedExhibitions.value = plannedExhibitions.value.filter(item => item.id !== itemToRemove.value.id)
    totalPlannedExhibitions.value--
  } catch (error) {
    console.error('移除计划参观展览失败:', error)
    ElMessage.error('移除失败，请稍后再试')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchPlannedExhibitions()
})
</script>

<style scoped>
.planned-exhibitions-container {
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

.exhibition-item-col {
  margin-bottom: 20px;
}

.exhibition-card {
  height: 100%;
  transition: transform 0.3s;
}

.exhibition-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
}

.exhibition-image {
  position: relative;
  height: 200px;
  overflow: hidden;
}

.exhibition-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  cursor: pointer;
  transition: transform 0.3s;
}

.exhibition-image img:hover {
  transform: scale(1.05);
}

.exhibition-status {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  color: white;
}

.status-upcoming {
  background-color: #1f2a24;
}

.status-visited {
  background-color: #4caf50;
}

.status-expired {
  background-color: #9e9e9e;
}

.exhibition-info {
  padding: 15px;
}

.exhibition-title {
  margin: 0 0 10px 0;
  font-size: 16px;
  cursor: pointer;
}

.exhibition-title:hover {
  color: #8a6a33;
}

.exhibition-date,
.exhibition-location,
.planned-date {
  display: flex;
  align-items: center;
  margin: 0 0 5px 0;
  font-size: 14px;
  color: #666;
}

.exhibition-date .el-icon,
.exhibition-location .el-icon,
.planned-date .el-icon {
  margin-right: 5px;
  font-size: 16px;
}

.planned-date {
  font-weight: 500;
  color: #8a6a33;
}

.exhibition-actions {
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