<template>
  <div class="not-found-page">
    <div class="container">
      <div class="not-found-content">
        <h1 class="error-code">404</h1>
        <h2 class="error-title">{{ $t('error.pageNotFound') || '页面未找到' }}</h2>
        <p class="error-message">{{ $t('error.pageNotFoundMessage') || '抱歉，您访问的页面不存在或已被移除' }}</p>
        <div class="error-actions">
          <router-link to="/" class="btn btn-primary">
            <el-icon><Back /></el-icon>
            {{ $t('error.backToHome') || '返回首页' }}
          </router-link>
        </div>
      </div>
      <div class="not-found-image">
      </div>
    </div>
  </div>
</template>

<script setup>
import { Back } from '@element-plus/icons-vue'
</script>

<style scoped>
.not-found-page {
  min-height: calc(100vh - 300px);
  display: flex;
  align-items: center;
  padding: 60px 0;
}

.container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
}

.not-found-content {
  flex: 1;
  padding-right: 40px;
  min-width: 300px;
}

.not-found-image {
  flex: 1;
  min-width: 300px;
  text-align: center;
}

.not-found-image img {
  max-width: 100%;
  height: auto;
  border-radius: 8px;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
}

.error-code {
  font-size: 120px;
  font-weight: 700;
  color: #8a6a33;
  margin: 0 0 20px;
  line-height: 1;
}

.error-title {
  font-size: 32px;
  color: #333;
  margin-bottom: 15px;
}

.error-message {
  font-size: 18px;
  color: #666;
  margin-bottom: 30px;
  max-width: 500px;
}

.error-actions {
  margin-top: 20px;
}

.error-actions .btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  font-size: 16px;
}

@media (max-width: 768px) {
  .container {
    flex-direction: column;
  }
  
  .not-found-content {
    padding-right: 0;
    text-align: center;
    margin-bottom: 40px;
  }
  
  .error-message {
    margin: 0 auto 30px;
  }
  
  .error-code {
    font-size: 100px;
  }
}
</style>