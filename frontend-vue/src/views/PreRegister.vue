<template>
  <div class="pre-register">
    <el-page-header @back="goBack" content="成果预注册" />

    <el-card class="form-card">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        label-position="right"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="成果名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入成果名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关联产品" prop="productId">
              <el-select v-model="form.productId" placeholder="请选择关联产品" style="width: 100%" @change="handleProductChange">
                <el-option
                  v-for="item in productOptions"
                  :key="item.id"
                  :label="`${item.name}（${item.id}）`"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="产品版本" prop="productExternalVersion">
              <el-input v-model="form.productExternalVersion" placeholder="如：V1.0.0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="成果形态" prop="achievementForm">
              <el-select v-model="form.achievementForm" placeholder="请选择成果形态" style="width: 100%" @change="handleAchievementFormChange">
                <el-option label="方案成果" value="方案成果" />
                <el-option label="系统成果" value="系统成果" />
                <el-option label="设计成果" value="设计成果" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="可售类型" prop="saleType">
              <el-select v-model="form.saleType" placeholder="请选择可售类型" style="width: 100%">
                <el-option label="方案可售" value="方案可售" />
                <el-option label="系统可售" value="系统可售" />
                <el-option label="内部应用" value="内部应用" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否有基线" prop="hasBaseline">
              <el-select v-model="form.hasBaseline" placeholder="请选择是否有基线" style="width: 100%">
                <el-option label="有基线" value="有基线" />
                <el-option label="无基线" value="无基线" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="需求提出人" prop="requirementProposer">
              <el-input v-model="form.requirementProposer" placeholder="请输入需求提出人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="负责人" prop="owner">
              <el-input v-model="form.owner" placeholder="请输入负责人" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="关联机构" prop="organizationName">
              <el-select v-model="form.organizationName" placeholder="请选择关联机构" style="width: 100%" @change="handleOrganizationChange">
                <el-option
                  v-for="item in organizationOptions"
                  :key="item.organization"
                  :label="item.organization"
                  :value="item.organization"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="部门名称">
              <el-input v-model="form.departmentName" disabled placeholder="根据机构自动填充" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关联模块">
              <el-input v-model="form.moduleId" placeholder="请输入关联模块" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="成果目标描述" prop="achievementTarget">
          <el-input
            v-model="form.achievementTarget"
            type="textarea"
            :rows="3"
            placeholder="请输入成果目标描述"
          />
        </el-form-item>

        <el-form-item label="上传功能清单" prop="functionListFile">
          <el-upload
            class="upload-demo"
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
              <div class="upload-tip" v-if="isSystemAchievement">
                <span style="color: #f56c6c;">* 系统成果必须上传功能清单</span>
              </div>
            </template>
          </el-upload>
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
            <el-form-item label="验收人" prop="acceptor">
              <el-input v-model="form.acceptor" placeholder="请输入验收人" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="验收方式和要求" prop="acceptanceMethod">
          <el-input
            v-model="form.acceptanceMethod"
            type="textarea"
            :rows="2"
            placeholder="请输入验收方式和要求"
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

        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            提交预注册
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import achievementApi from '../api/achievement'

const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)
const uploadUrl = '/api/upload'

const productOptions = [
  { id: 'CP_0001', name: '数字化智能营销平台' },
  { id: 'CP_0003', name: '对公金融服务平台' },
  { id: 'CP_0004', name: '个人金融服务平台' },
  { id: 'CP_0007', name: 'Finmall平台' },
  { id: 'CP_0008', name: 'Finmall资产底座' },
  { id: 'CP_0012', name: 'DPRO平台' },
  { id: 'CP_0014', name: '信创产品' },
  { id: 'CP_0018', name: '企业服务生态云平台' },
  { id: 'CP_0019', name: 'AI 手机银行' }
]

const organizationOptions = [
  { organization: 'AI方案研发机构', department: 'AI方案中心' },
  { organization: 'AI场景研发机构', department: 'AI方案中心' },
  { organization: '端技术底座机构', department: '能力中心' },
  { organization: 'AI技术底座机构', department: '能力中心' },
  { organization: 'POC方案验证机构', department: '能力中心' },
  { organization: '服务技术底座机构', department: '能力中心' },
  { organization: '数据中心本级', department: '数据中心' },
  { organization: '业务方案高风险机构', department: '业务方案中心' },
  { organization: '业务方案中风险机构', department: '业务方案中心' },
  { organization: '中台运营机构', department: 'FM平台中心' },
  { organization: '生产工具机构', department: 'FM平台中心' }
]

const form = reactive({
  name: '',
  productId: '',
  productName: '',
  productExternalVersion: '',
  achievementForm: '',
  saleType: '',
  hasBaseline: '无基线',
  requirementProposer: '',
  owner: '',
  organizationName: '',
  departmentName: '',
  moduleId: '',
  moduleName: '',
  achievementTarget: '',
  functionListFile: '',
  plannedAcceptanceDate: '',
  acceptanceMethod: '',
  acceptor: '',
  relatedProjectId: '',
  relatedProjectName: '',
  relatedOrderId: '',
  relatedOrderName: ''
})

const isSystemAchievement = computed(() => form.achievementForm === '系统成果')

const validateFunctionListFile = (rule, value, callback) => {
  if (isSystemAchievement.value && !value) {
    callback(new Error('系统成果必须上传功能清单'))
  } else {
    callback()
  }
}

const rules = {
  name: [{ required: true, message: '请输入成果名称', trigger: 'blur' }],
  productId: [{ required: true, message: '请选择关联产品', trigger: 'change' }],
  productExternalVersion: [{ required: true, message: '请输入产品版本', trigger: 'blur' }],
  achievementForm: [{ required: true, message: '请选择成果形态', trigger: 'change' }],
  saleType: [{ required: true, message: '请选择可售类型', trigger: 'change' }],
  hasBaseline: [{ required: true, message: '请选择是否有基线', trigger: 'change' }],
  requirementProposer: [{ required: true, message: '请输入需求提出人', trigger: 'blur' }],
  owner: [{ required: true, message: '请输入负责人', trigger: 'blur' }],
  organizationName: [{ required: true, message: '请选择关联机构', trigger: 'change' }],
  achievementTarget: [{ required: true, message: '请输入成果目标描述', trigger: 'blur' }],
  functionListFile: [{ validator: validateFunctionListFile, trigger: 'change' }],
  plannedAcceptanceDate: [{ required: true, message: '请选择计划验收日期', trigger: 'change' }],
  acceptanceMethod: [{ required: true, message: '请输入验收方式和要求', trigger: 'blur' }],
  acceptor: [{ required: true, message: '请输入验收人', trigger: 'blur' }]
}

const handleProductChange = (value) => {
  const product = productOptions.find(item => item.id === value)
  if (product) {
    form.productName = product.name
  }
}

const handleOrganizationChange = (value) => {
  const org = organizationOptions.find(item => item.organization === value)
  if (org) {
    form.departmentName = org.department
  }
}

const handleAchievementFormChange = () => {
  if (formRef.value) {
    formRef.value.validateField('functionListFile')
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
  router.push('/')
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        await achievementApi.preRegister(form)
        ElMessage.success('预注册成功')
        router.push('/')
      } catch (error) {
        ElMessage.error('预注册失败：' + (error.response?.data?.message || error.message))
      } finally {
        submitting.value = false
      }
    }
  })
}

const handleReset = () => {
  if (formRef.value) {
    formRef.value.resetFields()
    form.departmentName = ''
    form.productName = ''
  }
}
</script>

<style scoped>
.pre-register {
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
