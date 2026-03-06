<template>
  <div class="markdown-to-image">
    <!-- 移动端标签页切换 -->
    <el-tabs v-if="isMobile" v-model="activeTab" class="mobile-tabs">
      <el-tab-pane label="编辑" name="edit">
        <el-card class="editor-card mobile-card">
          <template #header>
            <div class="card-header">
              <span>📝 Markdown 编辑器</span>
              <el-button type="primary" size="small" @click="loadExample">
                示例
              </el-button>
            </div>
          </template>
          <el-input
            v-model="markdownText"
            type="textarea"
            :rows="20"
            placeholder="在此输入 Markdown 文本..."
            class="markdown-editor"
          />
        </el-card>
      </el-tab-pane>
      
      <el-tab-pane label="预览" name="preview">
        <el-card class="preview-card mobile-card">
          <template #header>
            <div class="card-header">
              <span>🎨 预览与导出</span>
              <div class="action-buttons">
                <el-button type="primary" size="small" @click="copyToClipboard" :loading="copying">
                  <el-icon><CopyDocument /></el-icon>
                </el-button>
                <el-button type="success" size="small" @click="exportImage" :loading="exporting">
                  <el-icon><Download /></el-icon>
                </el-button>
              </div>
            </div>
          </template>

          <!-- 模板选择 -->
          <div class="template-selector">
            <div class="template-label">选择模板：</div>
            <div class="template-grid">
              <div
                v-for="template in templates"
                :key="template.id"
                :class="['template-item', { active: selectedTemplate === template.id }]"
                @click="selectedTemplate = template.id"
              >
                <div class="template-preview" :style="template.previewStyle">
                  <span class="template-icon">{{ template.icon }}</span>
                </div>
                <div class="template-name">{{ template.name }}</div>
              </div>
            </div>
          </div>

          <!-- 预览区域 -->
          <div class="preview-wrapper">
            <div
              ref="previewRef"
              :class="['preview-content', `template-${selectedTemplate}`]"
              v-html="renderedHtml"
            ></div>
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- PC 端左右布局 -->
    <el-row v-else :gutter="24">
      <!-- 左侧编辑区 -->
      <el-col :span="12">
        <el-card class="editor-card">
          <template #header>
            <div class="card-header">
              <span>📝 Markdown 编辑器</span>
              <el-button type="primary" size="small" @click="loadExample">
                加载示例
              </el-button>
            </div>
          </template>
          <el-input
            v-model="markdownText"
            type="textarea"
            :rows="25"
            placeholder="在此输入 Markdown 文本..."
            class="markdown-editor"
          />
        </el-card>
      </el-col>

      <!-- 右侧预览区 -->
      <el-col :span="12">
        <el-card class="preview-card">
          <template #header>
            <div class="card-header">
              <span>🎨 预览与导出</span>
              <div class="action-buttons">
                <el-button type="primary" size="small" @click="copyToClipboard" :loading="copying">
                  <el-icon><CopyDocument /></el-icon>
                  复制图片
                </el-button>
                <el-button type="success" size="small" @click="exportImage" :loading="exporting">
                  <el-icon><Download /></el-icon>
                  导出图片
                </el-button>
              </div>
            </div>
          </template>

          <!-- 模板选择 -->
          <div class="template-selector">
            <div class="template-label">选择模板：</div>
            <div class="template-grid">
              <div
                v-for="template in templates"
                :key="template.id"
                :class="['template-item', { active: selectedTemplate === template.id }]"
                @click="selectedTemplate = template.id"
              >
                <div class="template-preview" :style="template.previewStyle">
                  <span class="template-icon">{{ template.icon }}</span>
                </div>
                <div class="template-name">{{ template.name }}</div>
              </div>
            </div>
          </div>

          <!-- 预览区域 -->
          <div class="preview-wrapper">
            <div
              ref="previewRef"
              :class="['preview-content', `template-${selectedTemplate}`]"
              v-html="renderedHtml"
            ></div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/atom-one-dark.css'
import html2canvas from 'html2canvas'
import { ElMessage } from 'element-plus'

const markdownText = ref('')
const selectedTemplate = ref('gradient')
const previewRef = ref(null)
const copying = ref(false)
const exporting = ref(false)
const isMobile = ref(false)
const activeTab = ref('edit')

// 检测是否为移动端
const checkMobile = () => {
  isMobile.value = window.innerWidth <= 768
}

// 配置 marked
marked.setOptions({
  highlight: function(code, lang) {
    if (lang && hljs.getLanguage(lang)) {
      return hljs.highlight(code, { language: lang }).value
    }
    return hljs.highlightAuto(code).value
  },
  breaks: true,
  gfm: true
})

// 模板配置
const templates = [
  {
    id: 'gradient',
    name: '渐变科技',
    icon: '🌈',
    previewStyle: {
      background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
    }
  },
  {
    id: 'elegant',
    name: '优雅卡片',
    icon: '✨',
    previewStyle: {
      background: 'linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)'
    }
  },
  {
    id: 'dark',
    name: '暗黑终端',
    icon: '🖥️',
    previewStyle: {
      background: 'linear-gradient(135deg, #0f2027 0%, #203a43 50%, #2c5364 100%)'
    }
  },
  {
    id: 'nature',
    name: '清新自然',
    icon: '🌿',
    previewStyle: {
      background: 'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)'
    }
  },
  {
    id: 'cyber',
    name: '赛博朋克',
    icon: '🔮',
    previewStyle: {
      background: 'linear-gradient(135deg, #ff0080 0%, #7928ca 50%, #0070f3 100%)'
    }
  }
]

// 渲染 Markdown
const renderedHtml = computed(() => {
  if (!markdownText.value) {
    return '<div class="empty-hint">在左侧输入 Markdown 文本，这里将实时预览效果</div>'
  }
  return marked(markdownText.value)
})

// 加载示例
const loadExample = () => {
  markdownText.value = `# 🚀 Markdown 转图片工具

## 功能特性

这是一个强大的 **Markdown 转图片** 工具，支持：

- ✅ 实时预览
- ✅ 多种精美模板
- ✅ 代码高亮
- ✅ 一键导出

## 代码示例

\`\`\`javascript
function greet(name) {
  console.log(\`Hello, \${name}!\`)
  return true
}

greet('World')
\`\`\`

## 表格支持

| 特性 | 状态 | 说明 |
|------|------|------|
| Markdown | ✅ | 完整支持 |
| 代码高亮 | ✅ | 多语言 |
| 导出图片 | ✅ | 高清输出 |

## 引用文字

> 优秀的工具能够提升效率，
> 让创作变得更加简单。

---

**试试选择不同的模板吧！** 🎨`
}

// 生成 Canvas（复用逻辑）
const generateCanvas = async () => {
  if (!previewRef.value) {
    throw new Error('预览区域未找到')
  }

  return await html2canvas(previewRef.value, {
    backgroundColor: null,
    scale: 2,
    logging: false,
    useCORS: true
  })
}

// 复制到剪贴板
const copyToClipboard = async () => {
  if (!previewRef.value) {
    ElMessage.warning('请先输入 Markdown 内容')
    return
  }

  copying.value = true

  try {
    const canvas = await generateCanvas()

    // 检测是否支持 Clipboard API
    if (!navigator.clipboard) {
      // 降级方案：移动端或不支持的浏览器，先下载提示用户
      ElMessage.warning('您的浏览器不支持直接复制图片，将为您下载图片')
      downloadFromCanvas(canvas)
      return
    }

    // 将 canvas 转为 Blob
    canvas.toBlob(async (blob) => {
      if (!blob) {
        ElMessage.error('图片生成失败')
        copying.value = false
        return
      }

      try {
        // 尝试使用 Clipboard API 复制图片
        const item = new ClipboardItem({ 'image/png': blob })
        await navigator.clipboard.write([item])
        
        ElMessage.success('图片已复制到剪贴板！')
      } catch (err) {
        console.error('复制失败:', err)
        
        // 如果是权限问题或不支持，降级为下载
        if (err.name === 'NotAllowedError' || err.name === 'TypeError') {
          ElMessage.warning('无法复制图片，将为您下载')
          downloadFromCanvas(canvas)
        } else {
          ElMessage.error('复制失败，请重试')
        }
      } finally {
        copying.value = false
      }
    }, 'image/png')
  } catch (error) {
    console.error('生成图片失败:', error)
    ElMessage.error('生成图片失败，请重试')
    copying.value = false
  }
}

// 从 Canvas 下载图片
const downloadFromCanvas = (canvas) => {
  const link = document.createElement('a')
  link.download = `markdown-${Date.now()}.png`
  link.href = canvas.toDataURL('image/png')
  link.click()
}

// 导出图片
const exportImage = async () => {
  if (!previewRef.value) {
    ElMessage.warning('请先输入 Markdown 内容')
    return
  }

  exporting.value = true

  try {
    const canvas = await generateCanvas()
    downloadFromCanvas(canvas)
    ElMessage.success('图片已下载！')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败，请重试')
  } finally {
    exporting.value = false
  }
}

onMounted(() => {
  loadExample()
  checkMobile()
  window.addEventListener('resize', checkMobile)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})
</script>

<style scoped>
.markdown-to-image {
  height: calc(100vh - 120px);
}

.editor-card,
.preview-card {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.markdown-editor {
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 14px;
}

:deep(.el-textarea__inner) {
  font-family: 'Fira Code', 'Consolas', monospace;
  line-height: 1.6;
}

.template-selector {
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #e8e8e8;
}

.template-label {
  font-weight: 600;
  margin-bottom: 12px;
  color: #333;
}

.template-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
}

.template-item {
  cursor: pointer;
  text-align: center;
  transition: all 0.3s;
}

.template-item:hover {
  transform: translateY(-2px);
}

.template-preview {
  width: 100%;
  height: 60px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 6px;
  border: 3px solid transparent;
  transition: all 0.3s;
}

.template-item.active .template-preview {
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.template-icon {
  font-size: 24px;
}

.template-name {
  font-size: 12px;
  color: #666;
}

.preview-wrapper {
  max-height: calc(100vh - 400px);
  overflow-y: auto;
  padding: 10px;
  background: #f5f5f5;
  border-radius: 8px;
}

.preview-content {
  padding: 40px;
  border-radius: 12px;
  min-height: 400px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  position: relative;
  overflow: hidden;
}

/* 渐变科技模板 */
.template-gradient {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.template-gradient :deep(h1),
.template-gradient :deep(h2),
.template-gradient :deep(h3) {
  color: #fff;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.template-gradient :deep(code) {
  background: rgba(255, 255, 255, 0.2);
  padding: 2px 6px;
  border-radius: 4px;
}

.template-gradient :deep(pre) {
  background: rgba(0, 0, 0, 0.3);
  border-radius: 8px;
  padding: 16px;
}

.template-gradient :deep(blockquote) {
  border-left: 4px solid rgba(255, 255, 255, 0.5);
  padding-left: 16px;
  margin: 16px 0;
  opacity: 0.9;
}

.template-gradient :deep(table) {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  overflow: hidden;
}

.template-gradient :deep(th) {
  background: rgba(255, 255, 255, 0.2);
}

/* 优雅卡片模板 */
.template-elegant {
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  color: #2c3e50;
}

.template-elegant :deep(h1) {
  color: #667eea;
  border-bottom: 3px solid #667eea;
  padding-bottom: 12px;
}

.template-elegant :deep(h2) {
  color: #764ba2;
}

.template-elegant :deep(code) {
  background: rgba(102, 126, 234, 0.1);
  color: #667eea;
  padding: 2px 6px;
  border-radius: 4px;
}

.template-elegant :deep(pre) {
  background: #2c3e50;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.template-elegant :deep(blockquote) {
  border-left: 4px solid #667eea;
  background: rgba(102, 126, 234, 0.05);
  padding: 12px 16px;
  border-radius: 4px;
}

.template-elegant :deep(table) {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

/* 暗黑终端模板 */
.template-dark {
  background: linear-gradient(135deg, #0f2027 0%, #203a43 50%, #2c5364 100%);
  color: #00ff41;
  font-family: 'Courier New', monospace;
}

.template-dark :deep(h1),
.template-dark :deep(h2),
.template-dark :deep(h3) {
  color: #00ff41;
  text-shadow: 0 0 10px rgba(0, 255, 65, 0.5);
}

.template-dark :deep(code) {
  background: rgba(0, 255, 65, 0.1);
  color: #00ff41;
  padding: 2px 6px;
  border-radius: 4px;
}

.template-dark :deep(pre) {
  background: rgba(0, 0, 0, 0.5);
  border: 1px solid #00ff41;
  border-radius: 8px;
  padding: 16px;
}

.template-dark :deep(blockquote) {
  border-left: 4px solid #00ff41;
  padding-left: 16px;
  opacity: 0.8;
}

.template-dark :deep(a) {
  color: #00d4ff;
}

.template-dark :deep(table) {
  border: 1px solid #00ff41;
}

.template-dark :deep(th) {
  background: rgba(0, 255, 65, 0.2);
}

/* 清新自然模板 */
.template-nature {
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
  color: #2d3436;
}

.template-nature :deep(h1) {
  color: #00b894;
  font-weight: 700;
}

.template-nature :deep(h2) {
  color: #fd79a8;
}

.template-nature :deep(code) {
  background: rgba(0, 184, 148, 0.15);
  color: #00b894;
  padding: 2px 6px;
  border-radius: 4px;
}

.template-nature :deep(pre) {
  background: rgba(255, 255, 255, 0.8);
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.template-nature :deep(blockquote) {
  border-left: 4px solid #fd79a8;
  background: rgba(253, 121, 168, 0.1);
  padding: 12px 16px;
  border-radius: 8px;
}

.template-nature :deep(table) {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 8px;
  overflow: hidden;
}

/* 赛博朋克模板 */
.template-cyber {
  background: linear-gradient(135deg, #ff0080 0%, #7928ca 50%, #0070f3 100%);
  color: #fff;
  position: relative;
}

.template-cyber::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: 
    repeating-linear-gradient(
      0deg,
      rgba(0, 0, 0, 0.1) 0px,
      rgba(0, 0, 0, 0.1) 1px,
      transparent 1px,
      transparent 2px
    );
  pointer-events: none;
}

.template-cyber :deep(h1),
.template-cyber :deep(h2),
.template-cyber :deep(h3) {
  color: #00ffff;
  text-shadow: 0 0 20px rgba(0, 255, 255, 0.8);
  font-weight: 900;
  text-transform: uppercase;
}

.template-cyber :deep(code) {
  background: rgba(0, 255, 255, 0.2);
  color: #00ffff;
  padding: 2px 6px;
  border-radius: 4px;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.3);
}

.template-cyber :deep(pre) {
  background: rgba(0, 0, 0, 0.6);
  border: 2px solid #ff0080;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 0 20px rgba(255, 0, 128, 0.5);
}

.template-cyber :deep(blockquote) {
  border-left: 4px solid #00ffff;
  background: rgba(0, 255, 255, 0.1);
  padding: 12px 16px;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.2);
}

.template-cyber :deep(table) {
  background: rgba(0, 0, 0, 0.4);
  border: 1px solid #00ffff;
}

.template-cyber :deep(th) {
  background: rgba(0, 255, 255, 0.2);
}

/* 通用样式 */
.preview-content :deep(h1) {
  font-size: 32px;
  margin-bottom: 20px;
  font-weight: 700;
}

.preview-content :deep(h2) {
  font-size: 24px;
  margin: 24px 0 16px;
  font-weight: 600;
}

.preview-content :deep(h3) {
  font-size: 20px;
  margin: 20px 0 12px;
  font-weight: 600;
}

.preview-content :deep(p) {
  line-height: 1.8;
  margin: 12px 0;
}

.preview-content :deep(ul),
.preview-content :deep(ol) {
  padding-left: 24px;
  line-height: 1.8;
}

.preview-content :deep(li) {
  margin: 8px 0;
}

.preview-content :deep(pre) {
  margin: 16px 0;
  overflow-x: auto;
}

.preview-content :deep(pre code) {
  background: transparent !important;
  padding: 0 !important;
}

.preview-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 16px 0;
}

.preview-content :deep(th),
.preview-content :deep(td) {
  padding: 12px;
  text-align: left;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.preview-content :deep(hr) {
  border: none;
  border-top: 2px solid rgba(255, 255, 255, 0.3);
  margin: 24px 0;
}

.empty-hint {
  text-align: center;
  padding: 60px 20px;
  color: #999;
  font-size: 16px;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .markdown-to-image {
    height: auto;
  }

  .mobile-tabs {
    height: calc(100vh - 120px);
  }

  .mobile-tabs :deep(.el-tabs__content) {
    height: calc(100% - 55px);
    overflow-y: auto;
  }

  .mobile-tabs :deep(.el-tab-pane) {
    height: 100%;
  }

  .mobile-card {
    height: 100%;
    margin-bottom: 0;
  }

  .mobile-card :deep(.el-card__body) {
    height: calc(100% - 60px);
    overflow-y: auto;
  }

  .card-header span {
    font-size: 14px;
  }

  .action-buttons {
    gap: 4px;
  }

  .action-buttons .el-button {
    padding: 8px 12px;
  }

  .action-buttons .el-button span {
    display: none;
  }

  .template-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
  }

  .template-preview {
    height: 50px;
  }

  .template-icon {
    font-size: 20px;
  }

  .template-name {
    font-size: 11px;
  }

  .preview-wrapper {
    max-height: none;
    padding: 5px;
  }

  .preview-content {
    padding: 20px;
    min-height: 300px;
  }

  .preview-content :deep(h1) {
    font-size: 24px;
  }

  .preview-content :deep(h2) {
    font-size: 20px;
  }

  .preview-content :deep(h3) {
    font-size: 18px;
  }

  .preview-content :deep(pre) {
    font-size: 12px;
    padding: 12px;
  }

  .preview-content :deep(table) {
    font-size: 12px;
  }

  .preview-content :deep(th),
  .preview-content :deep(td) {
    padding: 8px;
  }

  .markdown-editor {
    font-size: 13px;
  }

  :deep(.el-textarea__inner) {
    font-size: 13px;
  }
}

/* 小屏幕移动端 */
@media (max-width: 480px) {
  .template-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .preview-content {
    padding: 16px;
  }

  .preview-content :deep(h1) {
    font-size: 20px;
  }

  .preview-content :deep(h2) {
    font-size: 18px;
  }

  .preview-content :deep(h3) {
    font-size: 16px;
  }
}
</style>
