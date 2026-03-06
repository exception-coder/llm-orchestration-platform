<template>
  <div class="prompt-test">
    <el-row :gutter="20">
      <!-- 左侧：测试配置 -->
      <el-col :xs="24" :sm="24" :md="10" :lg="10">
        <el-card class="form-card">
          <template #header>
            <div class="card-header">
              <span>测试配置</span>
            </div>
          </template>

          <el-form :model="form" :label-width="isMobile ? '100%' : '120px'" :label-position="isMobile ? 'top' : 'right'">
            <!-- 模板选择 -->
            <el-form-item label="选择模板">
              <el-select 
                v-model="form.templateName" 
                placeholder="请选择模板"
                @change="handleTemplateChange"
                style="width: 100%"
              >
                <el-option
                  v-for="template in templates"
                  :key="template.templateName"
                  :label="template.templateName"
                  :value="template.templateName"
                >
                  <span>{{ template.templateName }}</span>
                  <span style="float: right; color: #8492a6; font-size: 13px" class="hidden-xs-only">
                    {{ template.category }}
                  </span>
                </el-option>
              </el-select>
            </el-form-item>

            <!-- 模型选择 -->
            <el-form-item label="选择模型">
              <el-select v-model="form.model" placeholder="请选择模型" style="width: 100%">
                <el-option-group
                  v-for="group in groupedModels"
                  :key="group.provider"
                  :label="group.provider"
                >
                  <el-option
                    v-for="model in group.models"
                    :key="model.code"
                    :label="model.name"
                    :value="model.code"
                  >
                    <span>{{ model.name }}</span>
                    <span style="float: right; color: #8492a6; font-size: 13px" class="hidden-xs-only">
                      {{ model.description }}
                    </span>
                  </el-option>
                </el-option-group>
              </el-select>
            </el-form-item>

            <!-- 模板变量 -->
            <el-form-item label="模板变量">
              <el-card shadow="never" style="width: 100%">
                <div v-for="(value, key) in form.variables" :key="key" class="variable-item">
                  <el-form-item :label="key" label-width="150px">
                    <el-input
                      v-if="typeof value === 'string' && value.length < 100"
                      v-model="form.variables[key]"
                      placeholder="请输入变量值"
                    />
                    <el-input
                      v-else
                      v-model="form.variables[key]"
                      type="textarea"
                      :rows="3"
                      placeholder="请输入变量值"
                    />
                  </el-form-item>
                </div>
              </el-card>
            </el-form-item>

            <!-- 高级参数 -->
            <el-form-item label="高级参数">
              <el-row :gutter="20">
                <el-col :xs="24" :sm="12">
                  <el-form-item label="Temperature" label-width="100px">
                    <el-slider
                      v-model="form.temperature"
                      :min="0"
                      :max="1"
                      :step="0.1"
                      show-input
                      :input-size="'small'"
                    />
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :sm="12">
                  <el-form-item label="Max Tokens" label-width="100px">
                    <el-input-number
                      v-model="form.maxTokens"
                      :min="100"
                      :max="4000"
                      :step="100"
                      style="width: 100%"
                    />
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form-item>

            <!-- 操作按钮 -->
            <el-form-item>
              <el-button type="primary" @click="handleTest" :loading="loading" size="large" style="width: 100%">
                <el-icon><Operation /></el-icon>
                开始测试
              </el-button>
              <el-button @click="handleReset" size="large" style="width: 100%; margin-top: 10px; margin-left: 0">
                <el-icon><RefreshLeft /></el-icon>
                重置
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 右侧：测试结果 -->
      <el-col :xs="24" :sm="24" :md="14" :lg="14">
        <el-card v-if="result" class="result-card">
          <template #header>
            <div class="card-header">
              <span>测试结果</span>
              <el-tag type="success">
                耗时: {{ result.executionTime }}ms
              </el-tag>
            </div>
          </template>

      <el-tabs v-model="activeTab">
        <!-- 渲染后的 Prompt -->
        <el-tab-pane label="渲染后的 Prompt" name="prompt">
          <el-input
            v-model="result.renderedPrompt"
            type="textarea"
            :rows="15"
            readonly
            class="result-textarea"
          />
          <div class="copy-btn-wrapper">
            <el-button @click="copyToClipboard(result.renderedPrompt)" size="small">
              <el-icon><CopyDocument /></el-icon>
              复制
            </el-button>
          </div>
        </el-tab-pane>

        <!-- LLM 输出 -->
        <el-tab-pane label="LLM 输出" name="output">
          <el-input
            v-model="result.output"
            type="textarea"
            :rows="15"
            readonly
            class="result-textarea"
          />
          <div class="copy-btn-wrapper">
            <el-button @click="copyToClipboard(result.output)" size="small">
              <el-icon><CopyDocument /></el-icon>
              复制
            </el-button>
          </div>
        </el-tab-pane>

        <!-- 性能指标 -->
        <el-tab-pane label="性能指标" name="metrics">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="使用模型">
              {{ result.model }}
            </el-descriptions-item>
            <el-descriptions-item label="执行耗时">
              {{ result.executionTime }}ms
            </el-descriptions-item>
            <el-descriptions-item label="Prompt Tokens">
              {{ result.tokenUsage?.promptTokens || 0 }}
            </el-descriptions-item>
            <el-descriptions-item label="Completion Tokens">
              {{ result.tokenUsage?.completionTokens || 0 }}
            </el-descriptions-item>
            <el-descriptions-item label="总 Tokens">
              {{ result.tokenUsage?.totalTokens || 0 }}
            </el-descriptions-item>
            <el-descriptions-item label="提供商">
              {{ result.provider || 'auto' }}
            </el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
      </el-tabs>
    </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { promptTestAPI, templateAPI } from '@/api'

const isMobile = ref(false)

const form = ref({
  templateName: '',
  model: 'gpt-3.5-turbo',
  variables: {},
  temperature: 0.7,
  maxTokens: 2000
})

const templates = ref([])
const models = ref([])
const result = ref(null)
const loading = ref(false)
const activeTab = ref('prompt')

// 按提供商分组的模型
const groupedModels = computed(() => {
  const groups = {}
  models.value.forEach(model => {
    if (!groups[model.provider]) {
      groups[model.provider] = {
        provider: model.provider,
        models: []
      }
    }
    groups[model.provider].models.push(model)
  })
  return Object.values(groups)
})

// 检测是否为移动端
const checkMobile = () => {
  isMobile.value = window.innerWidth <= 768
}

onMounted(async () => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
  await loadTemplates()
  await loadModels()
})

// 加载模板列表
const loadTemplates = async () => {
  try {
    templates.value = await templateAPI.getAll()
    if (templates.value.length > 0) {
      form.value.templateName = templates.value[0].templateName
      await handleTemplateChange()
    }
  } catch (error) {
    console.error('加载模板失败:', error)
  }
}

// 加载模型列表
const loadModels = async () => {
  try {
    models.value = await promptTestAPI.getModels()
  } catch (error) {
    console.error('加载模型失败:', error)
  }
}

// 模板变更处理
const handleTemplateChange = async () => {
  try {
    const variables = await promptTestAPI.getTemplateVariables(form.value.templateName)
    form.value.variables = variables
  } catch (error) {
    console.error('加载模板变量失败:', error)
  }
}

// 执行测试
const handleTest = async () => {
  loading.value = true
  result.value = null
  
  try {
    result.value = await promptTestAPI.test(form.value)
    ElMessage.success('测试完成')
    activeTab.value = 'output'
  } catch (error) {
    console.error('测试失败:', error)
  } finally {
    loading.value = false
  }
}

// 重置表单
const handleReset = () => {
  form.value = {
    templateName: templates.value[0]?.templateName || '',
    model: 'gpt-3.5-turbo',
    variables: {},
    temperature: 0.7,
    maxTokens: 2000
  }
  result.value = null
  handleTemplateChange()
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
.prompt-test {
  max-width: 1400px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.variable-item {
  margin-bottom: 0;
}

.result-textarea {
  font-family: 'Courier New', monospace;
  font-size: 14px;
}

.copy-btn-wrapper {
  margin-top: 10px;
  text-align: right;
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

  .variable-item :deep(.el-form-item__label) {
    width: 100% !important;
  }
}
</style>

