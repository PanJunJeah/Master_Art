<template>
  <div class="forgot-password-page">
    <!-- 英雄区域 -->
    <section class="hero-section">
      <div class="container">
        <h2>{{ $t('user.forgotPassword') || '忘记密码' }}</h2>
        <p>{{ $t('user.forgotPasswordDescription') || '重置您的密码，恢复账号访问' }}</p>
      </div>
    </section>
    
    <!-- 忘记密码表单 -->
    <section class="form-section">
      <div class="container">
        <div class="form-container">
          <h2 class="form-title">{{ $t('user.resetPassword') || '重置密码' }}</h2>
          
          <el-form 
            ref="resetForm" 
            :model="resetData" 
            :rules="resetRules" 
            label-position="top"
            @submit.prevent="handleSubmit"
          >
            <el-form-item :label="$t('user.email') || '邮箱'" prop="email">
              <el-input 
                v-model="resetData.email" 
                type="text"
                :placeholder="$t('user.emailPlaceholder') || '请输入邮箱'"
                prefix-icon="Message"
              ></el-input>
            </el-form-item>
            
            <el-form-item :label="$t('user.verificationCode') || '验证码'" prop="code">
              <div class="verification-code-container">
                <el-input 
                  v-model="resetData.code" 
                  :placeholder="$t('user.verificationCodePlaceholder') || '请输入验证码'"
                  prefix-icon="Key"
                ></el-input>
                <el-button 
                  type="primary" 
                  class="send-code-btn" 
                  :disabled="codeSending || countdown > 0"
                  @click="sendVerificationCode"
                >
                  {{ countdown > 0 ? `${countdown}秒后重试` : ($t('user.sendCode') || '发送验证码') }}
                </el-button>
              </div>
            </el-form-item>
            
            <el-form-item :label="$t('user.newPassword') || '新密码'" prop="password">
              <el-input 
                v-model="resetData.password" 
                type="password" 
                :placeholder="$t('user.newPasswordPlaceholder') || '请输入新密码'"
                prefix-icon="Lock"
                show-password
              ></el-input>
            </el-form-item>
            
            <el-form-item :label="$t('user.confirmPassword') || '确认密码'" prop="confirmPassword">
              <el-input 
                v-model="resetData.confirmPassword" 
                type="password" 
                :placeholder="$t('user.confirmPasswordPlaceholder') || '请确认新密码'"
                prefix-icon="Lock"
                show-password
              ></el-input>
            </el-form-item>
            
            <el-form-item>
              <el-button 
                type="primary" 
                native-type="submit" 
                class="submit-btn" 
                :loading="loading"
              >
                {{ $t('user.resetPassword') || '重置密码' }}
              </el-button>
            </el-form-item>
          </el-form>
          
          <div class="form-footer">
            <p>{{ $t('user.rememberPassword') || '想起密码了？' }} <router-link to="/login">{{ $t('user.loginNow') || '立即登录' }}</router-link></p>
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
import { Message, Lock, Key } from '@element-plus/icons-vue'
import service from '@/utils/request'

const router = useRouter()
const userStore = useUserStore()
const resetForm = ref(null)
const loading = ref(false)
const codeSending = ref(false)
const countdown = ref(0)

// 重置密码表单数据
const resetData = reactive({
  email: '',
  code: '',
  password: '',
  confirmPassword: ''
})

// 密码验证函数
const validatePass = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请输入新密码'))
  } else {
    if (resetData.confirmPassword !== '') {
      resetForm.value.validateField('confirmPassword')
    }
    callback()
  }
}

// 确认密码验证函数
const validateConfirmPass = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入新密码'))
  } else if (value !== resetData.password) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

// 表单验证规则
const resetRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { min: 6, max: 6, message: '验证码长度应为6位', trigger: 'blur' }
  ],
  password: [
    { required: true, validator: validatePass, trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度应为6-20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPass, trigger: 'blur' }
  ]
}

// 发送验证码
const sendVerificationCode = async () => {
  if (!resetData.email) {
    ElMessage.warning('请先输入邮箱')
    return
  }
  
  // 验证邮箱格式
  const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailPattern.test(resetData.email)) {
    ElMessage.warning('请输入正确的邮箱格式')
    return
  }
  
  codeSending.value = true
  try {
    // 调用发送验证码接口
    const response = await service.post('/api/auth/send-reset-code', { email: resetData.email })
    
    if (response.data.success) {
      ElMessage.success('验证码已发送到您的邮箱')
      // 开始倒计时
      countdown.value = 60
      const timer = setInterval(() => {
        countdown.value--
        if (countdown.value <= 0) {
          clearInterval(timer)
        }
      }, 1000)
    } else {
      ElMessage.error(response.data.message || '验证码发送失败')
    }
  } catch (error) {
    console.error('发送验证码出错:', error)
    ElMessage.error('发送验证码失败，请稍后再试')
  } finally {
    codeSending.value = false
  }
}

// 处理提交
const handleSubmit = async () => {
  if (!resetForm.value) return
  
  await resetForm.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        // 调用重置密码接口
        const response = await service.post('/api/auth/reset-password', {
          email: resetData.email,
          code: resetData.code,
          newPassword: resetData.password
        })
        
        if (response.data.success) {
          ElMessage.success('密码重置成功')
          router.push('/login')
        } else {
          ElMessage.error(response.data.message || '密码重置失败')
        }
      } catch (error) {
        console.error('重置密码出错:', error)
        ElMessage.error('重置密码过程中发生错误，请稍后再试')
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

.verification-code-container {
  display: flex;
  gap: 10px;
}

.send-code-btn {
  flex-shrink: 0;
  white-space: nowrap;
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
  
  .verification-code-container {
    flex-direction: column;
    gap: 10px;
  }
  
  .send-code-btn {
    width: 100%;
  }
}
</style>