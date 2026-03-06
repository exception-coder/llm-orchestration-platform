<template>
  <div id="app">
    <el-container class="layout-container">
      <!-- 移动端菜单按钮 -->
      <div class="mobile-menu-btn" @click="toggleMobileMenu" v-if="isMobile">
        <el-icon><Expand /></el-icon>
      </div>

      <!-- 侧边栏 -->
      <el-aside :width="sidebarWidth" :class="['sidebar', { 'mobile-sidebar': isMobile, 'mobile-sidebar-open': mobileMenuOpen }]">
        <div class="logo">
          <h2 v-if="!isMobile">🤖 LLM 编排平台</h2>
          <h2 v-else>🤖</h2>
        </div>
        <el-menu
          :default-active="activeMenu"
          router
          :collapse="isMobile && !mobileMenuOpen"
          class="sidebar-menu"
          @select="handleMenuSelect"
        >
          <el-menu-item index="/prompt-test">
            <el-icon><Operation /></el-icon>
            <span>Prompt 测试</span>
          </el-menu-item>
          <el-menu-item index="/content-optimization">
            <el-icon><Edit /></el-icon>
            <span>内容优化</span>
          </el-menu-item>
          <el-menu-item index="/template-management">
            <el-icon><Document /></el-icon>
            <span>模板管理</span>
          </el-menu-item>
          <el-menu-item index="/model-management">
            <el-icon><Setting /></el-icon>
            <span>模型管理</span>
          </el-menu-item>
          <el-menu-item index="/chat">
            <el-icon><ChatDotRound /></el-icon>
            <span>对话测试</span>
          </el-menu-item>
          <el-menu-item index="/prompt-comparison">
            <el-icon><TrendCharts /></el-icon>
            <span>Prompt A/B 对比</span>
          </el-menu-item>
          <el-menu-item index="/markdown-to-image">
            <el-icon><Picture /></el-icon>
            <span>Markdown 转图片</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <!-- 移动端遮罩层 -->
      <div 
        class="mobile-overlay" 
        v-if="isMobile && mobileMenuOpen" 
        @click="toggleMobileMenu"
      ></div>

      <!-- 主内容区 -->
      <el-container :class="{ 'main-container-mobile': isMobile }">
        <el-header class="header">
          <div class="header-content">
            <span class="page-title">{{ pageTitle }}</span>
          </div>
        </el-header>
        <el-main class="main-content">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const isMobile = ref(false)
const mobileMenuOpen = ref(false)

const activeMenu = computed(() => route.path)

const pageTitle = computed(() => {
  const titles = {
    '/prompt-test': 'Prompt 测试工具',
    '/content-optimization': '内容优化',
    '/template-management': '模板管理',
    '/model-management': '模型管理',
    '/chat': '对话测试',
    '/prompt-comparison': 'Prompt A/B 对比测试',
    '/markdown-to-image': 'Markdown 转图片'
  }
  return titles[route.path] || 'LLM 编排平台'
})

const sidebarWidth = computed(() => {
  if (isMobile.value) {
    return mobileMenuOpen.value ? '250px' : '64px'
  }
  return '250px'
})

// 检测是否为移动端
const checkMobile = () => {
  isMobile.value = window.innerWidth <= 768
  if (!isMobile.value) {
    mobileMenuOpen.value = false
  }
}

// 切换移动端菜单
const toggleMobileMenu = () => {
  mobileMenuOpen.value = !mobileMenuOpen.value
}

// 移动端选择菜单后自动关闭
const handleMenuSelect = () => {
  if (isMobile.value) {
    mobileMenuOpen.value = false
  }
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})
</script>

<style scoped>
#app {
  height: 100vh;
  overflow: hidden;
}

.layout-container {
  height: 100%;
  position: relative;
}

/* 移动端菜单按钮 */
.mobile-menu-btn {
  position: fixed;
  top: 16px;
  left: 16px;
  z-index: 2001;
  width: 40px;
  height: 40px;
  background: #1890ff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  transition: all 0.3s;
}

.mobile-menu-btn:hover {
  background: #40a9ff;
  transform: scale(1.1);
}

.mobile-menu-btn :deep(.el-icon) {
  font-size: 20px;
}

/* 侧边栏 */
.sidebar {
  background: #001529;
  color: #fff;
  overflow-y: auto;
  transition: all 0.3s;
}

/* 移动端侧边栏 */
.mobile-sidebar {
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  z-index: 2000;
  width: 64px !important;
}

.mobile-sidebar-open {
  width: 250px !important;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.15);
}

/* 移动端主内容区 */
.main-container-mobile {
  margin-left: 64px;
  width: calc(100% - 64px);
}

/* 移动端遮罩层 */
.mobile-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 1999;
  animation: fadeIn 0.3s;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.logo {
  padding: 20px;
  text-align: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo h2 {
  margin: 0;
  color: #fff;
  font-size: 18px;
  white-space: nowrap;
  overflow: hidden;
}

.sidebar-menu {
  border: none;
  background: transparent;
}

:deep(.el-menu-item) {
  color: rgba(255, 255, 255, 0.65);
}

:deep(.el-menu-item:hover) {
  color: #fff;
  background: rgba(255, 255, 255, 0.1);
}

:deep(.el-menu-item.is-active) {
  color: #fff;
  background: #1890ff;
}

/* 折叠菜单样式 */
:deep(.el-menu--collapse) {
  width: 64px;
}

:deep(.el-menu--collapse .el-menu-item) {
  padding: 0 20px;
}

.header {
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  padding: 0 24px;
}

.header-content {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-title {
  font-size: 20px;
  font-weight: 500;
  color: #333;
}

.main-content {
  background: #f0f2f5;
  padding: 24px;
  overflow-y: auto;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .header {
    padding: 0 16px;
  }

  .page-title {
    font-size: 16px;
  }

  .main-content {
    padding: 16px;
  }

  .logo {
    padding: 16px;
  }

  .logo h2 {
    font-size: 24px;
  }
}
</style>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial,
    'Noto Sans', sans-serif, 'Apple Color Emoji', 'Segoe UI Emoji', 'Segoe UI Symbol',
    'Noto Color Emoji';
}

#app {
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}
</style>

