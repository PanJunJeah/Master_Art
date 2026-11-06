<template>
  <div class="login-page">
    <!-- 英雄区域 -->
    <section class="hero-section">
      <div class="container">
        <h2>{{ $t('user.login') }}</h2>
        <p>{{'登录您的账号，探索更多艺术内容' }}</p>
      </div>
    </section>
    
    <!-- 登录表单 -->
    <section class="form-section">
      <div class="container">
        <div class="form-container">
          <h2 class="form-title">{{ $t('user.login') }}</h2>
          
          <el-form 
            ref="loginForm" 
            :model="loginData" 
            :rules="loginRules" 
            label-position="top"
            @submit.prevent="handleLogin"
          >
            <el-form-item :label="$t('user.username')" prop="username">
              <el-input 
                v-model="loginData.username" 
                :placeholder="'请输入用户名'"
                prefix-icon="User"
              ></el-input>
            </el-form-item>
            
            <el-form-item :label="$t('user.password')" prop="password">
              <el-input 
                v-model="loginData.password" 
                type="password" 
                :placeholder="'请输入密码'"
                prefix-icon="Lock"
                show-password
              ></el-input>
            </el-form-item>
            
            <!-- 角色选择 -->
            <el-form-item :label="'选择角色'" prop="role">
              <el-select 
                v-model="loginData.role" 
                :placeholder="$t('user.rolePlaceholder') || '请选择角色'"
                style="width: 100%"
              >
                <el-option label="普通用户" value="USER"></el-option>
                <el-option label="艺术家" value="ARTIST"></el-option>
                <el-option label="管理员" value="ADMIN"></el-option>
              </el-select>
            </el-form-item>
            
            <div class="form-options">
              <el-checkbox v-model="loginData.remember">{{ $t('user.rememberMe') }}</el-checkbox>
              <router-link to="/forgot-password" class="forgot-password">{{ $t('user.forgotPassword') }}</router-link>
            </div>
            
            <el-form-item>
              <el-button 
                type="primary" 
                native-type="submit" 
                class="submit-btn" 
                :loading="loading"
              >
                {{ $t('user.loginNow') }}
              </el-button>
            </el-form-item>
          </el-form>
          
          <div class="form-footer">
            <p>{{ $t('user.noAccount') }} <router-link to="/register">{{ $t('user.registerNow') }}</router-link></p>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import request from "@/utils/request.js";
import login from "@/views/Login.vue";

const router = useRouter()
const userStore = useUserStore()
const loginForm = ref(null)
const loading = ref(false)

// 登录表单数据
const loginData = reactive({
  username: '',
  password: '',
  remember: false,
  role: 'user' // 默认角色为普通用户
})

// 表单验证规则
const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度应为3-20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度应为6-20个字符', trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ]
}

// 处理登录
const handleLogin = async () => {
  if (!loginForm.value) return

  await loginForm.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const result = await request({
          url: "/api/users/login",
          method: "post",
          headers: { "Content-Type": "application/json" },
          data: {
            username: loginData.username,
            password: loginData.password,
            role: loginData.role
          }
        })
        sessionStorage.setItem('token', result.token)
        sessionStorage.setItem('role', loginData.role)
        sessionStorage.setItem('username', loginData.username)

        ElMessage.success('登录成功')
        router.push('/home')
      } catch (error) {
        ElMessage.error(error.message || '登录失败')
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
/* 英雄区域 */
.hero-section {
  background: linear-gradient(135deg, #1f2a24 0%, #2c3a32 55%, #3a4638 100%);
  color: white;
  padding: 60px 0;
  text-align: center;
  margin-bottom: 40px;
}

.hero-section h2 {
  font-size: 36px;
  margin-bottom: 20px;
}

.hero-section p {
  font-size: 18px;
  max-width: 600px;
  margin: 0 auto;
}

/* 表单部分 */
.form-section {
  padding: 0 0 60px;
}

.form-container {
  max-width: 450px;
  margin: 0 auto;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.1);
  padding: 30px;
}

.form-title {
  font-size: 24px;
  color: #333;
  margin-bottom: 30px;
  text-align: center;
  position: relative;
}

.form-title::after {
  content: '';
  display: block;
  width: 50px;
  height: 3px;
  background-color: #1f2a24;
  margin: 15px auto 0;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.forgot-password {
  color: #8a6a33;
  font-size: 14px;
}

.submit-btn {
  width: 100%;
  padding: 12px 0;
  font-size: 16px;
}

.form-footer {
  margin-top: 20px;
  text-align: center;
  color: #666;
}

.form-footer a {
  color: #8a6a33;
  font-weight: 500;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .hero-section {
    padding: 40px 0;
  }
  
  .hero-section h2 {
    font-size: 28px;
  }
  
  .hero-section p {
    font-size: 16px;
  }
  
  .form-container {
    padding: 20px;
    margin: 0 15px;
  }
}
</style>