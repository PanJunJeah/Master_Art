<template>
  <div class="home-page">
    <el-skeleton v-if="loading" :rows="8" animated class="home-skeleton" />
    <template v-else>
      <!-- ===== 英雄区：美术馆画廊风格 + 数据看板 ===== -->
      <section class="hero-section">
        <div class="hero-glow"></div>
        <div class="container hero-inner">
          <div class="hero-badge">
            <span class="badge-dot"></span> MasterArt Gallery · 探索艺术的奇妙世界
          </div>
          <h2 class="hero-title">在画布与光影之间<br>遇见不朽的艺术</h2>
          <p class="hero-sub">汇集世界各地的艺术珍品与杰出艺术家，足不出户逛遍名展。
            智能策展助手随时为您解读作品、查询排期余票、生成预约草稿。</p>
          <div class="hero-actions">
            <router-link to="/exhibitions" class="btn hero-btn primary">浏览展览</router-link>
            <router-link to="/ai-art-guide" class="btn hero-btn ghost">🤖 AI 智能策展</router-link>
          </div>

          <!-- 数据看板 -->
          <div class="stats-band">
            <div class="stat" v-for="s in statItems" :key="s.label">
              <span class="stat-value">{{ s.value }}</span>
              <span class="stat-label">{{ s.label }}</span>
            </div>
          </div>
        </div>
      </section>

      <!-- 精选展览 -->
      <section class="featured-section">
        <div class="container">
          <h2 class="section-title">精选展览</h2>
          <el-row :gutter="30">
            <template v-if="exhibitions.content && exhibitions.content.length > 0">
              <el-col :xs="24" :sm="12" :md="8" v-for="exhibition in exhibitions.content" :key="exhibition.id">
                <el-card class="art-card" shadow="hover" :body-style="{ padding: '0px' }">
                  <div class="card-image-container">
                    <img :src="exhibition.coverImageUrl || exhibition.posterUrl || 'https://img1.baidu.com/it/u=3480534588,1181213420&fm=253&fmt=auto&app=120&f=JPEG?w=750&h=500'" class="card-image" :alt="exhibition.name">
                    <div class="card-image-shade"></div>
                  </div>
                  <div class="card-content">
                    <h3 class="card-title">{{ exhibition.name }}</h3>
                    <p class="card-description">{{ truncateText(exhibition.description, 90) }}</p>
                    <div class="card-meta">
                      <span>{{ formatDate(exhibition.startTime) }} - {{ formatDate(exhibition.endTime) }}</span>
                      <span>{{ exhibition.location || '未知地点' }}</span>
                    </div>
                    <router-link :to="`/exhibitions/${exhibition.id}`" class="btn btn-primary card-btn">查看详情</router-link>
                  </div>
                </el-card>
              </el-col>
            </template>
            <el-col :span="24" v-else>
              <el-empty description="暂无展览信息" />
            </el-col>
          </el-row>
          <div class="view-more">
            <router-link to="/exhibitions" class="btn ghost-btn">查看更多展览 →</router-link>
          </div>
        </div>
      </section>

      <!-- 热门艺术品 -->
      <section class="featured-section alt">
        <div class="container">
          <h2 class="section-title">热门艺术品</h2>
          <el-row :gutter="30">
            <template v-if="artworks.content && artworks.content.length > 0">
              <el-col :xs="24" :sm="12" :md="8" v-for="artwork in artworks.content" :key="artwork.id">
                <el-card class="art-card" shadow="hover" :body-style="{ padding: '0px' }">
                  <div class="card-image-container">
                    <img :src="artwork.mainImageUrl || 'https://via.placeholder.com/300x200?text=艺术品图片'" class="card-image" :alt="artwork.name">
                    <div class="card-image-shade"></div>
                  </div>
                  <div class="card-content">
                    <h3 class="card-title">{{ artwork.name }}</h3>
                    <p class="card-description">{{ truncateText(artwork.description, 90) }}</p>
                    <div class="card-meta">
                      <span>{{ artwork.artist?.name || '未知艺术家' }}</span>
                      <span>{{ artwork.creationYear || '未知年代' }}</span>
                    </div>
                    <router-link :to="`/artworks/${artwork.id}`" class="btn btn-primary card-btn">查看详情</router-link>
                  </div>
                </el-card>
              </el-col>
            </template>
            <el-col :span="24" v-else>
              <el-empty description="暂无艺术品信息" />
            </el-col>
          </el-row>
          <div class="view-more">
            <router-link to="/artworks" class="btn ghost-btn">查看更多艺术品 →</router-link>
          </div>
        </div>
      </section>

      <!-- 杰出艺术家 -->
      <section class="featured-section">
        <div class="container">
          <h2 class="section-title">杰出艺术家</h2>
          <el-row :gutter="30">
            <template v-if="artists.content && artists.content.length > 0">
              <el-col :xs="24" :sm="12" :md="8" v-for="artist in artists.content" :key="artist.id">
                <el-card class="art-card" shadow="hover" :body-style="{ padding: '0px' }">
                  <div class="card-image-container">
                    <img :src="artist.photoUrl || artist.portraitUrl || 'https://via.placeholder.com/300x200?text=艺术家图片'" class="card-image" :alt="artist.name">
                    <div class="card-image-shade"></div>
                  </div>
                  <div class="card-content">
                    <h3 class="card-title">{{ artist.name }}</h3>
                    <p class="card-description">{{ truncateText(artist.biography, 90) }}</p>
                    <div class="card-meta">
                      <span>{{ artist.nationality || '未知国籍' }}</span>
                      <span>{{ formatLifespan(artist) }}</span>
                    </div>
                    <router-link :to="`/artists/${artist.id}`" class="btn btn-primary card-btn">查看详情</router-link>
                  </div>
                </el-card>
              </el-col>
            </template>
            <el-col :span="24" v-else>
              <el-empty description="暂无艺术家信息" />
            </el-col>
          </el-row>
          <div class="view-more">
            <router-link to="/artists" class="btn ghost-btn">查看更多艺术家 →</router-link>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../utils/request'

// 数据列表
const exhibitions = ref({ content: [] })
const artworks = ref({ content: [] })
const artists = ref({ content: [] })
const loading = ref(true)

// ===== 数据看板（动画计数） =====
const statItems = ref([
  { label: '在展艺术品', key: 'artworks', value: 0 },
  { label: '展览总数', key: 'exhibitions', value: 0 },
  { label: '进行中展览', key: 'ongoingExhibitions', value: 0 },
  { label: '入驻艺术家', key: 'artists', value: 0 }
])

// 数字滚动动画
function animateValue(item, target, duration = 1200) {
  const start = performance.now()
  const tick = (now) => {
    const progress = Math.min((now - start) / duration, 1)
    const eased = 1 - Math.pow(1 - progress, 3)
    item.value = Math.round(target * eased)
    if (progress < 1) requestAnimationFrame(tick)
  }
  requestAnimationFrame(tick)
}

const fetchStats = async () => {
  try {
    const res = await request({
      url: '/api/stats/overview',
      method: 'get',
      headers: { 'Content-Type': 'application/json' }
    })
    const data = res?.data || res || {}
    statItems.value.forEach((item) => animateValue(item, Number(data[item.key]) || 0))
  } catch (e) {
    console.error('获取统计数据失败', e)
  }
}

// 工具函数
function truncateText(text, length) {
  if (!text) return '暂无描述'
  return text.length > length ? text.substring(0, length) + '...' : text
}

function formatDate(dateString) {
  if (!dateString) return '未知'
  return new Date(dateString).toLocaleDateString()
}

function formatLifespan(artist) {
  if (!artist.birthYear && !artist.birthDate) return '未知年代'
  return `${artist.birthYear || '?'} - ${artist.deathYear || '至今'}`
}

// 获取展览列表
const getAllExhibitions = async () => {
  try {
    const res = await request({ url: '/api/exhibitions/get-list', method: 'get', headers: { 'Content-Type': 'application/json' } })
    if (res && Array.isArray(res.content)) exhibitions.value = res
    else if (res && res.data && Array.isArray(res.data.content)) exhibitions.value = { content: res.data.content }
    else exhibitions.value = { content: [] }
  } catch (error) {
    exhibitions.value = { content: [] }
  } finally {
    loading.value = false
  }
}

// 获取艺术品列表
const getAllArtworks = async () => {
  try {
    const res = await request({ url: '/api/artworks/getAllArtworks', method: 'get', headers: { 'Content-Type': 'application/json' } })
    if (res && Array.isArray(res.content)) artworks.value = res
    else if (res && res.data && Array.isArray(res.data.content)) artworks.value = { content: res.data.content }
    else artworks.value = { content: [] }
  } catch (error) {
    artworks.value = { content: [] }
  } finally {
    loading.value = false
  }
}

// 获取艺术家列表
const getAllArtists = async () => {
  try {
    const res = await request({ url: '/api/artists/getAllArtists', method: 'get', headers: { 'Content-Type': 'application/json' } })
    if (res && res.content) artists.value = res
    else artists.value = { content: [] }
  } catch (error) {
    artists.value = { content: [] }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchStats()
  getAllExhibitions()
  getAllArtworks()
  getAllArtists()
})
</script>

<style scoped>
.home-skeleton { padding: 40px 0; }

/* ===== 英雄区 ===== */
.hero-section {
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #1f2a24 0%, #2c3a32 55%, #3a4638 100%);
  color: #f5efe4;
  padding: 84px 0 0;
  margin-bottom: 60px;
}

.hero-glow {
  position: absolute;
  inset: 0;
  background:
      radial-gradient(900px 380px at 82% -10%, rgba(201, 168, 106, 0.35), transparent 60%),
      radial-gradient(600px 300px at 8% 110%, rgba(201, 168, 106, 0.18), transparent 60%);
  pointer-events: none;
}

.hero-inner { position: relative; text-align: center; }

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  letter-spacing: 2px;
  color: #d9c391;
  border: 1px solid rgba(217, 195, 145, 0.45);
  border-radius: 999px;
  padding: 6px 18px;
  margin-bottom: 22px;
}

.badge-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #d9c391;
  box-shadow: 0 0 12px #d9c391;
}

.hero-title {
  font-family: Georgia, 'Noto Serif SC', 'Songti SC', serif;
  font-size: 46px;
  line-height: 1.3;
  letter-spacing: 3px;
  margin-bottom: 18px;
  color: #f5efe4;
}

.hero-sub {
  max-width: 640px;
  margin: 0 auto 28px;
  color: #cfd6cc;
  line-height: 1.9;
}

.hero-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  flex-wrap: wrap;
}

.hero-btn {
  padding: 12px 30px;
  font-size: 15px;
  border-radius: 999px;
}

.hero-btn.primary {
  background: #d9c391;
  color: #1f2a24;
  font-weight: 600;
}

.hero-btn.primary:hover {
  background: #e8d7ae;
  color: #1f2a24;
  transform: translateY(-2px);
  box-shadow: 0 10px 26px rgba(217, 195, 145, 0.35);
}

.hero-btn.ghost {
  border: 1px solid rgba(217, 195, 145, 0.6);
  color: #e8ddc4;
}

.hero-btn.ghost:hover {
  background: rgba(217, 195, 145, 0.12);
  color: #f5efe4;
  transform: translateY(-2px);
}

/* 数据看板 */
.stats-band {
  margin-top: 56px;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  border-top: 1px solid rgba(217, 195, 145, 0.22);
  background: rgba(0, 0, 0, 0.14);
  backdrop-filter: blur(4px);
}

.stat {
  padding: 26px 10px;
  border-right: 1px solid rgba(217, 195, 145, 0.18);
}

.stat:last-child { border-right: none; }

.stat-value {
  display: block;
  font-family: Georgia, serif;
  font-size: 34px;
  font-weight: 700;
  color: #d9c391;
}

.stat-label {
  font-size: 13px;
  color: #cfd6cc;
  letter-spacing: 1px;
}

/* ===== 区块 ===== */
.featured-section {
  margin-bottom: 70px;
}

.featured-section.alt {
  background: linear-gradient(180deg, #f1ecdf 0%, #f6f3ee 100%);
  padding: 50px 0 70px;
  margin-bottom: 0;
}

.section-title {
  font-size: 28px;
  color: #23201c;
  margin-bottom: 34px;
  text-align: center;
  position: relative;
  font-family: Georgia, 'Noto Serif SC', 'Songti SC', serif;
  letter-spacing: 2px;
}

.section-title::after {
  content: '';
  display: block;
  width: 54px;
  height: 3px;
  background: linear-gradient(90deg, transparent, #b08d57, transparent);
  margin: 14px auto 0;
}

/* 卡片 */
.art-card {
  margin-bottom: 30px;
  border-radius: 14px;
  border: 1px solid #e9e2d4;
  background: #fffdf9;
  overflow: hidden;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.art-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 16px 40px rgba(60, 50, 30, 0.16);
}

.card-image-container {
  position: relative;
  height: 200px;
  overflow: hidden;
}

.card-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.55s ease;
}

.card-image-shade {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 60%, rgba(31, 42, 36, 0.35));
  pointer-events: none;
}

.art-card:hover .card-image { transform: scale(1.06); }

.card-content { padding: 20px; }

.card-title {
  font-family: Georgia, 'Noto Serif SC', serif;
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 8px;
  color: #23201c;
}

.card-description {
  font-size: 14px;
  color: #6f6553;
  margin-bottom: 14px;
  min-height: 42px;
  line-height: 1.7;
}

.card-meta {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 12px;
  color: #9a8f7d;
  margin-bottom: 16px;
}

.card-btn {
  display: block;
  width: 100%;
  text-align: center;
  background: #1f2a24;
  color: #f5efe4;
}

.card-btn:hover {
  background: #8a6a33;
  color: #fff;
}

/* 查看更多 */
.view-more { text-align: center; margin-top: 6px; }

.ghost-btn {
  border: 1px solid #b08d57;
  color: #8a6a33;
  border-radius: 999px;
  padding: 9px 26px;
}

.ghost-btn:hover {
  background: #8a6a33;
  color: #fff;
}

/* 响应式 */
@media (max-width: 768px) {
  .hero-section { padding-top: 54px; }
  .hero-title { font-size: 30px; }
  .stats-band { grid-template-columns: repeat(2, 1fr); }
  .stat:nth-child(2) { border-right: none; }
  .stat { border-bottom: 1px solid rgba(217, 195, 145, 0.18); }
}
</style>
