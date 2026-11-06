<template>
  <div class="model-viewer-container">
    <div class="model-loading" v-if="loading">
      <el-icon class="loading-icon"><Loading /></el-icon>
      <div class="loading-text">{{ $t('common.loading') }}</div>
    </div>
    
    <div class="model-viewer-content" v-else>
      <div class="model-canvas-container" ref="modelContainer">
        <!-- 3D模型将在这里渲染 -->
      </div>
      
      <!-- 控制面板 -->
      <div class="model-controls">
        <el-button-group>
          <el-tooltip :content="$t('model.rotate')" placement="top">
            <el-button @click="toggleAutoRotate">
              <el-icon><RefreshRight /></el-icon>
            </el-button>
          </el-tooltip>
          
          <el-tooltip :content="$t('model.reset')" placement="top">
            <el-button @click="resetCamera">
              <el-icon><Refresh /></el-icon>
            </el-button>
          </el-tooltip>
          
          <el-tooltip :content="$t('model.wireframe')" placement="top">
            <el-button @click="toggleWireframe">
              <el-icon><Grid /></el-icon>
            </el-button>
          </el-tooltip>
          
          <el-tooltip :content="$t('model.fullscreen')" placement="top">
            <el-button @click="toggleFullscreen">
              <el-icon><FullScreen /></el-icon>
            </el-button>
          </el-tooltip>
        </el-button-group>
      </div>
      
      <!-- 信息面板 -->
      <div class="model-info" v-if="showInfo">
        <h3>{{ modelName }}</h3>
        <p>{{ modelDescription }}</p>
        <div class="model-meta" v-if="modelMeta">
          <div class="model-meta-item" v-for="(value, key) in modelMeta" :key="key">
            <span class="meta-label">{{ key }}:</span>
            <span>{{ value }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, defineProps, defineEmits, watch } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

// 定义属性
const props = defineProps({
  modelUrl: {
    type: String,
    required: true
  },
  modelName: {
    type: String,
    default: '3D模型'
  },
  modelDescription: {
    type: String,
    default: ''
  },
  modelMeta: {
    type: Object,
    default: () => ({})
  },
  showInfo: {
    type: Boolean,
    default: true
  },
  autoRotate: {
    type: Boolean,
    default: true
  },
  backgroundColor: {
    type: String,
    default: '#f0f0f0'
  }
})

// 定义事件
const emit = defineEmits(['model-loaded', 'model-error'])

// 状态变量
const loading = ref(true)
const modelContainer = ref(null)

// Three.js 变量
let scene, camera, renderer, controls, model
let isAutoRotating = props.autoRotate
let isWireframe = false
let isFullscreen = false

// 初始化Three.js场景
function initThreeJS() {
  // 创建场景
  scene = new THREE.Scene()
  scene.background = new THREE.Color(props.backgroundColor)
  
  // 创建相机
  camera = new THREE.PerspectiveCamera(75, modelContainer.value.clientWidth / modelContainer.value.clientHeight, 0.1, 1000)
  camera.position.z = 5
  
  // 创建渲染器
  renderer = new THREE.WebGLRenderer({ antialias: true })
  renderer.setSize(modelContainer.value.clientWidth, modelContainer.value.clientHeight)
  renderer.setPixelRatio(window.devicePixelRatio)
  renderer.shadowMap.enabled = true
  modelContainer.value.appendChild(renderer.domElement)
  
  // 创建控制器
  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.dampingFactor = 0.05
  controls.autoRotate = isAutoRotating
  controls.autoRotateSpeed = 1.0
  
  // 添加灯光
  const ambientLight = new THREE.AmbientLight(0xffffff, 0.5)
  scene.add(ambientLight)
  
  const directionalLight = new THREE.DirectionalLight(0xffffff, 1)
  directionalLight.position.set(1, 1, 1)
  directionalLight.castShadow = true
  scene.add(directionalLight)
  
  // 加载模型
  loadModel()
  
  // 开始动画循环
  animate()
  
  // 添加窗口大小调整监听
  window.addEventListener('resize', onWindowResize)
}

// 加载3D模型
function loadModel() {
  const loader = new GLTFLoader()
  
  loader.load(
    props.modelUrl,
    (gltf) => {
      model = gltf.scene
      
      // 调整模型大小和位置
      const box = new THREE.Box3().setFromObject(model)
      const size = box.getSize(new THREE.Vector3()).length()
      const center = box.getCenter(new THREE.Vector3())
      
      model.position.x = -center.x
      model.position.y = -center.y
      model.position.z = -center.z
      
      camera.position.z = size * 2
      camera.far = size * 10
      camera.updateProjectionMatrix()
      
      scene.add(model)
      loading.value = false
      emit('model-loaded', model)
    },
    (xhr) => {
      // 加载进度
      console.log((xhr.loaded / xhr.total * 100) + '% loaded')
    },
    (error) => {
      console.error('模型加载失败:', error)
      loading.value = false
      emit('model-error', error)
    }
  )
}

// 动画循环
function animate() {
  requestAnimationFrame(animate)
  controls.update()
  renderer.render(scene, camera)
}

// 窗口大小调整处理
function onWindowResize() {
  if (!modelContainer.value) return
  
  camera.aspect = modelContainer.value.clientWidth / modelContainer.value.clientHeight
  camera.updateProjectionMatrix()
  renderer.setSize(modelContainer.value.clientWidth, modelContainer.value.clientHeight)
}

// 切换自动旋转
function toggleAutoRotate() {
  isAutoRotating = !isAutoRotating
  controls.autoRotate = isAutoRotating
}

// 重置相机位置
function resetCamera() {
  if (!model) return
  
  const box = new THREE.Box3().setFromObject(model)
  const size = box.getSize(new THREE.Vector3()).length()
  
  camera.position.set(0, 0, size * 2)
  controls.target.set(0, 0, 0)
  controls.update()
}

// 切换线框模式
function toggleWireframe() {
  isWireframe = !isWireframe
  
  if (model) {
    model.traverse((child) => {
      if (child.isMesh) {
        child.material.wireframe = isWireframe
      }
    })
  }
}

// 切换全屏模式
function toggleFullscreen() {
  isFullscreen = !isFullscreen
  
  if (isFullscreen) {
    if (modelContainer.value.requestFullscreen) {
      modelContainer.value.requestFullscreen()
    } else if (modelContainer.value.webkitRequestFullscreen) {
      modelContainer.value.webkitRequestFullscreen()
    } else if (modelContainer.value.msRequestFullscreen) {
      modelContainer.value.msRequestFullscreen()
    }
  } else {
    if (document.exitFullscreen) {
      document.exitFullscreen()
    } else if (document.webkitExitFullscreen) {
      document.webkitExitFullscreen()
    } else if (document.msExitFullscreen) {
      document.msExitFullscreen()
    }
  }
}

// 监听模型URL变化
watch(() => props.modelUrl, (newUrl, oldUrl) => {
  if (newUrl !== oldUrl) {
    // 清除旧模型
    if (model) {
      scene.remove(model)
      model = null
    }
    
    loading.value = true
    loadModel()
  }
})

// 组件挂载时初始化
onMounted(() => {
  if (modelContainer.value) {
    initThreeJS()
  }
})

// 组件卸载前清理资源
onBeforeUnmount(() => {
  if (renderer) {
    renderer.dispose()
  }
  
  if (controls) {
    controls.dispose()
  }
  
  window.removeEventListener('resize', onWindowResize)
})
</script>

<style scoped>
.model-viewer-container {
  position: relative;
  width: 100%;
  height: 400px;
  border-radius: 8px;
  overflow: hidden;
  background-color: v-bind(backgroundColor);
}

.model-loading {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-color: rgba(240, 240, 240, 0.8);
  z-index: 10;
}

.loading-icon {
  font-size: 40px;
  color: #8a6a33;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-text {
  margin-top: 15px;
  font-size: 16px;
  color: #666;
}

.model-canvas-container {
  width: 100%;
  height: 100%;
}

.model-controls {
  position: absolute;
  bottom: 15px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 5;
  background-color: rgba(255, 255, 255, 0.8);
  border-radius: 4px;
  padding: 5px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.model-info {
  position: absolute;
  top: 15px;
  left: 15px;
  max-width: 300px;
  background-color: rgba(255, 255, 255, 0.8);
  border-radius: 4px;
  padding: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  z-index: 5;
}

.model-info h3 {
  font-size: 16px;
  margin-bottom: 5px;
  color: #333;
}

.model-info p {
  font-size: 14px;
  color: #666;
  margin-bottom: 10px;
}

.model-meta {
  font-size: 12px;
}

.model-meta-item {
  display: flex;
  margin-bottom: 5px;
}

.meta-label {
  font-weight: bold;
  margin-right: 5px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .model-viewer-container {
    height: 300px;
  }
  
  .model-info {
    max-width: 200px;
    font-size: 12px;
  }
}
</style>