import request from '@/utils/request'

/**
 * 获取艺术家统计数据
 * @returns {Promise} 返回包含艺术家统计数据的Promise
 */
export function getArtistStats() {
  return request({
    url: '/api/artist/stats',
    method: 'get'
  })
}

/**
 * 获取艺术家最近上传的作品
 * @param {Object} params 查询参数，包含limit等
 * @returns {Promise} 返回包含最近作品的Promise
 */
export function getRecentArtworks(params) {
  return request({
    url: '/api/artist/artworks/recent',
    method: 'get',
    params
  })
}

/**
 * 获取艺术家作品列表
 * @param {Object} params 查询参数，包含页码、每页数量等
 * @returns {Promise} 返回包含作品列表的Promise
 */
export function getArtistArtworks(params) {
  return request({
    url: '/api/artworks/getAllArtworks',
    method: 'get',
    params
  })
}

/**
 * 获取艺术家详细统计数据（用于统计页面）
 * @param {Object} params 查询参数，包含时间范围等
 * @returns {Promise} 返回包含详细统计数据的Promise
 */
export function getArtistDetailedStats(params) {
  return request({
    url: '/api/artist/stats/detailed',
    method: 'get',
    params
  })
}

/**
 * 获取艺术家热门作品
 * @param {Object} params 查询参数
 * @returns {Promise} 返回包含热门作品的Promise
 */
export function getPopularArtworks(params) {
  return request({
    url: '/api/artist/artworks/popular',
    method: 'get',
    params
  })
}