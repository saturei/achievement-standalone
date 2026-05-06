<template>
  <div class="achievement-list">
    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键词">
          <el-input
            v-model="searchForm.keyword"
            placeholder="搜索成果名称、描述"
            clearable
            @clear="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="选择状态" clearable>
            <el-option label="预注册" value="pre_register" />
            <el-option label="注册" value="register" />
            <el-option label="登记" value="recorded" />
            <el-option label="下架" value="offline" />
            <el-option label="已删除" value="deleted" />
          </el-select>
        </el-form-item>
        <el-form-item label="成果形态">
          <el-select v-model="searchForm.achievementForm" placeholder="选择成果形态" clearable>
            <el-option label="系统成果" value="系统成果" />
            <el-option label="方案成果" value="方案成果" />
          </el-select>
        </el-form-item>
        <el-form-item label="产品">
          <el-select v-model="searchForm.productId" placeholder="选择产品" clearable>
            <el-option label="数字化智能营销平台（CP_0001）" value="CP_0001" />
            <el-option label="对公金融服务平台（CP_0003）" value="CP_0003" />
            <el-option label="个人金融服务平台（CP_0004）" value="CP_0004" />
            <el-option label="Finmall平台（CP_0007）" value="CP_0007" />
            <el-option label="Finmall资产底座（CP_0008）" value="CP_0008" />
            <el-option label="DPRO平台（CP_0012）" value="CP_0012" />
            <el-option label="信创产品（CP_0014）" value="CP_0014" />
            <el-option label="企业服务生态云平台（CP_0018）" value="CP_0018" />
            <el-option label="AI 手机银行（CP_0019）" value="CP_0019" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划验收日期">
          <el-date-picker
            v-model="searchForm.plannedAcceptanceMonth"
            type="month"
            format="YYYY-MM"
            value-format="YYYY-MM"
            placeholder="选择月份"
            clearable
          />
        </el-form-item>
        <el-form-item label="机构">
          <el-select v-model="searchForm.organizationName" placeholder="选择机构" clearable>
            <el-option v-for="item in organizationOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="primary" @click="handlePreRegister">预注册</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background: #409eff">
              <el-icon :size="30"><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.totalCount }}</div>
              <div class="stat-label">总成果数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background: #67c23a">
              <el-icon :size="30"><Edit /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.preRegisterCount }}</div>
              <div class="stat-label">预注册</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background: #e6a23c">
              <el-icon :size="30"><Checked /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.registerCount }}</div>
              <div class="stat-label">已注册</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background: #f56c6c">
              <el-icon :size="30"><SuccessFilled /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.recordCount }}</div>
              <div class="stat-label">已登记</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 成果列表 -->
    <el-card class="table-card">
      <el-table
        :data="achievements"
        v-loading="loading"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="departmentName" label="部门" min-width="120" />
        <el-table-column prop="organizationName" label="机构" min-width="150" />
        <el-table-column prop="name" label="成果名称" min-width="150">
          <template #default="{ row }">
            <el-link type="primary" @click="viewDetail(row.id)">
              {{ row.name }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="100" />
        <el-table-column label="产品" width="200">
          <template #default="{ row }">
            {{ formatProduct(row.productId, row.productName) }}
          </template>
        </el-table-column>
        <el-table-column prop="achievementForm" label="成果形态" width="100" />
        <el-table-column prop="owner" label="负责人" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewDetail(row.id)">
              查看
            </el-button>
            <el-button
              link
              type="success"
              v-if="row.status === 'PRE_REGISTER'"
              @click="handleRegister(row)"
            >
              注册
            </el-button>
            <el-button
              link
              type="warning"
              v-if="row.status === 'REGISTER'"
              @click="handleRecord(row)"
            >
              登记
            </el-button>
            <el-button
              link
              type="danger"
              v-if="row.status === 'RECORDED'"
              @click="handleOffline(row)"
            >
              下架
            </el-button>
            <el-button
              link
              type="success"
              v-if="row.status === 'OFFLINE'"
              @click="handleOnline(row)"
            >
              上架
            </el-button>
            <el-button
              link
              type="primary"
              v-if="row.status !== 'PRE_REGISTER' && row.status !== 'DELETED'"
              @click="handleChange(row)"
            >
              变更
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handlePageSizeChange"
        @current-change="handlePageChange"
        class="pagination"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import achievementApi from '../api/achievement'

const router = useRouter()
const loading = ref(false)
const achievements = ref([])
const organizationOptions = ref([])
const statistics = ref({
  totalCount: 0,
  preRegisterCount: 0,
  registerCount: 0,
  recordCount: 0,
  offlineCount: 0
})

const searchForm = reactive({
  keyword: '',
  status: '',
  achievementForm: '',
  productId: '',
  plannedAcceptanceMonth: '',
  organizationName: ''
})

const productMap = {
  'CP_0001': '数字化智能营销平台',
  'CP_0003': '对公金融服务平台',
  'CP_0004': '个人金融服务平台',
  'CP_0007': 'Finmall平台',
  'CP_0008': 'Finmall资产底座',
  'CP_0012': 'DPRO平台',
  'CP_0014': '信创产品',
  'CP_0018': '企业服务生态云平台',
  'CP_0019': 'AI 手机银行'
}

const formatProduct = (productId, productName) => {
  if (!productId) return '-'
  const name = productMap[productId] || productName
  return name ? `${name}（${productId}）` : productId
}

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

onMounted(() => {
  loadStatistics()
  loadAchievements()
  loadOrganizationOptions()
})

const loadStatistics = async () => {
  try {
    const data = await achievementApi.getStatistics()
    statistics.value = data
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

const loadAchievements = async () => {
  loading.value = true
  try {
    const data = await achievementApi.getAchievements({
      page: pagination.page,
      pageSize: pagination.pageSize,
      ...searchForm
    })
    achievements.value = data.items
    pagination.total = data.total
  } catch (error) {
    ElMessage.error('加载成果列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadAchievements()
}

const handlePageChange = () => {
  loadAchievements()
}

const handlePageSizeChange = () => {
  pagination.page = 1
  loadAchievements()
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.status = ''
  searchForm.achievementForm = ''
  searchForm.productId = ''
  searchForm.plannedAcceptanceMonth = ''
  searchForm.organizationName = ''
  handleSearch()
}

const viewDetail = (id) => {
  router.push(`/achievement/${id}`)
}

const handleRegister = (row) => {
  router.push(`/achievement/${row.id}/register`)
}

const handleRecord = (row) => {
  router.push(`/achievement/${row.id}/record`)
}

const handleChange = (row) => {
  router.push(`/achievement/${row.id}/change`)
}

const handleOffline = async (row) => {
  try {
    await ElMessageBox.confirm('确认要下架该成果吗？', '提示', {
      type: 'warning'
    })
    await achievementApi.offline(row.id, { offlineReason: '手动下架' })
    ElMessage.success('下架成功')
    loadAchievements()
    loadStatistics()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('下架失败')
    }
  }
}

const handleOnline = async (row) => {
  try {
    await ElMessageBox.confirm('确认要上架该成果吗？', '提示', {
      type: 'warning'
    })
    await achievementApi.online(row.id, {})
    ElMessage.success('上架成功')
    loadAchievements()
    loadStatistics()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('上架失败')
    }
  }
}

const getStatusType = (status) => {
  const map = {
    'PRE_REGISTER': 'info',
    'REGISTER': 'warning',
    'RECORDED': 'success',
    'OFFLINE': 'danger',
    'DELETED': ''
  }
  return map[status] || ''
}

const getStatusText = (status) => {
  const map = {
    'PRE_REGISTER': '预注册',
    'REGISTER': '注册',
    'RECORDED': '登记',
    'OFFLINE': '下架',
    'DELETED': '已删除'
  }
  return map[status] || status
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

const handlePreRegister = () => {
  router.push('/pre-register')
}

const loadOrganizationOptions = async () => {
  try {
    const data = await achievementApi.getOrganizations()
    organizationOptions.value = data
  } catch (error) {
    console.error('加载机构列表失败:', error)
  }
}
</script>

<style scoped>
.achievement-list {
  padding: 0;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  margin-right: 15px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
}

.search-card {
  margin-bottom: 20px;
}

.search-card .el-select {
  width: 150px;
}

.search-card .el-input {
  width: 200px;
}

.table-card {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
