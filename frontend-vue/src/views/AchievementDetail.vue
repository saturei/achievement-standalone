<template>
  <div class="achievement-detail">
    <el-page-header @back="goBack" title="返回">
      <template #content>
        <span class="text-large font-600 mr-3">{{ achievement?.name || '成果详情' }}</span>
        <el-tag :type="getStatusType(achievement?.status)" size="small">
          {{ getStatusText(achievement?.status) }}
        </el-tag>
      </template>
      <template #extra>
        <div class="header-actions">
          <el-button 
            v-if="achievement?.status === 'REGISTER'" 
            type="success" 
            @click="handleRecord"
          >
            登记
          </el-button>
          <el-button 
            v-if="achievement?.status !== 'PRE_REGISTER' && achievement?.status !== 'DELETED'" 
            type="warning" 
            @click="handleChange"
          >
            变更
          </el-button>
          <el-button 
            v-if="achievement?.status === 'PRE_REGISTER'" 
            type="danger" 
            @click="handleDelete"
          >
            删除
          </el-button>
        </div>
      </template>
    </el-page-header>

    <el-divider />

    <el-tabs v-model="activeTab" class="detail-tabs">
      <el-tab-pane label="基础信息" name="basic">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="成果名称">{{ formatValue(achievement?.name) }}</el-descriptions-item>
          <el-descriptions-item label="成果形态">{{ formatValue(achievement?.achievementForm) }}</el-descriptions-item>
          <el-descriptions-item label="所属机构">{{ formatValue(achievement?.organizationName) }}</el-descriptions-item>
          <el-descriptions-item label="部门">{{ formatValue(achievement?.departmentName) }}</el-descriptions-item>
          <el-descriptions-item label="成果版本">{{ formatValue(achievement?.version) }}</el-descriptions-item>
          <el-descriptions-item label="是否有基线">{{ formatValue(achievement?.hasBaseline) }}</el-descriptions-item>
          <el-descriptions-item label="成果需求提出人">{{ formatValue(achievement?.requirementProposer) }}</el-descriptions-item>
          <el-descriptions-item label="成果可售类型">{{ formatValue(achievement?.saleType) }}</el-descriptions-item>
          <el-descriptions-item label="关联产品">{{ formatValue(achievement?.productName) }}</el-descriptions-item>
          <el-descriptions-item label="关联项目">{{ formatValue(achievement?.relatedProjectName) }}</el-descriptions-item>
          <el-descriptions-item label="关联订单">{{ formatValue(achievement?.relatedOrderName) }}</el-descriptions-item>
          <el-descriptions-item label="关联订单编号">{{ formatValue(achievement?.relatedOrderId) }}</el-descriptions-item>
          <el-descriptions-item label="异常变更原因" :span="2">{{ formatValue(achievement?.changeReason) }}</el-descriptions-item>
          <el-descriptions-item label="创建人">{{ formatValue(achievement?.createdBy) }}</el-descriptions-item>
          <el-descriptions-item label="更新人">{{ formatValue(achievement?.updatedBy) }}</el-descriptions-item>
          <el-descriptions-item label="最后更新时间">{{ formatDateTime(achievement?.updatedAt) }}</el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>

      <el-tab-pane label="计划信息" name="plan">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="成果目标描述" :span="2">{{ formatValue(achievement?.achievementTarget) }}</el-descriptions-item>
          <el-descriptions-item label="成果对应功能清单" :span="2">
            <div v-if="achievement?.functionListFile" class="file-link">
              <el-link type="primary" @click="downloadFile(achievement.functionListFile)">
                <el-icon><Document /></el-icon>
                {{ getFileName(achievement.functionListFile) }}
              </el-link>
            </div>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="验收人（可多人）">{{ formatValue(achievement?.acceptor) }}</el-descriptions-item>
          <el-descriptions-item label="成果验收方式" :span="2">{{ formatValue(achievement?.acceptanceMethod) }}</el-descriptions-item>
          <el-descriptions-item label="计划验收日期">{{ formatValue(achievement?.plannedAcceptanceDate) }}</el-descriptions-item>
          <el-descriptions-item label="预估验收年月">{{ formatValue(achievement?.estimatedAcceptanceMonth) }}</el-descriptions-item>
          <el-descriptions-item label="验收组织">{{ formatValue(achievement?.acceptanceOrganization) }}</el-descriptions-item>
          <el-descriptions-item label="验收要求" :span="2">{{ formatValue(achievement?.acceptanceRequirements) }}</el-descriptions-item>
          <el-descriptions-item label="关联套餐">{{ formatValue(achievement?.packageIds) }}</el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>

      <el-tab-pane label="实际信息" name="actual">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="实际验收日期">{{ formatValue(achievement?.actualAcceptanceDate) }}</el-descriptions-item>
          <el-descriptions-item label="预注册时间">{{ formatDateTime(achievement?.preRegisterTime) }}</el-descriptions-item>
          <el-descriptions-item label="注册时间">{{ formatDateTime(achievement?.registerTime) }}</el-descriptions-item>
          <el-descriptions-item label="登记时间">{{ formatDateTime(achievement?.recordTime) }}</el-descriptions-item>
          <el-descriptions-item label="DEMO地址" :span="2">
            <a v-if="achievement?.demoUrl" :href="achievement.demoUrl" target="_blank" class="link">{{ achievement.demoUrl }}</a>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="成果验收提交物" :span="2">
            <div v-if="achievement?.deliverables" class="file-list">
              <div v-for="(file, index) in parseFiles(achievement.deliverables)" :key="index" class="file-link">
                <el-link type="primary" @click="downloadFile(file)">
                  <el-icon><Document /></el-icon>
                  {{ getFileName(file) }}
                </el-link>
              </div>
            </div>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="代码仓库/在线文档地址" :span="2">
            <a v-if="achievement?.codeRepositoryUrl" :href="achievement.codeRepositoryUrl" target="_blank" class="link">{{ achievement.codeRepositoryUrl }}</a>
            <span v-else>-</span>
          </el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>

      <el-tab-pane label="变更历史" name="history">
        <el-table :data="historyList" v-loading="historyLoading" border>
          <el-table-column prop="changeTime" label="变更时间" width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.changeTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="fromVersion" label="原版本" width="100" />
          <el-table-column prop="toVersion" label="新版本" width="100" />
          <el-table-column prop="changedFields" label="变更字段" width="200">
            <template #default="{ row }">
              {{ formatChangedFields(row.changedFields) }}
            </template>
          </el-table-column>
          <el-table-column prop="changeDescription" label="变更说明" />
          <el-table-column prop="changedBy" label="操作人" width="100" />
        </el-table>
        <el-empty v-if="!historyLoading && historyList.length === 0" description="暂无变更历史" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document } from '@element-plus/icons-vue'
import achievementApi from '../api/achievement'

const route = useRoute()
const router = useRouter()

const achievement = ref(null)
const historyList = ref([])
const historyLoading = ref(false)
const activeTab = ref('basic')

const statusMap = {
  'PRE_REGISTER': { text: '预注册', type: 'info' },
  'REGISTER': { text: '注册', type: 'warning' },
  'RECORDED': { text: '登记', type: 'success' },
  'OFFLINE': { text: '下架', type: 'danger' },
  'DELETED': { text: '已删除', type: 'info' },
  'pre_register': { text: '预注册', type: 'info' },
  'register': { text: '注册', type: 'warning' },
  'recorded': { text: '登记', type: 'success' },
  'offline': { text: '下架', type: 'danger' },
  'deleted': { text: '已删除', type: 'info' }
}

const getStatusText = (status) => {
  return statusMap[status]?.text || status
}

const getStatusType = (status) => {
  return statusMap[status]?.type || 'info'
}

const formatValue = (value) => {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  return value
}

const formatDateTime = (value) => {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN')
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
  'relatedOrderId': '关联订单',
  'moduleId': '模块'
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

const parseFiles = (filesStr) => {
  if (!filesStr) return []
  try {
    return JSON.parse(filesStr)
  } catch {
    return filesStr.split(',').filter(f => f.trim())
  }
}

const downloadFile = (filePath) => {
  if (filePath) {
    window.open(filePath, '_blank')
  }
}

const goBack = () => {
  router.push('/')
}

const loadDetail = async () => {
  try {
    const res = await achievementApi.getAchievement(route.params.id)
    achievement.value = res
  } catch (error) {
    ElMessage.error('加载详情失败')
    console.error(error)
  }
}

const loadHistory = async () => {
  historyLoading.value = true
  try {
    const res = await achievementApi.getHistory(route.params.id)
    historyList.value = res || []
  } catch (error) {
    console.error('加载变更历史失败', error)
  } finally {
    historyLoading.value = false
  }
}

const handleChange = () => {
  router.push(`/achievement/${route.params.id}/change`)
}

const handleRecord = () => {
  router.push(`/achievement/${route.params.id}/record`)
}

const handleDelete = async () => {
  try {
    await ElMessageBox.confirm('确定要删除该成果吗？删除后将无法恢复。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await achievementApi.delete(route.params.id)
    ElMessage.success('删除成功')
    router.push('/')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadDetail()
  loadHistory()
})
</script>

<style scoped>
.achievement-detail {
  padding: 20px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.detail-tabs {
  margin-top: 20px;
}

.el-descriptions {
  margin-bottom: 20px;
}

.el-descriptions :deep(.el-descriptions__label) {
  width: 120px;
  font-weight: 500;
}

.el-descriptions :deep(.el-descriptions__content) {
  word-break: break-all;
}

.link {
  color: #409eff;
  text-decoration: none;
}

.link:hover {
  text-decoration: underline;
}

.file-link {
  margin-bottom: 5px;
}

.file-list {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
</style>
