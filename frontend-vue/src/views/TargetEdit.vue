<template>
  <div class="target-edit">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>编辑实际数据</span>
          <el-button @click="handleBack">
            <el-icon><Back /></el-icon>
            返回
          </el-button>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="editForm"
        :rules="rules"
        label-width="120px"
        style="max-width: 600px;"
      >
        <el-form-item label="机构选择" prop="organizationId">
          <el-select
            v-model="editForm.organizationId"
            placeholder="请选择机构"
            filterable
            style="width: 100%;"
          >
            <el-option
              v-for="org in organizationList"
              :key="org.id"
              :label="org.name"
              :value="org.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="数据类型" prop="dataType">
          <el-select
            v-model="editForm.dataType"
            placeholder="请选择数据类型"
            style="width: 100%;"
          >
            <el-option label="签约金额" value="signing" />
            <el-option label="确权金额" value="confirmation" />
            <el-option label="研发成果数" value="rd" />
            <el-option label="预算支出" value="budget" />
          </el-select>
        </el-form-item>

        <el-form-item label="年月选择" prop="yearMonth">
          <el-date-picker
            v-model="editForm.yearMonth"
            type="month"
            placeholder="选择年月"
            format="YYYY-MM"
            value-format="YYYY-MM"
            style="width: 100%;"
          />
        </el-form-item>

        <el-form-item label="实际值" prop="actualValue">
          <el-input-number
            v-model="editForm.actualValue"
            :precision="2"
            :min="0"
            :max="999999999"
            style="width: 100%;"
          />
        </el-form-item>

        <el-form-item label="单位">
          <el-tag>{{ getUnit() }}</el-tag>
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="editForm.remark"
            type="textarea"
            :rows="4"
            placeholder="请输入备注信息"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSave" :loading="saveLoading">
            <el-icon><Check /></el-icon>
            保存
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
          <el-button @click="handleBack">
            <el-icon><Close /></el-icon>
            取消
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 历史记录 -->
    <el-card class="history-card">
      <template #header>
        <div class="card-header">
          <span>历史记录</span>
        </div>
      </template>
      <el-table :data="historyData" stripe border style="width: 100%">
        <el-table-column prop="organizationName" label="机构" width="200" />
        <el-table-column prop="dataType" label="数据类型" width="120">
          <template #default="{ row }">
            {{ getDataTypeText(row.dataType) }}
          </template>
        </el-table-column>
        <el-table-column prop="yearMonth" label="年月" width="100" />
        <el-table-column prop="actualValue" label="实际值" width="120" align="right">
          <template #default="{ row }">
            {{ formatValue(row.actualValue, row.dataType) }}
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="200" />
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="createdBy" label="创建人" width="100" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back, Check, Refresh, Close } from '@element-plus/icons-vue'
import targetApi from '../api/target'

const router = useRouter()
const formRef = ref(null)
const saveLoading = ref(false)

// 编辑表单
const editForm = reactive({
  organizationId: '',
  dataType: '',
  yearMonth: '',
  actualValue: 0,
  remark: ''
})

// 表单验证规则
const rules = {
  organizationId: [
    { required: true, message: '请选择机构', trigger: 'change' }
  ],
  dataType: [
    { required: true, message: '请选择数据类型', trigger: 'change' }
  ],
  yearMonth: [
    { required: true, message: '请选择年月', trigger: 'change' }
  ],
  actualValue: [
    { required: true, message: '请输入实际值', trigger: 'blur' }
  ]
}

// 机构列表（示例数据）
const organizationList = ref([
  { id: 'ORG_001', name: '北京分公司' },
  { id: 'ORG_002', name: '上海分公司' },
  { id: 'ORG_003', name: '广州分公司' },
  { id: 'ORG_004', name: '深圳分公司' },
  { id: 'ORG_005', name: '杭州分公司' }
])

// 历史记录数据
const historyData = ref([])

// 获取单位
const getUnit = () => {
  const units = {
    signing: '万元',
    confirmation: '万元',
    rd: '个',
    budget: '万元'
  }
  return units[editForm.dataType] || ''
}

// 获取数据类型文本
const getDataTypeText = (type) => {
  const texts = {
    signing: '签约金额',
    confirmation: '确权金额',
    rd: '研发成果数',
    budget: '预算支出'
  }
  return texts[type] || type
}

// 格式化值
const formatValue = (value, type) => {
  if (type === 'rd') {
    return value
  }
  return value.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

// 保存数据
const handleSave = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      saveLoading.value = true
      try {
        await targetApi.saveActualData(editForm)
        ElMessage.success('保存成功')
        loadHistory()
        handleReset()
      } catch (error) {
        console.error('保存失败:', error)
        ElMessage.error('保存失败')
      } finally {
        saveLoading.value = false
      }
    }
  })
}

// 重置表单
const handleReset = () => {
  if (formRef.value) {
    formRef.value.resetFields()
  }
  editForm.actualValue = 0
  editForm.remark = ''
}

// 返回
const handleBack = () => {
  router.push('/target-statistics')
}

// 加载历史记录
const loadHistory = async () => {
  try {
    // 这里应该调用API获取历史记录
    // const data = await targetApi.getHistory()
    // historyData.value = data
    
    // 模拟数据
    historyData.value = [
      {
        organizationName: '北京分公司',
        dataType: 'signing',
        yearMonth: '2026-01',
        actualValue: 1500,
        remark: '一月签约数据',
        createdAt: '2026-02-01 10:30:00',
        createdBy: '张三'
      },
      {
        organizationName: '上海分公司',
        dataType: 'confirmation',
        yearMonth: '2026-01',
        actualValue: 1200,
        remark: '一月确权数据',
        createdAt: '2026-02-01 11:20:00',
        createdBy: '李四'
      }
    ]
  } catch (error) {
    console.error('加载历史记录失败:', error)
  }
}

onMounted(() => {
  // 设置默认年月为当前月
  const now = new Date()
  editForm.yearMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  
  loadHistory()
})
</script>

<style scoped>
.target-edit {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.history-card {
  margin-top: 20px;
}
</style>
