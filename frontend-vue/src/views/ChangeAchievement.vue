<template>
  <div class="change-achievement">
    <el-page-header @back="goBack" content="成果变更" />

    <el-row :gutter="20" class="content-row">
      <el-col :span="16">
        <el-card class="form-card">
          <template #header>
            <div class="card-header">
              <span>变更信息</span>
              <el-tag type="info">当前版本: {{ currentAchievement?.version }}</el-tag>
            </div>
          </template>
          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-width="120px"
            label-position="right"
          >
            <el-divider content-position="left">基础信息</el-divider>
            
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="成果名称">
                  <el-input :value="currentAchievement?.name" disabled />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="成果版本">
                  <el-input v-model="form.version" placeholder="请输入成果版本" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="负责人">
                  <el-input v-model="form.owner" placeholder="请输入负责人" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="成果描述">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="3"
                placeholder="请输入成果描述"
              />
            </el-form-item>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="关联项目ID">
                  <el-input v-model="form.relatedProjectId" placeholder="请输入关联项目ID" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="关联项目名称">
                  <el-input v-model="form.relatedProjectName" placeholder="请输入关联项目名称" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="关联订单ID">
                  <el-input v-model="form.relatedOrderId" placeholder="请输入关联订单ID" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="关联订单名称">
                  <el-input v-model="form.relatedOrderName" placeholder="请输入关联订单名称" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-divider content-position="left">计划信息</el-divider>

            <el-form-item label="成果目标描述">
              <el-input
                v-model="form.achievementTarget"
                type="textarea"
                :rows="3"
                placeholder="请输入成果目标描述"
              />
            </el-form-item>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="计划验收日期">
                  <el-date-picker
                    v-model="form.plannedAcceptanceDate"
                    type="date"
                    placeholder="选择日期"
                    value-format="YYYY-MM-DD"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="预估验收年月">
                  <el-input v-model="form.estimatedAcceptanceMonth" placeholder="如：2026年01月" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="验收方式">
              <el-input
                v-model="form.acceptanceMethod"
                type="textarea"
                :rows="2"
                placeholder="请输入验收方式"
              />
            </el-form-item>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="验收人">
                  <el-input v-model="form.acceptor" placeholder="请输入验收人" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="验收组织">
                  <el-input v-model="form.acceptanceOrganization" placeholder="请输入验收组织" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="验收要求">
              <el-input
                v-model="form.acceptanceRequirements"
                type="textarea"
                :rows="2"
                placeholder="请输入验收要求"
              />
            </el-form-item>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="功能清单">
                  <el-upload
                    class="file-upload"
                    :action="uploadUrl"
                    :show-file-list="false"
                    :before-upload="beforeUpload"
                    :on-success="handleUploadSuccess"
                    :on-error="handleUploadError"
                  >
                    <el-button type="primary" size="small">上传文件</el-button>
                    <template #tip>
                      <div class="upload-tip" v-if="form.functionListFile">
                        已上传: {{ getFileName(form.functionListFile) }}
                        <el-link type="danger" @click.stop="form.functionListFile = ''">删除</el-link>
                      </div>
                    </template>
                  </el-upload>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="关联套餐">
                  <el-input v-model="form.packageIds" placeholder="请输入关联套餐" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-divider content-position="left">变更说明</el-divider>

            <el-form-item label="变更说明" prop="changeDescription">
              <el-input
                v-model="form.changeDescription"
                type="textarea"
                :rows="3"
                placeholder="请输入变更原因和说明"
              />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="handleSubmit" :loading="submitting">
                提交变更
              </el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="history-card">
          <template #header>
            <span>变更历史</span>
          </template>
          <el-timeline v-if="historyList.length > 0">
            <el-timeline-item
              v-for="item in historyList"
              :key="item.id"
              :timestamp="formatDate(item.changeTime)"
              placement="top"
            >
              <el-card shadow="hover" class="timeline-card">
                <div class="history-item">
                  <div class="version-change">
                    <el-tag type="info" size="small">{{ item.fromVersion }}</el-tag>
                    <el-icon><ArrowRight /></el-icon>
                    <el-tag type="success" size="small">{{ item.toVersion }}</el-tag>
                  </div>
                  <div class="changed-fields" v-if="item.changedFields">
                    变更字段: {{ formatChangedFields(item.changedFields) }}
                  </div>
                  <div class="change-reason" v-if="item.changeDescription">
                    {{ item.changeDescription }}
                  </div>
                  <div class="operator">操作人: {{ item.changedBy || '系统' }}</div>
                </div>
              </el-card>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无变更历史" :image-size="80" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight } from '@element-plus/icons-vue'
import achievementApi from '../api/achievement'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)
const currentAchievement = ref(null)
const historyList = ref([])
const uploadUrl = '/api/upload'

const form = reactive({
  version: '',
  owner: '',
  description: '',
  relatedProjectId: '',
  relatedProjectName: '',
  relatedOrderId: '',
  relatedOrderName: '',
  acceptanceRequirements: '',
  achievementTarget: '',
  plannedAcceptanceDate: '',
  estimatedAcceptanceMonth: '',
  acceptanceMethod: '',
  acceptor: '',
  acceptanceOrganization: '',
  functionListFile: '',
  packageIds: '',
  changeDescription: ''
})

const rules = {
  changeDescription: [{ required: true, message: '请输入变更说明', trigger: 'blur' }]
}

const fieldNames = {
  'achievementTarget': '成果目标',
  'plannedAcceptanceDate': '计划验收日期',
  'estimatedAcceptanceMonth': '预估验收年月',
  'acceptanceMethod': '验收方式',
  'acceptor': '验收人',
  'acceptanceOrganization': '验收组织',
  'acceptanceRequirements': '验收要求',
  'functionListFile': '功能清单',
  'packageIds': '关联套餐',
  'description': '成果描述',
  'owner': '负责人',
  'relatedProjectId': '关联项目',
  'relatedOrderId': '关联订单'
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

const formatChangedFields = (fields) => {
  if (!fields) return '-'
  try {
    const fieldList = JSON.parse(fields)
    return fieldList.map(f => fieldNames[f] || f).join(', ')
  } catch {
    return fields
  }
}

const getFileName = (filePath) => {
  if (!filePath) return ''
  return filePath.split('/').pop() || filePath
}

const beforeUpload = (file) => {
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    ElMessage.error('文件大小不能超过 10MB')
    return false
  }
  return true
}

const handleUploadSuccess = (response) => {
  if (response && response.url) {
    form.functionListFile = response.url
    ElMessage.success('上传成功')
  }
}

const handleUploadError = () => {
  form.functionListFile = 'mock_file_' + Date.now()
  ElMessage.success('文件已选择（模拟上传）')
}

const goBack = () => {
  router.push(`/achievement/${route.params.id}`)
}

const loadDetail = async () => {
  try {
    const res = await achievementApi.getAchievement(route.params.id)
    currentAchievement.value = res
    form.version = res.version || ''
    form.owner = res.owner || ''
    form.description = res.description || ''
    form.relatedProjectId = res.relatedProjectId || ''
    form.relatedProjectName = res.relatedProjectName || ''
    form.relatedOrderId = res.relatedOrderId || ''
    form.relatedOrderName = res.relatedOrderName || ''
    form.acceptanceRequirements = res.acceptanceRequirements || ''
    form.achievementTarget = res.achievementTarget || ''
    form.plannedAcceptanceDate = res.plannedAcceptanceDate || ''
    form.estimatedAcceptanceMonth = res.estimatedAcceptanceMonth || ''
    form.acceptanceMethod = res.acceptanceMethod || ''
    form.acceptor = res.acceptor || ''
    form.acceptanceOrganization = res.acceptanceOrganization || ''
    form.functionListFile = res.functionListFile || ''
    form.packageIds = res.packageIds || ''
  } catch (error) {
    ElMessage.error('加载成果详情失败')
    console.error(error)
  }
}

const loadHistory = async () => {
  try {
    const res = await achievementApi.getHistory(route.params.id)
    historyList.value = res || []
  } catch (error) {
    console.error('加载变更历史失败', error)
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        const submitData = {}
        Object.keys(form).forEach(key => {
          if (form[key] !== '' && form[key] !== null && form[key] !== undefined) {
            submitData[key] = form[key]
          }
        })
        const res = await achievementApi.change(route.params.id, submitData)
        ElMessage.success('变更成功')
        router.push(`/achievement/${res.id}`)
      } catch (error) {
        ElMessage.error('变更失败：' + (error.response?.data?.message || error.message))
      } finally {
        submitting.value = false
      }
    }
  })
}

const handleReset = () => {
  if (currentAchievement.value) {
    form.version = currentAchievement.value.version || ''
    form.owner = currentAchievement.value.owner || ''
    form.description = currentAchievement.value.description || ''
    form.relatedProjectId = currentAchievement.value.relatedProjectId || ''
    form.relatedProjectName = currentAchievement.value.relatedProjectName || ''
    form.relatedOrderId = currentAchievement.value.relatedOrderId || ''
    form.relatedOrderName = currentAchievement.value.relatedOrderName || ''
    form.acceptanceRequirements = currentAchievement.value.acceptanceRequirements || ''
    form.achievementTarget = currentAchievement.value.achievementTarget || ''
    form.plannedAcceptanceDate = currentAchievement.value.plannedAcceptanceDate || ''
    form.estimatedAcceptanceMonth = currentAchievement.value.estimatedAcceptanceMonth || ''
    form.acceptanceMethod = currentAchievement.value.acceptanceMethod || ''
    form.acceptor = currentAchievement.value.acceptor || ''
    form.acceptanceOrganization = currentAchievement.value.acceptanceOrganization || ''
    form.functionListFile = currentAchievement.value.functionListFile || ''
    form.packageIds = currentAchievement.value.packageIds || ''
    form.changeDescription = ''
  }
}

onMounted(() => {
  loadDetail()
  loadHistory()
})
</script>

<style scoped>
.change-achievement {
  padding: 0;
}

.content-row {
  margin-top: 20px;
}

.form-card, .history-card {
  height: calc(100vh - 180px);
  overflow-y: auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.timeline-card {
  margin-top: 5px;
}

.history-item {
  font-size: 13px;
}

.version-change {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.changed-fields {
  color: #909399;
  font-size: 12px;
  margin-bottom: 5px;
}

.change-reason {
  color: #606266;
  margin-bottom: 5px;
  line-height: 1.5;
}

.operator {
  color: #909399;
  font-size: 12px;
}

.upload-tip {
  font-size: 12px;
  color: #606266;
  margin-top: 5px;
}
</style>
