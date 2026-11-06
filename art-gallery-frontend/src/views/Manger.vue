
<template>
  <div class="app-container">
    <!-- 加载中状态 -->
    <div class="loading-container" v-if="loading">
      <el-icon class="loading-icon"><Loading /></el-icon>
      <div class="loading-text">{{ $t('common.loading') }}</div>
    </div>

    <!-- 主应用内容 -->
    <div v-else>
      <!-- 头部导航 -->
      <header class="site-header">
        <div class="container">
          <div class="logo">
            <router-link to="/home">
              <h1>{{ $t('site.title') }}</h1>
            </router-link>
          </div>
          <nav class="main-nav">
            <el-menu
                :default-active="activeIndex"
                mode="horizontal"
                router
                background-color="transparent"
                text-color="#333"
                active-text-color="#8a6a33"
            >
              <el-menu-item index="/home">{{ $t('nav.home') }}</el-menu-item>
              <el-menu-item index="/exhibitions">{{ $t('nav.exhibitions') }}</el-menu-item>
              <el-menu-item index="/artworks">{{ $t('nav.artworks') }}</el-menu-item>
              <el-menu-item index="/artists">{{ $t('nav.artists') }}</el-menu-item>
              <el-menu-item index="/social-platform">艺术社区</el-menu-item>
              <el-menu-item index="/ai-art-guide">{{ $t('nav.aiArtGuide') }}</el-menu-item>
              <el-menu-item index="/about">{{ $t('nav.about') }}</el-menu-item>
            </el-menu>
          </nav>
          <div class="user-actions">
            <!-- 语言切换 -->
            <div class="language-switcher">
              <el-dropdown @command="changeLanguage">
                <span class="language-selector">
                  <el-icon><Message /></el-icon>
                  {{ currentLanguageLabel }}
                  <el-icon><ArrowDown /></el-icon>
                </span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="zh">中文</el-dropdown-item>
                    <el-dropdown-item command="en">English</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>

            <!-- 用户菜单 -->
            <template v-if="userStore.isLoggedIn">
              <el-dropdown>
                <span class="user-profile">
                  <el-avatar :size="30" :src="userStore.currentUser?.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"/>
                  {{ userStore.currentUser?.username }}
                  <el-icon><ArrowDown /></el-icon>
                </span>
                <template #dropdown>
                  <el-dropdown-menu>
<!--                    <el-dropdown-item @click="navigateTo('/profile')">-->
<!--                      <el-icon><User /></el-icon> 查看资料-->
<!--                    </el-dropdown-item>-->

                    <el-dropdown-item
                        v-if="['ADMIN', 'USER','ARTIST'].includes(role)"
                        @click="navigateTo('/admin/dashboard')">
                      <el-icon><Setting /></el-icon> 进入后台
                    </el-dropdown-item>

                    <el-dropdown-item @click="navigateTo('/my-reservations')">
                      <el-icon><Ticket /></el-icon> 我的预约
                    </el-dropdown-item>

                    <el-dropdown-item
                        v-if="userStore.userRole === 'ARTIST'"
                        @click="navigateTo('/artist/dashboard')">
                      <el-icon><Brush /></el-icon> 艺术家中心
                    </el-dropdown-item>

                    <el-dropdown-item divided @click="handleLogout">
                      <el-icon><SwitchButton /></el-icon> 退出登录
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
            <template v-else>
              <el-button type="text" @click="router.push('/login')">登录</el-button>
              <el-button type="text" @click="router.push('/register')">注册</el-button>
            </template>
          </div>
        </div>
      </header>

      <!-- 主内容区域 -->
      <main class="site-main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>

      <!-- 页脚 -->
      <footer class="site-footer">
        <div class="container">
          <div class="footer-content">
            <div class="footer-logo">
              <h3>{{ $t('site.title') }}</h3>
              <p>{{ $t('site.slogan') }}</p>
              <div class="social-links">
                <a href="#" class="social-link"><Icon icon="mdi:facebook" /></a>
                <a href="#" class="social-link"><Icon icon="mdi:instagram" /></a>
                <a href="#" class="social-link"><Icon icon="mdi:twitter" /></a>
              </div>
            </div>
            <div class="footer-links">
              <h4>{{ $t('footer.quickLinks') }}</h4>
              <ul>
                <li><router-link to="/home">{{ $t('nav.home') }}</router-link></li>
                <li><router-link to="/exhibitions">{{ $t('nav.exhibitions') }}</router-link></li>
                <li><router-link to="/artworks">{{ $t('nav.artworks') }}</router-link></li>
                <li><router-link to="/artists">{{ $t('nav.artists') }}</router-link></li>
                <li><router-link to="/about">{{ $t('nav.about') }}</router-link></li>
              </ul>
            </div>

            <div class="footer-contact">
              <h4>{{ $t('footer.contactUs') }}</h4>
              <p>{{ $t('footer.email') }}: info@masterart.com</p>
              <p>{{ $t('footer.phone') }}: 18285672864</p>
              <p>{{ $t('footer.address') }}: {{ $t('footer.addressValue') }}</p>
              <div class="newsletter">
                <h4>{{ $t('footer.newsletter') }}</h4>
                <div class="newsletter-form">
                  <el-input v-model="newsletterEmail" :placeholder="$t('footer.emailPlaceholder')" />
                  <el-button type="primary">{{ $t('footer.subscribe') }}</el-button>
                </div>
              </div>
            </div>
          </div>
          <div class="footer-bottom">
            <p>&copy; {{ new Date().getFullYear() }} {{ $t('site.title') }}. {{ $t('footer.rights') }}</p>
          </div>
        </div>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  User,
  Setting,
  Brush,
  SwitchButton,
  ArrowDown,
  Message,
  Loading,
  Ticket
} from '@element-plus/icons-vue'
import { Icon } from '@iconify/vue'
import { useUserStore } from "../stores/index.js"

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const { locale } = useI18n()
const loading = ref(true)
const newsletterEmail = ref('')

console.log('token:',userStore.isLoggedIn )
// 计算当前激活的导航项
const activeIndex = computed(() => {
  return route.path
})
const role=sessionStorage.getItem('role')
// 计算当前语言标签
const currentLanguageLabel = computed(() => {
  return locale.value === 'zh' ? '中文' : 'English'
})

// 导航方法
const navigateTo = (path) => {
  router.push(path)
}

// 切换语言
const changeLanguage = (lang) => {
  locale.value = lang
  localStorage.setItem('language', lang)
}

// 登出处理方法
const handleLogout = () => {
  userStore.logout()
  sessionStorage.removeItem('token')
  sessionStorage.removeItem('username')
  sessionStorage.removeItem('role')
  router.push('/login')
}

onMounted(async () => {
  // 设置语言
  const savedLanguage = localStorage.getItem('language')
  if (savedLanguage) {
    locale.value = savedLanguage
  }

  console.log('App mounted, token exists:', !!userStore.token)



  // 模拟加载过程
  setTimeout(() => {
    loading.value = false
  }, 1000)
})
</script>

<style>
/* 全局样式 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Helvetica Neue', Arial, sans-serif;
  line-height: 1.6;
  color: #333;
  background-color: #f8f9fa;
}

.container {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 15px;
}

a {
  text-decoration: none;
  color: #8a6a33;
  transition: color 0.3s ease;
}

a:hover {
  color: #8a6a33;
}
/* 用户头像和名称样式 */
.user-profile {
  display: flex;
  align-items: center;
  cursor: pointer;
  color: #333;
  font-weight: 500;
  padding: 0 10px;
  gap: 8px;
}

/* 下拉菜单项样式 */
.el-dropdown-menu__item {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 分隔线样式 */
.el-dropdown-menu__item--divided {
  border-top: 1px solid #eee;
  margin-top: 6px;
  padding-top: 6px;
}
/* 按钮样式 */
.btn {
  display: inline-block;
  padding: 8px 16px;
  border-radius: 4px;
  font-weight: 500;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-primary {
  background-color: #1f2a24;
  color: white;
  border: none;
}

.btn-primary:hover {
  background-color: #8a6a33;
  color: white;
}

.btn-login, .btn-register {
  background-color: transparent;
  border: 1px solid #8a6a33;
  color: #8a6a33;
  margin-left: 10px;
}

.btn-login:hover, .btn-register:hover {
  background-color: #1f2a24;
  color: white;
}

/* 加载动画 */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
}

.loading-icon {
  font-size: 40px;
  color: #8a6a33;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-text {
  margin-top: 20px;
  font-size: 18px;
  color: #666;
}

/* 头部样式（美术馆：暖白 + 金色细线） */
.site-header {
  background-color: #fffdf9;
  box-shadow: 0 2px 14px rgba(60, 50, 30, 0.08);
  border-bottom: 1px solid #e9e2d4;
  padding: 14px 0;
  position: sticky;
  top: 0;
  z-index: 1000;
}

.site-header .container {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.logo h1 {
  font-size: 24px;
  font-weight: 700;
  color: #23201c;
  letter-spacing: 1px;
  font-family: Georgia, 'Noto Serif SC', 'Songti SC', serif;
}

.logo h1::first-letter {
  color: #8a6a33;
}

.main-nav {
  flex: 1;
  margin: 0 20px;
}

.el-menu--horizontal {
  border-bottom: none;
  background: transparent;
}

.language-selector,
.user-profile {
  display: flex;
  align-items: center;
  cursor: pointer;
  color: #333;
  font-weight: 500;
  padding: 0 10px;
}

.language-selector .el-icon,
.user-profile .el-icon {
  margin-left: 5px;
}

.language-switcher {
  margin-right: 15px;
}

/* 主要内容区域 */
.site-main {
  min-height: calc(100vh - 200px);
  padding: 30px 0;
}

/* 页脚样式（墨绿炭黑 + 金色点缀） */
.site-footer {
  background: linear-gradient(180deg, #1f2a24 0%, #182019 100%);
  color: #cfd6cc;
  padding: 50px 0 20px;
  border-top: 3px solid #8a6a33;
}

.footer-content {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 30px;
  margin-bottom: 30px;
}

.footer-logo h3 {
  font-size: 20px;
  margin-bottom: 15px;
  color: #d9c391;
  font-family: Georgia, 'Noto Serif SC', serif;
}

.footer-links h4, .footer-contact h4 {
  font-size: 16px;
  margin-bottom: 15px;
  color: #e8ddc4;
}

.footer-links ul {
  list-style: none;
}

.footer-links li {
  margin-bottom: 10px;
}

.footer-links a {
  color: #a9b3a4;
  transition: color 0.25s ease;
}

.footer-links a:hover {
  color: #d9c391;
}

.footer-contact p {
  margin-bottom: 10px;
  color: #a9b3a4;
}

.social-links {
  display: flex;
  gap: 15px;
  margin-top: 15px;
}

.social-link {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: rgba(217, 195, 145, 0.14);
  color: #d9c391;
  transition: all 0.3s ease;
}

.social-link:hover {
  background-color: #8a6a33;
  color: #fff;
  transform: translateY(-3px);
}

.newsletter {
  margin-top: 20px;
}

.newsletter-form {
  display: flex;
  margin-top: 10px;
  gap: 10px;
}

.footer-bottom {
  text-align: center;
  padding-top: 20px;
  border-top: 1px solid #34402f;
  font-size: 14px;
  color: #8d9688;
}

/* 页面过渡动画 */
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
  .site-header .container {
    flex-direction: column;
  }

  .logo {
    margin-bottom: 15px;
  }

  .main-nav {
    margin: 10px 0;
    width: 100%;
  }

  .user-actions {
    margin-top: 15px;
  }
}

/* ===== 全局精细化（美化） ===== */
body {
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: #f2eee4;
}

::-webkit-scrollbar-thumb {
  background: #cfc4ad;
  border-radius: 8px;
}

::-webkit-scrollbar-thumb:hover {
  background: #b08d57;
}

::selection {
  background: rgba(176, 141, 87, 0.28);
}
</style>