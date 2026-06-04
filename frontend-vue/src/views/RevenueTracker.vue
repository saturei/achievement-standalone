<template>
  <div class="revenue-tracker">
    <!-- 月度收入趋势图 -->
    <el-card class="chart-card">
      <template #header>
        <div class="card-header">
          <span>月度确权收入趋势</span>
        </div>
      </template>
      <v-chart :option="trendOption" style="height:320px" :loading="chartLoading" />
    </el-card>

    <!-- 收入明细表格 -->
    <el-card>
      <template #header>
        <div class="card-header">
          <span>确权收入明细</span>
          <div class="header-filters">
            <el-select v-model="filterMonth" placeholder="确权认定月份" clearable style="width:180px;margin-right:12px" @change="loadData">
              <el-option v-for="m in months" :key="m" :label="m" :value="m" />
            </el-select>
            <el-select v-model="filterOrgUnit" placeholder="经营机构" clearable filterable style="width:200px" @change="loadData">
              <el-option v-for="o in orgUnits" :key="o" :label="o" :value="o" />
            </el-select>
          </div>
        </div>
      </template>

      <el-table
        :data="tableData"
        v-loading="loading"
        stripe
        border
        style="width:100%;font-size:13px"
        max-height="65vh"
        show-summary
        :summary-method="getSummary"
      >
        <el-table-column prop="order_no" label="订单编号" width="150" show-overflow-tooltip sortable />
        <el-table-column prop="contract_no" label="订单所属合同" width="150" show-overflow-tooltip sortable />
        <el-table-column prop="contract_name" label="合同名称" width="180" show-overflow-tooltip sortable />
        <el-table-column prop="customer" label="客户" width="160" show-overflow-tooltip sortable />
        <el-table-column label="确权收入(万元)" width="130" align="right" prop="revenue_amount_wan" sortable>
          <template #default="{ row }">
            {{ formatWan(row.revenue_amount_wan) }}
          </template>
        </el-table-column>
        <el-table-column label="确认收入" width="120" align="right" prop="confirmed_revenue" sortable>
          <template #default="{ row }">
            {{ formatWan(row.confirmed_revenue) }}
          </template>
        </el-table-column>
        <el-table-column label="结转成本" width="120" align="right" prop="cost_carryover" sortable>
          <template #default="{ row }">
            {{ formatWan(row.carry_over_cost) }}
          </template>
        </el-table-column>
        <el-table-column label="交付毛利" width="120" align="right" prop="delivery_margin" sortable>
          <template #default="{ row }">
            {{ formatWan(row.delivery_margin) }}
          </template>
        </el-table-column>
        <el-table-column label="确权认定时间" width="130" prop="recognition_month" sortable>
          <template #default="{ row }">
            {{ formatDate(row.recognition_time) }}
          </template>
        </el-table-column>
        <el-table-column prop="profit_loss_subject" label="损益科目" width="120" show-overflow-tooltip sortable />
        <el-table-column prop="org_unit" label="经营机构" width="140" show-overflow-tooltip sortable />
        <el-table-column prop="accounting_type" label="核算类型" width="100" sortable />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent])

const loading = ref(false)
const chartLoading = ref(false)
const filterMonth = ref('')
const filterOrgUnit = ref('')
const tableData = ref([])
const months = ref([])
const orgUnits = ref([])
const revenueMonthlyData = ref([])

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

// 月度趋势图
const trendOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    formatter: (params) => {
      const p = params[0]
      return `${p.axisValue}<br/>确权收入(万元): ${Number(p.value).toFixed(2)}`
    }
  },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true, top: 10 },
  xAxis: {
    type: 'category',
    data: revenueMonthlyData.value.map(d => d.recognition_month || ''),
    boundaryGap: false
  },
  yAxis: {
    type: 'value',
    name: '万元',
    axisLabel: { formatter: (v) => (v / 10000).toFixed(0) + '万' }
  },
  series: [{
    type: 'line',
    data: revenueMonthlyData.value.map(d => d.amount || 0),
    smooth: true,
    symbol: 'circle',
    symbolSize: 6,
    lineStyle: { color: '#409eff', width: 2.5 },
    itemStyle: { color: '#409eff' },
    areaStyle: {
      color: {
        type: 'linear',
        x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [
          { offset: 0, color: 'rgba(64,158,255,0.25)' },
          { offset: 1, color: 'rgba(64,158,255,0.03)' }
        ]
      }
    }
  }]
}))

// 汇总行
const getSummary = ({ columns, data }) => {
  const sums = []
  const revTotal = data.reduce((acc, r) => acc + (Number(r.revenue_amount_wan) || 0), 0)
  const confirmedTotal = data.reduce((acc, r) => acc + (Number(r.confirmed_revenue) || 0), 0)
  const costTotal = data.reduce((acc, r) => acc + (Number(r.carry_over_cost) || 0), 0)
  const marginTotal = data.reduce((acc, r) => acc + (Number(r.delivery_margin) || 0), 0)

  columns.forEach((col, idx) => {
    if (idx === 0) {
      sums[idx] = '合计'
    } else if (col.label === '确权收入(万元)') {
      sums[idx] = formatWan(revTotal)
    } else if (col.label === '确认收入') {
      sums[idx] = formatWan(confirmedTotal)
    } else if (col.label === '结转成本') {
      sums[idx] = formatWan(costTotal)
    } else if (col.label === '交付毛利') {
      sums[idx] = formatWan(marginTotal)
    } else {
      sums[idx] = ''
    }
  })
  return sums
}

const loadFilterOptions = async () => {
  try {
    const res = await axios.get('/api/dashboard/filter-options')
    const data = res.data || {}
    orgUnits.value = (data.orgUnits || []).map(o => o.org_unit)
    months.value = (data.recognitionMonths || []).map(m => m.recognition_month)
  } catch (e) {
    ElMessage.error('加载筛选选项失败')
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const params = {}
    if (filterMonth.value) params.month = filterMonth.value
    if (filterOrgUnit.value) params.orgUnit = filterOrgUnit.value
    const res = await axios.get('/api/dashboard/table/revenue-details', { params })
    tableData.value = res.data || []
  } catch (e) {
    ElMessage.error('加载确权收入数据失败')
  } finally {
    loading.value = false
  }
}

const loadRevenueMonthly = async () => {
  chartLoading.value = true
  try {
    const res = await axios.get('/api/dashboard/revenue-monthly')
    revenueMonthlyData.value = res.data || []
  } catch (e) {
    ElMessage.error('加载月度收入趋势失败')
  } finally {
    chartLoading.value = false
  }
}

onMounted(async () => {
  await loadFilterOptions()
  loadData()
  loadRevenueMonthly()
})
</script>

<style scoped>
.revenue-tracker {
  padding: 0;
}

.chart-card {
  margin-bottom: 20px;
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
