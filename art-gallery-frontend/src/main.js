import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import i18n from './i18n'
import './style.css'

// 创建应用实例
const app = createApp(App)

// 创建并使用Pinia（确保在使用store之前初始化）
const pinia = createPinia()
app.use(pinia)

// 使用其他插件
app.use(router)
app.use(ElementPlus)
app.use(i18n)
app.config.globalProperties.$t = i18n.global.t
// 注册Element Plus图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 配置全局axios拦截器
import axios from 'axios'
axios.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`
  }
  return config
})

axios.interceptors.response.use(
    response => response,
    async error => {
        if (error.response && error.response.status === 401) {
            // 未授权，清除token并重定向到登录页
            localStorage.removeItem('token')
            try {
                await router.push('/login')
                // 路由跳转成功的处理逻辑
            } catch (err) {
                // 路由跳转失败的处理逻辑
                console.error(err)
            }
        }
        return Promise.reject(error)
    }
)


// 挂载应用（确保只调用一次）
app.mount('#app')

