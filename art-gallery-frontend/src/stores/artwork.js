import { defineStore } from 'pinia'
import { ref } from 'vue'
import service from '@/utils/request'

export const useArtworkStore = defineStore('artwork', () => {
  const artworks = ref([])
  const loading = ref(false)
  
  // 设置艺术品列表的方法
  function setArtworks(newArtworks) {
    artworks.value = newArtworks
  }

  // 获取艺术品列表
  async function fetchArtworks(params = {}) {
    loading.value = true
    try {
      const response = await service.get('/api/artworks', { params })
      // 检查response.data是否有content字段（分页响应）
      const artworkData = response.data.content || response.data || []
      artworks.value = Array.isArray(artworkData) ? artworkData.map(artwork => enhanceArtwork(artwork)) : []
      return artworkData
    } catch (e) {
      console.error('获取艺术品列表失败:', e)
      artworks.value = [] // 确保在错误情况下重置为空数组
      return []
    } finally {
      loading.value = false
    }
  }

  // 获取单个艺术品详情
  async function fetchArtworkById(id) {
    loading.value = true
    try {
      const response = await service.get(`/api/artworks/${id}`)
      return enhanceArtwork(response.data)
    } catch (e) {
      console.error(`获取艺术品详情失败 (ID: ${id}):`, e)
      return null
    } finally {
      loading.value = false
    }
  }

  // 获取艺术家的作品
  async function fetchArtworksByArtist(artistId) {
    loading.value = true
    try {
      const response = await service.get(`/api/artworks/artist/${artistId}`)
      return response.data.map(artwork => enhanceArtwork(artwork))
    } catch (e) {
      console.error(`获取艺术家作品失败 (艺术家ID: ${artistId}):`, e)
      return []
    } finally {
      loading.value = false
    }
  }

  // 按流派获取作品
  async function fetchArtworksByStyle(style) {
    loading.value = true
    try {
      const response = await service.get('/api/artworks', { params: { style } })
      return response.data.map(artwork => enhanceArtwork(artwork))
    } catch (e) {
      console.error(`获取流派作品失败 (流派: ${style}):`, e)
      return []
    } finally {
      loading.value = false
    }
  }

  // 为艺术品添加默认名称和图片
  function enhanceArtwork(artwork) {
    if (!artwork) return artwork
    
    return {
      ...artwork,
      name: artwork.name || getDefaultArtworkName(artwork.id),
      mainImageUrl: artwork.mainImageUrl || getDefaultArtworkImage(artwork.id)
    }
  }

  // 为艺术品提供默认名称
  function getDefaultArtworkName(id) {
    const names = [
      '星夜',
      '睡莲',
      '亚维农少女',
      '南瓜',
      '向日葵',
      '呐喊',
      '蒙娜丽莎',
      '戴珍珠耳环的少女',
      '格尔尼卡',
      '最后的晚餐',
      '维纳斯的诞生',
      '自由引导人民',
      '雨伞',
      '大碗岛的星期天下午'
    ]
    return names[id % names.length] || `艺术品 ${id}`
  }

  // 为艺术品提供默认图片
  function getDefaultArtworkImage(id) {
    const images = [
      'https://images.unsplash.com/photo-1579783902614-a3fb3927b6a5?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxzZWFyY2h8Mnx8c3RhcnJ5JTIwbmlnaHR8ZW58MHx8MHx8&auto=format&fit=crop&w=500&q=60',
      'https://images.unsplash.com/photo-1563266847-495247064878?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxzZWFyY2h8M3x8d2F0ZXIlMjBsaWxpZXN8ZW58MHx8MHx8&auto=format&fit=crop&w=500&q=60',
      'https://images.unsplash.com/photo-1578301978693-85fa9c0320b9?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxzZWFyY2h8MTB8fGN1YmlzbXxlbnwwfHwwfHw%3D&auto=format&fit=crop&w=500&q=60',
      'https://images.unsplash.com/photo-1509114397022-ed747cca3f65?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxzZWFyY2h8M3x8cHVtcGtpbnxlbnwwfHwwfHw%3D&auto=format&fit=crop&w=500&q=60',
      'https://images.unsplash.com/photo-1597848212624-a19eb35e2651?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxzZWFyY2h8Mnx8c3VuZmxvd2Vyc3xlbnwwfHwwfHw%3D&auto=format&fit=crop&w=500&q=60',
      'https://images.unsplash.com/photo-1608316677206-84a8ead2a2d1?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxzZWFyY2h8Mnx8c2NyZWFtfGVufDB8fDB8fA%3D%3D&auto=format&fit=crop&w=500&q=60',
      'https://images.unsplash.com/photo-1607462109225-6b64ae2dd3cb?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxzZWFyY2h8MXx8bW9uYSUyMGxpc2F8ZW58MHx8MHx8&auto=format&fit=crop&w=500&q=60',
      'https://images.unsplash.com/photo-1578301978162-7aae4d755744?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxzZWFyY2h8NXx8Z2lybCUyMHdpdGglMjBwZWFybCUyMGVhcnJpbmd8ZW58MHx8MHx8&auto=format&fit=crop&w=500&q=60',
      'https://images.unsplash.com/photo-1621866908556-4f0a830707d2?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxzZWFyY2h8Mnx8Z3Vlcm5pY2F8ZW58MHx8MHx8&auto=format&fit=crop&w=500&q=60',
      'https://images.unsplash.com/photo-1577083552431-6e5fd01aa342?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxzZWFyY2h8Mnx8bGFzdCUyMHN1cHBlcnxlbnwwfHwwfHw%3D&auto=format&fit=crop&w=500&q=60',
      'https://images.unsplash.com/photo-1579783901586-d88db74b4fe4?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxzZWFyY2h8Mnx8YmlydGglMjBvZiUyMHZlbnVzfGVufDB8fDB8fA%3D%3D&auto=format&fit=crop&w=500&q=60',
      'https://images.unsplash.com/photo-1569668723493-a5f17c2e76a6?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxzZWFyY2h8Mnx8ZnJlbmNoJTIwcmV2b2x1dGlvbnxlbnwwfHwwfHw%3D&auto=format&fit=crop&w=500&q=60',
      'https://images.unsplash.com/photo-1519681393784-d120267933ba?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxzZWFyY2h8Mnx8dW1icmVsbGF8ZW58MHx8MHx8&auto=format&fit=crop&w=500&q=60',
      'https://images.unsplash.com/photo-1506126613408-eca07ce68773?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxzZWFyY2h8Mnx8cGFya3xlbnwwfHwwfHw%3D&auto=format&fit=crop&w=500&q=60'
    ]
    return images[id % images.length] || 'https://via.placeholder.com/300x300?text=艺术品图片'
  }

  return {
    artworks,
    loading,
    setArtworks,
    fetchArtworks,
    fetchArtworkById,
    fetchArtworksByArtist,
    fetchArtworksByStyle
  }
})