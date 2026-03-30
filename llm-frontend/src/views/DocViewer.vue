<template>
  <div class="doc-viewer">
    <el-container class="doc-container">
      <!-- 左侧目录树 -->
      <el-aside width="280px" class="doc-sidebar">
        <div class="sidebar-header">
          <span class="sidebar-title">📚 文档中心</span>
          <el-input
            v-model="searchKeyword"
            placeholder="AI 语义检索..."
            size="small"
            clearable
            @keyup.enter="handleSearch"
            style="margin-top:8px"
          >
            <template #append>
              <el-button :icon="Search" @click="handleSearch" :loading="searchLoading" />
            </template>
          </el-input>
        </div>

        <!-- 搜索结果 -->
        <div v-if="searchResults.length > 0" class="search-results">
          <div class="section-label">搜索结果</div>
          <div
            v-for="hit in searchResults"
            :key="hit.path"
            class="search-hit"
            @click="loadDoc(hit.path)"
          >
            <el-icon><Document /></el-icon>
            <div class="hit-info">
              <div class="hit-name">{{ hit.name }}</div>
              <div class="hit-content">{{ hit.content }}</div>
            </div>
          </div>
          <el-button link size="small" @click="clearSearch" style="margin-top:4px">清除搜索</el-button>
        </div>

        <!-- 目录树 -->
        <div v-else>
          <el-skeleton v-if="treeLoading" :rows="6" animated style="padding:16px" />
          <el-tree
            v-else
            :data="treeData"
            :props="treeProps"
            @node-click="handleNodeClick"
            default-expand-all
            highlight-current
            class="doc-tree"
          >
            <template #default="{ node, data }">
              <span class="tree-node">
                <el-icon v-if="data.type === 'DIRECTORY'"><FolderOpened /></el-icon>
                <el-icon v-else><Document /></el-icon>
                <span class="node-label">{{ data.name }}</span>
              </span>
            </template>
          </el-tree>
        </div>
      </el-aside>

      <!-- 右侧内容区 -->
      <el-main class="doc-main">
        <div v-if="!currentDoc" class="doc-welcome">
          <el-empty description="请从左侧选择文档">
            <template #image>
              <el-icon style="font-size:64px;color:#c0c4cc"><Document /></el-icon>
            </template>
          </el-empty>
        </div>

        <div v-else class="doc-content-wrapper">
          <div class="doc-toolbar">
            <span class="doc-path">{{ currentDoc.path }}</span>
            <el-button size="small" @click="currentDoc = null">
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
          <el-skeleton v-if="contentLoading" :rows="15" animated style="padding:24px" />
          <div
            v-else
            class="markdown-body"
            ref="markdownRef"
            v-html="renderedContent"
          />
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import mermaid from 'mermaid'
import { docViewerAPI } from '@/api/index.js'
import { Search } from '@element-plus/icons-vue'

// mermaid 初始化
mermaid.initialize({
  startOnLoad: false,
  theme: 'default',
  securityLevel: 'loose'
})

const treeData = ref([])
const treeLoading = ref(true)
const currentDoc = ref(null)
const contentLoading = ref(false)
const searchKeyword = ref('')
const searchResults = ref([])
const searchLoading = ref(false)
const markdownRef = ref(null)

const treeProps = {
  children: 'children',
  label: 'name'
}

const renderedContent = computed(() => {
  if (!currentDoc.value) return ''
  return marked(currentDoc.value.content)
})

onMounted(() => {
  loadTree()
})

watch(renderedContent, async () => {
  await nextTick()
  renderMermaid()
})

async function loadTree() {
  try {
    const result = await docViewerAPI.getTree()
    treeData.value = result.items || []
  } catch (e) {
    ElMessage.error('加载目录树失败')
  } finally {
    treeLoading.value = false
  }
}

async function loadDoc(path) {
  contentLoading.value = true
  try {
    const doc = await docViewerAPI.getContent(path)
    currentDoc.value = doc
  } catch (e) {
    ElMessage.error('加载文档失败')
  } finally {
    contentLoading.value = false
  }
}

function handleNodeClick(data) {
  if (data.type === 'FILE') {
    loadDoc(data.path)
  }
}

async function handleSearch() {
  const keyword = searchKeyword.value.trim()
  if (!keyword) return
  searchLoading.value = true
  try {
    const result = await docViewerAPI.search(keyword)
    searchResults.value = result.hits || []
    if (searchResults.value.length === 0) {
      ElMessage.info('未找到相关文档')
    }
  } catch (e) {
    if (e.message && e.message.includes('503')) {
      ElMessage.warning('检索服务不可用，请确保 Qdrant 已启动')
    } else {
      ElMessage.error('检索失败')
    }
  } finally {
    searchLoading.value = false
  }
}

function clearSearch() {
  searchKeyword.value = ''
  searchResults.value = []
}

async function renderMermaid() {
  if (!markdownRef.value) return
  const blocks = markdownRef.value.querySelectorAll('code.language-mermaid')
  for (const block of blocks) {
    try {
      const id = `mermaid-${Math.random().toString(36).substr(2, 9)}`
      const source = block.textContent
      const { svg } = await mermaid.render(id, source)
      const container = document.createElement('div')
      container.className = 'mermaid-container'
      container.innerHTML = svg
      block.parentElement.replaceWith(container)
    } catch (e) {
      console.warn('Mermaid 渲染失败:', e)
    }
  }
}
</script>

<style scoped>
.doc-viewer {
  height: 100%;
  padding: 0;
  box-sizing: border-box;
}
.doc-container {
  height: 100%;
}
.doc-sidebar {
  display: flex;
  flex-direction: column;
  border-right: 1px solid #e4e7ed;
  background: #fff;
  overflow-y: auto;
}
.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
}
.sidebar-title {
  font-weight: 600;
  font-size: 15px;
  color: #303133;
}
.doc-tree {
  padding: 8px;
}
.tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}
.node-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.section-label {
  font-size: 11px;
  color: #909399;
  padding: 8px 16px 4px;
  font-weight: 600;
  text-transform: uppercase;
}
.search-results {
  padding: 4px 0;
}
.search-hit {
  display: flex;
  gap: 8px;
  padding: 8px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f5f5f5;
}
.search-hit:hover {
  background: #f5f7fa;
}
.hit-info { flex: 1; overflow: hidden; }
.hit-name { font-size: 13px; font-weight: 500; color: #303133; }
.hit-content {
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.doc-main {
  padding: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: #f8f9fa;
}
.doc-welcome {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}
.doc-content-wrapper {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.doc-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  font-size: 12px;
  color: #909399;
}
.doc-path { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.markdown-body {
  flex: 1;
  overflow-y: auto;
  padding: 32px 48px;
  background: #fff;
  font-size: 14px;
  line-height: 1.8;
  color: #24292e;
}
</style>

<style>
/* Markdown 全局样式 */
.markdown-body h1, .markdown-body h2, .markdown-body h3,
.markdown-body h4, .markdown-body h5, .markdown-body h6 {
  margin-top: 24px;
  margin-bottom: 12px;
  font-weight: 600;
  line-height: 1.4;
  color: #1a1a1a;
}
.markdown-body h1 { font-size: 28px; border-bottom: 2px solid #e1e4e8; padding-bottom: 8px; }
.markdown-body h2 { font-size: 22px; border-bottom: 1px solid #e1e4e8; padding-bottom: 6px; }
.markdown-body h3 { font-size: 18px; }
.markdown-body p { margin: 12px 0; }
.markdown-body code {
  background: #f6f8fa;
  border-radius: 4px;
  padding: 2px 6px;
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 13px;
  color: #e36209;
}
.markdown-body pre {
  background: #f6f8fa;
  border-radius: 6px;
  padding: 16px;
  overflow-x: auto;
  margin: 16px 0;
}
.markdown-body pre code {
  background: none;
  padding: 0;
  color: #24292e;
}
.markdown-body table {
  border-collapse: collapse;
  width: 100%;
  margin: 16px 0;
}
.markdown-body th, .markdown-body td {
  border: 1px solid #e1e4e8;
  padding: 8px 12px;
  text-align: left;
}
.markdown-body th { background: #f6f8fa; font-weight: 600; }
.markdown-body tr:hover { background: #f8f9fa; }
.markdown-body blockquote {
  border-left: 4px solid #0070f3;
  background: #f0f7ff;
  padding: 8px 16px;
  margin: 16px 0;
  color: #555;
}
.markdown-body ul, .markdown-body ol { padding-left: 24px; margin: 8px 0; }
.markdown-body li { margin: 4px 0; }
.markdown-body a { color: #0366d6; text-decoration: none; }
.markdown-body a:hover { text-decoration: underline; }
.markdown-body hr { border: none; border-top: 1px solid #e1e4e8; margin: 24px 0; }
.mermaid-container { text-align: center; margin: 24px 0; overflow-x: auto; }
.mermaid-container svg { max-width: 100%; }
</style>
