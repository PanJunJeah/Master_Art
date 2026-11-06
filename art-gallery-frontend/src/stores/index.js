import { defineStore } from 'pinia'
import service from '@/utils/request'
import { useRouter } from 'vue-router'
import request from "@/utils/request.js";


// 用户状态管理
export const useUserStore = defineStore('user', {
  state: () => ({
    user: sessionStorage.getItem('user'),
    token: sessionStorage.getItem('token') || null,
    role: sessionStorage.getItem('userRole') || null,
    loading: false
  }),
  getters: {
    isLoggedIn: (state) => state.token,

    // 修复 currentUser getter
    currentUser: (state) => state.user,

    // 修复 userRole getter
    userRole: (state) => state.role,
  },
  actions: {
    async login(username, password, role) {
      this.loading = true;
      try {
        const response = await service.post('/api/users/login', { username, password, role });

        // 确保响应结构正确
        if (!response.data || !response.data.token) {
          throw new Error('无效的登录响应: 缺少 token');
        }

        // 更新状态
        this.token = response.data.token;
        this.role = role.toUpperCase();
        this.user = response.data.user || {
          id: response.data.userId,
          username,
          role: role.toUpperCase()
        };

        // 持久化存储
        localStorage.setItem('token', this.token);
        localStorage.setItem('userRole', this.role);
        localStorage.setItem('user', JSON.stringify(this.user));

        return { success: true };
      } catch (error) {
        console.error('登录失败:', error);
        return {
          success: false,
          message: error.response?.data?.message ||
              error.message ||
              '登录失败，请检查用户名和密码'
        };
      } finally {
        this.loading = false;
      }
    },
    async register(userData) {
      this.loading = true
      try {
        const response = await service.post('/api/users/register', userData)
        this.token = response.data.token
        this.role = (userData.role || 'user').toUpperCase()
        localStorage.setItem('token', this.token)
        localStorage.setItem('userRole', this.role)
        await this.fetchUserInfo()
        return { success: true }
      } catch (error) {
        console.error('注册失败:', error)
        return { success: false, message: error.response?.data?.message || '注册失败，请稍后再试' }
      } finally {
        this.loading = false
      }
    },

    logout() {
      this.user = null
      this.token = null
      this.role = null
      localStorage.removeItem('token')
      localStorage.removeItem('userRole')
      
      // 获取router实例并导航到首页
      this.token = null;
      this.user = null;
      localStorage.removeItem('token');

      // 刷新页面以确保状态完全重置
      setTimeout(() => {
        window.location.reload()
      }, 100)
    }
  }
})

// 展览状态管理
export const useExhibitionStore = defineStore('exhibition', {
  state: () => ({
    exhibitions: [],
    currentExhibition: null,
    loading: false,
    locationOptions: [],  // 改为直接存储字符串数组
    themeOptions: []      // 改为直接存储字符串数组
  }),
  actions: {
    async fetchExhibitionsWithFilter(params) {
      this.loading = true;
      try {
        const response = await service.get('/api/exhibitions/getAllExhibitions', { params });

        // 统一响应处理
        const responseData = response?.data || response;
        let exhibitionData = [];
        let total = 0;

        // 处理嵌套数据结构
        if (responseData?.data?.content) {
          exhibitionData = responseData.data.content;
          total = responseData.data.totalItems || 0;
        }
        else if (responseData?.content) {
          exhibitionData = responseData.content;
          total = responseData.totalItems || 0;
        }
        else if (Array.isArray(responseData)) {
          exhibitionData = responseData;
          total = responseData.length;
        }

        this.exhibitions = exhibitionData;
        this.totalExhibitions = total;

        return exhibitionData;
      } catch (error) {
        console.error('获取展览列表失败:', error);
        this.exhibitions = [];
        this.totalExhibitions = 0;
        return [];
      } finally {
        this.loading = false;
      }
    },

    async fetchLocationOptions() {
      try {
        const response = await service.get('/api/exhibitions/locations')
        console.log('地点API原始响应:', response)

        // 处理不同的响应格式
        let locations = []
        if (Array.isArray(response.data)) {
          locations = response.data
        } else if (response.data?.data && Array.isArray(response.data.data)) {
          locations = response.data.data
        } else if (Array.isArray(response)) {
          locations = response
        }

        console.log('处理后的地点数据:', locations)
        this.locationOptions = locations

      } catch (error) {
        console.error('获取展览地点选项失败:', error)
        this.locationOptions = []
      }
    },

    async fetchThemeOptions() {
      try {
        const response = await service.get('/api/exhibitions/themes')
        console.log('主题API原始响应:', response)

        // 处理不同的响应格式
        let themes = []
        if (Array.isArray(response.data)) {
          themes = response.data
        } else if (response.data?.data && Array.isArray(response.data.data)) {
          themes = response.data.data
        } else if (Array.isArray(response)) {
          themes = response
        }

        console.log('处理后的主题数据:', themes)
        this.themeOptions = themes

      } catch (error) {
        console.error('获取展览主题选项失败:', error)
        this.themeOptions = []
      }
    }
  }
})

// 艺术品状态管理
export const useArtworkStore = defineStore('artwork', {
  state: () => ({
    artworks: [],
    totalArtworks: 0,
    currentPage: 1,
    pageSize: 12,
    currentArtwork: null,
    loading: false,
    // 新增筛选选项状态
    styleOptions: [],
    periodOptions: [],
    artistOptions: []
  }),
  actions: {
    async fetchArtworks(params = {}) {
      this.loading = true;
      try {
        // 构建请求参数
        const requestParams = {
          page: params.page - 1, // 后端分页从0开始
          size: params.pageSize,
          style: params.style,
          period: params.period,
          artist: params.artist,
          sort: params.sort
        };

        // 清理空值参数
        Object.keys(requestParams).forEach(key => {
          if (requestParams[key] === null || requestParams[key] === undefined || requestParams[key] === '') {
            delete requestParams[key];
          }
        });

        console.log('发送艺术品API请求参数:', requestParams);

        const response = await service.get('/api/artworks/getAllArtworks', { params: requestParams });

        // 处理响应
        const responseData = response?.data || response;
        let artworkData = [];
        let total = 0;

        // 统一处理不同响应格式
        if (responseData?.data?.content) {
          artworkData = responseData.data.content;
          total = responseData.data.totalItems || 0;
        }
        else if (responseData?.content) {
          artworkData = responseData.content;
          total = responseData.totalItems || 0;
        }
        else if (Array.isArray(responseData)) {
          artworkData = responseData;
          total = responseData.length;
        }

        // 更新状态
        this.artworks = artworkData;
        this.totalArtworks = total;

        return { artworks: artworkData, total };

      } catch (error) {
        console.error('获取艺术品列表失败:', error);
        this.artworks = [];
        this.totalArtworks = 0;
        return { artworks: [], total: 0 };
      } finally {
        this.loading = false;
      }
    },

    // 获取艺术流派选项
    async fetchStyleOptions() {
      try {
        // 修正API路径，与后端ArtworkController中的/genres接口对应
        const response = await service.get('/api/artworks/genres');
        console.log('艺术流派API响应:', response);

        let styles = [];

        // 处理不同的响应格式
        if (Array.isArray(response.data)) {
          styles = response.data;
        } else if (response.data?.data && Array.isArray(response.data.data)) {
          styles = response.data.data;
        } else if (response.data?.content && Array.isArray(response.data.content)) {
          styles = response.data.content;
        } else if (response.data?.data?.content && Array.isArray(response.data.data.content)) {
          styles = response.data.data.content;
        }

        console.log('处理后的艺术流派:', styles);
        this.styleOptions = styles;
      } catch (error) {
        console.error('获取艺术流派选项失败:', error);
        this.styleOptions = [];
      }
    },

    async fetchPeriodOptions() {
      try {
        // 确保API路径正确，与后端ArtworkController中的/creation-years接口对应
        const response = await service.get('/api/artworks/creation-years');
        console.log('创作时期API响应:', response);

        let periods = [];

        // 处理不同的响应格式，直接获取响应数据
        if (response && response.data !== undefined) {
          // 如果response.data直接是数组
          if (Array.isArray(response.data)) {
            periods = response.data;
          }
          // 如果response.data是包含data属性的对象，且data是数组
          else if (response.data?.data && Array.isArray(response.data.data)) {
            periods = response.data.data;
          }
          // 如果response.data是包含content属性的对象，且content是数组
          else if (response.data?.content && Array.isArray(response.data.content)) {
            periods = response.data.content;
          }
          // 如果response.data是包含data.content的嵌套对象，且content是数组
          else if (response.data?.data?.content && Array.isArray(response.data.data.content)) {
            periods = response.data.data.content;
          }
        }

        console.log('处理后的创作时期:', periods);
        this.periodOptions = periods;
      } catch (error) {
        console.error('获取创作时期选项失败:', error);
        this.periodOptions = [];
      }
    },

    async fetchArtistOptions() {
      try {
        // 确保API路径正确，与后端ArtistController中的/getAllArtists接口对应
        const response = await service.get('/api/artists/getAllArtists');
        console.log('艺术家API响应:', response);

        let artists = [];

        // 处理不同的响应格式，根据ArtistController的返回结构调整
        if (response && response.data !== undefined) {
          // 如果response.data直接是数组
          if (Array.isArray(response.data)) {
            artists = response.data;
          }
          // 如果response.data是包含content属性的对象，且content是数组
          // 这与ArtistController返回的格式匹配：response.put("content", artistPage.getContent());
          else if (response.data?.content && Array.isArray(response.data.content)) {
            artists = response.data.content;
          }
          // 如果response.data是包含data.content的嵌套对象，且content是数组
          else if (response.data?.data?.content && Array.isArray(response.data.data.content)) {
            artists = response.data.data.content;
          }
          // 如果response.data是包含data属性的对象，且data是数组
          else if (response.data?.data && Array.isArray(response.data.data)) {
            artists = response.data.data;
          }
        }

        console.log('处理后的艺术家:', artists);

        // 确保artists数组中的对象有id和name属性
        this.artistOptions = artists
          .filter(a => a && a.id && a.name) // 过滤掉没有id或name的项
          .map(a => ({
            value: a.id,
            label: a.name
          }));
      } catch (error) {
        console.error('获取艺术家选项失败:', error);
        this.artistOptions = [];
      }
    },

    // 更新分页参数
    setPagination(page, pageSize) {
      this.currentPage = page;
      this.pageSize = pageSize;
    },

    async fetchArtworkById(id) {
      this.loading = true
      try {
        const response = await service.get(`/api/artworks/${id}`)
        this.currentArtwork = response.data
      } catch (error) {
        console.error('获取艺术品详情失败:', error)
      } finally {
        this.loading = false
      }
    }
  }
})

// 艺术家状态管理
export const useArtistStore = defineStore('artist', {
  state: () => ({
    artists: [],
    currentArtist: null,
    loading: false,
    filters: {
      nationality: null,
      style: null,
      period: null
    }
  }),
  actions: {
    async fetchArtists(params = {}) {
      this.loading = true
      try {
        const response = await service.get('/api/artists/getAllArtists', { params: { ...this.filters, ...params } })
        // 兼容多种后端返回结构
        let artistData = []
        if (response && response.data) {
          if (Array.isArray(response.data.content)) {
            artistData = response.data.content
          } else if (Array.isArray(response.data.items)) {
            artistData = response.data.items
          } else if (Array.isArray(response.data)) {
            artistData = response.data
          } else {
            artistData = []
          }
        }
        this.artists = Array.isArray(artistData) ? artistData : []
      } catch (error) {
        console.error('获取艺术家列表失败:', error)
        this.artists = [] // 确保在错误情况下重置为空数组
      } finally {
        this.loading = false
      }
    },
    async fetchArtistById(id) {
      this.loading = true
      try {
        const response = await service.get(`/api/artists/${id}`)
        this.currentArtist = response.data
      } catch (error) {
        console.error('获取艺术家详情失败:', error)
      } finally {
        this.loading = false
      }
    },
    setFilter(key, value) {
      this.filters[key] = value
    },
    clearFilters() {
      this.filters = {
        nationality: null,
        style: null,
        period: null
      }
    }
  }
})