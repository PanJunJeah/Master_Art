import { defineStore } from 'pinia'
import { ref } from 'vue'
import service from '@/utils/request'

export const useArtistStore = defineStore('artist', () => {
  const artists = ref([])
  const currentArtist = ref(null)
  const loading = ref(false)
  const total = ref(0)

  // 获取艺术家列表
  async function fetchArtists(params = {}) {
    loading.value = true
    try {
      const response = await service.get('/api/artists/getAllArtists', { params })
      // 处理后端返回的数据结构 { content: [...], totalItems: 123, currentPage: 0, totalPages: 10 }
      if (response.data.content) {
        artists.value = response.data.content
        total.value = response.data.totalItems || 0
      } else if (Array.isArray(response.data.items)) {
        // 兼容旧格式 { items: [...], total: 123 }
        artists.value = response.data.items
        total.value = response.data.total || 0
      } else if (Array.isArray(response.data)) {
        // 处理直接返回数组的情况
        artists.value = response.data
        total.value = response.data.length
      } else {
        // 其他情况，设置为空数组
        artists.value = []
        total.value = 0
        console.warn('未知的响应数据格式:', response.data)
      }
      return response.data
    } catch (e) {
      console.error('获取艺术家列表失败:', e)
      return []
    } finally {
      loading.value = false
    }
  }

  // 获取艺术家详情
  async function fetchArtistById(id) {
    loading.value = true
    try {
      const response = await service.get(`/api/artists/${id}`)
      currentArtist.value = response.data
      return response.data
    } catch (e) {
      console.error(`获取艺术家详情失败 (ID: ${id}):`, e)
      return null
    } finally {
      loading.value = false
    }
  }

  return {
    artists,
    currentArtist,
    loading,
    total,
    fetchArtists,
    fetchArtistById
  }
})