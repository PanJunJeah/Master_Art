import { createRouter, createWebHashHistory } from 'vue-router'
import { h } from 'vue'
import AIArtGuide from "@/views/AIArtGuide.vue";

const routes = [
  {
    path: '/',
    name: 'manger',
    component: () => import('../views/Manger.vue'),
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('../views/Home.vue'),
        meta: { title: '首页 - 艺术品展览网站' }
      },
      {
        path: '/exhibitions',
        name: 'Exhibitions',
        component: () => import('../views/Exhibitions.vue'),
        meta: { title: '展览 - 艺术品展览网站' }
      },
      {
        path: '/exhibitions/:id',
        name: 'ExhibitionDetail',
        component: () => import('../views/ExhibitionDetail.vue'),
        meta: { title: '展览详情 - 艺术品展览网站' }
      },
      {
        path: '/artworks',
        name: 'Artworks',
        component: () => import('../views/Artworks.vue'),
        meta: { title: '艺术品 - 艺术品展览网站' }
      },
      {
        path: '/artworks/:id',
        name: 'ArtworkDetail',
        component: () => import('../views/ArtworkDetail.vue'),
        meta: { title: '艺术品详情 - 艺术品展览网站' }
      },
      {
        path: '/artists',
        name: 'Artists',
        component: () => import('../views/Artists.vue'),
        meta: { title: '艺术家 - 艺术品展览网站' }
      },
      {
        path: '/artists/:id',
        name: 'ArtistDetail',
        component: () => import('../views/ArtistDetail.vue'),
        meta: { title: '艺术家详情 - 艺术品展览网站' }
      },
      {
        path: '/social-platform',
        name: 'SocialPlatform',
        component: () => import('../views/SocialPlatform.vue'),
        meta: { title: '艺术社区' }
      },
      {
        path: '/ai-art-guide',
        name: 'AIArtGuide',
        component: AIArtGuide,
        meta: { title: 'AI 智能策展' }
      },
      {
        path: '/about',
        name: 'About',
        component: () => import('../views/About.vue'),
        meta: { title: '关于我们 - 艺术品展览网站' }
      },
      {
        path: '/login',
        name: 'Login',
        component: () => import('../views/Login.vue'),
        meta: { title: '登录 - 艺术品展览网站' }
      },
      {
        path: '/register',
        name: 'Register',
        component: () => import('../views/Register.vue'),
        meta: { title: '注册 - 艺术品展览网站' }
      },
      {
        path: '/forgot-password',
        name: 'ForgotPassword',
        component: () => import('../views/forgot-password.vue'),
        meta: { title: '忘记密码 - 艺术品展览网站' }
      },
      // 用户相关页面路由
      {
        path: '/profile',
        name: 'UserProfile',
        component: () => import('../views/Profile.vue'),
        meta: { title: '个人中心 - 艺术品展览网站', requiresAuth: true }
      },
      {
        path: '/favorites',
        name: 'UserFavorites',
        component: () => import('../views/Favorites.vue'),
        meta: { title: '收藏艺术品 - 艺术品展览网站', requiresAuth: true }
      },
      {
        path: '/planned-exhibitions',
        name: 'UserPlannedExhibitions',
        component: () => import('../views/PlannedExhibitions.vue'),
        meta: { title: '计划参观展览 - 艺术品展览网站', requiresAuth: true }
      },
      {
        path: '/my-reservations',
        name: 'MyReservations',
        component: () => import('../views/MyReservations.vue'),
        meta: { title: '我的预约 - 艺术品展览网站', requiresAuth: true }
      },
      {
        path: '/history',
        name: 'UserHistory',
        component: () => import('../views/History.vue'),
        meta: { title: '浏览历史 - 艺术品展览网站', requiresAuth: true }
      },
      // 后台管理路由
      {
        path: '/admin',
        component: () => import('../views/admin/AdminLayout.vue'),
        meta: { requiresAuth: true, role: 'admin' },
        children: [
          {
            path: 'dashboard',
            name: 'AdminDashboard',
            component: () => import('../views/admin/Dashboard.vue'),
            meta: { title: '后台管理 - 艺术品展览网站' }
          },
          {
            path: 'artworks',
            name: 'AdminArtworks',
            component: { render: () => h('div', '艺术品管理页面') },
            meta: { title: '艺术品管理 - 艺术品展览网站' }
          },
          {
            path: 'exhibitions',
            name: 'AdminExhibitions',
            component: { render: () => h('div', '展览管理页面') },
            meta: { title: '展览管理 - 艺术品展览网站' }
          },
          {
            path: 'users',
            name: 'AdminUsers',
            component: { render: () => h('div', '用户管理页面') },
            meta: { title: '用户管理 - 艺术品展览网站' }
          }
        ]
      },
      // 艺术家中心路由
      {
        path: '/artist',
        component: () => import('../views/artist/ArtistLayout.vue'),
        meta: { requiresAuth: true, role: 'artist' },
        children: [
          {
            path: 'dashboard',
            name: 'ArtistDashboard',
            component: () => import('../views/artist/ArtistDashboard.vue'),
            meta: { title: '艺术家中心 - 艺术品展览网站' }
          },
          {
            path: 'artworks',
            name: 'ArtistArtworks',
            component: { render: () => h('div', '我的作品页面') },
            meta: { title: '我的作品 - 艺术品展览网站' }
          },
          {
            path: 'upload',
            name: 'ArtistUpload',
            component: { render: () => h('div', '上传新作品页面') },
            meta: { title: '上传新作品 - 艺术品展览网站' }
          },
          {
            path: 'statistics',
            name: 'ArtistStatistics',
            component: { render: () => h('div', '统计数据页面') },
            meta: { title: '统计数据 - 艺术品展览网站' }
          }
        ]
      },
      // 404路由放在最后
      {
        path: '/:pathMatch(.*)*',
        name: 'NotFound',
        component: () => import('../views/NotFound.vue'),
        meta: { title: '页面未找到 - 艺术品展览网站' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

// 全局前置守卫，设置页面标题
router.beforeEach((to, from, next) => {
  // 设置页面标题
  if (to.meta.title) {
    document.title = to.meta.title
  }
  next()
})

export default router
