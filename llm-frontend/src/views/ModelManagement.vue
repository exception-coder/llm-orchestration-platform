<template>
  <div class="model-management">
    <el-row :gutter="20">
      <!-- 左侧：模型列表 -->
      <el-col :xs="24" :sm="24" :md="10" :lg="10">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>模型列表</span>
              <el-button type="primary" size="small" @click="handleCreate">
                <el-icon><Plus /></el-icon>
                <span class="btn-text">新建模型</span>
              </el-button>
            </div>
          </template>

          <el-tabs v-model="activeProvider" @tab-change="handleProviderChange">
            <el-tab-pane label="全部" name="all" />
            <el-tab-pane
              v-for="provider in providers"
              :key="provider"
              :label="getProviderLabel(provider)"
              :name="provider"
            />
          </el-tabs>

          <el-table
            :data="filteredModels"
            highlight-current-row
            @current-change="handleSelectModel"
            style="width: 100%"
            max-height="550"
          >
            <el-table-column prop="modelName" label="模型名称" min-width="150" />
            <el-table-column prop="provider" label="提供商" width="100" class-name="hidden-xs-only">
              <template #default="{ row }">
                <el-tag :type="getProviderTagType(row.provider)" size="small">
                  {{ row.provider }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="enabled" label="状态" width="80" class-name="hidden-xs-only">
              <template #default="{ row }">
                <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
                  {{ row.enabled ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button
                  type="danger"
                  size="small"
                  link
                  @click="handleDelete(row)"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 右侧：模型详情/编辑 -->
      <el-col :xs="24" :sm="24" :md="14" :lg="14">
        <el-card class="detail-card">
          <template #header>
            <div class="card-header">
              <span>{{ isEditing ? '编辑模型' : '模型详情' }}</span>
              <div class="action-buttons">
                <el-button
                  v-if="!isEditing && selectedModel"
                  type="primary"
                  size="small"
                  @click="handleEdit"
                >
                  <el-icon><Edit /></el-icon>
                  <span class="btn-text">编辑</span>
                </el-button>
                <el-button
                  v-if="isEditing"
                  type="success"
                  size="small"
                  @click="handleSave"
                  :loading="saving"
                >
                  <el-icon><Check /></el-icon>
                  <span class="btn-text">保存</span>
                </el-button>
                <el-button
                  v-if="isEditing"
                  size="small"
                  @click="handleCancel"
                >
                  <el-icon><Close /></el-icon>
                  <span class="btn-text">取消</span>
                </el-button>
              </div>
            </div>
          </template>

          <div v-if="!selectedModel && !isCreating" class="empty-state">
            <el-empty description="请选择或创建一个模型" />
          </div>

          <el-form
            v-else
            :model="editForm"
            :label-width="isMobile ? '100%' : '120px'"
            :label-position="isMobile ? 'top' : 'right'"
            :disabled="!isEditing"
          >
            <el-form-item label="模型代码">
              <el-input
                v-model="editForm.modelCode"
                placeholder="例如：gpt-4, deepseek-chat"
                :disabled="!isCreating"
              />
              <div class="help-text">
                <el-icon><InfoFilled /></el-icon>
                模型的唯一标识符，创建后不可修改
              </div>
            </el-form-item>

            <el-form-item label="提供商">
              <el-select
                v-model="editForm.provider"
                placeholder="请选择或输入提供商"
                style="width: 100%"
                filterable
                allow-create
                default-first-option
              >
                <el-option
                  v-for="provider in providers"
                  :key="provider"
                  :label="getProviderLabel(provider)"
                  :value="provider"
                />
              </el-select>
              <div class="help-text">
                <el-icon><InfoFilled /></el-icon>
                可以从现有提供商中选择，也可以直接输入新的提供商名称
              </div>
            </el-form-item>

            <el-form-item label="模型名称">
              <el-input
                v-model="editForm.modelName"
                placeholder="例如：GPT-4, DeepSeek Chat"
              />
              <div class="help-text">
                <el-icon><InfoFilled /></el-icon>
                用于前端显示的友好名称
              </div>
            </el-form-item>

            <el-form-item label="描述">
              <el-input
                v-model="editForm.description"
                type="textarea"
                :rows="3"
                placeholder="模型的简要描述，例如：最强大的模型，适合复杂任务"
              />
            </el-form-item>

            <el-form-item label="排序顺序">
              <el-input-number
                v-model="editForm.sortOrder"
                :min="0"
                :max="1000"
                placeholder="数字越小越靠前"
              />
              <div class="help-text">
                <el-icon><InfoFilled /></el-icon>
                控制模型在列表中的显示顺序，数字越小越靠前
              </div>
            </el-form-item>

            <el-form-item label="启用状态">
              <el-switch
                v-model="editForm.enabled"
                active-text="启用"
                inactive-text="禁用"
              />
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { modelConfigAPI } from '@/api'

const isMobile = ref(false)

const models = ref([])
const selectedModel = ref(null)
const activeProvider = ref('all')
const isEditing = ref(false)
const isCreating = ref(false)
const saving = ref(false)

const editForm = ref({
  modelCode: '',
  provider: 'openai',
  modelName: '',
  description: '',
  enabled: true,
  sortOrder: 0
})

// 动态提取提供商列表
const providers = computed(() => {
  const providerSet = new Set()
  models.value.forEach(model => {
    if (model.provider) {
      providerSet.add(model.provider)
    }
  })
  return Array.from(providerSet).sort()
})

// 过滤后的模型列表
const filteredModels = computed(() => {
  if (activeProvider.value === 'all') return models.value
  return models.value.filter(m => m.provider === activeProvider.value)
})

// 获取提供商显示名称
const getProviderLabel = (provider) => {
  const labels = {
    openai: 'OpenAI',
    deepseek: 'DeepSeek',
    ollama: 'Ollama'
  }
  return labels[provider] || provider.charAt(0).toUpperCase() + provider.slice(1)
}

// 获取提供商标签类型
const getProviderTagType = (provider) => {
  const types = {
    openai: 'success',
    deepseek: 'warning',
    ollama: 'info'
  }
  return types[provider] || ''
}

// 检测是否为移动端
const checkMobile = () => {
  isMobile.value = window.innerWidth <= 768
}

onMounted(async () => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
  await loadModels()
})

// 加载模型列表
const loadModels = async () => {
  try {
    models.value = await modelConfigAPI.getAll()
  } catch (error) {
    ElMessage.error('加载模型列表失败')
    console.error('加载模型失败:', error)
  }
}

// 切换提供商
const handleProviderChange = () => {
  // 检查当前选中的模型是否在过滤后的列表中
  if (selectedModel.value) {
    const isInFilteredList = filteredModels.value.some(
      m => m.modelCode === selectedModel.value.modelCode
    )
    
    // 如果不在过滤后的列表中，清空选中状态
    if (!isInFilteredList) {
      selectedModel.value = null
      isEditing.value = false
      isCreating.value = false
    }
    // 如果在列表中，保持选中状态，不做任何操作
  }
}

// 选择模型
const handleSelectModel = (row) => {
  if (!row) return
  selectedModel.value = row
  isEditing.value = false
  isCreating.value = false
  editForm.value = { ...row }
}

// 创建模型
const handleCreate = () => {
  selectedModel.value = null
  isEditing.value = true
  isCreating.value = true
  
  // 如果当前选择了某个提供商，默认使用该提供商
  const defaultProvider = activeProvider.value !== 'all' 
    ? activeProvider.value 
    : (providers.value.length > 0 ? providers.value[0] : 'openai')
  
  editForm.value = {
    modelCode: '',
    provider: defaultProvider,
    modelName: '',
    description: '',
    enabled: true,
    sortOrder: 0
  }
}

// 编辑模型
const handleEdit = () => {
  isEditing.value = true
}

// 保存模型
const handleSave = async () => {
  if (!editForm.value.modelCode) {
    ElMessage.warning('请输入模型代码')
    return
  }
  if (!editForm.value.modelName) {
    ElMessage.warning('请输入模型名称')
    return
  }
  if (!editForm.value.provider) {
    ElMessage.warning('请选择提供商')
    return
  }

  saving.value = true
  try {
    await modelConfigAPI.save(editForm.value)
    ElMessage.success('保存成功')
    await loadModels()
    isEditing.value = false
    isCreating.value = false
    
    // 重新选中当前模型
    const saved = models.value.find(m => m.modelCode === editForm.value.modelCode)
    if (saved) {
      selectedModel.value = saved
    }
  } catch (error) {
    ElMessage.error('保存失败')
    console.error('保存失败:', error)
  } finally {
    saving.value = false
  }
}

// 取消编辑
const handleCancel = () => {
  if (isCreating.value) {
    selectedModel.value = null
    isCreating.value = false
  } else if (selectedModel.value) {
    editForm.value = { ...selectedModel.value }
  }
  isEditing.value = false
}

// 删除模型
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除模型 "${row.modelName}" 吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await modelConfigAPI.delete(row.modelCode)
    ElMessage.success('删除成功')
    await loadModels()
    
    if (selectedModel.value?.modelCode === row.modelCode) {
      selectedModel.value = null
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
      console.error('删除失败:', error)
    }
  }
}
</script>

<style scoped>
.model-management {
  max-width: 1600px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.empty-state {
  padding: 60px 0;
}

.help-text {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .detail-card {
    margin-top: 20px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .action-buttons {
    display: flex;
    gap: 8px;
    width: 100%;
  }

  .action-buttons .el-button {
    flex: 1;
  }

  .btn-text {
    display: none;
  }

  :deep(.el-form-item__label) {
    width: 100% !important;
    text-align: left;
    margin-bottom: 8px;
  }

  :deep(.el-form-item__content) {
    margin-left: 0 !important;
  }

  :deep(.hidden-xs-only) {
    display: none;
  }
}
</style>

