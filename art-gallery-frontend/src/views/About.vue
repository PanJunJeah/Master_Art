<template>
  <div class="about-page">
    <!-- 英雄区域 -->
    <section class="hero-section">
      <div class="container">
        <h2>关于我们</h2>
        <p>了解艺术品展览网站的使命和愿景</p>
      </div>
    </section>
    
    <!-- 关于我们内容 -->
    <section class="about-section">
      <div class="container">
        <el-row :gutter="30">
          <el-col :md="12">
            <div class="about-content">
              <h2 class="section-title">我们的故事</h2>
              <p>艺术品展览网站成立于2025年，是一个致力于推广艺术文化、连接艺术家与艺术爱好者的在线平台。我们的使命是让艺术变得更加亲民，让更多人能够欣赏到世界各地的艺术珍品。</p>
              <p>我们与全球众多艺术机构、博物馆和画廊合作，为用户提供高质量的艺术品展览信息和艺术家介绍。通过我们的平台，用户可以轻松浏览各类展览、艺术品和艺术家的详细信息，感受艺术的魅力。</p>
              <h3>我们的愿景</h3>
              <p>我们希望成为连接艺术与大众的桥梁，让艺术融入每个人的生活。我们相信，艺术不仅仅是博物馆里的展品，更是一种能够丰富人们精神世界的力量。</p>
            </div>
          </el-col>
          <el-col :md="12">
            <div class="about-image">
              <img src="https://img0.baidu.com/it/u=2775625135,3602014877&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=600?text=关于我们" alt="关于我们">
            </div>
          </el-col>
        </el-row>
      </div>
    </section>
    
    <!-- 我们的团队 -->
    <section class="team-section">
      <div class="container">
        <h2 class="section-title">我们的团队</h2>
        <p class="section-description">我们拥有一支热爱艺术、专业敬业的团队，致力于为用户提供最优质的艺术体验</p>
        
        <el-row :gutter="30">
          <el-col :xs="24" :sm="12" :md="8" :lg="6">
            <div class="team-member">
              <div class="member-avatar">
                <img src="https://via.placeholder.com/200x200?text=张总监" alt="张总监">
              </div>
              <h3 class="member-name">张总监</h3>
              <p class="member-title">创始人 & 艺术总监</p>
              <p class="member-bio">拥有20年艺术策展经验，曾在多家知名美术馆担任策展人</p>
            </div>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :lg="6">
            <div class="team-member">
              <div class="member-avatar">
                <img src="https://via.placeholder.com/200x200?text=李经理" alt="李经理">
              </div>
              <h3 class="member-name">李经理</h3>
              <p class="member-title">运营总监</p>
              <p class="member-bio">资深艺术市场专家，负责平台的日常运营和合作伙伴关系</p>
            </div>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :lg="6">
            <div class="team-member">
              <div class="member-avatar">
                <img src="https://via.placeholder.com/200x200?text=王编辑" alt="王编辑">
              </div>
              <h3 class="member-name">王编辑</h3>
              <p class="member-title">内容主编</p>
              <p class="member-bio">艺术史博士，负责平台的艺术品介绍和艺术家传记编写</p>
            </div>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8" :lg="6">
            <div class="team-member">
              <div class="member-avatar">
                <img src="https://via.placeholder.com/200x200?text=赵设计师" alt="赵设计师">
              </div>
              <h3 class="member-name">赵设计师</h3>
              <p class="member-title">UI/UX设计师</p>
              <p class="member-bio">拥有丰富的用户体验设计经验，致力于打造直观易用的艺术平台</p>
            </div>
          </el-col>
        </el-row>
      </div>
    </section>
    
    <!-- 联系我们 -->
    <section class="contact-section">
      <div class="container">
        <h2 class="section-title">联系我们</h2>
        <p class="section-description">如果您有任何问题或建议，欢迎随时与我们联系</p>
        
        <el-row :gutter="30">
          <el-col :md="12">
            <div class="contact-info">
              <div class="contact-item">
                <el-icon><Location /></el-icon>
                <div class="contact-text">
                  <h3>地址</h3>
                  <p>贵州大学</p>
                </div>
              </div>
              <div class="contact-item">
                <el-icon><Message /></el-icon>
                <div class="contact-text">
                  <h3>邮箱</h3>
                  <p>info@masterart.com</p>
                </div>
              </div>
              <div class="contact-item">
                <el-icon><Phone /></el-icon>
                <div class="contact-text">
                  <h3>电话</h3>
                  <p>+86 18285672864</p>
                </div>
              </div>
              <div class="social-links">
                <a href="#" class="social-link"><el-icon><Facebook /></el-icon></a>
                <a href="#" class="social-link"><el-icon><Instagram /></el-icon></a>
                <a href="#" class="social-link"><el-icon><Twitter /></el-icon></a>
              </div>
            </div>
          </el-col>
          <el-col :md="12">
            <div class="contact-form">
              <el-form>
                <el-form-item>
                  <el-input v-model="contactForm.name" placeholder="您的姓名"></el-input>
                </el-form-item>
                <el-form-item>
                  <el-input v-model="contactForm.email" placeholder="您的邮箱"></el-input>
                </el-form-item>
                <el-form-item>
                  <el-input v-model="contactForm.subject" placeholder="主题"></el-input>
                </el-form-item>
                <el-form-item>
                  <el-input v-model="contactForm.message" type="textarea" :rows="4" placeholder="您的留言"></el-input>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="submitContactForm">发送留言</el-button>
                </el-form-item>
              </el-form>
            </div>
          </el-col>
        </el-row>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Location, Message, Phone } from '@element-plus/icons-vue'
import { Icon } from '@iconify/vue'

// 联系表单数据
const contactForm = ref({
  name: '',
  email: '',
  subject: '',
  message: ''
})

// 提交联系表单
const submitContactForm = () => {
  // 这里应该添加表单验证
  if (!contactForm.value.name || !contactForm.value.email || !contactForm.value.message) {
    ElMessage.warning('请填写必要的信息')
    return
  }
  
  // 模拟提交成功
  ElMessage.success('留言已发送，我们会尽快回复您')
  
  // 清空表单
  contactForm.value = {
    name: '',
    email: '',
    subject: '',
    message: ''
  }
}
</script>

<style scoped>
/* 英雄区域 */
.hero-section {
  background: linear-gradient(135deg, #1f2a24 0%, #2c3a32 55%, #3a4638 100%);
  color: white;
  padding: 80px 0;
  text-align: center;
  margin-bottom: 40px;
}

.hero-section h2 {
  font-size: 36px;
  margin-bottom: 20px;
}

.hero-section p {
  font-size: 18px;
  max-width: 600px;
  margin: 0 auto;
}

/* 关于我们内容 */
.about-section {
  padding: 60px 0;
  background-color: #fff;
}

.about-content {
  padding-right: 30px;
}

.about-content h2 {
  margin-bottom: 20px;
  position: relative;
}

.about-content h2::after {
  content: '';
  display: block;
  width: 50px;
  height: 3px;
  background-color: #1f2a24;
  margin-top: 15px;
}

.about-content h3 {
  margin: 25px 0 15px;
  color: #333;
}

.about-content p {
  margin-bottom: 15px;
  line-height: 1.8;
  color: #666;
}

.about-image img {
  width: 100%;
  border-radius: 8px;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
}

/* 团队部分 */
.team-section {
  padding: 60px 0;
  background-color: #f8f9fa;
}

.section-title {
  font-size: 28px;
  color: #333;
  margin-bottom: 20px;
  text-align: center;
  position: relative;
}

.section-title::after {
  content: '';
  display: block;
  width: 50px;
  height: 3px;
  background-color: #1f2a24;
  margin: 15px auto 0;
}

.section-description {
  text-align: center;
  max-width: 700px;
  margin: 0 auto 40px;
  color: #666;
}

.team-member {
  text-align: center;
  margin-bottom: 30px;
  background-color: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.05);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.team-member:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
}

.member-avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  overflow: hidden;
  margin: 0 auto 15px;
}

.member-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.member-name {
  font-size: 18px;
  color: #333;
  margin-bottom: 5px;
}

.member-title {
  font-size: 14px;
  color: #8a6a33;
  margin-bottom: 10px;
}

.member-bio {
  font-size: 14px;
  color: #666;
}

/* 联系部分 */
.contact-section {
  padding: 60px 0;
  background-color: #fff;
}

.contact-info {
  margin-bottom: 30px;
}

.contact-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 25px;
}

.contact-item .el-icon {
  font-size: 24px;
  color: #8a6a33;
  margin-right: 15px;
  margin-top: 5px;
}

.contact-text h3 {
  font-size: 18px;
  margin-bottom: 5px;
  color: #333;
}

.contact-text p {
  color: #666;
}

.social-links {
  display: flex;
  margin-top: 20px;
}

.social-link {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background-color: #f0f2f5;
  border-radius: 50%;
  margin-right: 10px;
  transition: background-color 0.3s ease;
}

.social-link:hover {
  background-color: #1f2a24;
  color: white;
}

.contact-form {
  background-color: #f8f9fa;
  padding: 30px;
  border-radius: 8px;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.05);
}

/* 响应式调整 */
@media (max-width: 768px) {
  .about-content {
    padding-right: 0;
    margin-bottom: 30px;
  }
  
  .hero-section {
    padding: 50px 0;
  }
  
  .hero-section h2 {
    font-size: 28px;
  }
  
  .hero-section p {
    font-size: 16px;
  }
}
</style>