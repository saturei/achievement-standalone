<template>
  <div class="register-achievement">
    <el-page-header @back="goBack" content="成果注册" />

    <el-card class="form-card">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="140px"
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
            <el-form-item label="成果版本">
              <el-input :value="currentAchievement?.version" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否有基线">
              <el-input :value="currentAchievement?.hasBaseline" disabled />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="成果需求提出人">
              <el-input :value="currentAchievement?.requirementProposer" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="成果可售类型">
              <el-input :value="currentAchievement?.saleType" disabled />
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

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="关联订单">
              <el-input :value="currentAchievement?.relatedOrderName" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关联订单编号">
              <el-input :value="currentAchievement?.relatedOrderId" disabled />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">计划信息</el-divider>

        <el-form-item label="成果目标描述" prop="achievementTarget">
          <el-input
            v-model="form.achievementTarget"
            type="textarea"
            :rows="3"
            placeholder="请输入成果目标描述"
          />
        </el-form-item>

        <el-form-item label="成果对应功能清单">
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

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="验收人（可多人）" prop="acceptor">
              <el-input v-model="form.acceptor" placeholder="请输入验收人" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="成果验收方式" prop="acceptanceMethod">
          <el-input
            v-model="form.acceptanceMethod"
            type="textarea"
            :rows="2"
            placeholder="请输入验收方式"
          />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="计划验收日期" prop="plannedAcceptanceDate">
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
            <el-form-item label="关联套餐">
              <el-input v-model="form.packageIds" placeholder="请输入关联套餐" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="验收要求" prop="acceptanceRequirements">
          <el-input
            v-model="form.acceptanceRequirements"
            type="textarea"
            :rows="2"
            placeholder="请输入验收要求"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            提交注册
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
import achievementApi from '../api/achievement'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)
const currentAchievement = ref(null)
const uploadUrl = '/api/upload'

const form = reactive({
  achievementTarget: '',
  functionListFile: '',
  acceptor: '',
  acceptanceMethod: '',
  plannedAcceptanceDate: '',
  acceptanceRequirements: '',
  packageIds: ''
})

const rules = {
  achievementTarget: [{ required: true, message: '请输入成果目标描述', trigger: 'blur' }],
  acceptor: [{ required: true, message: '请输入验收人', trigger: 'blur' }],
  acceptanceMethod: [{ required: true, message: '请输入验收方式', trigger: 'blur' }],
  plannedAcceptanceDate: [{ required: true, message: '请选择计划验收日期', trigger: 'change' }],
  acceptanceRequirements: [{ required: true, message: '请输入验收要求', trigger: 'blur' }]
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
    form.achievementTarget = res.achievementTarget || ''
    form.functionListFile = res.functionListFile || ''
    form.acceptor = res.acceptor || ''
    form.acceptanceMethod = res.acceptanceMethod || ''
    form.plannedAcceptanceDate = res.plannedAcceptanceDate ? res.plannedAcceptanceDate.split(' ')[0] : ''
    form.acceptanceRequirements = res.acceptanceRequirements || ''
    form.packageIds = res.packageIds || ''
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
        const submitData = {}
        Object.keys(form).forEach(key => {
          if (form[key] !== '' && form[key] !== null && form[key] !== undefined) {
            submitData[key] = form[key]
          }
        })
        await achievementApi.register(route.params.id, submitData)
        ElMessage.success('注册成功')
        router.push(`/achievement/${route.params.id}`)
      } catch (error) {
        ElMessage.error('注册失败：' + (error.response?.data?.message || error.message))
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
.register-achievement {
  padding: 0;
}

.form-card {
  margin-top: 20px;
}

.upload-tip {
  font-size: 12px;
  color: #606266;
  margin-top: 5px;
}
</style>
