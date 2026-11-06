<template>
  <div class="social-platform-container">
    <!-- 帖子发布区域 -->
    <el-card class="post-editor">
      <el-form :model="postForm" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="postForm.title" placeholder="输入帖子标题"/>
        </el-form-item>
        <el-form-item label="内容">
          <el-input
            v-model="postForm.content"
            type="textarea"
            :rows="4"
            placeholder="分享你的艺术见解..."
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSubmit">发布</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 帖子列表 -->
    <div class="post-list">
      <el-card
        v-for="post in posts"
        :key="post.id"
        class="post-item"
      >
        <div class="post-header">
          <el-avatar :size="40" :src="post.author.avatar"/>
          <div class="post-author">
            <h3>{{ post.author.name }}</h3>
            <span class="post-time">{{ formatTime(post.createdAt) }}</span>
          </div>
        </div>
        <div class="post-content">
          <h4>{{ post.title }}</h4>
          <p>{{ post.content }}</p>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

const postForm = ref({
  title: '',
  content: ''
})

const posts = ref([
  {
    id: 1,
    title: '印象派绘画技法分享',
    content: '最近在研究莫奈的笔触运用...',
    author: {
      name: '张艺',
      avatar: 'https://img1.baidu.com/it/u=2269292895,825961646&fm=253&fmt=auto&app=120&f=JPEG?w=800&h=800'
    },
    createdAt: new Date('2024-03-15')
  }
])

const handleSubmit = () => {
  if (!postForm.value.title || !postForm.value.content) {
    ElMessage.error('请填写完整内容')
    return
  }
  
  // TODO: 调用后端API提交帖子
  posts.value.unshift({
    id: posts.value.length + 1,
    ...postForm.value,
    author: {
      name: '当前用户',
      avatar: 'https://example.com/avatar.jpg'
    },
    createdAt: new Date()
  })
  
  postForm.value = { title: '', content: '' }
  ElMessage.success('发布成功')
}

const formatTime = (date) => {
  return new Date(date).toLocaleString()
}
</script>

<style scoped>
.social-platform-container {
  max-width: 800px;
  margin: 20px auto;
  padding: 20px;
}

.post-editor {
  margin-bottom: 30px;
}

.post-list {
  display: grid;
  gap: 20px;
}

.post-item {
  transition: box-shadow 0.3s;
}

.post-header {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
}

.post-author {
  margin-left: 15px;
}

.post-author h3 {
  margin: 0;
  font-size: 16px;
}

.post-time {
  font-size: 12px;
  color: #999;
}

.post-content h4 {
  margin: 10px 0;
  color: #333;
}

.post-content p {
  color: #666;
  line-height: 1.6;
}
</style>
