<template>
  <div class="cost-tracker">
    <!-- ==================== Section 1: 部门-机构视角 ==================== -->
    <el-collapse v-model="activeCollapse" class="section-collapse">
      <el-collapse-item name="dept" class="collapse-item">
        <template #title>
          <span class="collapse-title">部门-机构视角</span>
        </template>

        <!-- 筛选 -->
        <div class="section-filters">
          <span class="filter-label">部门：</span>
          <el-select
            v-model="deptFilter.dept"
            placeholder="全部部门"
            clearable
            style="width: 200px; margin-right: 16px"
            @change="onDeptChange"
          >
            <el-option v-for="d in deptOptions" :key="d" :label="d" :value="d" />
          </el-select>
          <span class="filter-label">机构：</span>
          <el-select
            v-model="deptFilter.org"
            placeholder="全部机构"
            clearable
            style="width: 220px"
            @change="loadDeptMonthly"
          >
            <el-option v-for="o in filteredOrgs" :key="o" :label="o" :value="o" />
          </el-select>
        </div>

        <!-- 月度成本趋势柱状图 -->
        <el-card class="chart-card">
          <template #header><span>月度成本趋势（预算 vs 实际）</span></template>
          <v-chart :option="deptChartOption" style="height: 350px" :loading="deptChartLoading" />
        </el-card>

        <!-- 部门汇总表 -->
        <el-card>
          <template #header><span>部门成本汇总</span></template>
          <el-table
            :data="deptSummaryData"
            v-loading="deptLoading"
            stripe
            border
            max-height="50vh"
            show-summary
            :summary-method="deptSummaryFn"
          >
            <el-table-column prop="department" label="部门" min-width="140" show-overflow-tooltip sortable />
            <el-table-column prop="order_count" label="订单数" width="90" align="center" sortable />
            <el-table-column prop="project_count" label="项目数" width="90" align="center" sortable />
            <el-table-column label="年度预算(万元)" width="140" align="right" sort-by="annual_budget" sortable>
              <template #default="{ row }">{{ formatWan(row.annual_budget) }}</template>
            </el-table-column>
            <el-table-column label="实际成本合计(万元)" width="160" align="right" sort-by="total_actual_cost" sortable>
              <template #default="{ row }">
                <span :style="{ color: (row.total_actual_cost || 0) > (row.annual_budget || 0) ? '#f56c6c' : '#67c23a' }">
                  {{ formatWan(row.total_actual_cost) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="预估人天" width="110" align="right" sort-by="total_estimated_days" sortable>
              <template #default="{ row }">{{ formatNum(row.total_estimated_days) }}</template>
            </el-table-column>
            <el-table-column label="实际出勤人天" width="130" align="right" sort-by="total_attendance_days" sortable>
              <template #default="{ row }">{{ formatNum(row.total_attendance_days) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-collapse-item>

      <!-- ==================== Section 2: 产品-项目视角 ==================== -->
      <el-collapse-item name="product" class="collapse-item">
        <template #title>
          <span class="collapse-title">产品-项目视角</span>
        </template>

        <!-- 筛选 -->
        <div class="section-filters">
          <span class="filter-label">产品：</span>
          <el-select
            v-model="productFilter.productId"
            placeholder="全部产品"
            clearable
            style="width: 220px; margin-right: 16px"
            @change="onProductChange"
          >
            <el-option v-for="p in productOptions" :key="p" :label="p" :value="p" />
          </el-select>
          <span class="filter-label">项目：</span>
          <el-select
            v-model="productFilter.projectId"
            placeholder="全部项目"
            clearable
            style="width: 220px"
            @change="loadProductMonthly"
          >
            <el-option v-for="p in filteredProjects" :key="p" :label="p" :value="p" />
          </el-select>
        </div>

        <!-- 月度成本趋势柱状图 -->
        <el-card class="chart-card">
          <template #header><span>月度成本趋势（预算 vs 实际）</span></template>
          <v-chart :option="productChartOption" style="height: 350px" :loading="productChartLoading" />
        </el-card>

        <!-- 产品汇总表 -->
        <el-card>
          <template #header><span>产品成本汇总</span></template>
          <el-table
            :data="productSummaryData"
            v-loading="productLoading"
            stripe
            border
            max-height="50vh"
            show-summary
            :summary-method="productSummaryFn"
          >
            <el-table-column prop="product_id" label="产品ID" min-width="140" show-overflow-tooltip sortable />
            <el-table-column label="预算合计(万元)" width="140" align="right" sort-by="total_budget" sortable>
              <template #default="{ row }">{{ formatWan(row.total_budget) }}</template>
            </el-table-column>
            <el-table-column label="实际成本合计(万元)" width="160" align="right" sort-by="total_actual_cost" sortable>
              <template #default="{ row }">{{ formatWan(row.total_actual_cost) }}</template>
            </el-table-column>
            <el-table-column label="预估人天" width="110" align="right" sort-by="total_estimated_days" sortable>
              <template #default="{ row }">{{ formatNum(row.total_estimated_days) }}</template>
            </el-table-column>
            <el-table-column label="实际出勤人天" width="130" align="right" sort-by="total_attendance_days" sortable>
              <template #default="{ row }">{{ formatNum(row.total_attendance_days) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-collapse-item>
    </el-collapse>

    <!-- ==================== Section 3: 订单成本明细 ==================== -->
    <el-card style="margin-top: 16px">
      <template #header>
        <div class="card-header">
          <span>订单成本明细</span>
          <div class="header-filters">
            <el-input
              v-model="orderFilter.keyword"
              placeholder="搜索订单编号"
              clearable
              style="width: 200px; margin-right: 12px"
              @clear="onOrderSearch"
              @keyup.enter="onOrderSearch"
            >
              <template #append>
                <el-button :icon="Search" @click="onOrderSearch" />
              </template>
            </el-input>
            <el-select
              v-model="orderFilter.department"
              placeholder="所属部门"
              clearable
              style="width: 160px; margin-right: 12px"
              @change="onOrderFilterChange"
            >
              <el-option v-for="d in deptOptions" :key="d" :label="d" :value="d" />
            </el-select>
            <el-select
              v-model="orderFilter.productId"
              placeholder="产品"
              clearable
              style="width: 160px; margin-right: 12px"
              @change="onOrderFilterChange"
            >
              <el-option v-for="p in productOptions" :key="p" :label="p" :value="p" />
            </el-select>
            <el-select
              v-model="orderFilter.projectId"
              placeholder="项目"
              clearable
              style="width: 160px"
              @change="onOrderFilterChange"
            >
              <el-option v-for="p in allProjectOptions" :key="p" :label="p" :value="p" />
            </el-select>
          </div>
        </div>
      </template>

      <el-table
        :data="orderData"
        v-loading="orderLoading"
        stripe
        border
        max-height="55vh"
        show-summary
        :summary-method="orderSummaryFn"
      >
        <el-table-column prop="orderId" label="订单编号" width="150" show-overflow-tooltip sortable />
        <el-table-column prop="orderName" label="订单名称" min-width="180" show-overflow-tooltip sortable />
        <el-table-column prop="department" label="所属部门" width="130" show-overflow-tooltip sortable />
        <el-table-column prop="productId" label="产品ID" width="120" show-overflow-tooltip sortable />
        <el-table-column prop="projectId" label="项目编号" width="150" show-overflow-tooltip sortable />
        <el-table-column label="订单预算(万元)" width="140" align="right" sort-by="orderBudget" sortable>
          <template #default="{ row }">{{ formatWan(row.orderBudget) }}</template>
        </el-table-column>
        <el-table-column label="已入账成本(万元)" width="150" align="right" sort-by="bookedCost" sortable>
          <template #default="{ row }">{{ formatWan(row.bookedCost) }}</template>
        </el-table-column>
        <el-table-column label="未入账成本(万元)" width="150" align="right" sort-by="unbookedCost" sortable>
          <template #default="{ row }">{{ formatWan(row.unbookedCost) }}</template>
        </el-table-column>
        <el-table-column label="实际成本合计(万元)" width="160" align="right" sort-by="actualCostTotal" sortable>
          <template #default="{ row }">{{ formatWan(row.actualCostTotal) }}</template>
        </el-table-column>
        <el-table-column label="预估人天" width="100" align="right" sort-by="estimatedManDays" sortable>
          <template #default="{ row }">{{ formatNum(row.estimatedManDays) }}</template>
        </el-table-column>
        <el-table-column label="实际出勤人天" width="130" align="right" sort-by="actualAttendanceDays" sortable>
          <template #default="{ row }">{{ formatNum(row.actualAttendanceDays) }}</template>
        </el-table-column>
        <el-table-column label="实际绩效人天" width="130" align="right" sort-by="actualPerformanceDays" sortable>
          <template #default="{ row }">{{ formatNum(row.actualPerformanceDays) }}</template>
        </el-table-column>
        <el-table-column prop="executor" label="执行人" width="100" sortable />
        <el-table-column prop="responsible" label="责任人" width="100" sortable />
        <el-table-column prop="orderStatus" label="状态" width="100" sortable>
          <template #default="{ row }">
            <el-tag :type="row.orderStatus === '已完成' ? 'success' : 'info'" size="small">
              {{ row.orderStatus || '-' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="orderPagination.page"
          v-model:page-size="orderPagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="orderPagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="onOrderSizeChange"
          @current-change="onOrderPageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import axios from 'axios'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'

use([CanvasRenderer, BarChart, GridComponent, TooltipComponent, LegendComponent])

// ==================== 格式化工具 ====================
const formatWan = (v) => {
  if (v === null || v === undefined || v === '') return '0.00'
  return (Number(v) / 10000).toFixed(2)
}

const formatNum = (v) => {
  if (v === null || v === undefined || v === '') return '0.0'
  return Number(v).toFixed(1)
}

// ==================== 月份标签 ====================
const monthLabels = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']

// ==================== 折叠面板 ====================
const activeCollapse = ref(['dept'])

// ==================== 全局下拉选项 ====================
const deptOptions = ref([])
const productOptions = ref([])
const allProjectOptions = ref([])

// 部门-机构映射 { department: [orgs] }
const deptOrgMap = ref({})
// 产品-项目映射 { productId: [projectIds] }
const productProjectMap = ref({})

// ==================== Section 1: 部门-机构视角 ====================
const deptFilter = reactive({
  dept: '',
  org: ''
})

const deptLoading = ref(false)
const deptChartLoading = ref(false)
const deptSummaryData = ref([])
const deptMonthlyData = ref([])   // API 返回的 monthly 数组
const deptAnnualBudget = ref(0)
const deptTotalActual = ref(0)

// 根据所选部门过滤机构
const filteredOrgs = computed(() => {
  if (!deptFilter.dept) {
    // 未选部门时显示所有机构
    const all = []
    Object.values(deptOrgMap.value).forEach(arr => {
      arr.forEach(o => { if (!all.includes(o)) all.push(o) })
    })
    return all
  }
  return deptOrgMap.value[deptFilter.dept] || []
})

// 部门月度成本柱状图
const deptChartOption = computed(() => {
  // 构建 12 个月份的预算和实际数据
  const monthlyMap = {}
  deptMonthlyData.value.forEach(item => {
    monthlyMap[item.month] = item
  })

  // 年度预算平分到12个月
  const monthlyBudget = deptAnnualBudget.value / 12

  const budgets = []
  const actuals = []
  for (let m = 1; m <= 12; m++) {
    budgets.push(monthlyBudget)
    actuals.push(monthlyMap[m] ? (monthlyMap[m].actual || 0) : 0)
  }

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        let html = params[0].axisValue + '<br/>'
        params.forEach(p => {
          html += `${p.marker}${p.seriesName}: ${formatWan(p.value)} 万元<br/>`
        })
        return html
      }
    },
    legend: { data: ['预算', '实际成本'], top: 5 },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true, top: 40 },
    xAxis: {
      type: 'category',
      data: monthLabels,
      axisLabel: { fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      name: '万元',
      axisLabel: {
        formatter: (v) => {
          if (v >= 10000) return (v / 10000).toFixed(0) + '万'
          return (v / 10000).toFixed(2)
        }
      }
    },
    series: [
      {
        name: '预算',
        type: 'bar',
        data: budgets,
        itemStyle: { color: '#a0cfff' },
        barWidth: '35%'
      },
      {
        name: '实际成本',
        type: 'bar',
        data: actuals,
        itemStyle: { color: '#f56c6c' },
        barWidth: '35%'
      }
    ]
  }
})

// 部门汇总
const deptSummaryFn = ({ columns, data }) => {
  const sums = []
  columns.forEach((col, idx) => {
    if (idx === 0) {
      sums[idx] = '合计'
    } else if (col.label === '订单数') {
      sums[idx] = data.reduce((s, r) => s + (Number(r.order_count) || 0), 0)
    } else if (col.label === '项目数') {
      sums[idx] = data.reduce((s, r) => s + (Number(r.project_count) || 0), 0)
    } else if (col.label === '年度预算(万元)') {
      sums[idx] = formatWan(data.reduce((s, r) => s + (Number(r.annual_budget) || 0), 0))
    } else if (col.label === '实际成本合计(万元)') {
      sums[idx] = formatWan(data.reduce((s, r) => s + (Number(r.total_actual_cost) || 0), 0))
    } else if (col.label === '预估人天') {
      sums[idx] = formatNum(data.reduce((s, r) => s + (Number(r.total_estimated_days) || 0), 0))
    } else if (col.label === '实际出勤人天') {
      sums[idx] = formatNum(data.reduce((s, r) => s + (Number(r.total_attendance_days) || 0), 0))
    } else {
      sums[idx] = ''
    }
  })
  return sums
}

// ==================== Section 2: 产品-项目视角 ====================
const productFilter = reactive({
  productId: '',
  projectId: ''
})

const productLoading = ref(false)
const productChartLoading = ref(false)
const productSummaryData = ref([])
const productMonthlyData = ref([])
const productTotalBudget = ref(0)
const productTotalActual = ref(0)

// 根据所选产品过滤项目
const filteredProjects = computed(() => {
  if (!productFilter.productId) {
    const all = []
    Object.values(productProjectMap.value).forEach(arr => {
      arr.forEach(p => { if (!all.includes(p)) all.push(p) })
    })
    return all
  }
  return productProjectMap.value[productFilter.productId] || []
})

// 产品月度成本柱状图
const productChartOption = computed(() => {
  const monthlyMap = {}
  productMonthlyData.value.forEach(item => {
    monthlyMap[item.month] = item
  })

  const budgets = []
  const actuals = []
  for (let m = 1; m <= 12; m++) {
    budgets.push(monthlyMap[m] ? (monthlyMap[m].budget || 0) : 0)
    actuals.push(monthlyMap[m] ? (monthlyMap[m].actual || 0) : 0)
  }

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        let html = params[0].axisValue + '<br/>'
        params.forEach(p => {
          html += `${p.marker}${p.seriesName}: ${formatWan(p.value)} 万元<br/>`
        })
        return html
      }
    },
    legend: { data: ['预算', '实际成本'], top: 5 },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true, top: 40 },
    xAxis: {
      type: 'category',
      data: monthLabels,
      axisLabel: { fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      name: '万元',
      axisLabel: {
        formatter: (v) => {
          if (v >= 10000) return (v / 10000).toFixed(0) + '万'
          return (v / 10000).toFixed(2)
        }
      }
    },
    series: [
      {
        name: '预算',
        type: 'bar',
        data: budgets,
        itemStyle: { color: '#a0cfff' },
        barWidth: '35%'
      },
      {
        name: '实际成本',
        type: 'bar',
        data: actuals,
        itemStyle: { color: '#f56c6c' },
        barWidth: '35%'
      }
    ]
  }
})

// 产品汇总
const productSummaryFn = ({ columns, data }) => {
  const sums = []
  columns.forEach((col, idx) => {
    if (idx === 0) {
      sums[idx] = '合计'
    } else if (col.label === '预算合计(万元)') {
      sums[idx] = formatWan(data.reduce((s, r) => s + (Number(r.total_budget) || 0), 0))
    } else if (col.label === '实际成本合计(万元)') {
      sums[idx] = formatWan(data.reduce((s, r) => s + (Number(r.total_actual_cost) || 0), 0))
    } else if (col.label === '预估人天') {
      sums[idx] = formatNum(data.reduce((s, r) => s + (Number(r.total_estimated_days) || 0), 0))
    } else if (col.label === '实际出勤人天') {
      sums[idx] = formatNum(data.reduce((s, r) => s + (Number(r.total_attendance_days) || 0), 0))
    } else {
      sums[idx] = ''
    }
  })
  return sums
}

// ==================== Section 3: 订单成本明细 ====================
const orderFilter = reactive({
  keyword: '',
  department: '',
  productId: '',
  projectId: ''
})

const orderLoading = ref(false)
const orderData = ref([])
const orderPagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

// 订单汇总
const orderSummaryFn = ({ columns, data }) => {
  const sums = []
  columns.forEach((col, idx) => {
    if (idx === 0) {
      sums[idx] = '合计'
    } else if (col.label === '订单预算(万元)') {
      sums[idx] = formatWan(data.reduce((s, r) => s + (Number(r.orderBudget) || 0), 0))
    } else if (col.label === '已入账成本(万元)') {
      sums[idx] = formatWan(data.reduce((s, r) => s + (Number(r.bookedCost) || 0), 0))
    } else if (col.label === '未入账成本(万元)') {
      sums[idx] = formatWan(data.reduce((s, r) => s + (Number(r.unbookedCost) || 0), 0))
    } else if (col.label === '实际成本合计(万元)') {
      sums[idx] = formatWan(data.reduce((s, r) => s + (Number(r.actualCostTotal) || 0), 0))
    } else if (col.label === '预估人天') {
      sums[idx] = formatNum(data.reduce((s, r) => s + (Number(r.estimatedManDays) || 0), 0))
    } else if (col.label === '实际出勤人天') {
      sums[idx] = formatNum(data.reduce((s, r) => s + (Number(r.actualAttendanceDays) || 0), 0))
    } else if (col.label === '实际绩效人天') {
      sums[idx] = formatNum(data.reduce((s, r) => s + (Number(r.actualPerformanceDays) || 0), 0))
    } else {
      sums[idx] = ''
    }
  })
  return sums
}

// ==================== 数据加载 ====================

// 加载部门-机构映射
const loadDeptOrgMap = async () => {
  try {
    const res = await axios.get('/api/cost/department-org-map')
    const data = res.data || []
    const map = {}
    const depts = new Set()
    data.forEach(item => {
      depts.add(item.department)
      if (!map[item.department]) {
        map[item.department] = []
      }
      if (item.organization && !map[item.department].includes(item.organization)) {
        map[item.department].push(item.organization)
      }
    })
    deptOrgMap.value = map
    deptOptions.value = [...depts].sort()
  } catch (e) {
    console.error('加载部门-机构映射失败', e)
  }
}

// 加载产品-项目映射
const loadProductProjectMap = async () => {
  try {
    const res = await axios.get('/api/cost/product-project-map')
    const data = res.data || []
    const map = {}
    const products = new Set()
    const allProjects = new Set()
    data.forEach(item => {
      products.add(item.productId)
      allProjects.add(item.projectId)
      if (!map[item.productId]) {
        map[item.productId] = []
      }
      if (item.projectId && !map[item.productId].includes(item.projectId)) {
        map[item.productId].push(item.projectId)
      }
    })
    productProjectMap.value = map
    productOptions.value = [...products].sort()
    allProjectOptions.value = [...allProjects].sort()
  } catch (e) {
    console.error('加载产品-项目映射失败', e)
  }
}

// Section 1: 加载部门月度数据
const loadDeptMonthly = async () => {
  deptLoading.value = true
  deptChartLoading.value = true
  try {
    const params = {}
    if (deptFilter.dept) params.department = deptFilter.dept
    const res = await axios.get('/api/cost/department-monthly', { params })
    const responseData = res.data || {}
    deptMonthlyData.value = responseData.monthly || []
    deptAnnualBudget.value = responseData.annualBudget || 0
    deptTotalActual.value = responseData.totalActual || 0
    deptSummaryData.value = responseData.summary || []
  } catch (e) {
    ElMessage.error('加载部门月度数据失败')
    deptMonthlyData.value = []
    deptSummaryData.value = []
  } finally {
    deptLoading.value = false
    deptChartLoading.value = false
  }
}

// 部门变更时重新加载
const onDeptChange = () => {
  deptFilter.org = ''
  loadDeptMonthly()
}

// Section 2: 加载产品月度数据
const loadProductMonthly = async () => {
  productLoading.value = true
  productChartLoading.value = true
  try {
    const params = {}
    if (productFilter.productId) params.productId = productFilter.productId
    const res = await axios.get('/api/cost/product-monthly', { params })
    const responseData = res.data || {}
    productMonthlyData.value = responseData.monthly || []
    productTotalBudget.value = responseData.totalBudget || 0
    productTotalActual.value = responseData.totalActual || 0
    productSummaryData.value = responseData.summary || []
  } catch (e) {
    ElMessage.error('加载产品月度数据失败')
    productMonthlyData.value = []
    productSummaryData.value = []
  } finally {
    productLoading.value = false
    productChartLoading.value = false
  }
}

// 产品变更时重新加载
const onProductChange = () => {
  productFilter.projectId = ''
  loadProductMonthly()
}

// Section 3: 加载订单明细
const loadOrderDetails = async () => {
  orderLoading.value = true
  try {
    const params = {
      page: orderPagination.page,
      pageSize: orderPagination.pageSize
    }
    if (orderFilter.keyword) params.keyword = orderFilter.keyword
    if (orderFilter.department) params.department = orderFilter.department
    if (orderFilter.productId) params.productId = orderFilter.productId
    if (orderFilter.projectId) params.projectId = orderFilter.projectId
    const res = await axios.get('/api/cost/order-details', { params })
    const responseData = res.data || {}
    orderData.value = responseData.items || []
    orderPagination.total = responseData.total || 0
    orderPagination.page = responseData.page || 1
    orderPagination.pageSize = responseData.pageSize || 20
  } catch (e) {
    ElMessage.error('加载订单明细失败')
    orderData.value = []
  } finally {
    orderLoading.value = false
  }
}

// 搜索
const onOrderSearch = () => {
  orderPagination.page = 1
  loadOrderDetails()
}

// 筛选变更
const onOrderFilterChange = () => {
  orderPagination.page = 1
  loadOrderDetails()
}

// 分页
const onOrderPageChange = (page) => {
  orderPagination.page = page
  loadOrderDetails()
}

const onOrderSizeChange = (size) => {
  orderPagination.pageSize = size
  orderPagination.page = 1
  loadOrderDetails()
}

// ==================== 初始化 ====================
onMounted(async () => {
  await Promise.all([loadDeptOrgMap(), loadProductProjectMap()])
  loadDeptMonthly()
  loadProductMonthly()
  loadOrderDetails()
})
</script>

<style scoped>
.cost-tracker {
  padding: 0;
}

.section-collapse {
  border: none;
}

.section-collapse :deep(.el-collapse-item__header) {
  background: #f5f7fa;
  padding: 0 16px;
  height: 48px;
  font-size: 15px;
  font-weight: 600;
  border-bottom: 1px solid #e4e7ed;
  border-radius: 4px;
}

.section-collapse :deep(.el-collapse-item__wrap) {
  border: none;
  padding-bottom: 8px;
}

.collapse-item {
  margin-bottom: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  overflow: hidden;
}

.collapse-title {
  color: #303133;
}

.section-filters {
  display: flex;
  align-items: center;
  padding: 12px 0;
}

.filter-label {
  font-size: 13px;
  color: #606266;
  margin-right: 8px;
  white-space: nowrap;
}

.chart-card {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.header-filters {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
