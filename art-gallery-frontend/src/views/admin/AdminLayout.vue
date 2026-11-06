<template>
  <div class="admin-layout">
    <!-- 侧边栏导航 -->
    <div class="admin-sidebar">
      <div class="sidebar-header">
        <router-link to="/">
          <h2>{{ $t('site.title') }}</h2>
        </router-link>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="sidebar-menu"
        :collapse="isCollapse"
        background-color="#001529"
        text-color="#fff"
        active-text-color="#8a6a33"
        router
      >
        <el-menu-item index="/admin/dashboard">
          <el-icon><DataBoard /></el-icon>
          <span>{{ $t('admin.dashboard') || '仪表盘' }}</span>
        </el-menu-item>
        <el-menu-item index="/admin/artworks">
          <el-icon><Picture /></el-icon>
          <span>{{ $t('admin.artworks') || '艺术品管理' }}</span>
        </el-menu-item>
        <el-menu-item index="/admin/exhibitions">
          <el-icon><Calendar /></el-icon>
          <span>{{ $t('admin.exhibitions') || '展览管理' }}</span>
        </el-menu-item>
        <el-menu-item index="/admin/artists">
          <el-icon><User /></el-icon>
          <span>{{ $t('admin.artists') || '艺术家管理' }}</span>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><UserFilled /></el-icon>
          <span>{{ $t('admin.users') || '用户管理' }}</span>
        </el-menu-item>
        <el-menu-item index="/admin/settings">
          <el-icon><Setting /></el-icon>
          <span>{{ $t('admin.settings') || '系统设置' }}</span>
        </el-menu-item>
      </el-menu>
      <div class="sidebar-footer">
        <el-button 
          type="text" 
          class="collapse-btn" 
          @click="toggleCollapse"
        >
          <el-icon v-if="isCollapse"><Expand /></el-icon>
          <el-icon v-else><Fold /></el-icon>
        </el-button>
      </div>
    </div>

    <!-- 主内容区域 -->
    <div class="admin-content" :class="{ 'content-expanded': isCollapse }">
      <!-- 顶部导航栏 -->
      <div class="admin-header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/admin/dashboard' }">{{ $t('admin.dashboard') || '仪表盘' }}</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentPageTitle">{{ currentPageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="admin-user">
              <el-avatar :size="32" :src="userAvatar">{{ userInitials }}</el-avatar>
              <span class="admin-username">{{ username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">{{ $t('user.profile') || '个人资料' }}</el-dropdown-item>
                <el-dropdown-item command="settings">{{ $t('user.settings') || '账号设置' }}</el-dropdown-item>
                <el-dropdown-item command="logout" divided>{{ $t('user.logout') || '退出登录' }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <!-- 页面内容 -->
      <div class="admin-page-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../../stores'
import {
  DataBoard, Picture, Calendar, User, UserFilled, Setting,
  Expand, Fold, ArrowDown
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 侧边栏折叠状态
const isCollapse = ref(false)

// 计算当前激活的菜单项
const activeMenu = computed(() => {
  return route.path
})

// 计算当前页面标题
const currentPageTitle = computed(() => {
  // 根据路由路径返回对应的页面标题
  const pathMap = {
    '/admin/dashboard': '仪表盘',
    '/admin/artworks': '艺术品管理',
    '/admin/exhibitions': '展览管理',
    '/admin/artists': '艺术家管理',
    '/admin/users': '用户管理',
    '/admin/settings': '系统设置'
  }
  return pathMap[route.path] || ''
})

// 用户信息
const username = computed(() => {
  return userStore.currentUser?.username || '管理员'
})

const userAvatar = computed(() => {
  return userStore.currentUser?.avatar || ''
})

const userInitials = computed(() => {
  const name = username.value
  return name ? name.charAt(0).toUpperCase() : 'A'
})

// 切换侧边栏折叠状态
const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

// 处理下拉菜单命令
const handleCommand = (command) => {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  } else if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'settings') {
    router.push('/admin/settings')
  }
}

onMounted(() => {
  // 检查用户是否有管理员权限
  if (userStore.userRole !== 'admin') {
    router.push('/')
  }
})
</script>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
}

.admin-sidebar {
  width: 240px;
  background-color: #001529;
  color: #fff;
  transition: width 0.3s;
  display: flex;
  flex-direction: column;
  position: fixed;
  height: 100vh;
  z-index: 1000;
}

.admin-sidebar.collapsed {
  width: 64px;
}

.sidebar-header {
  padding: 16px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.sidebar-header h2 {
  color: #fff;
  margin: 0;
  font-size: 18px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar-menu {
  flex: 1;
  border-right: none;
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  justify-content: center;
}

.collapse-btn {
  color: #fff;
}

.admin-content {
  flex: 1;
  margin-left: 240px;
  transition: margin-left 0.3s;
}

.content-expanded {
  margin-left: 64px;
}

.admin-header {
  height: 64px;
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  position: sticky;
  top: 0;
  z-index: 999;
}

.admin-user {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.admin-username {
  margin: 0 8px;
}

.admin-page-content {
  padding: 24px;
  background-color: #f0f2f5;
  min-height: calc(100vh - 64px);
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .admin-sidebar {
    width: 64px;
  }
  
  .admin-content {
    margin-left: 64px;
  }
  
  .admin-sidebar.collapsed {
    width: 0;
    overflow: hidden;
  }
  
  .content-expanded {
    margin-left: 0;
  }
}
</style>