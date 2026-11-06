<template>
  <div class="artist-upload-container">
    <div class="page-header">
      <h2>{{ $t('artist.uploadArtwork') || '上传新作品' }}</h2>
    </div>

    <el-card class="upload-card">
      <el-form
        :model="artworkForm"
        :rules="rules"
        ref="artworkFormRef"
        label-position="top"
        v-loading="loading"
      >
        <!-- 基本信息 -->
        <h3>{{ $t('artworks.basicInfo') || '基本信息' }}</h3>
        <el-divider />

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item :label="$t('artworks.title') || '作品标题'" prop="title">
              <el-input v-model="artworkForm.title" maxlength="100" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('artworks.creationYear') || '创作年份'" prop="creationYear">
              <el-date-picker
                v-model="artworkForm.creationYear"
                type="year"
                :placeholder="$t('artworks.selectYear') || '选择年份'"
                format="YYYY"
                value-format="YYYY"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item :label="$t('artworks.style') || '艺术流派'" prop="style">
              <el-select
                v-model="artworkForm.style"
                :placeholder="$t('artworks.selectStyle') || '选择艺术流派'"
                style="width: 100%"
              >
                <el-option label="现代主义" value="modernism" />
                <el-option label="印象派" value="impressionism" />
                <el-option label="抽象艺术" value="abstract" />
                <el-option label="写实主义" value="realism" />
                <el-option label="其他" value="other" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('artworks.medium') || '创作媒介'" prop="medium">
              <el-select
                v-model="artworkForm.medium"
                :placeholder="$t('artworks.selectMedium') || '选择创作媒介'"
                style="width: 100%"
              >
                <el-option label="油画" value="oil" />
                <el-option label="水彩" value="watercolor" />
                <el-option label="丙烯" value="acrylic" />
                <el-option label="数字艺术" value="digital" />
                <el-option label="雕塑" value="sculpture" />
                <el-option label="摄影" value="photography" />
                <el-option label="混合媒介" value="mixed" />
                <el-option label="其他" value="other" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item :label="$t('artworks.description') || '作品描述'" prop="description">
          <el-input
            v-model="artworkForm.description"
            type="textarea"
            :rows="4"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item :label="$t('artworks.dimensions') || '尺寸'" prop="dimensions">
              <el-input v-model="artworkForm.dimensions" placeholder="例如: 60cm x 80cm" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('artworks.price') || '价格'" prop="price">
              <el-input-number
                v-model="artworkForm.price"
                :min="0"
                :precision="2"
                :step="100"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 作品图片上传 -->
        <h3>{{ $t('artworks.images') || '作品图片' }}</h3>
        <el-divider />

        <el-form-item :label="$t('artworks.mainImage') || '主图'" prop="mainImage">
          <el-upload
            class="artwork-uploader"
            action="/api/upload"
            :show-file-list="false"
            :on-success="handleMainImageSuccess"
            :before-upload="beforeImageUpload"
          >
            <img v-if="artworkForm.mainImage" :src="artworkForm.mainImage" class="artwork-image" />
            <el-icon v-else class="artwork-uploader-icon"><Plus /></el-icon>
          </el-upload>
          <div class="upload-tip">
            {{ $t('artworks.mainImageTip') || '请上传清晰的作品主图，建议尺寸不小于1200x800像素' }}
          </div>
        </el-form-item>

        <el-form-item :label="$t('artworks.additionalImages') || '附加图片'">
          <el-upload
            class="artwork-uploader-list"
            action="/api/upload"
            list-type="picture-card"
            :file-list="additionalImages"
            :on-preview="handlePictureCardPreview"
            :on-remove="handleRemove"
            :on-success="handleAdditionalImageSuccess"
            :before-upload="beforeImageUpload"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div class="upload-tip">
            {{ $t('artworks.additionalImagesTip') || '可上传作品细节、不同角度的照片，最多5张' }}
          </div>
        </el-form-item>

        <!-- 3D模型上传（可选） -->
        <h3>{{ $t('artworks.3dModel') || '3D模型（可选）' }}</h3>
        <el-divider />

        <el-form-item :label="$t('artworks.uploadModel') || '上传3D模型'">
          <el-upload
            class="model-uploader"
            action="/api/upload/model"
            :on-success="handleModelSuccess"
            :before-upload="beforeModelUpload"
            :limit="1"
          >
            <el-button type="primary">
              <el-icon><Upload /></el-icon>
              {{ $t('artworks.selectModel') || '选择模型文件' }}
            </el-button>
          </el-upload>
          <div class="upload-tip">
            {{ $t('artworks.modelTip') || '支持.glb、.gltf格式，文件大小不超过20MB' }}
          </div>
        </el-form-item>

        <!-- 发布设置 -->
        <h3>{{ $t('artworks.publishSettings') || '发布设置' }}</h3>
        <el-divider />

        <el-form-item :label="$t('artworks.tags') || '标签'" prop="tags">
          <el-select
            v-model="artworkForm.tags"
            multiple
            filterable
            allow-create
            default-first-option
            :placeholder="$t('artworks.inputTags') || '输入标签'"
            style="width: 100%"
          >
            <el-option
              v-for="tag in commonTags"
              :key="tag"
              :label="tag"
              :value="tag"
            />
          </el-select>
          <div class="upload-tip">
            {{ $t('artworks.tagsTip') || '添加标签有助于用户更好地找到您的作品' }}
          </div>
        </el-form-item>

        <el-form-item :label="$t('artworks.status') || '状态'" prop="status">
          <el-radio-group v-model="artworkForm.status">
            <el-radio label="published">{{ $t('artworks.publish') || '立即发布' }}</el-radio>
            <el-radio label="draft">{{ $t('artworks.saveDraft') || '保存为草稿' }}</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="submitForm">{{ $t('common.save') }}</el-button>
          <el-button @click="resetForm">{{ $t('common.cancel') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 图片预览对话框 -->
    <el-dialog v-model="dialogVisible">
      <img w-full :src="dialogImageUrl" alt="Preview Image" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Upload } from '@element-plus/icons-vue'
import { useArtworkStore } from '../../stores/artwork.js'

const router = useRouter()
const artworkStore = useArtworkStore()
const artworkFormRef = ref(null)
const loading = ref(false)

// 图片预览相关
const dialogImageUrl = ref('')
const dialogVisible = ref(false)
const additionalImages = ref([])

// 常用标签
const commonTags = [
  '风景', '人物', '抽象', '城市', '自然', '动物', '静物', '建筑', '概念艺术', '传统艺术'
]

// 表单数据
const artworkForm = reactive({
  title: '',
  creationYear: '',
  style: '',
  medium: '',
  description: '',
  dimensions: '',
  price: 0,
  mainImage: '',
  additionalImages: [],
  modelUrl: '',
  tags: [],
  status: 'draft'
})

// 表单验证规则
const rules = {
  title: [
    { required: true, message: '请输入作品标题', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在2到100个字符之间', trigger: 'blur' }
  ],
  creationYear: [
    { required: true, message: '请选择创作年份', trigger: 'change' }
  ],
  style: [
    { required: true, message: '请选择艺术流派', trigger: 'change' }
  ],
  medium: [
    { required: true, message: '请选择创作媒介', trigger: 'change' }
  ],
  description: [
    { required: true, message: '请输入作品描述', trigger: 'blur' },
    { min: 10, max: 1000, message: '长度在10到1000个字符之间', trigger: 'blur' }
  ],
  mainImage: [
    { required: true, message: '请上传作品主图', trigger: 'change' }
  ],
  tags: [
    { required: true, message: '请至少添加一个标签', trigger: 'change' },
    { type: 'array', min: 1, max: 10, message: '标签数量在1到10个之间', trigger: 'change' }
  ]
}

// 处理主图上传成功
const handleMainImageSuccess = (response, uploadFile) => {
  artworkForm.mainImage = response.url
  ElMessage.success('主图上传成功')
}

// 处理附加图片上传成功
const handleAdditionalImageSuccess = (response, uploadFile) => {
  artworkForm.additionalImages.push(response.url)
  additionalImages.value.push({
    name: uploadFile.name,
    url: response.url
  })
  ElMessage.success('图片上传成功')
}

// 处理3D模型上传成功
const handleModelSuccess = (response, uploadFile) => {
  artworkForm.modelUrl = response.url
  ElMessage.success('3D模型上传成功')
}

// 图片上传前验证
const beforeImageUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过5MB!')
    return false
  }
  return true
}

// 模型上传前验证
const beforeModelUpload = (file) => {
  const validFormats = ['.glb', '.gltf']
  const isValidFormat = validFormats.some(format => file.name.endsWith(format))
  const isLt20M = file.size / 1024 / 1024 < 20

  if (!isValidFormat) {
    ElMessage.error('只能上传.glb或.gltf格式的3D模型文件!')
    return false
  }
  if (!isLt20M) {
    ElMessage.error('模型文件大小不能超过20MB!')
    return false
  }
  return true
}

// 处理图片预览
const handlePictureCardPreview = (uploadFile) => {
  dialogImageUrl.value = uploadFile.url
  dialogVisible.value = true
}

// 处理图片移除
const handleRemove = (uploadFile, uploadFiles) => {
  const index = artworkForm.additionalImages.findIndex(url => url === uploadFile.url)
  if (index !== -1) {
    artworkForm.additionalImages.splice(index, 1)
  }
}

// 提交表单
const submitForm = async () => {
  if (!artworkFormRef.value) return
  
  await artworkFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        // 这里应该调用API保存艺术品
        // const response = await artworkStore.createArtwork(artworkForm)
        
        // 模拟API调用
        await new Promise(resolve => setTimeout(resolve, 1000))
        
        ElMessage.success(artworkForm.status === 'published' ? '作品发布成功' : '草稿保存成功')
        router.push('/artist/artworks')
      } catch (error) {
        console.error('保存作品失败:', error)
        ElMessage.error('保存失败，请稍后再试')
      } finally {
        loading.value = false
      }
    } else {
      ElMessage.warning('请完善表单信息')
      return false
    }
  })
}

// 重置表单
const resetForm = () => {
  artworkFormRef.value?.resetFields()
  additionalImages.value = []
  router.push('/artist/artworks')
}

onMounted(() => {
  // 可以在这里加载一些初始数据，比如从草稿中恢复等
})
</script>

<style scoped>
.artist-upload-container {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.upload-card {
  margin-bottom: 30px;
}

h3 {
  margin-top: 30px;
  margin-bottom: 10px;
  font-weight: 500;
  color: #333;
}

.artwork-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 300px;
  height: 200px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.artwork-uploader:hover {
  border-color: #409eff;
}

.artwork-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 178px;
  height: 178px;
  line-height: 178px;
  text-align: center;
}

.artwork-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-tip {
  font-size: 12px;
  color: #606266;
  margin-top: 5px;
}

.model-uploader {
  margin-bottom: 10px;
}

.el-form-item {
  margin-bottom: 22px;
}
</style>