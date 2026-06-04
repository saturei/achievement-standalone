<template>
  <div class="dashboard-page">
    <!-- KPI 卡片行 -->
    <el-row :gutter="20" class="kpi-row">
      <el-col :span="6">
        <el-card shadow="hover" v-loading="kpiLoading">
          <div class="kpi-card">
            <div class="kpi-label">签约总额(万元)</div>
            <div class="kpi-value">{{ formatWan(kpiData.signingTotal) }}</div>
            <div class="kpi-sub">合同签约 + 派单签约</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" v-loading="kpiLoading">
          <div class="kpi-card">
            <div class="kpi-label">确权收入(万元)</div>
            <div class="kpi-value">{{ formatWan(kpiData.revenueTotal) }}</div>
            <div class="kpi-sub">累计确权收入</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" v-loading="kpiLoading">
          <div class="kpi-card">
            <div class="kpi-label">交付毛利(万元)</div>
            <div class="kpi-value">{{ (kpiData.deliveryMargin / 100).toFixed(2) }}</div>
            <div class="kpi-sub">确权收入 - 结转成本</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" v-loading="kpiLoading">
          <div class="kpi-card">
            <div class="kpi-label">成果验收</div>
            <div class="kpi-value">
              {{ kpiData.acceptedAchievements }}<span style="font-size:16px;color:#909399"> / {{ kpiData.totalAchievements }}</span>
            </div>
            <div class="kpi-sub">
              验收率 {{ kpiData.totalAchievements > 0 ? ((kpiData.acceptedAchievements / kpiData.totalAchievements) * 100).toFixed(1) : '0.0' }}%
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表行1：部门签约排名 + 签约风险结构 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>部门签约排名</span>
            </div>
          </template>
          <v-chart :option="deptSigningOption" style="height:350px" :loading="chartLoading1" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>签约风险结构</span>
            </div>
          </template>
          <v-chart :option="riskPieOption" style="height:350px" :loading="chartLoading2" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表行2：月度确权收入趋势 + 成果状态分布 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>月度确权收入趋势</span>
            </div>
          </template>
          <v-chart :option="revenueTrendOption" style="height:350px" :loading="chartLoading3" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>成果状态分布</span>
            </div>
          </template>
          <v-chart :option="achievementFormOption" style="height:350px" :loading="chartLoading4" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, PieChart, LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent } from 'echarts/components'

use([CanvasRenderer, BarChart, PieChart, LineChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent])

const kpiLoading = ref(false)
const chartLoading1 = ref(false)
const chartLoading2 = ref(false)
const chartLoading3 = ref(false)
const chartLoading4 = ref(false)

const kpiData = reactive({
  signingTotal: 0,
  revenueTotal: 0,
  deliveryMargin: 0,
  acceptedAchievements: 0,
  totalAchievements: 0
})

const formatWan = (val) => {
  if (val === null || val === undefined || val === '') return '0.00'
  return (Number(val) / 10000).toFixed(2)
}

// ---------- 部门签约排名 ----------
const deptSigningData = ref([])
const deptSigningOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'shadow' },
    formatter: (params) => {
      const p = params[0]
      return `${p.axisValue}<br/>签约总额(万元): ${Number(p.value).toFixed(2)}`
    }
  },
  grid: { left: '3%', right: '8%', bottom: '3%', containLabel: true, top: 10 },
  xAxis: {
    type: 'category',
    data: deptSigningData.value.map(d => d.department || ''),
    axisLabel: { rotate: 30, fontSize: 11 }
  },
  yAxis: {
    type: 'value',
    name: '万元',
    axisLabel: { formatter: (v) => (v / 10000).toFixed(0) + '万' }
  },
  series: [{
    type: 'bar',
    data: deptSigningData.value.map(d => d.total_amount || 0),
    itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] },
    barWidth: '50%',
    label: {
      show: true,
      position: 'top',
      formatter: (p) => Number(p.value).toFixed(2)
    }
  }]
}))

// ---------- 签约风险结构 ----------
const riskPieData = ref([])
const riskPieOption = computed(() => ({
  tooltip: {
    trigger: 'item',
    formatter: (params) => `${params.name}: ${Number(params.value).toFixed(2)} 万元 (${params.percent}%)`
  },
  legend: { orient: 'vertical', left: 'left', top: 'center' },
  series: [{
    type: 'pie',
    radius: ['40%', '70%'],
    center: ['60%', '55%'],
    avoidLabelOverlap: false,
    itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
    label: { show: true, formatter: '{b}: {d}%' },
    emphasis: {
      label: { show: true, fontSize: 16, fontWeight: 'bold' }
    },
    data: riskPieData.value.map(d => ({
      name: d.signing_risk_level || '未知',
      value: d.amount || 0
    }))
  }]
}))

// ---------- 月度确权收入趋势 ----------
const revenueMonthlyData = ref([])
const revenueTrendOption = computed(() => ({
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
    symbolSize: 8,
    lineStyle: { color: '#67c23a', width: 3 },
    itemStyle: { color: '#67c23a' },
    areaStyle: {
      color: {
        type: 'linear',
        x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [
          { offset: 0, color: 'rgba(103,194,58,0.3)' },
          { offset: 1, color: 'rgba(103,194,58,0.05)' }
        ]
      }
    },
    label: {
      show: true,
      position: 'top',
      formatter: (p) => Number(p.value).toFixed(2)
    }
  }]
}))

// ---------- 成果状态分布 ----------
const achievementFormData = ref([])
const achievementFormOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'shadow' }
  },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true, top: 10 },
  xAxis: {
    type: 'category',
    data: achievementFormData.value.map(d => d.achievement_form || '未知'),
    axisLabel: { fontSize: 11 }
  },
  yAxis: {
    type: 'value',
    name: '数量',
    minInterval: 1
  },
  series: [{
    type: 'bar',
    data: achievementFormData.value.map(d => d.count || 0),
    itemStyle: {
      color: '#409eff',
      borderRadius: [4, 4, 0, 0]
    },
    barWidth: '50%',
    label: {
      show: true,
      position: 'top',
      formatter: (p) => p.value
    }
  }]
}))

// 加载 KPI
const loadKpis = async () => {
  kpiLoading.value = true
  try {
    const res = await axios.get('/api/dashboard/kpi')
    const d = res.data || {}
    kpiData.signingTotal = (Number(d.signingContractTotal) || 0) + (Number(d.signingOrderTotal) || 0)
    kpiData.revenueTotal = Number(d.revenueTotal) || 0
    kpiData.deliveryMargin = Number(d.deliveryMargin) || 0
    kpiData.acceptedAchievements = Number(d.acceptedAchievements) || 0
    kpiData.totalAchievements = Number(d.totalAchievements) || 0
  } catch (e) {
    ElMessage.error('加载KPI数据失败')
  } finally {
    kpiLoading.value = false
  }
}

// 加载部门签约排名
const loadDeptSigning = async () => {
  chartLoading1.value = true
  try {
    const res = await axios.get('/api/dashboard/department-signing')
    deptSigningData.value = res.data || []
  } catch (e) {
    ElMessage.error('加载部门签约排名失败')
  } finally {
    chartLoading1.value = false
  }
}

// 加载签约风险结构
const loadSigningRisk = async () => {
  chartLoading2.value = true
  try {
    const res = await axios.get('/api/dashboard/signing-risk')
    riskPieData.value = res.data || []
  } catch (e) {
    ElMessage.error('加载签约风险数据失败')
  } finally {
    chartLoading2.value = false
  }
}

// 加载月度确权收入趋势
const loadRevenueMonthly = async () => {
  chartLoading3.value = true
  try {
    const res = await axios.get('/api/dashboard/revenue-monthly')
    revenueMonthlyData.value = res.data || []
  } catch (e) {
    ElMessage.error('加载月度收入数据失败')
  } finally {
    chartLoading3.value = false
  }
}

// 加载成果状态分布
const loadAchievementStatus = async () => {
  chartLoading4.value = true
  try {
    const res = await axios.get('/api/dashboard/achievement-status')
    achievementFormData.value = res.data?.byForm || []
  } catch (e) {
    ElMessage.error('加载成果状态数据失败')
  } finally {
    chartLoading4.value = false
  }
}

onMounted(() => {
  loadKpis()
  loadDeptSigning()
  loadSigningRisk()
  loadRevenueMonthly()
  loadAchievementStatus()
})
</script>

<style scoped>
.dashboard-page {
  padding: 0;
}

.kpi-row {
  margin-bottom: 20px;
}

.kpi-card {
  text-align: center;
  padding: 8px 0;
}

.kpi-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 12px;
}

.kpi-value {
  font-size: 32px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 8px;
}

.kpi-sub {
  font-size: 12px;
  color: #c0c4cc;
}

.chart-row {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: 600;
}
</style>
