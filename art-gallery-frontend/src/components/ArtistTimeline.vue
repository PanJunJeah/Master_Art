<template>
  <div class="artist-timeline-container">
    <h3 class="timeline-title">{{ title || '艺术生涯时间轴' }}</h3>
    
    <!-- 时间轴导航 -->
    <div class="timeline-navigation" v-if="events.length > 0">
      <el-scrollbar>
        <div class="timeline-years">
          <div 
            v-for="(event, index) in events" 
            :key="index"
            class="timeline-year-marker"
            :class="{ 'active': selectedEventIndex === index }"
            @click="selectEvent(index)"
          >
            <div class="year-dot"></div>
            <div class="year-label">{{ event.year }}</div>
          </div>
        </div>
      </el-scrollbar>
    </div>
    
    <!-- 时间轴内容 -->
    <div class="timeline-content" v-if="events.length > 0">
      <transition name="fade" mode="out-in">
        <div class="timeline-event" :key="selectedEventIndex">
          <div class="event-year">{{ events[selectedEventIndex].year }}</div>
          <div class="event-title">{{ events[selectedEventIndex].title }}</div>
          <div class="event-description">{{ events[selectedEventIndex].description }}</div>
          
          <div class="event-image" v-if="events[selectedEventIndex].imageUrl">
            <img :src="events[selectedEventIndex].imageUrl" :alt="events[selectedEventIndex].title">
          </div>
        </div>
      </transition>
      
      <!-- 导航按钮 -->
      <div class="timeline-controls">
        <el-button 
          type="text" 
          :disabled="selectedEventIndex === 0"
          @click="prevEvent"
        >
          <el-icon><ArrowLeft /></el-icon> 上一个
        </el-button>
        
        <el-button 
          type="text" 
          :disabled="selectedEventIndex === events.length - 1"
          @click="nextEvent"
        >
          下一个 <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>
    </div>
    
    <!-- 无数据提示 -->
    <div class="no-events" v-else>
      <el-empty description="暂无艺术家生涯数据"></el-empty>
    </div>
  </div>
</template>

<script setup>
import { ref, defineProps, defineEmits } from 'vue'

// 定义属性
const props = defineProps({
  title: {
    type: String,
    default: ''
  },
  events: {
    type: Array,
    default: () => []
  }
})

// 定义事件
const emit = defineEmits(['event-selected'])

// 当前选中的事件索引
const selectedEventIndex = ref(0)

// 选择事件
function selectEvent(index) {
  selectedEventIndex.value = index
  emit('event-selected', props.events[index])
}

// 上一个事件
function prevEvent() {
  if (selectedEventIndex.value > 0) {
    selectEvent(selectedEventIndex.value - 1)
  }
}

// 下一个事件
function nextEvent() {
  if (selectedEventIndex.value < props.events.length - 1) {
    selectEvent(selectedEventIndex.value + 1)
  }
}
</script>

<style scoped>
.artist-timeline-container {
  margin: 40px 0;
  padding: 20px;
  background-color: #f9f9f9;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.timeline-title {
  font-size: 20px;
  margin-bottom: 20px;
  color: #333;
  text-align: center;
}

/* 时间轴导航 */
.timeline-navigation {
  margin-bottom: 30px;
}

.timeline-years {
  display: flex;
  padding: 10px 0;
  min-width: 100%;
}

.timeline-year-marker {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-right: 50px;
  cursor: pointer;
  position: relative;
}

.timeline-year-marker:last-child {
  margin-right: 0;
}

.timeline-year-marker::after {
  content: '';
  position: absolute;
  top: 10px;
  left: 25px;
  width: 50px;
  height: 2px;
  background-color: #ddd;
}

.timeline-year-marker:last-child::after {
  display: none;
}

.year-dot {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background-color: #ddd;
  transition: all 0.3s ease;
}

.year-label {
  margin-top: 8px;
  font-size: 14px;
  color: #666;
  transition: all 0.3s ease;
}

.timeline-year-marker.active .year-dot {
  background-color: #1f2a24;
  transform: scale(1.2);
}

.timeline-year-marker.active .year-label {
  color: #8a6a33;
  font-weight: bold;
}

/* 时间轴内容 */
.timeline-content {
  padding: 20px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.timeline-event {
  animation: fadeIn 0.5s ease-in-out;
}

.event-year {
  display: inline-block;
  padding: 5px 10px;
  background-color: #1f2a24;
  color: white;
  border-radius: 4px;
  font-weight: bold;
  margin-bottom: 15px;
}

.event-title {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 10px;
  color: #333;
}

.event-description {
  font-size: 16px;
  line-height: 1.6;
  color: #666;
  margin-bottom: 20px;
}

.event-image {
  margin-top: 20px;
  text-align: center;
}

.event-image img {
  max-width: 100%;
  max-height: 300px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

/* 导航按钮 */
.timeline-controls {
  display: flex;
  justify-content: space-between;
  margin-top: 20px;
}

/* 动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .timeline-years {
    padding: 10px 20px;
  }
  
  .timeline-year-marker {
    margin-right: 30px;
  }
  
  .timeline-year-marker::after {
    width: 30px;
  }
  
  .event-title {
    font-size: 18px;
  }
  
  .event-description {
    font-size: 14px;
  }
}
</style>