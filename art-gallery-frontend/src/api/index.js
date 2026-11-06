import axios from 'axios'

// 创建 axios 实例
const api = axios.create({
    baseURL: 'http://localhost:8080', // 后端 API 基础地址
    timeout: 10000, // 请求超时时间
    headers: {
        'Content-Type': 'application/json'
    }
})

// 请求拦截器（添加认证 token）
api.interceptors.request.use(config => {
    const token = localStorage.getItem('jwt_token')
    if (token) {
        config.headers.Authorization = `Bearer ${token}`
    }
    return config
}, error => {
    return Promise.reject(error)
})

// 响应拦截器（统一处理错误）
api.interceptors.response.use(response => {
    return response.data
}, error => {
    // 统一处理错误响应
    const errorMessage = error.response?.data?.message ||
        error.message ||
        '请求失败，请稍后重试'

    // 可以根据状态码做特殊处理
    if (error.response?.status === 401) {
        // 处理未授权
        console.error('未授权，请重新登录')
        // 这里可以重定向到登录页
    }

    return Promise.reject(errorMessage)
})

export default api