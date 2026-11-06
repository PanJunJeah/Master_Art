<template>
  <div class="register-page">
    <!-- 英雄区域 -->
    <section class="hero-section">
      <div class="container">
        <h2>{{ $t('user.register') }}</h2>
        <p>{{ $t('user.registerDescription') || '创建您的账号，开启艺术之旅' }}</p>
      </div>
    </section>
    
    <!-- 注册表单 -->
    <section class="form-section">
      <div class="container">
        <div class="form-container">
          <h2 class="form-title">{{ $t('user.register') }}</h2>

          <!-- 显示错误信息 -->
          <div v-if="errorMessage" class="error-message">
            {{ errorMessage }}
          </div>

          <el-form
              ref="registerForm"
              :model="registerData"
              :rules="registerRules"
              label-position="top"
              @submit.prevent="handleRegister"
          >
            <el-form-item :label="$t('user.username')" prop="username">
              <el-input 
                v-model="registerData.username" 
                :placeholder="$t('user.usernamePlaceholder') || '请输入用户名'"
                prefix-icon="User"
              ></el-input>
            </el-form-item>
            
            <el-form-item :label="$t('user.email')" prop="email">
              <el-input 
                v-model="registerData.email" 
                type="email"
                :placeholder="$t('user.emailPlaceholder') || '请输入邮箱'"
                prefix-icon="Message"
              ></el-input>
            </el-form-item>
            
            <el-form-item :label="$t('user.password')" prop="password">
              <el-input 
                v-model="registerData.password" 
                type="password" 
                :placeholder="$t('user.passwordPlaceholder') || '请输入密码'"
                prefix-icon="Lock"
                show-password
              ></el-input>
            </el-form-item>
            
            <el-form-item :label="$t('user.confirmPassword')" prop="confirmPassword">
              <el-input 
                v-model="registerData.confirmPassword" 
                type="password" 
                :placeholder="$t('user.confirmPasswordPlaceholder') || '请确认密码'"
                prefix-icon="Lock"
                show-password
              ></el-input>
            </el-form-item>
            
            <!-- 角色选择 -->
            <el-form-item :label="$t('user.role') || '选择角色'" prop="role">
              <el-select 
                v-model="registerData.role" 
                :placeholder="$t('user.rolePlaceholder') || '请选择角色'"
                style="width: 100%"
              >
                <el-option label="普通用户" value="user"></el-option>
                <el-option label="艺术家" value="artist"></el-option>
                <el-option label="管理员" value="admin"></el-option>
              </el-select>
            </el-form-item>
            
            <div class="form-options">
              <el-checkbox v-model="registerData.agreement" required>
                {{ $t('user.agreement') || '我已阅读并同意' }} <a href="#">{{ $t('user.termsOfService') || '服务条款' }}</a>
              </el-checkbox>
            </div>
            
            <el-form-item>
              <el-button 
                type="primary" 
                native-type="submit" 
                class="submit-btn" 
                :loading="loading"
                :disabled="!registerData.agreement"
              >
                {{ $t('user.registerNow') }}
              </el-button>
            </el-form-item>
          </el-form>

          <div class="form-footer">
            <p>{{ $t('user.alreadyHaveAccount') }} <router-link to="/login">{{ $t('user.loginNow') }}</router-link></p>
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
import { User, Message, Lock } from '@element-plus/icons-vue'
import api from '@/api'

const router = useRouter()
const userStore = useUserStore()
const registerForm = ref(null)
const loading = ref(false)
// 注册表单数据
const registerData = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  agreement: false,
  role: 'user' // 默认角色为普通用户
})

// 密码验证函数
const validatePass = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请输入密码'))
  } else {
    if (registerData.confirmPassword !== '') {
      registerForm.value.validateField('confirmPassword')
    }
    callback()
  }
}

// 确认密码验证函数
const validateConfirmPass = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== registerData.password) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

// 表单验证规则
const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度应为3-20个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, validator: validatePass, trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度应为6-20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPass, trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ]
}

// 处理注册
const handleRegister = async () => {
  if (!registerForm.value) return

  await registerForm.value.validate(async (valid) => {
    if (valid) {
      if (!registerData.agreement) {
        ElMessage.warning('请阅读并同意服务条款')
        return
      }

      loading.value = true

      try {
        // 使用 registerData 发送请求
        const response = await api.post('/api/users/register', {
          username: registerData.username,
          email: registerData.email,
          password: registerData.password,
          role: registerData.role
        })

        // 检查响应是否包含 token
        if (response.data && response.data.token) {
          // 保存 token 到 localStorage
          localStorage.setItem('jwt_token', response.data.token)

          // 保存用户信息到 store
          if (response.data.user) {
            userStore.setUser(response.data.user)
          }

          // 显示成功消息
          ElMessage.success('注册成功！正在跳转首页...')

          // 重定向到首页
          setTimeout(() => {
            router.push('/')
          }, 1500)
        } else {
          // 特殊处理：成功但没有 token 的情况
          ElMessage.warning('注册成功，但未返回登录令牌。请手动登录')
          router.push('/login')
        }
      } catch (error) {
        // 只显示服务器返回的错误消息
        if (error.response?.data?.message) {
          ElMessage.error(error.response.data.message)
        } else {
          ElMessage.error('注册失败，请稍后重试')
        }
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
  margin-bottom: 20px;
}

.form-options a {
  color: #8a6a33;
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
/* 错误消息样式 */
.error-message {
  color: #f56c6c;
  padding: 10px;
  margin-bottom: 15px;
  border-radius: 4px;
  background-color: #fef0f0;
  text-align: center;
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