<template>
  <div class="target-statistics">
    <!-- 顶部筛选区域 -->
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="年度">
          <el-select v-model="filterForm.year" placeholder="选择年度" @change="handleFilterChange">
            <el-option label="2024年" :value="2024" />
            <el-option label="2025年" :value="2025" />
            <el-option label="2026年" :value="2026" />
          </el-select>
        </el-form-item>
        <el-form-item label="部门">
          <el-select v-model="filterForm.department" placeholder="全部部门" clearable @change="handleDepartmentChange">
            <el-option v-for="item in departmentOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="机构">
          <el-select v-model="filterForm.organizations" placeholder="全部机构" clearable multiple collapse-tags collapse-tags-tooltip @change="handleFilterChange">
            <el-option v-for="item in filteredOrganizationOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="filterForm.owner" placeholder="全部负责人" clearable @change="handleFilterChange">
            <el-option v-for="item in ownerOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterForm.status" placeholder="全部状态" clearable @change="handleFilterChange">
            <el-option label="预注册" value="PRE_REGISTER" />
            <el-option label="已注册" value="REGISTER" />
            <el-option label="已登记" value="RECORDED" />
          </el-select>
        </el-form-item>
        <el-form-item label="细分目标">
          <el-select v-model="filterForm.subCategories" placeholder="全部细分目标" clearable multiple collapse-tags collapse-tags-tooltip @change="handleFilterChange">
            <el-option v-for="item in subCategoryFilterOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 目标达成卡片区域 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card-wrapper">
          <div class="stat-card">
            <div class="stat-header">
              <el-icon :size="24" color="#409eff"><TrendCharts /></el-icon>
              <span class="stat-title">签约目标</span>
            </div>
            <div class="stat-body">
              <div class="stat-item">
                <span class="label">年度目标</span>
                <span class="value">{{ formatMoney(statistics.signing.target) }} 万元</span>
              </div>
              <div class="stat-item">
                <span class="label">实际完成</span>
                <span class="value highlight">{{ formatMoney(statistics.signing.actual) }} 万元</span>
              </div>
              <div class="stat-item">
                <span class="label">达成率</span>
                <el-progress 
                  :percentage="statistics.signing.rate" 
                  :color="getProgressColor(statistics.signing.rate)"
                  :stroke-width="10"
                />
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover" class="stat-card-wrapper">
          <div class="stat-card">
            <div class="stat-header">
              <el-icon :size="24" color="#67c23a"><CircleCheckFilled /></el-icon>
              <span class="stat-title">确权目标</span>
            </div>
            <div class="stat-body">
              <div class="stat-item">
                <span class="label">年度目标</span>
                <span class="value">{{ formatMoney(statistics.confirmation.target) }} 万元</span>
              </div>
              <div class="stat-item">
                <span class="label">实际完成</span>
                <span class="value highlight">{{ formatMoney(statistics.confirmation.actual) }} 万元</span>
              </div>
              <div class="stat-item">
                <span class="label">达成率</span>
                <el-progress 
                  :percentage="statistics.confirmation.rate" 
                  :color="getProgressColor(statistics.confirmation.rate)"
                  :stroke-width="10"
                />
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover" class="stat-card-wrapper">
          <div class="stat-card">
            <div class="stat-header">
              <el-icon :size="24" color="#e6a23c"><Medal /></el-icon>
              <span class="stat-title">研发成果</span>
            </div>
            <div class="stat-body">
              <div class="stat-item">
                <span class="label">年度目标</span>
                <span class="value">{{ statistics.rd.target }} 个</span>
              </div>
              <div class="stat-item">
                <span class="label">计划成果</span>
                <span class="value">{{ statistics.rd.planned }} 个</span>
              </div>
              <div class="stat-item">
                <span class="label">实际完成</span>
                <span class="value highlight">{{ statistics.rd.actual }} 个</span>
              </div>
              <div class="stat-item">
                <span class="label">达成率</span>
                <el-progress 
                  :percentage="statistics.rd.rate" 
                  :color="getProgressColor(statistics.rd.rate)"
                  :stroke-width="10"
                />
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover" class="stat-card-wrapper">
          <div class="stat-card">
            <div class="stat-header">
              <el-icon :size="24" color="#f56c6c"><Wallet /></el-icon>
              <span class="stat-title">预算控制</span>
            </div>
            <div class="stat-body">
              <div class="stat-item">
                <span class="label">年度预算</span>
                <span class="value">{{ formatMoney(statistics.budget.target) }} 万元</span>
              </div>
              <div class="stat-item">
                <span class="label">实际支出</span>
                <span class="value highlight">{{ formatMoney(statistics.budget.actual) }} 万元</span>
              </div>
              <div class="stat-item">
                <span class="label">执行率</span>
                <el-progress 
                  :percentage="statistics.budget.rate" 
                  :color="getBudgetProgressColor(statistics.budget.rate)"
                  :stroke-width="10"
                />
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 目标达成可视化图表区域 -->
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <el-icon :size="18" color="#409eff"><TrendCharts /></el-icon>
              <span>签约收入 — 季度目标 vs 实际</span>
            </div>
          </template>
          <div ref="signingChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <el-icon :size="18" color="#e6a23c"><TrendCharts /></el-icon>
              <span>确权收入 — 季度目标 vs 实际</span>
            </div>
          </template>
          <div ref="confirmationChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="16" style="margin-top: 16px;">
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <el-icon :size="18" color="#67c23a"><Medal /></el-icon>
              <span>研发成果 — 月度分布</span>
            </div>
          </template>
          <div ref="rdChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <el-icon :size="18" color="#f56c6c"><Wallet /></el-icon>
              <span>预算控制 — 季度预算 vs 实际支出</span>
            </div>
          </template>
          <div ref="budgetChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 数据表格区域 -->
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>机构目标达成进度</span>
          <el-button type="primary" size="small" @click="handleDefineParams">
            <el-icon><Setting /></el-icon>
            定义列表参数
          </el-button>
        </div>
      </template>
      <el-table
        :data="tableData"
        v-loading="tableLoading"
        stripe
        border
        style="width: 100%"
        max-height="68vh"
      >
        <el-table-column prop="departmentName" label="部门" min-width="120" fixed />
        <el-table-column prop="organizationName" label="机构" min-width="150" fixed />
        <el-table-column prop="category" label="科目" min-width="100" />
        <el-table-column prop="subCategory" label="细分目标" min-width="150" />
        <el-table-column prop="owner" label="负责人" min-width="100" />
        <el-table-column label="年度目标" align="center">
          <el-table-column label="目标" width="110" align="right">
            <template #default="{ row }">
              {{ row.subCategory === '研发成果' ? formatInteger(row.annualTarget) : formatMoney(row.annualTarget) }}
            </template>
          </el-table-column>
          <el-table-column label="实际" width="110" align="right">
            <template #default="{ row }">
              {{ row.subCategory === '研发成果' ? formatInteger(row.actualValue) : formatMoney(row.actualValue) }}
            </template>
          </el-table-column>
        </el-table-column>
        <el-table-column label="Q1" align="center">
          <el-table-column prop="q1Target" label="目标" width="90" align="right" :formatter="formatMoney" />
          <el-table-column prop="q1Actual" label="实际" width="90" align="right" :formatter="formatInteger" />
        </el-table-column>
        <el-table-column label="Q2" align="center">
          <el-table-column prop="q2Target" label="目标" width="90" align="right" :formatter="formatMoney" />
          <el-table-column prop="q2Actual" label="实际" width="90" align="right" :formatter="formatInteger" />
        </el-table-column>
        <el-table-column label="Q3" align="center">
          <el-table-column prop="q3Target" label="目标" width="90" align="right" :formatter="formatMoney" />
          <el-table-column prop="q3Actual" label="实际" width="90" align="right" :formatter="formatInteger" />
        </el-table-column>
        <el-table-column label="Q4" align="center">
          <el-table-column prop="q4Target" label="目标" width="90" align="right" :formatter="formatMoney" />
          <el-table-column prop="q4Actual" label="实际" width="90" align="right" :formatter="formatInteger" />
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEditRow(row)">
              编辑
            </el-button>
            <el-button link type="danger" @click="handleDeleteRow(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 定义列表参数对话框 -->
    <el-dialog
      v-model="defineDialogVisible"
      title="定义列表参数"
      width="700px"
    >
      <el-form :model="defineForm" label-width="120px" :rules="defineRules" ref="defineFormRef">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="机构" prop="organization">
              <el-select v-model="defineForm.organization" placeholder="选择机构" style="width: 100%" @change="handleDefineOrgChange">
                <el-option
                  v-for="item in organizationDepartmentOptions"
                  :key="item.organization"
                  :label="item.organization"
                  :value="item.organization"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门" prop="department">
              <el-input v-model="defineForm.department" disabled placeholder="根据机构自动填充" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="科目" prop="category">
              <el-select v-model="defineForm.category" placeholder="选择科目" style="width: 100%" @change="handleDefineCategoryChange">
                <el-option label="价值" value="价值" />
                <el-option label="费用" value="费用" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="细分目标" prop="subCategory">
              <el-select v-model="defineForm.subCategory" placeholder="选择细分目标" style="width: 100%" @change="handleDefineSubCategoryChange">
                <el-option v-for="item in subCategoryOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="负责人" prop="owner">
          <el-input v-model="defineForm.owner" placeholder="请输入负责人" />
        </el-form-item>
        <el-divider content-position="left">季度目标</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Q1目标" prop="q1Target">
              <el-input v-model="defineForm.q1Target" placeholder="Q1目标" style="width: 100%" :disabled="defineForm.subCategory === '研发成果'" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Q1实际">
              <el-input v-model="defineForm.q1Actual" placeholder="Q1实际" style="width: 100%" :disabled="defineForm.subCategory === '研发成果'" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Q2目标" prop="q2Target">
              <el-input v-model="defineForm.q2Target" placeholder="Q2目标" style="width: 100%" :disabled="defineForm.subCategory === '研发成果'" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Q2实际">
              <el-input v-model="defineForm.q2Actual" placeholder="Q2实际" style="width: 100%" :disabled="defineForm.subCategory === '研发成果'" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Q3目标" prop="q3Target">
              <el-input v-model="defineForm.q3Target" placeholder="Q3目标" style="width: 100%" :disabled="defineForm.subCategory === '研发成果'" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Q3实际">
              <el-input v-model="defineForm.q3Actual" placeholder="Q3实际" style="width: 100%" :disabled="defineForm.subCategory === '研发成果'" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Q4目标" prop="q4Target">
              <el-input v-model="defineForm.q4Target" placeholder="Q4目标" style="width: 100%" :disabled="defineForm.subCategory === '研发成果'" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Q4实际">
              <el-input v-model="defineForm.q4Actual" placeholder="Q4实际" style="width: 100%" :disabled="defineForm.subCategory === '研发成果'" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">汇总</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="年度目标（万元）">
              <el-input :model-value="annualTargetTotal" disabled placeholder="Q1+Q2+Q3+Q4自动汇总" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年度实际（万元）">
              <el-input :model-value="actualValueTotal" disabled placeholder="Q1+Q2+Q3+Q4自动汇总" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-alert v-if="defineForm.subCategory === '研发成果'" type="info" show-icon :closable="false" style="margin-top: 10px;">
          <template #title>
            研发成果实际值自动从成果列表统计（{{ defineForm.rdAchievementCount }} 个），目标值不在此处设置
          </template>
        </el-alert>
      </el-form>
      <template #footer>
        <el-button @click="defineDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDefineParams" :loading="defineLoading">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { TrendCharts, CircleCheckFilled, Medal, Wallet, Plus, Setting } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import targetApi from '../api/target'

const signingChartRef = ref(null)
const confirmationChartRef = ref(null)
const rdChartRef = ref(null)
const budgetChartRef = ref(null)

let signingChartInstance = null
let confirmationChartInstance = null
let rdChartInstance = null
let budgetChartInstance = null

const quarterlySummary = ref({ signing: [], confirmation: [], budget: [] })

// 筛选表单
const filterForm = ref({
  year: 2026,
  department: '',
  organizations: [],
  owner: '',
  status: '',
  subCategories: []
})

// 筛选选项
const departmentOptions = ref([])
const organizationOptions = ref([])
const ownerOptions = ref([])
const subCategoryFilterOptions = ref([])

// 统计数据
const statistics = ref({
  signing: { target: 0, actual: 0, rate: 0 },
  confirmation: { target: 0, actual: 0, rate: 0 },
  rd: { target: 0, planned: 0, actual: 0, rate: 0 },
  budget: { target: 0, actual: 0, rate: 0 }
})

// 表格数据
const tableData = ref([])
const tableLoading = ref(false)

// 月度分布数据
const monthlyData = ref([])

// 定义列表参数对话框
const defineDialogVisible = ref(false)
const defineLoading = ref(false)
const defineFormRef = ref(null)
const defineForm = reactive({
  id: '',
  department: '',
  organization: '',
  category: '',
  subCategory: '',
  targetType: '',
  year: 2026,
  annualTarget: 0,
  actualValue: 0,
  q1Target: 0,
  q2Target: 0,
  q3Target: 0,
  q4Target: 0,
  q1Actual: 0,
  q2Actual: 0,
  q3Actual: 0,
  q4Actual: 0,
  owner: '',
  rdAchievementCount: 0
})

const defineRules = {
  organization: [{ required: true, message: '请选择机构', trigger: 'change' }],
  category: [{ required: true, message: '请选择科目', trigger: 'change' }]
}

const subCategoryMap = {
  '价值': ['签约收入（高）', '签约收入（中）', '确权收入（高）', '确权收入（中）', '研发成果'],
  '费用': ['费用']
}

const subCategoryOptions = computed(() => {
  return subCategoryMap[defineForm.category] || []
})

const annualTargetTotal = computed(() => {
  const q1 = Number(defineForm.q1Target) || 0
  const q2 = Number(defineForm.q2Target) || 0
  const q3 = Number(defineForm.q3Target) || 0
  const q4 = Number(defineForm.q4Target) || 0
  return q1 + q2 + q3 + q4
})

const actualValueTotal = computed(() => {
  const q1 = Number(defineForm.q1Actual) || 0
  const q2 = Number(defineForm.q2Actual) || 0
  const q3 = Number(defineForm.q3Actual) || 0
  const q4 = Number(defineForm.q4Actual) || 0
  return q1 + q2 + q3 + q4
})

const organizationDepartmentOptions = [
  { organization: 'AI方案研发机构', department: 'AI方案中心' },
  { organization: 'AI场景研发机构', department: 'AI方案中心' },
  { organization: 'AI方案中心本级', department: 'AI方案中心' },
  { organization: '端技术底座机构', department: '能力中心' },
  { organization: 'AI技术底座机构', department: '能力中心' },
  { organization: 'POC方案验证机构', department: '能力中心' },
  { organization: '服务技术底座机构', department: '能力中心' },
  { organization: '能力中心本级', department: '能力中心' },
  { organization: '数据中心本级', department: '数据中心' },
  { organization: '业务方案高风险机构', department: '业务方案中心' },
  { organization: '业务方案中风险机构', department: '业务方案中心' },
  { organization: '业务方案中心本级', department: '业务方案中心' },
  { organization: '中台运营机构', department: 'FM平台中心' },
  { organization: '生产工具机构', department: 'FM平台中心' },
  { organization: 'FM平台中心本级', department: 'FM平台中心' }
]

const filteredOrganizationOptions = computed(() => {
  if (filterForm.value.department) {
    return organizationDepartmentOptions
      .filter(item => item.department === filterForm.value.department)
      .map(item => item.organization)
  }
  return organizationOptions.value
})

onMounted(() => {
  loadFilterOptions()
  loadStatistics()
  loadMonthlyDistribution()
  loadQuarterlySummary()
  loadTableData()
  nextTick(() => {
    initAllCharts()
  })
})

onUnmounted(() => {
  signingChartInstance?.dispose()
  confirmationChartInstance?.dispose()
  rdChartInstance?.dispose()
  budgetChartInstance?.dispose()
})

// 加载筛选选项
const loadFilterOptions = async () => {
  try {
    const [products, organizations, owners] = await Promise.all([
      targetApi.getAllProducts(),
      targetApi.getAllOrganizations(),
      targetApi.getAllOwners()
    ])
    departmentOptions.value = products || []
    organizationOptions.value = organizations || []
    ownerOptions.value = owners || []
  } catch (error) {
    console.error('加载筛选选项失败:', error)
  }
  // 细分目标从 defineForm 的已知值中获取
  subCategoryFilterOptions.value = ['签约收入（高）', '签约收入（中）', '确权收入（高）', '确权收入（中）', '研发成果', '费用']
}

// 加载统计数据
const loadStatistics = async () => {
  try {
    const params = {
      dimension: 'organization',
      year: filterForm.value.year,
      product: filterForm.value.department || undefined,
      organization: filterForm.value.organizations.length > 0 ? filterForm.value.organizations.join(',') : undefined,
      owner: filterForm.value.owner || undefined,
      subCategory: filterForm.value.subCategories.length > 0 ? filterForm.value.subCategories.join(',') : undefined
    }
    const response = await targetApi.getStatistics(params)
    if (response && response.statistics) {
      const summary = response.summary || {}

      const rdActual = response.rdActual || 0
      const rdPlanned = response.rdPlanned || 0
      const rdTarget = response.rdTarget || 0
      
      statistics.value = {
        signing: { 
          target: summary.signingTarget ? summary.signingTarget / 10000 : 0, 
          actual: summary.signingActual ? summary.signingActual / 10000 : 0, 
          rate: summary.signingRate || 0 
        },
        confirmation: { 
          target: summary.confirmationTarget ? summary.confirmationTarget / 10000 : 0, 
          actual: summary.confirmationActual ? summary.confirmationActual / 10000 : 0, 
          rate: summary.confirmationRate || 0 
        },
        rd: { 
          target: rdTarget,
          planned: rdPlanned,
          actual: rdActual,
          rate: rdTarget > 0 ? Math.round((rdActual / rdTarget) * 100) : 0 
        },
        budget: { 
          target: summary.budgetTarget ? summary.budgetTarget / 10000 : 0, 
          actual: summary.budgetActual ? summary.budgetActual / 10000 : 0, 
          rate: summary.budgetRate || 0 
        }
      }
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
    ElMessage.error('加载统计数据失败')
  }
}

// 加载月度分布数据
const loadMonthlyDistribution = async () => {
  try {
    const response = await targetApi.getMonthlyDistribution(filterForm.value.year)
    if (response && response.monthlyData) {
      monthlyData.value = response.monthlyData
      updateAllCharts()
    }
  } catch (error) {
    console.error('加载月度分布数据失败:', error)
    ElMessage.error('加载月度分布数据失败')
  }
}

// 加载表格数据
const loadTableData = async () => {
  tableLoading.value = true
  try {
    const params = {
      dimension: 'organization',
      year: filterForm.value.year,
      product: filterForm.value.department || undefined,
      organization: filterForm.value.organizations.length > 0 ? filterForm.value.organizations.join(',') : undefined,
      owner: filterForm.value.owner || undefined,
      subCategory: filterForm.value.subCategories.length > 0 ? filterForm.value.subCategories.join(',') : undefined
    }
    const response = await targetApi.getStatistics(params)
    if (response && response.statistics) {
      tableData.value = response.statistics.map(item => {
        const isRd = item.subCategory === '研发成果'
        const fmt = (val) => isRd ? (val || 0) : (val ? (val / 10000) : 0)
        return {
          id: item.id,
          name: item.name,
          departmentName: item.departmentName || '',
          productName: item.productName || '',
          organizationName: item.organizationName || item.name,
          category: item.category || '',
          subCategory: item.subCategory || '',
          targetType: item.targetType || '',
          owner: item.owner || '',
          annualTarget: fmt(item.annualTarget),
          actualValue: fmt(item.actualValue),
          q1Target: fmt(item.q1Target),
          q1Actual: fmt(item.q1Actual),
          q2Target: fmt(item.q2Target),
          q2Actual: fmt(item.q2Actual),
          q3Target: fmt(item.q3Target),
          q3Actual: fmt(item.q3Actual),
          q4Target: fmt(item.q4Target),
          q4Actual: fmt(item.q4Actual)
        }
      })
    }
  } catch (error) {
    console.error('加载表格数据失败:', error)
    ElMessage.error('加载表格数据失败')
  } finally {
    tableLoading.value = false
  }
}

const loadQuarterlySummary = async () => {
  try {
    const response = await targetApi.getQuarterlySummary({
      year: filterForm.value.year,
      organization: filterForm.value.organizations.length > 0 ? filterForm.value.organizations.join(',') : undefined
    })
    if (response) {
      quarterlySummary.value = response
    }
    updateAllCharts()
  } catch (error) {
    console.error('加载季度汇总失败:', error)
  }
}

const initAllCharts = () => {
  if (signingChartRef.value) signingChartInstance = echarts.init(signingChartRef.value)
  if (confirmationChartRef.value) confirmationChartInstance = echarts.init(confirmationChartRef.value)
  if (rdChartRef.value) rdChartInstance = echarts.init(rdChartRef.value)
  if (budgetChartRef.value) budgetChartInstance = echarts.init(budgetChartRef.value)
  updateAllCharts()
}

const updateAllCharts = () => {
  updateRdChart()
  renderQuarterlyComparisonChart(signingChartInstance, quarterlySummary.value.signing || [], '#a0cfff', '#409eff')
  renderQuarterlyComparisonChart(confirmationChartInstance, quarterlySummary.value.confirmation || [], '#f4cfa0', '#e6a23c')
  renderQuarterlyComparisonChart(budgetChartInstance, quarterlySummary.value.budget || [], '#f0a0a0', '#f56c6c')
}

const renderQuarterlyComparisonChart = (chartInstance, data, targetColor, actualColor) => {
  if (!chartInstance) return
  const toWan = (val) => val ? (val / 10000) : 0
  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        const idx = params[0]?.dataIndex ?? 0
        const t = toWan(data[idx]?.target || 0)
        const a = toWan(data[idx]?.actual || 0)
        const r = data[idx]?.completionRate || 0
        return params[0]?.axisValue + '<br/>目标: ' + t.toFixed(2) + ' 万元<br/>实际: ' + a.toFixed(2) + ' 万元<br/>达成率: ' + Number(r).toFixed(2) + '%'
      }
    },
    legend: { data: ['目标', '实际'], top: 5 },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true, top: 40 },
    xAxis: { type: 'category', data: ['Q1', 'Q2', 'Q3', 'Q4'] },
    yAxis: { type: 'value', axisLabel: { formatter: (v) => (v / 10000).toFixed(0) + '万' } },
    series: [
      {
        name: '目标', type: 'bar',
        data: data.map(d => d?.target || 0),
        itemStyle: { color: targetColor },
        barWidth: '35%',
        label: { show: true, position: 'top', formatter: (p) => p.value > 0 ? (p.value / 10000).toFixed(0) + '万' : '' }
      },
      {
        name: '实际', type: 'bar',
        data: data.map(d => d?.actual || 0),
        itemStyle: { color: actualColor },
        barWidth: '35%',
        label: { show: true, position: 'top', formatter: (p) => p.value > 0 ? (p.value / 10000).toFixed(0) + '万' : '' }
      }
    ]
  }
  chartInstance.setOption(option)
}

const updateRdChart = () => {
  if (!rdChartInstance) return

  const months = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']

  const preRegisterData = monthlyData.value.map(item => {
    const statusData = item.byStatus?.find(s => s.status === 'PRE_REGISTER')
    return statusData ? statusData.count : 0
  })

  const registerData = monthlyData.value.map(item => {
    const statusData = item.byStatus?.find(s => s.status === 'REGISTER')
    return statusData ? statusData.count : 0
  })

  const recordedData = monthlyData.value.map(item => {
    const statusData = item.byStatus?.find(s => s.status === 'RECORDED')
    return statusData ? statusData.count : 0
  })

  const series = []
  const legendData = []

  if (!filterForm.value.status || filterForm.value.status === 'PRE_REGISTER') {
    series.push({
      name: '预注册', type: 'bar', stack: 'total', data: preRegisterData,
      itemStyle: { color: '#e6a23c' },
      label: { show: true, position: 'inside', formatter: (params) => params.value > 0 ? params.value : '' }
    })
    legendData.push('预注册')
  }

  if (!filterForm.value.status || filterForm.value.status === 'REGISTER') {
    series.push({
      name: '已注册', type: 'bar', stack: 'total', data: registerData,
      itemStyle: { color: '#409eff' },
      label: { show: true, position: 'inside', formatter: (params) => params.value > 0 ? params.value : '' }
    })
    legendData.push('已注册')
  }

  if (!filterForm.value.status || filterForm.value.status === 'RECORDED') {
    series.push({
      name: '已登记', type: 'bar', stack: 'total', data: recordedData,
      itemStyle: { color: '#67c23a' },
      label: { show: true, position: 'inside', formatter: (params) => params.value > 0 ? params.value : '' }
    })
    legendData.push('已登记')
  }

  const option = {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: legendData, top: 10 },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true, top: 50 },
    xAxis: { type: 'category', data: months, axisLabel: { interval: 0 } },
    yAxis: { type: 'value', name: '成果数量' },
    series: series
  }
  rdChartInstance.setOption(option)
}

// 部门选择变化
const handleDepartmentChange = () => {
  filterForm.value.organizations = []
  handleFilterChange()
}

// 筛选条件变化
const handleFilterChange = () => {
  loadStatistics()
  loadMonthlyDistribution()
  loadQuarterlySummary()
  loadTableData()
}

// 定义列表参数
const handleDefineParams = () => {
  defineForm.id = ''
  defineForm.department = ''
  defineForm.organization = ''
  defineForm.category = ''
  defineForm.subCategory = ''
  defineForm.year = filterForm.value.year
  defineForm.annualTarget = 0
  defineForm.actualValue = 0
  defineForm.q1Target = 0
  defineForm.q2Target = 0
  defineForm.q3Target = 0
  defineForm.q4Target = 0
  defineForm.q1Actual = 0
  defineForm.q2Actual = 0
  defineForm.q3Actual = 0
  defineForm.q4Actual = 0
  defineForm.owner = ''
  defineForm.rdAchievementCount = 0
  defineDialogVisible.value = true
}

// 编辑行数据
const handleEditRow = (row) => {
  defineForm.id = row.id || ''
  defineForm.department = row.departmentName || ''
  defineForm.organization = row.organizationName || ''
  defineForm.category = row.category || ''
  defineForm.subCategory = row.subCategory || ''
  defineForm.targetType = row.targetType || ''
  defineForm.year = filterForm.value.year
  defineForm.annualTarget = row.annualTarget ? parseFloat(row.annualTarget) : 0
  defineForm.actualValue = row.actualValue ? parseFloat(row.actualValue) : 0
  defineForm.q1Target = row.q1Target ? parseFloat(row.q1Target) : 0
  defineForm.q2Target = row.q2Target ? parseFloat(row.q2Target) : 0
  defineForm.q3Target = row.q3Target ? parseFloat(row.q3Target) : 0
  defineForm.q4Target = row.q4Target ? parseFloat(row.q4Target) : 0
  defineForm.q1Actual = row.q1Actual ? parseFloat(row.q1Actual) : 0
  defineForm.q2Actual = row.q2Actual ? parseFloat(row.q2Actual) : 0
  defineForm.q3Actual = row.q3Actual ? parseFloat(row.q3Actual) : 0
  defineForm.q4Actual = row.q4Actual ? parseFloat(row.q4Actual) : 0
  defineForm.owner = row.owner || ''
  defineForm.rdAchievementCount = 0
  loadRdAchievementCount()
  defineDialogVisible.value = true
}

// 机构选择变化
const handleDefineOrgChange = (value) => {
  const org = organizationDepartmentOptions.find(item => item.organization === value)
  if (org) {
    defineForm.department = org.department
  }
}

// 科目选择变化
const handleDefineCategoryChange = (value) => {
  defineForm.subCategory = ''
}

// 细分目标选择变化
const handleDefineSubCategoryChange = (value) => {
  if (value === '研发成果') {
    loadRdAchievementCount()
  }
}

// 加载研发成果数量
const loadRdAchievementCount = async () => {
  try {
    const response = await targetApi.getMonthlyDistribution(filterForm.value.year)
    if (response && response.summary) {
      defineForm.rdAchievementCount = response.summary.totalCount || 0
    }
  } catch (error) {
    console.error('加载研发成果数量失败:', error)
  }
}

// 删除行数据
const handleDeleteRow = (row) => {
  ElMessageBox.confirm(
    `确定要删除「${row.organizationName} - ${row.subCategory}」的目标数据吗？`,
    '确认删除',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await targetApi.deleteTarget(row.id)
      ElMessage.success('删除成功')
      loadStatistics()
      loadTableData()
      loadFilterOptions()
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

// 保存定义列表参数
const saveDefineParams = async () => {
  if (!defineFormRef.value) return

  await defineFormRef.value.validate(async (valid) => {
    if (!valid) return

    defineLoading.value = true
    try {
      const targetType = defineForm.category
      const toYuan = (val) => { const n = parseFloat(val); return isNaN(n) ? 0 : n * 10000 }
      const payload = {
        department: defineForm.department,
        organization: defineForm.organization,
        category: defineForm.category,
        subCategory: defineForm.subCategory,
        targetType: targetType,
        year: defineForm.year,
        annualTarget: defineForm.subCategory === '研发成果' ? 0 : toYuan(annualTargetTotal.value),
        q1Target: defineForm.subCategory === '研发成果' ? 0 : toYuan(defineForm.q1Target),
        q2Target: defineForm.subCategory === '研发成果' ? 0 : toYuan(defineForm.q2Target),
        q3Target: defineForm.subCategory === '研发成果' ? 0 : toYuan(defineForm.q3Target),
        q4Target: defineForm.subCategory === '研发成果' ? 0 : toYuan(defineForm.q4Target),
        q1Actual: defineForm.subCategory === '研发成果' ? 0 : toYuan(defineForm.q1Actual),
        q2Actual: defineForm.subCategory === '研发成果' ? 0 : toYuan(defineForm.q2Actual),
        q3Actual: defineForm.subCategory === '研发成果' ? 0 : toYuan(defineForm.q3Actual),
        q4Actual: defineForm.subCategory === '研发成果' ? 0 : toYuan(defineForm.q4Actual),
        owner: defineForm.owner
      }
      if (defineForm.id) {
        await targetApi.updateTarget(defineForm.id, payload)
      } else {
        await targetApi.createTarget(payload)
      }
      ElMessage.success('保存成功')
      defineDialogVisible.value = false
      loadStatistics()
      loadTableData()
      loadFilterOptions()
    } catch (error) {
      console.error('保存失败:', error)
      ElMessage.error('保存失败')
    } finally {
      defineLoading.value = false
    }
  })
}

// 格式化金额（千分位，两位小数）- 兼容直接调用和表格 formatter
const formatMoney = (...args) => {
  const num = args.length >= 3 ? args[2] : args[0]
  if (num === null || num === undefined || num === '') return '0.00'
  const val = typeof num === 'string' ? parseFloat(num) : num
  if (isNaN(val)) return '0.00'
  return val.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 格式化整数 - 兼容直接调用和表格 formatter
const formatInteger = (...args) => {
  const num = args.length >= 3 ? args[2] : args[0]
  if (num === null || num === undefined || num === '') return '0'
  const val = typeof num === 'string' ? parseInt(num, 10) : num
  if (isNaN(val)) return '0'
  return val.toLocaleString('zh-CN')
}

// 获取进度条颜色
const getProgressColor = (percentage) => {
  if (percentage >= 100) return '#67c23a'
  if (percentage >= 80) return '#409eff'
  if (percentage >= 60) return '#e6a23c'
  return '#f56c6c'
}

// 获取预算进度条颜色
const getBudgetProgressColor = (percentage) => {
  if (percentage <= 80) return '#67c23a'
  if (percentage <= 100) return '#e6a23c'
  return '#f56c6c'
}
</script>

<style scoped>
.target-statistics {
  padding: 0;
}

.filter-card {
  margin-bottom: 20px;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card-wrapper {
  height: 100%;
}

.stat-card {
  height: 100%;
}

.stat-header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #ebeef5;
}

.stat-title {
  font-size: 16px;
  font-weight: bold;
  margin-left: 10px;
  color: #303133;
}

.stat-body {
  padding: 0 10px;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.stat-item .label {
  color: #909399;
  font-size: 14px;
}

.stat-item .value {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.stat-item .value.highlight {
  color: #409eff;
}

.chart-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-container {
  width: 100%;
  height: 400px;
}

.table-card {
  margin-bottom: 20px;
}

.el-upload__tip {
  margin-top: 10px;
  color: #909399;
  font-size: 12px;
}

.filter-card .el-form-item {
  margin-bottom: 10px;
}

.filter-card .el-select {
  min-width: 120px;
  max-width: 300px;
}

.filter-card .el-select .el-input__wrapper {
  width: auto;
}
</style>
