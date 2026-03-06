<template>
  <div class="content-optimization">
    <el-row :gutter="20">
      <!-- 左侧：输入表单 -->
      <el-col :xs="24" :sm="24" :md="12" :lg="12">
        <el-card class="form-card">
          <template #header>
            <div class="card-header">
              <span>内容优化配置</span>
            </div>
          </template>

          <el-form :model="form" :label-width="isMobile ? '100%' : '120px'" :label-position="isMobile ? 'top' : 'right'">
            <!-- 原始内容 -->
            <el-form-item label="原始内容">
              <el-input
                v-model="form.originalContent"
                type="textarea"
                :rows="6"
                placeholder="请输入需要优化的原始内容"
                maxlength="5000"
                show-word-limit
              />
            </el-form-item>

            <!-- 目标平台 -->
            <el-form-item label="目标平台">
              <el-select v-model="form.platform" placeholder="请选择平台" style="width: 100%">
                <el-option
                  v-for="platform in platforms"
                  :key="platform.code"
                  :label="platform.name"
                  :value="platform.code"
                >
                  <div style="display: flex; justify-content: space-between">
                    <span>{{ platform.name }}</span>
                    <span style="color: #8492a6; font-size: 12px" class="hidden-xs-only">
                      {{ platform.description }}
                    </span>
                  </div>
                </el-option>
              </el-select>
            </el-form-item>

            <!-- 内容风格 -->
            <el-form-item label="内容风格">
              <el-select v-model="form.style" placeholder="请选择风格" style="width: 100%">
                <el-option
                  v-for="style in styles"
                  :key="style.code"
                  :label="style.name"
                  :value="style.code"
                >
                  <div style="display: flex; justify-content: space-between">
                    <span>{{ style.name }}</span>
                    <span style="color: #8492a6; font-size: 12px" class="hidden-xs-only">
                      {{ style.description }}
                    </span>
                  </div>
                </el-option>
              </el-select>
            </el-form-item>

            <!-- 内容类型 -->
            <el-form-item label="内容类型">
              <el-select v-model="form.contentType" placeholder="请选择类型" style="width: 100%">
                <el-option
                  v-for="type in contentTypes"
                  :key="type.code"
                  :label="type.name"
                  :value="type.code"
                />
              </el-select>
            </el-form-item>

            <!-- 目标受众 -->
            <el-form-item label="目标受众">
              <el-input
                v-model="form.targetAudience"
                placeholder="例如：18-35岁女性"
              />
            </el-form-item>

            <!-- 额外要求 -->
            <el-form-item label="额外要求">
              <el-input
                v-model="form.additionalRequirements"
                type="textarea"
                :rows="3"
                placeholder="其他特殊要求（可选）"
              />
            </el-form-item>

            <!-- 生成数量 -->
            <el-form-item label="生成数量">
              <el-radio-group v-model="form.count">
                <el-radio :label="1">1个</el-radio>
                <el-radio :label="3">3个</el-radio>
                <el-radio :label="5">5个</el-radio>
              </el-radio-group>
            </el-form-item>

            <!-- 操作按钮 -->
            <el-form-item>
              <el-button type="primary" @click="handleOptimize" :loading="loading" size="large" style="width: 100%">
                <el-icon><MagicStick /></el-icon>
                开始优化
              </el-button>
              <el-button @click="handleReset" size="large" style="width: 100%; margin-top: 10px; margin-left: 0">
                <el-icon><RefreshLeft /></el-icon>
                重置
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 右侧：优化结果 -->
      <el-col :xs="24" :sm="24" :md="12" :lg="12">
        <el-card class="result-card">
          <template #header>
            <div class="card-header">
              <span>优化结果</span>
              <el-tag v-if="results.length > 0" type="success">
                共 {{ results.length }} 个版本
              </el-tag>
            </div>
          </template>

          <div v-if="results.length === 0" class="empty-result">
            <el-empty description="暂无优化结果，请先配置并开始优化" />
          </div>

          <el-tabs v-else v-model="activeResultTab">
            <el-tab-pane
              v-for="(result, index) in results"
              :key="index"
              :label="`版本 ${index + 1}`"
              :name="String(index)"
            >
              <!-- 优化后的内容 -->
              <div class="result-section">
                <h4>优化后的内容</h4>
                <el-input
                  v-model="result.optimizedContent"
                  type="textarea"
                  :rows="10"
                  readonly
                  class="result-textarea"
                />
                <div class="copy-btn-wrapper">
                  <el-button @click="copyToClipboard(result.optimizedContent)" size="small">
                    <el-icon><CopyDocument /></el-icon>
                    复制内容
                  </el-button>
                </div>
              </div>

              <!-- 建议标题 -->
              <div class="result-section">
                <h4>建议标题</h4>
                <el-tag
                  v-for="(title, idx) in result.suggestedTitles"
                  :key="idx"
                  class="tag-item"
                  @click="copyToClipboard(title)"
                  style="cursor: pointer"
                >
                  {{ title }}
                </el-tag>
              </div>

              <!-- 建议标签 -->
              <div class="result-section">
                <h4>建议标签</h4>
                <el-tag
                  v-for="(tag, idx) in result.suggestedTags"
                  :key="idx"
                  type="info"
                  class="tag-item"
                  @click="copyToClipboard(tag)"
                  style="cursor: pointer"
                >
                  {{ tag }}
                </el-tag>
              </div>

              <!-- 优化说明 -->
              <div class="result-section">
                <h4>优化说明</h4>
                <el-alert
                  :title="result.optimizationNotes"
                  type="info"
                  :closable="false"
                />
              </div>

              <!-- 元信息 -->
              <div class="result-section">
                <el-descriptions :column="2" size="small" border>
                  <el-descriptions-item label="平台">
                    {{ result.platform }}
                  </el-descriptions-item>
                  <el-descriptions-item label="风格">
                    {{ result.style }}
                  </el-descriptions-item>
                  <el-descriptions-item label="Token使用">
                    {{ result.tokenUsage?.totalTokens || 0 }}
                  </el-descriptions-item>
                </el-descriptions>
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { contentOptimizationAPI } from '@/api'

const isMobile = ref(false)

const form = ref({
  originalContent: '',
  platform: 'XIAOHONGSHU',
  style: 'CASUAL',
  contentType: 'LIFESTYLE',
  targetAudience: '',
  additionalRequirements: '',
  count: 1
})

const platforms = ref([])
const styles = ref([])
const contentTypes = ref([])
const results = ref([])
const loading = ref(false)
const activeResultTab = ref('0')

// 检测是否为移动端
const checkMobile = () => {
  isMobile.value = window.innerWidth <= 768
}

onMounted(async () => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
  await loadOptions()
})

// 加载选项数据
const loadOptions = async () => {
  try {
    platforms.value = await contentOptimizationAPI.getPlatforms()
    styles.value = await contentOptimizationAPI.getStyles()
    contentTypes.value = await contentOptimizationAPI.getContentTypes()
  } catch (error) {
    console.error('加载选项失败:', error)
  }
}

// 执行优化
const handleOptimize = async () => {
  if (!form.value.originalContent.trim()) {
    ElMessage.warning('请输入原始内容')
    return
  }

  loading.value = true
  results.value = []

  try {
    const response = await contentOptimizationAPI.optimize(form.value)
    
    // 处理单个或多个结果
    if (Array.isArray(response)) {
      results.value = response
    } else {
      results.value = [response]
    }
    
    activeResultTab.value = '0'
    ElMessage.success('优化完成')
  } catch (error) {
    console.error('优化失败:', error)
  } finally {
    loading.value = false
  }
}

// 重置表单
const handleReset = () => {
  form.value = {
    originalContent: '',
    platform: 'XIAOHONGSHU',
    style: 'CASUAL',
    contentType: 'LIFESTYLE',
    targetAudience: '',
    additionalRequirements: '',
    count: 1
  }
  results.value = []
}

// 复制到剪贴板
const copyToClipboard = (text) => {
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}
</script>

<style scoped>
.content-optimization {
  max-width: 1600px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.empty-result {
  padding: 40px 0;
}

.result-section {
  margin-bottom: 24px;
}

.result-section h4 {
  margin-bottom: 12px;
  color: #333;
  font-size: 14px;
  font-weight: 600;
}

.result-textarea {
  font-family: 'Courier New', monospace;
  font-size: 14px;
}

.copy-btn-wrapper {
  margin-top: 10px;
  text-align: right;
}

.tag-item {
  margin-right: 8px;
  margin-bottom: 8px;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .result-card {
    margin-top: 20px;
  }

  :deep(.el-form-item__label) {
    width: 100% !important;
    text-align: left;
    margin-bottom: 8px;
  }

  :deep(.el-form-item__content) {
    margin-left: 0 !important;
  }

  .hidden-xs-only {
    display: none;
  }

  :deep(.el-radio-group) {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  :deep(.el-radio) {
    margin-right: 0 !important;
    margin-left: 0 !important;
  }
}
</style>

