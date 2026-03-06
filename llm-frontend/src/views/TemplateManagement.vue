<template>
  <div class="template-management">
    <el-row :gutter="20">
      <!-- 左侧：模板列表 -->
      <el-col :xs="24" :sm="24" :md="10" :lg="10">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>模板列表</span>
              <el-button type="primary" size="small" @click="handleCreate">
                <el-icon><Plus /></el-icon>
                <span class="btn-text">新建模板</span>
              </el-button>
            </div>
          </template>

          <el-input
            v-model="searchText"
            placeholder="搜索模板名称"
            prefix-icon="Search"
            style="margin-bottom: 16px"
          />

          <el-table
            :data="filteredTemplates"
            highlight-current-row
            @current-change="handleSelectTemplate"
            style="width: 100%"
            max-height="600"
          >
            <el-table-column prop="templateName" label="模板名称" min-width="180" />
            <el-table-column prop="category" label="分类" width="100" class-name="hidden-xs-only">
              <template #default="{ row }">
                <el-tag size="small">{{ row.category }}</el-tag>
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

      <!-- 右侧：模板详情/编辑 -->
      <el-col :xs="24" :sm="24" :md="14" :lg="14">
        <el-card class="detail-card">
          <template #header>
            <div class="card-header">
              <span>{{ isEditing ? '编辑模板' : '模板详情' }}</span>
              <div class="action-buttons">
                <el-button
                  v-if="!isEditing && selectedTemplate"
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

          <div v-if="!selectedTemplate && !isCreating" class="empty-state">
            <el-empty description="请选择或创建一个模板" />
          </div>

          <el-form
            v-else
            :model="editForm"
            :label-width="isMobile ? '100%' : '120px'"
            :label-position="isMobile ? 'top' : 'right'"
            :disabled="!isEditing"
          >
            <el-form-item label="模板名称">
              <el-input
                v-model="editForm.templateName"
                placeholder="例如：content-optimization"
                :disabled="!isCreating"
              />
            </el-form-item>

            <el-form-item label="分类">
              <el-select v-model="editForm.category" placeholder="请选择分类" style="width: 100%">
                <el-option label="content - 内容创作" value="content" />
                <el-option label="chat - 对话交互" value="chat" />
                <el-option label="development - 开发辅助" value="development" />
                <el-option label="analysis - 数据分析" value="analysis" />
                <el-option label="translation - 翻译转换" value="translation" />
              </el-select>
            </el-form-item>

            <el-form-item label="描述">
              <el-input
                v-model="editForm.description"
                type="textarea"
                :rows="2"
                placeholder="模板的简要描述"
              />
            </el-form-item>

            <el-form-item label="模板内容">
              <el-input
                v-model="editForm.templateContent"
                type="textarea"
                :rows="12"
                placeholder="使用 {variableName} 作为变量占位符"
                class="template-content"
              />
              <div class="help-text">
                <el-icon><InfoFilled /></el-icon>
                提示：使用 {variableName} 格式定义变量，例如 {userQuestion}、{platformCharacteristics}
              </div>
            </el-form-item>

            <el-form-item label="变量示例">
              <el-input
                v-model="editForm.variableExamples"
                type="textarea"
                :rows="8"
                placeholder='JSON 格式的变量示例，例如：{"name": "张三", "age": "25"}'
                class="template-content"
              />
              <div class="help-text">
                <el-icon><InfoFilled /></el-icon>
                提示：用于 Prompt 测试时的默认变量值，必须是有效的 JSON 格式
              </div>
              <el-button
                v-if="isEditing"
                size="small"
                type="primary"
                link
                @click="formatJSON"
                style="margin-top: 8px"
              >
                <el-icon><MagicStick /></el-icon>
                格式化 JSON
              </el-button>
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
import { templateAPI } from '@/api'

const isMobile = ref(false)

const templates = ref([])
const selectedTemplate = ref(null)
const searchText = ref('')
const isEditing = ref(false)
const isCreating = ref(false)
const saving = ref(false)

const editForm = ref({
  templateName: '',
  category: '',
  description: '',
  templateContent: '',
  variableExamples: ''
})

// 过滤后的模板列表
const filteredTemplates = computed(() => {
  if (!searchText.value) return templates.value
  return templates.value.filter(t =>
    t.templateName.toLowerCase().includes(searchText.value.toLowerCase())
  )
})

// 检测是否为移动端
const checkMobile = () => {
  isMobile.value = window.innerWidth <= 768
}

onMounted(async () => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
  await loadTemplates()
})

// 加载模板列表
const loadTemplates = async () => {
  try {
    templates.value = await templateAPI.getAll()
  } catch (error) {
    console.error('加载模板失败:', error)
  }
}

// 选择模板
const handleSelectTemplate = (row) => {
  if (!row) return
  selectedTemplate.value = row
  isEditing.value = false
  isCreating.value = false
  editForm.value = { ...row }
}

// 创建模板
const handleCreate = () => {
  selectedTemplate.value = null
  isEditing.value = true
  isCreating.value = true
  editForm.value = {
    templateName: '',
    category: 'content',
    description: '',
    templateContent: '',
    variableExamples: ''
  }
}

// 编辑模板
const handleEdit = () => {
  isEditing.value = true
}

// 保存模板
const handleSave = async () => {
  if (!editForm.value.templateName) {
    ElMessage.warning('请输入模板名称')
    return
  }
  if (!editForm.value.templateContent) {
    ElMessage.warning('请输入模板内容')
    return
  }

  // 验证变量示例是否为有效 JSON
  if (editForm.value.variableExamples) {
    try {
      JSON.parse(editForm.value.variableExamples)
    } catch (error) {
      ElMessage.warning('变量示例必须是有效的 JSON 格式')
      return
    }
  }

  saving.value = true
  try {
    await templateAPI.save(editForm.value)
    ElMessage.success('保存成功')
    await loadTemplates()
    isEditing.value = false
    isCreating.value = false
    
    // 重新选中当前模板
    const saved = templates.value.find(t => t.templateName === editForm.value.templateName)
    if (saved) {
      selectedTemplate.value = saved
    }
  } catch (error) {
    ElMessage.error('保存失败')
    console.error('保存失败:', error)
  } finally {
    saving.value = false
  }
}

// 格式化 JSON
const formatJSON = () => {
  if (!editForm.value.variableExamples) {
    ElMessage.warning('请先输入 JSON 内容')
    return
  }
  
  try {
    const parsed = JSON.parse(editForm.value.variableExamples)
    editForm.value.variableExamples = JSON.stringify(parsed, null, 2)
    ElMessage.success('格式化成功')
  } catch (error) {
    ElMessage.error('JSON 格式错误，无法格式化')
  }
}

// 取消编辑
const handleCancel = () => {
  if (isCreating.value) {
    selectedTemplate.value = null
    isCreating.value = false
  } else if (selectedTemplate.value) {
    editForm.value = { ...selectedTemplate.value }
  }
  isEditing.value = false
}

// 删除模板
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除模板 "${row.templateName}" 吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await templateAPI.delete(row.templateName)
    ElMessage.success('删除成功')
    await loadTemplates()
    
    if (selectedTemplate.value?.templateName === row.templateName) {
      selectedTemplate.value = null
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}
</script>

<style scoped>
.template-management {
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

.template-content {
  font-family: 'Courier New', monospace;
  font-size: 14px;
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

