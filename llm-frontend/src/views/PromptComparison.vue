<template>
  <div class="prompt-comparison">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>Prompt A/B 对比测试</span>
          <el-button type="primary" @click="handleCompare" :loading="comparing" :disabled="!canCompare">
            <el-icon><Operation /></el-icon>
            开始对比
          </el-button>
        </div>
      </template>

      <!-- 配置区域 -->
      <el-form :model="config" :label-width="isMobile ? '100%' : '120px'" :label-position="isMobile ? 'top' : 'right'">
        <el-row :gutter="20">
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-divider content-position="left">基础配置</el-divider>
            
            <el-form-item label="选择模板">
              <el-select 
                v-model="config.templateName" 
                placeholder="请选择模板" 
                style="width: 100%"
                @change="handleTemplateChange"
                filterable
              >
                <el-option
                  v-for="template in templates"
                  :key="template.templateName"
                  :label="`${template.templateName} (${template.category})`"
                  :value="template.templateName"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="模型">
              <el-select v-model="config.model" placeholder="选择模型" style="width: 100%" filterable>
                <el-option
                  v-for="model in availableModels"
                  :key="model.code"
                  :label="`${model.name} (${model.provider})`"
                  :value="model.code"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="输出模式">
              <el-switch
                v-model="config.stream"
                active-text="流式"
                inactive-text="非流式"
              />
            </el-form-item>
          </el-col>

          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-divider content-position="left">变量配置</el-divider>
            
            <el-form-item label="变量 JSON">
              <el-input
                v-model="config.variables"
                type="textarea"
                :rows="6"
                placeholder='{"key": "value"}'
              />
              <el-button 
                size="small" 
                type="primary" 
                link 
                @click="loadTemplateVariables"
                style="margin-top: 8px"
              >
                <el-icon><MagicStick /></el-icon>
                加载模板示例变量
              </el-button>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <!-- Prompt 对比区域 -->
      <el-divider content-position="left">Prompt 内容对比</el-divider>
      
      <el-row :gutter="20">
        <el-col :xs="24" :sm="24" :md="12" :lg="12">
          <div class="prompt-section">
            <div class="section-header">
              <span class="section-title">A版本：原始模板</span>
              <el-tag type="success">基准版本</el-tag>
            </div>
            <el-input
              v-model="promptA"
              type="textarea"
              :rows="12"
              readonly
              placeholder="选择模板后自动加载"
            />
          </div>
        </el-col>

        <el-col :xs="24" :sm="24" :md="12" :lg="12">
          <div class="prompt-section">
            <div class="section-header">
              <span class="section-title">B版本：调整后模板</span>
              <el-tag type="warning">测试版本</el-tag>
            </div>
            <el-input
              v-model="promptB"
              type="textarea"
              :rows="12"
              placeholder="在此修改 Prompt 内容进行对比测试"
            />
            <el-button 
              size="small" 
              type="primary" 
              link 
              @click="resetPromptB"
              style="margin-top: 8px"
            >
              <el-icon><RefreshLeft /></el-icon>
              重置为原始版本
            </el-button>
          </div>
        </el-col>
      </el-row>

      <!-- 结果对比区域 -->
      <el-divider content-position="left">输出结果对比</el-divider>
      
      <el-row :gutter="20">
        <el-col :xs="24" :sm="24" :md="12" :lg="12">
          <div class="result-section">
            <div class="section-header">
              <span class="section-title">A版本输出</span>
              <el-tag v-if="resultA.status === 'loading'" type="info">
                <el-icon class="is-loading"><Loading /></el-icon>
                生成中...
              </el-tag>
              <el-tag v-else-if="resultA.status === 'success'" type="success">完成</el-tag>
              <el-tag v-else-if="resultA.status === 'error'" type="danger">失败</el-tag>
            </div>
            <div class="result-content markdown-rendered" v-html="renderMarkdown(resultA.content)"></div>
            <div v-if="resultA.tokenUsage" class="result-meta">
              <el-tag size="small">Tokens: {{ resultA.tokenUsage.totalTokens }}</el-tag>
              <el-tag size="small" type="info">耗时: {{ resultA.duration }}ms</el-tag>
            </div>
          </div>
        </el-col>

        <el-col :xs="24" :sm="24" :md="12" :lg="12">
          <div class="result-section">
            <div class="section-header">
              <span class="section-title">B版本输出</span>
              <el-tag v-if="resultB.status === 'loading'" type="info">
                <el-icon class="is-loading"><Loading /></el-icon>
                生成中...
              </el-tag>
              <el-tag v-else-if="resultB.status === 'success'" type="success">完成</el-tag>
              <el-tag v-else-if="resultB.status === 'error'" type="danger">失败</el-tag>
            </div>
            <div class="result-content markdown-rendered" v-html="renderMarkdown(resultB.content)"></div>
            <div v-if="resultB.tokenUsage" class="result-meta">
              <el-tag size="small">Tokens: {{ resultB.tokenUsage.totalTokens }}</el-tag>
              <el-tag size="small" type="info">耗时: {{ resultB.duration }}ms</el-tag>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { templateAPI, modelConfigAPI, promptTestAPI } from '@/api'
import { useResponsive } from '@/composables/useResponsive'
import { useMarkdown } from '@/composables/useMarkdown'
import { useSSEStream } from '@/composables/useSSEStream'

const { isMobile } = useResponsive()
const { renderMarkdown } = useMarkdown()
const { fetchSSE } = useSSEStream()

const config = ref({
  templateName: '',
  model: '',
  stream: false,  // 对比测试默认使用非流式
  variables: ''
})

const templates = ref([])
const availableModels = ref([])
const promptA = ref('')
const promptB = ref('')
const comparing = ref(false)

const resultA = ref({
  status: '',  // loading, success, error
  content: '',
  tokenUsage: null,
  duration: 0
})

const resultB = ref({
  status: '',
  content: '',
  tokenUsage: null,
  duration: 0
})

const canCompare = computed(() => {
  return config.value.templateName && 
         config.value.model && 
         promptA.value && 
         promptB.value && 
         config.value.variables
})

onMounted(async () => {
  await Promise.all([
    loadTemplates(),
    loadModels()
  ])
})

// 加载模板列表
const loadTemplates = async () => {
  try {
    templates.value = await templateAPI.getAll()
  } catch (error) {
    ElMessage.error('加载模板列表失败')
    console.error(error)
  }
}

// 加载模型列表
const loadModels = async () => {
  try {
    const models = await modelConfigAPI.getAll()
    availableModels.value = models.map(m => ({
      code: m.modelCode,
      name: m.modelName,
      provider: m.provider
    }))
    
    if (availableModels.value.length > 0 && !config.value.model) {
      config.value.model = availableModels.value[0].code
    }
  } catch (error) {
    ElMessage.error('加载模型列表失败')
    console.error(error)
  }
}

// 模板变更
const handleTemplateChange = async () => {
  try {
    const template = await templateAPI.getByName(config.value.templateName)
    promptA.value = template.templateContent
    promptB.value = template.templateContent
    
    // 清空之前的结果
    resultA.value = { status: '', content: '', tokenUsage: null, duration: 0 }
    resultB.value = { status: '', content: '', tokenUsage: null, duration: 0 }
  } catch (error) {
    ElMessage.error('加载模板失败')
    console.error(error)
  }
}

// 加载模板变量示例
const loadTemplateVariables = async () => {
  if (!config.value.templateName) {
    ElMessage.warning('请先选择模板')
    return
  }
  
  try {
    const variables = await promptTestAPI.getTemplateVariables(config.value.templateName)
    config.value.variables = JSON.stringify(variables, null, 2)
    ElMessage.success('已加载模板示例变量')
  } catch (error) {
    ElMessage.error('加载变量示例失败')
    console.error(error)
  }
}

// 重置 B 版本
const resetPromptB = () => {
  promptB.value = promptA.value
  ElMessage.success('已重置为原始版本')
}

// 开始对比
const handleCompare = async () => {
  // 验证变量 JSON
  let variables
  try {
    variables = JSON.parse(config.value.variables)
  } catch (error) {
    ElMessage.error('变量 JSON 格式错误')
    return
  }

  comparing.value = true
  
  // 重置结果
  resultA.value = { status: 'loading', content: '', tokenUsage: null, duration: 0 }
  resultB.value = { status: 'loading', content: '', tokenUsage: null, duration: 0 }

  // 并行执行两个测试
  await Promise.all([
    executePrompt('A', promptA.value, variables),
    executePrompt('B', promptB.value, variables)
  ])

  comparing.value = false
}

// 执行 Prompt 测试
const executePrompt = async (version, promptContent, variables) => {
  const result = version === 'A' ? resultA : resultB
  const startTime = Date.now()

  try {
    let renderedPrompt = promptContent
    for (const [key, value] of Object.entries(variables)) {
      renderedPrompt = renderedPrompt.replaceAll(`{${key}}`, value)
    }

    const url = config.value.stream ? '/chat/stream' : '/chat'

    await fetchSSE(url, {
      conversationId: `comparison-${version}-${Date.now()}`,
      message: renderedPrompt,
      model: config.value.model
    }, {
      onContent: (content) => {
        result.value.content += content
      },
      onTokenUsage: (tokenUsage) => {
        result.value.tokenUsage = tokenUsage
      }
    })

    result.value.status = 'success'
    result.value.duration = Date.now() - startTime
  } catch (error) {
    console.error(`版本${version}执行失败:`, error)
    result.value.status = 'error'
    result.value.content = `执行失败: ${error.message}`
  }
}
</script>

<style scoped>
.prompt-comparison {
  max-width: 1800px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.prompt-section,
.result-section {
  border: 1px solid var(--app-border-base);
  border-radius: var(--app-radius-sm);
  padding: 16px;
  background: var(--app-bg-light);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.section-title {
  font-weight: 600;
  font-size: 14px;
  color: var(--app-text-primary);
}

.result-content {
  min-height: 200px;
  max-height: 600px;
  overflow-y: auto;
  padding: 16px;
  background: var(--app-bg-card);
  border-radius: var(--app-radius-sm);
  line-height: 1.6;
}

.empty-result {
  color: var(--app-text-secondary);
  text-align: center;
  padding: 60px 0;
}

.result-meta {
  margin-top: 12px;
  display: flex;
  gap: 8px;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .prompt-section,
  .result-section {
    margin-bottom: 20px;
  }

  :deep(.el-form-item__label) {
    width: 100% !important;
    text-align: left;
    margin-bottom: 8px;
  }

  :deep(.el-form-item__content) {
    margin-left: 0 !important;
  }

  .result-content {
    max-height: 400px;
  }
}
</style>

