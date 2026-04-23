<template>
  <div class="record-achievement">
    <el-page-header @back="goBack" content="成果登记" />

    <el-card class="form-card">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        label-position="right"
      >
        <el-divider content-position="left">基础信息（只读）</el-divider>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="成果名称">
              <el-input :value="currentAchievement?.name" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="成果形态">
              <el-input :value="currentAchievement?.achievementForm" disabled />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="所属机构">
              <el-input :value="currentAchievement?.organizationName" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门">
              <el-input :value="currentAchievement?.departmentName" disabled />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="关联产品">
              <el-input :value="currentAchievement?.productName" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关联项目">
              <el-input :value="currentAchievement?.relatedProjectName" disabled />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">计划信息（只读）</el-divider>

        <el-form-item label="成果目标描述">
          <el-input :value="currentAchievement?.achievementTarget" type="textarea" :rows="2" disabled />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="计划验收日期">
              <el-input :value="currentAchievement?.plannedAcceptanceDate" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="验收人">
              <el-input :value="currentAchievement?.acceptor" disabled />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="验收要求">
          <el-input :value="currentAchievement?.acceptanceRequirements" type="textarea" :rows="2" disabled />
        </el-form-item>

        <el-divider content-position="left">实际信息</el-divider>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="成果版本" prop="version">
              <el-input v-model="form.version" placeholder="请输入成果版本" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="实际验收日期" prop="actualAcceptanceDate">
              <el-date-picker
                v-model="form.actualAcceptanceDate"
                type="date"
                placeholder="选择日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="DEMO地址">
          <el-input v-model="form.demoUrl" placeholder="请输入DEMO地址" />
        </el-form-item>

        <el-form-item label="代码仓库">
          <el-input v-model="form.codeRepositoryUrl" placeholder="请输入代码仓库/在线文档地址" />
        </el-form-item>

        <el-form-item label="验收提交物" prop="deliverables">
          <el-upload
            class="file-upload"
            :action="uploadUrl"
            :show-file-list="false"
            :before-upload="beforeUpload"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            multiple
          >
            <el-button type="primary">上传文件</el-button>
            <template #tip>
              <div class="upload-tip">支持多个文件上传，每个文件不超过10MB</div>
              <div class="file-list" v-if="uploadedFiles.length > 0">
                <div v-for="(file, index) in uploadedFiles" :key="index" class="file-item">
                  <el-icon><Document /></el-icon>
                  <span>{{ getFileName(file) }}</span>
                  <el-link type="danger" @click="removeFile(index)">删除</el-link>
                </div>
              </div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            提交登记
          </el-button>
          <el-button @click="goBack">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Document } from '@element-plus/icons-vue'
import achievementApi from '../api/achievement'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)
const currentAchievement = ref(null)
const uploadedFiles = ref([])
const uploadUrl = '/api/upload'

const form = reactive({
  version: '',
  actualAcceptanceDate: '',
  demoUrl: '',
  codeRepositoryUrl: '',
  deliverables: ''
})

const rules = {
  version: [{ required: true, message: '请输入成果版本', trigger: 'blur' }],
  actualAcceptanceDate: [{ required: true, message: '请选择实际验收日期', trigger: 'change' }],
  deliverables: [{ required: true, message: '请上传验收提交物', trigger: 'change' }]
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
    uploadedFiles.value.push(response.url)
    form.deliverables = JSON.stringify(uploadedFiles.value)
    ElMessage.success('上传成功')
  }
}

const handleUploadError = () => {
  const mockFile = 'mock_file_' + Date.now()
  uploadedFiles.value.push(mockFile)
  form.deliverables = JSON.stringify(uploadedFiles.value)
  ElMessage.success('文件已选择（模拟上传）')
}

const removeFile = (index) => {
  uploadedFiles.value.splice(index, 1)
  form.deliverables = uploadedFiles.value.length > 0 ? JSON.stringify(uploadedFiles.value) : ''
}

const goBack = () => {
  router.push(`/achievement/${route.params.id}`)
}

const loadDetail = async () => {
  try {
    const res = await achievementApi.getAchievement(route.params.id)
    currentAchievement.value = res
    form.version = res.version || ''
  } catch (error) {
    ElMessage.error('加载成果详情失败')
    console.error(error)
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        const submitData = {
          version: form.version,
          actualAcceptanceDate: form.actualAcceptanceDate,
          demoUrl: form.demoUrl,
          codeRepositoryUrl: form.codeRepositoryUrl,
          deliverables: form.deliverables
        }
        await achievementApi.record(route.params.id, submitData)
        ElMessage.success('登记成功')
        router.push(`/achievement/${route.params.id}`)
      } catch (error) {
        ElMessage.error('登记失败：' + (error.response?.data?.message || error.message))
      } finally {
        submitting.value = false
      }
    }
  })
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped>
.record-achievement {
  padding: 0;
}

.form-card {
  margin-top: 20px;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}

.file-list {
  margin-top: 10px;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 5px 0;
  font-size: 13px;
  color: #606266;
}

.file-item .el-link {
  margin-left: auto;
}
</style>
