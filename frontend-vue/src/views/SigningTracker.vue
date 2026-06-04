<template>
  <div class="signing-tracker">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>签约跟踪</span>
          <div class="header-filters">
            <el-select v-model="filterDepartment" placeholder="所属部门" clearable filterable style="width:180px;margin-right:12px" @change="handleFilterChange">
              <el-option v-for="d in departments" :key="d" :label="d" :value="d" />
            </el-select>
            <el-select v-model="filterRiskLevel" placeholder="签约风险等级" clearable style="width:140px;margin-right:12px" @change="handleFilterChange">
              <el-option v-for="r in riskLevels" :key="r" :label="r" :value="r" />
            </el-select>
            <el-select v-model="filterQuarter" placeholder="签约季度" clearable style="width:140px" @change="handleFilterChange">
              <el-option v-for="q in quarters" :key="q" :label="q" :value="q" />
            </el-select>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- 合同签约 -->
        <el-tab-pane label="合同签约" name="contracts">
          <el-table
            :data="contractsData"
            v-loading="contractsLoading"
            stripe
            border
            style="width:100%;font-size:13px"
            max-height="65vh"
            show-summary
            :summary-method="contractsSummary"
          >
            <el-table-column prop="contract_no" label="合同编号" width="150" show-overflow-tooltip sortable />
            <el-table-column prop="customer_name" label="客户名称" width="180" show-overflow-tooltip sortable />
            <el-table-column prop="contract_name" label="合同名称" width="200" show-overflow-tooltip sortable />
            <el-table-column label="签约金额(万元)" width="140" align="right" prop="signing_amount_wan" sortable>
              <template #default="{ row }">
                {{ formatWan(row.signing_amount_wan) }}
              </template>
            </el-table-column>
            <el-table-column prop="accounting_type" label="核算类型" width="100" sortable />
            <el-table-column prop="signing_risk_level" label="签约风险等级" width="120" align="center" sortable>
              <template #default="{ row }">
                <el-tag
                  :type="row.signing_risk_level === '高' ? 'danger' : row.signing_risk_level === '中' ? 'warning' : 'success'"
                  size="small"
                >
                  {{ row.signing_risk_level || '-' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="operator" label="经营岗" width="100" sortable />
            <el-table-column prop="department" label="所属部门" width="130" show-overflow-tooltip sortable />
            <el-table-column prop="signing_quarter" label="签约季度" width="110" sortable />
            <el-table-column label="签约日期" width="120" prop="order_create_date" sortable>
              <template #default="{ row }">
                {{ formatDate(row.signing_date) }}
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 派单签约 -->
        <el-tab-pane label="派单签约" name="orders">
          <el-table
            :data="ordersData"
            v-loading="ordersLoading"
            stripe
            border
            style="width:100%;font-size:13px"
            max-height="65vh"
            show-summary
            :summary-method="ordersSummary"
          >
            <el-table-column prop="order_no" label="订单编号" width="150" show-overflow-tooltip sortable />
            <el-table-column prop="contract_no" label="合同编号" width="150" show-overflow-tooltip sortable />
            <el-table-column prop="customer" label="客户" width="180" show-overflow-tooltip sortable />
            <el-table-column label="派单金额(万元)" width="140" align="right" prop="order_amount_wan" sortable>
              <template #default="{ row }">
                {{ formatWan(row.order_amount_wan) }}
              </template>
            </el-table-column>
            <el-table-column label="计划确权金额" width="140" align="right" prop="planned_recognition_amount" sortable>
              <template #default="{ row }">
                {{ formatWan(row.planned_recognition_amount) }}
              </template>
            </el-table-column>
            <el-table-column prop="accounting_type" label="核算类型" width="100" sortable />
            <el-table-column prop="operator" label="经营岗" width="100" sortable />
            <el-table-column prop="department" label="所属部门" width="130" show-overflow-tooltip sortable />
            <el-table-column prop="signing_quarter" label="签约季度" width="110" sortable />
            <el-table-column label="签约日期" width="120" prop="order_create_date" sortable>
              <template #default="{ row }">
                {{ formatDate(row.order_create_date) }}
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const activeTab = ref('contracts')

// 筛选
const filterDepartment = ref('')
const filterRiskLevel = ref('')
const filterQuarter = ref('')

const departments = ref([])
const riskLevels = ref([])
const quarters = ref([])

// 合同签约
const contractsData = ref([])
const contractsLoading = ref(false)

// 派单签约
const ordersData = ref([])
const ordersLoading = ref(false)

const formatWan = (val) => {
  if (val === null || val === undefined || val === '') return '0.00'
  return (Number(val) / 10000).toFixed(2)
}

const formatDate = (val) => {
  if (!val) return '-'
  const n = Number(val)
  const d = isNaN(n) ? new Date(val) : new Date(n)
  if (isNaN(d.getTime())) return String(val)
  return d.toISOString().slice(0, 10)
}

// 合同签约汇总
const contractsSummary = ({ columns, data }) => {
  const sums = []
  columns.forEach((col, idx) => {
    if (idx === 0) {
      sums[idx] = '合计'
    } else if (col.property === 'signing_amount_wan') {
      const total = data.reduce((acc, r) => acc + (Number(r.signing_amount_wan) || 0), 0)
      sums[idx] = formatWan(total)
    } else {
      sums[idx] = ''
    }
  })
  return sums
}

// 派单签约汇总
const ordersSummary = ({ columns, data }) => {
  const sums = []
  columns.forEach((col, idx) => {
    if (idx === 0) {
      sums[idx] = '合计'
    } else if (col.property === 'order_amount_wan') {
      const total = data.reduce((acc, r) => acc + (Number(r.order_amount_wan) || 0), 0)
      sums[idx] = formatWan(total)
    } else if (col.property === 'planned_recognition_amount') {
      const total = data.reduce((acc, r) => acc + (Number(r.planned_recognition_amount) || 0), 0)
      sums[idx] = formatWan(total)
    } else {
      sums[idx] = ''
    }
  })
  return sums
}

// 加载筛选选项
const loadFilterOptions = async () => {
  try {
    const res = await axios.get('/api/dashboard/filter-options')
    const data = res.data || {}
    departments.value = (data.departments || []).map(d => d.department)
    riskLevels.value = (data.riskLevels || []).map(r => r.signing_risk_level)
    quarters.value = (data.quarters || []).map(q => q.signing_quarter)
  } catch (e) {
    ElMessage.error('加载筛选选项失败')
  }
}

// 加载合同签约
const loadContracts = async () => {
  contractsLoading.value = true
  try {
    const params = {}
    if (filterDepartment.value) params.department = filterDepartment.value
    if (filterRiskLevel.value) params.riskLevel = filterRiskLevel.value
    if (filterQuarter.value) params.quarter = filterQuarter.value
    const res = await axios.get('/api/dashboard/table/signing-contracts', { params })
    contractsData.value = res.data || []
  } catch (e) {
    ElMessage.error('加载合同签约数据失败')
  } finally {
    contractsLoading.value = false
  }
}

// 加载派单签约
const loadOrders = async () => {
  ordersLoading.value = true
  try {
    const params = {}
    if (filterDepartment.value) params.department = filterDepartment.value
    if (filterQuarter.value) params.quarter = filterQuarter.value
    const res = await axios.get('/api/dashboard/table/signing-orders', { params })
    ordersData.value = res.data || []
  } catch (e) {
    ElMessage.error('加载派单签约数据失败')
  } finally {
    ordersLoading.value = false
  }
}

const handleFilterChange = () => {
  if (activeTab.value === 'contracts') {
    loadContracts()
  } else {
    loadOrders()
  }
}

const handleTabChange = (tab) => {
  if (tab === 'contracts') {
    loadContracts()
  } else {
    loadOrders()
  }
}

onMounted(async () => {
  await loadFilterOptions()
  loadContracts()
})
</script>

<style scoped>
.signing-tracker {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-filters {
  display: flex;
  align-items: center;
}
</style>
