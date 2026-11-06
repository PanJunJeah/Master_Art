<template>
  <div class="profile-container">
    <el-card class="profile-card">
      <template #header>
        <div class="card-header">
          <h2>{{ $t('user.profile') }}</h2>
        </div>
      </template>
      
      <div class="profile-content" v-loading="loading">
        <!-- 用户基本信息 -->
        <div class="user-info">
          <div class="avatar-container">
            <el-avatar :size="100" :src="userAvatar">{{ userInitials }}</el-avatar>
            <el-button size="small" type="primary" class="upload-btn" @click="handleAvatarUpload">
              {{ $t('user.changeAvatar') || '更换头像' }}
            </el-button>
          </div>
          
          <div class="user-details">
            <el-form :model="userForm" label-position="top" :rules="rules" ref="userFormRef">
              <el-form-item :label="$t('user.username') || '用户名'" prop="username">
                <el-input v-model="userForm.username" :disabled="true" />
              </el-form-item>
              
              <el-form-item :label="$t('user.email') || '邮箱'" prop="email">
                <el-input v-model="userForm.email" />
              </el-form-item>
              
              <el-form-item :label="$t('user.fullName') || '姓名'" prop="fullName">
                <el-input v-model="userForm.fullName" />
              </el-form-item>
              
              <el-form-item :label="$t('user.phone') || '电话'" prop="phone">
                <el-input v-model="userForm.phone" />
              </el-form-item>
              
              <el-form-item :label="$t('user.bio') || '个人简介'" prop="bio">
                <el-input type="textarea" v-model="userForm.bio" :rows="4" />
              </el-form-item>
              
              <el-form-item>
                <el-button type="primary" @click="saveProfile">{{ $t('common.save') }}</el-button>
                <el-button @click="resetForm">{{ $t('common.cancel') }}</el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>
        
        <!-- 安全设置 -->
        <div class="security-settings">
          <h3>{{ $t('user.securitySettings') || '安全设置' }}</h3>
          
          <el-divider />
          
          <div class="security-item">
            <div class="security-info">
              <h4>{{ $t('user.changePassword') || '修改密码' }}</h4>
              <p>{{ $t('user.passwordLastChanged') || '上次修改时间' }}: {{ passwordLastChanged }}</p>
            </div>
            <el-button @click="showChangePasswordDialog">{{ $t('user.change') || '修改' }}</el-button>
          </div>
          
          <el-divider />
          
          <div class="security-item">
            <div class="security-info">
              <h4>{{ $t('user.twoFactorAuth') || '两步验证' }}</h4>
              <p>{{ twoFactorEnabled ? ($t('user.enabled') || '已启用') : ($t('user.disabled') || '未启用') }}</p>
            </div>
            <el-switch v-model="twoFactorEnabled" @change="toggleTwoFactor" />
          </div>
        </div>
      </div>
    </el-card>
    
    <!-- 修改密码对话框 -->
    <el-dialog
      v-model="changePasswordVisible"
      :title="$t('user.changePassword') || '修改密码'"
      width="500px"
    >
      <el-form :model="passwordForm" label-position="top" :rules="passwordRules" ref="passwordFormRef">
        <el-form-item :label="$t('user.currentPassword') || '当前密码'" prop="currentPassword">
          <el-input v-model="passwordForm.currentPassword" type="password" show-password />
        </el-form-item>
        
        <el-form-item :label="$t('user.newPassword') || '新密码'" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password />
        </el-form-item>
        
        <el-form-item :label="$t('user.confirmPassword') || '确认密码'" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="changePasswordVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="changePassword">{{ $t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { useUserStore } from '../stores'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const loading = ref(false)
const changePasswordVisible = ref(false)
const twoFactorEnabled = ref(false)
const userFormRef = ref(null)
const passwordFormRef = ref(null)

// 用户表单数据
const userForm = reactive({
  username: '',
  email: '',
  fullName: '',
  phone: '',
  bio: ''
})

// 密码表单数据
const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 表单验证规则
const rules = {
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  fullName: [
    { required: true, message: '请输入姓名', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ]
}

// 密码表单验证规则
const passwordRules = {
  currentPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 计算用户头像
const userAvatar = computed(() => {
  return userStore.currentUser?.avatar || ''
})

// 计算用户名首字母（用于没有头像时显示）
const userInitials = computed(() => {
  const username = userStore.currentUser?.username || ''
  return username.charAt(0).toUpperCase()
})

// 密码上次修改时间
const passwordLastChanged = computed(() => {
  // 这里应该从用户数据中获取，暂时模拟
  return '2023-05-15'
})

// 初始化用户数据
const initUserData = () => {
  const user = userStore.currentUser
  if (user) {
    userForm.username = user.username || ''
    userForm.email = user.email || ''
    userForm.fullName = user.fullName || ''
    userForm.phone = user.phone || ''
    userForm.bio = user.bio || ''
    twoFactorEnabled.value = user.twoFactorEnabled || false
  }
}

// 保存个人资料
const saveProfile = async () => {
  if (!userFormRef.value) return
  
  await userFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        // 这里应该调用API保存用户资料
        // await userStore.updateProfile(userForm)
        ElMessage.success('个人资料保存成功')
      } catch (error) {
        console.error('保存个人资料失败:', error)
        ElMessage.error('保存失败，请稍后再试')
      } finally {
        loading.value = false
      }
    }
  })
}

// 重置表单
const resetForm = () => {
  userFormRef.value?.resetFields()
  initUserData()
}

// 显示修改密码对话框
const showChangePasswordDialog = () => {
  changePasswordVisible.value = true
  passwordForm.currentPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}

// 修改密码
const changePassword = async () => {
  if (!passwordFormRef.value) return
  
  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        // 这里应该调用API修改密码
        // await userStore.changePassword(passwordForm)
        ElMessage.success('密码修改成功')
        changePasswordVisible.value = false
      } catch (error) {
        console.error('修改密码失败:', error)
        ElMessage.error('修改失败，请检查当前密码是否正确')
      } finally {
        loading.value = false
      }
    }
  })
}

// 处理头像上传
const handleAvatarUpload = () => {
  // 这里应该实现头像上传逻辑
  ElMessage.info('头像上传功能待实现')
}

// 切换两步验证
const toggleTwoFactor = async (value) => {
  loading.value = true
  try {
    // 这里应该调用API启用/禁用两步验证
    // await userStore.toggleTwoFactor(value)
    ElMessage.success(value ? '两步验证已启用' : '两步验证已禁用')
  } catch (error) {
    console.error('切换两步验证失败:', error)
    ElMessage.error('操作失败，请稍后再试')
    // 恢复原状态
    twoFactorEnabled.value = !value
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  // 获取用户信息
  if (userStore.isLoggedIn && !userStore.currentUser) {
    userStore.fetchUserInfo()
  }
  
  // 初始化用户数据
  initUserData()
})
</script>

<style scoped>
.profile-container {
  max-width: 1000px;
  margin: 20px auto;
  padding: 0 20px;
}

.profile-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.profile-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 30px;
}

@media (max-width: 768px) {
  .profile-content {
    grid-template-columns: 1fr;
  }
}

.avatar-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 20px;
}

.upload-btn {
  margin-top: 10px;
}

.security-settings h3 {
  margin-bottom: 20px;
  color: #333;
}

.security-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
}

.security-info h4 {
  margin: 0 0 5px 0;
  color: #333;
}

.security-info p {
  margin: 0;
  color: #666;
  font-size: 14px;
}
</style>