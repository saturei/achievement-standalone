<template>
  <div class="achievement-tracker">
    <!-- 汇总卡片 -->
    <el-row :gutter="20" class="summary-row">
      <el-col :span="8">
        <el-card shadow="hover">
          <div class="summary-card">
            <div class="summary-label">已验收数</div>
            <div class="summary-value accepted">{{ acceptedCount }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <div class="summary-card">
            <div class="summary-label">计划中数</div>
            <div class="summary-value planned">{{ plannedCount }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <div class="summary-card">
            <div class="summary-label">总数</div>
            <div class="summary-value total">{{ totalCount }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 成果列表 -->
    <el-card>
      <template #header>
        <div class="card-header">
          <span>成果列表</span>
          <div class="header-filters">
            <el-select v-model="filterOrgUnit" placeholder="所属机构" clearable filterable style="width:200px;margin-right:12px" @change="loadData">
              <el-option v-for="o in orgUnits" :key="o" :label="o" :value="o" />
            </el-select>
            <el-select v-model="filterStatus" placeholder="验收状态" clearable style="width:140px" @change="loadData">
              <el-option label="已验收" value="accepted" />
              <el-option label="计划中" value="planned" />
              <el-option label="全部" value="all" />
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
      >
        <el-table-column prop="achievement_name" label="成果名称" width="180" show-overflow-tooltip sortable />
        <el-table-column prop="linked_product" label="关联产品" width="150" show-overflow-tooltip sortable />
        <el-table-column prop="linked_package" label="关联套餐" width="150" show-overflow-tooltip sortable />
        <el-table-column prop="achievement_form" label="成果形态" width="110" sortable />
        <el-table-column prop="achievement_version" label="成果版本" width="100" sortable />
        <el-table-column label="计划验收日期" width="130" prop="planned_accept_date" sortable>
          <template #default="{ row }">
            {{ formatDate(row.planned_accept_date) }}
          </template>
        </el-table-column>
        <el-table-column label="实际验收日期" width="130" prop="actual_accept_date" sortable>
          <template #default="{ row }">
            {{ formatDate(row.actual_accept_date) }}
          </template>
        </el-table-column>
        <el-table-column label="验收状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.actual_accept_date" type="success" size="small">已验收</el-tag>
            <el-tag v-else-if="row.planned_accept_date" type="warning" size="small">计划中</el-tag>
            <el-tag v-else type="info" size="small">待计划</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="org_unit" label="所属机构" width="150" show-overflow-tooltip sortable />
        <el-table-column prop="department" label="部门" width="130" show-overflow-tooltip sortable />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const loading = ref(false)
const filterOrgUnit = ref('')
const filterStatus = ref('all')
const tableData = ref([])
const orgUnits = ref([])

const formatDate = (val) => {
  if (!val) return '-'
  const n = Number(val)
  const d = isNaN(n) ? new Date(val) : new Date(n)
  if (isNaN(d.getTime())) return String(val)
  return d.toISOString().slice(0, 10)
}

// 汇总计数
const acceptedCount = computed(() =>
  tableData.value.filter(r => r.actual_accept_date).length
)

const plannedCount = computed(() =>
  tableData.value.filter(r => r.planned_accept_date && !r.actual_accept_date).length
)

const totalCount = computed(() => tableData.value.length)

const loadFilterOptions = async () => {
  try {
    const res = await axios.get('/api/dashboard/filter-options')
    const data = res.data || {}
    orgUnits.value = (data.orgUnits || []).map(o => o.org_unit)
  } catch (e) {
    ElMessage.error('加载筛选选项失败')
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const params = {}
    if (filterOrgUnit.value) params.orgUnit = filterOrgUnit.value
    if (filterStatus.value && filterStatus.value !== 'all') params.status = filterStatus.value
    const res = await axios.get('/api/dashboard/table/achievements', { params })
    tableData.value = res.data || []
  } catch (e) {
    ElMessage.error('加载成果数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadFilterOptions()
  loadData()
})
</script>

<style scoped>
.achievement-tracker {
  padding: 0;
}

.summary-row {
  margin-bottom: 20px;
}

.summary-card {
  text-align: center;
  padding: 12px 0;
}

.summary-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 10px;
}

.summary-value {
  font-size: 36px;
  font-weight: bold;
}

.summary-value.accepted {
  color: #67c23a;
}

.summary-value.planned {
  color: #e6a23c;
}

.summary-value.total {
  color: #409eff;
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
