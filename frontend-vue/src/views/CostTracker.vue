<template>
  <div class="cost-tracker">
    <!-- 部门预算 vs 实际 柱状图 -->
    <el-card class="chart-card">
      <template #header>
        <div class="card-header">
          <span>部门预算 vs 实际支出</span>
        </div>
      </template>
      <v-chart :option="budgetBarOption" style="height:350px" :loading="chartLoading" />
    </el-card>

    <!-- 部门预算汇总表 -->
    <el-card>
      <template #header>
        <div class="card-header">
          <span>部门预算执行情况</span>
        </div>
      </template>
      <el-table
        :data="budgetData"
        v-loading="budgetLoading"
        stripe
        border
        style="width:100%;font-size:13px"
        max-height="65vh"
      >
        <el-table-column prop="dept_name" label="部门名称" width="160" show-overflow-tooltip sortable />
        <el-table-column prop="dept_head" label="部门负责人" width="120" sortable />
        <el-table-column label="预算(万元)" width="130" align="right" prop="budget_total" sortable>
          <template #default="{ row }">
            {{ formatWan(row.budget) }}
          </template>
        </el-table-column>
        <el-table-column label="实际支出(万元)" width="140" align="right" prop="actual_cost_total" sortable>
          <template #default="{ row }">
            {{ formatWan(row.actual) }}
          </template>
        </el-table-column>
        <el-table-column label="差额(万元)" width="130" align="right" sort-by="diff" sortable>
          <template #default="{ row }">
            <span :style="{ color: (row.diff || 0) > 0 ? '#f56c6c' : '#67c23a' }">
              {{ formatWan(row.diff) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="执行率(%)" width="110" align="right" sort-by="execute_rate" sortable>
          <template #default="{ row }">
            <el-progress
              :percentage="Math.min(Number(row.execute_rate) || 0, 100)"
              :color="getProgressColor(row.execute_rate)"
              :stroke-width="8"
            />
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 含成本的确权收入明细 -->
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>收入成本明细</span>
        </div>
      </template>
      <el-table
        :data="revenueWithCostData"
        v-loading="revenueLoading"
        stripe
        border
        style="width:100%;font-size:13px"
        max-height="65vh"
        show-summary
        :summary-method="revenueCostSummary"
      >
        <el-table-column prop="order_no" label="订单编号" width="150" show-overflow-tooltip sortable />
        <el-table-column prop="contract_name" label="合同名称" width="180" show-overflow-tooltip sortable />
        <el-table-column label="确权收入(万元)" width="140" align="right" prop="revenue_amount_wan" sortable>
          <template #default="{ row }">
            {{ formatWan(row.revenue_amount_wan) }}
          </template>
        </el-table-column>
        <el-table-column label="结转成本(万元)" width="140" align="right" prop="cost_carryover" sortable>
          <template #default="{ row }">
            {{ formatWan(row.carry_over_cost) }}
          </template>
        </el-table-column>
        <el-table-column label="交付毛利(万元)" width="140" align="right" prop="delivery_margin" sortable>
          <template #default="{ row }">
            {{ formatWan(row.delivery_margin) }}
          </template>
        </el-table-column>
        <el-table-column prop="recognition_month" label="确权月份" width="120" sortable />
        <el-table-column prop="org_unit" label="经营机构" width="150" show-overflow-tooltip sortable />
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
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'

use([CanvasRenderer, BarChart, GridComponent, TooltipComponent, LegendComponent])

const chartLoading = ref(false)
const budgetLoading = ref(false)
const revenueLoading = ref(false)
const budgetData = ref([])
const revenueWithCostData = ref([])

const formatWan = (val) => {
  if (val === null || val === undefined || val === '') return '0.00'
  return (Number(val) / 10000).toFixed(2)
}

// 预算 vs 实际柱状图
const budgetBarOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'shadow' },
    formatter: (params) => {
      let html = params[0]?.axisValue || ''
      params.forEach(p => {
        html += `<br/>${p.seriesName}: ${Number(p.value).toFixed(2)} 万元`
      })
      return html
    }
  },
  legend: { data: ['预算', '实际支出'], top: 5 },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true, top: 40 },
  xAxis: {
    type: 'category',
    data: budgetData.value.map(d => d.dept_name || ''),
    axisLabel: { rotate: 25, fontSize: 11 }
  },
  yAxis: {
    type: 'value',
    name: '万元',
    axisLabel: { formatter: (v) => (v / 10000).toFixed(0) + '万' }
  },
  series: [
    {
      name: '预算',
      type: 'bar',
      data: budgetData.value.map(d => d.budget || 0),
      itemStyle: { color: '#a0cfff', borderRadius: [4, 4, 0, 0] },
      barWidth: '35%',
      label: { show: true, position: 'top', formatter: (p) => p.value > 0 ? Number(p.value).toFixed(2) : '', fontSize: 10 }
    },
    {
      name: '实际支出',
      type: 'bar',
      data: budgetData.value.map(d => d.actual || 0),
      itemStyle: { color: '#f56c6c', borderRadius: [4, 4, 0, 0] },
      barWidth: '35%',
      label: { show: true, position: 'top', formatter: (p) => p.value > 0 ? Number(p.value).toFixed(2) : '', fontSize: 10 }
    }
  ]
}))

const getProgressColor = (rate) => {
  const r = Number(rate) || 0
  if (r > 100) return '#f56c6c'
  if (r >= 90) return '#e6a23c'
  if (r >= 70) return '#409eff'
  return '#67c23a'
}

// 收入成本汇总
const revenueCostSummary = ({ columns, data }) => {
  const sums = []
  const revTotal = data.reduce((acc, r) => acc + (Number(r.revenue_amount_wan) || 0), 0)
  const costTotal = data.reduce((acc, r) => acc + (Number(r.carry_over_cost) || 0), 0)
  const marginTotal = data.reduce((acc, r) => acc + (Number(r.delivery_margin) || 0), 0)

  columns.forEach((col, idx) => {
    if (idx === 0) {
      sums[idx] = '合计'
    } else if (col.label === '确权收入(万元)') {
      sums[idx] = formatWan(revTotal)
    } else if (col.label === '结转成本(万元)') {
      sums[idx] = formatWan(costTotal)
    } else if (col.label === '交付毛利(万元)') {
      sums[idx] = formatWan(marginTotal)
    } else {
      sums[idx] = ''
    }
  })
  return sums
}

const loadBudget = async () => {
  budgetLoading.value = true
  chartLoading.value = true
  try {
    const res = await axios.get('/api/dashboard/department-budget')
    budgetData.value = res.data || []
  } catch (e) {
    ElMessage.error('加载部门预算数据失败')
  } finally {
    budgetLoading.value = false
    chartLoading.value = false
  }
}

const loadRevenueWithCost = async () => {
  revenueLoading.value = true
  try {
    const res = await axios.get('/api/dashboard/table/revenue-details')
    const all = res.data || []
    // 仅显示有收入且有成本的记录
    revenueWithCostData.value = all.filter(
      r => (Number(r.revenue_amount_wan) || 0) > 0 || (Number(r.carry_over_cost) || 0) > 0
    )
  } catch (e) {
    ElMessage.error('加载收入成本明细失败')
  } finally {
    revenueLoading.value = false
  }
}

onMounted(() => {
  loadBudget()
  loadRevenueWithCost()
})
</script>

<style scoped>
.cost-tracker {
  padding: 0;
}

.chart-card {
  margin-bottom: 20px;
}

.table-card {
  margin-top: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
