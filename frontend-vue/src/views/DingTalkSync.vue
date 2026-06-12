<template>
  <div class="sync-page">
    <!-- Sheet 列表 -->
    <el-card class="sync-card">
      <template #header>
        <div class="card-header">
          <span>钉钉数据同步</span>
          <el-button type="primary" @click="handleSyncAll" :loading="syncingAll">
            全部同步
          </el-button>
        </div>
      </template>

      <el-table :data="sheets" stripe v-loading="loadingSheets" row-key="sheetId">
        <el-table-column prop="sheetName" label="数据表" width="200" />
        <el-table-column prop="tableName" label="目标表" width="180" />
        <el-table-column label="当前记录数" width="110" align="center">
          <template #default="{ row }">{{ row.recordCount }}</template>
        </el-table-column>
        <el-table-column label="最近同步" width="170">
          <template #default="{ row }">
            <span v-if="row.lastSyncTime">{{ formatTime(row.lastSyncTime) }}</span>
            <span v-else class="never-sync">从未同步</span>
          </template>
        </el-table-column>
        <el-table-column label="最近同步数量" width="110" align="center">
          <template #default="{ row }">
            {{ row.lastSyncCount ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="160" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              type="primary"
              :loading="syncingRows.has(row.sheetId)"
              @click="handleSyncOne(row)"
            >
              同步覆盖
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 同步日志 -->
    <el-card class="log-card">
      <template #header>
        <div class="card-header">
          <span>同步日志 (最近50条)</span>
          <el-button size="small" @click="loadLogs">刷新</el-button>
        </div>
      </template>

      <el-table :data="logs" stripe v-loading="loadingLogs" row-key="id" max-height="400">
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="sheetName" label="数据表" width="140" />
        <el-table-column prop="tableName" label="目标表" width="160" />
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-tag :type="row.operation === 'ROLLBACK' ? 'warning' : 'info'" size="small">
              {{ row.operation === 'ROLLBACK' ? '回滚' : '同步' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="同步前" width="90" align="center">
          <template #default="{ row }">{{ row.recordCountBefore ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="同步后" width="90" align="center">
          <template #default="{ row }">{{ row.recordCountAfter ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="errorMessage" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'SUCCESS' && row.operation === 'SYNC'"
              size="small"
              type="danger"
              plain
              @click="handleRollback(row)"
              :loading="rollingBack === row.id"
            >
              回滚
            </el-button>
            <span v-else-if="row.operation === 'ROLLBACK'" class="rolled-back">已回滚</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const sheets = ref([])
const loadingSheets = ref(false)
const syncingRows = reactive(new Set())
const syncingAll = ref(false)

const logs = ref([])
const loadingLogs = ref(false)
const rollingBack = ref(null)

onMounted(() => {
  loadSheets()
  loadLogs()
})

const loadSheets = async () => {
  loadingSheets.value = true
  try {
    const res = await axios.get('/api/sync/sheets')
    sheets.value = res.data || []
  } catch (e) {
    ElMessage.error('加载数据表列表失败')
  } finally {
    loadingSheets.value = false
  }
}

const loadLogs = async () => {
  loadingLogs.value = true
  try {
    const res = await axios.get('/api/sync/logs', { params: { limit: 50 } })
    logs.value = res.data || []
  } catch (e) {
    // silent
  } finally {
    loadingLogs.value = false
  }
}

const handleSyncOne = async (row) => {
  syncingRows.add(row.sheetId)
  try {
    const res = await axios.post(`/api/sync/sheets/${row.sheetId}/sync`)
    const data = res.data
    if (data.error) {
      ElMessage.error(`同步失败: ${data.error}`)
    } else {
      ElMessage.success(
        `${data.sheetName || row.sheetName} 同步完成：` +
        `${data.recordCountBefore || 0} → ${data.recordCountAfter || 0}`
      )
      loadSheets()
      loadLogs()
    }
  } catch (e) {
    ElMessage.error('同步失败: ' + (e.response?.data?.error || e.message))
  } finally {
    syncingRows.delete(row.sheetId)
  }
}

const handleSyncAll = async () => {
  await ElMessageBox.confirm(
    '确认同步所有数据表？每张表都会完全覆盖当前数据。',
    '全部同步',
    { type: 'warning', confirmButtonText: '确认同步', cancelButtonText: '取消' }
  )
  syncingAll.value = true
  let success = 0
  let fail = 0
  for (const row of sheets.value) {
    try {
      const res = await axios.post(`/api/sync/sheets/${row.sheetId}/sync`)
      if (res.data.error) fail++
      else success++
    } catch (e) {
      fail++
    }
  }
  syncingAll.value = false
  ElMessage.success(`全部同步完成：成功 ${success} 张，失败 ${fail} 张`)
  loadSheets()
  loadLogs()
}

const handleRollback = async (row) => {
  await ElMessageBox.confirm(
    `确认回滚「${row.sheetName}」到同步前状态？\n当前数据将丢失，恢复为同步前的 ${row.recordCountBefore} 条记录。`,
    '确认回滚',
    { type: 'warning', confirmButtonText: '确认回滚', cancelButtonText: '取消' }
  )
  rollingBack.value = row.id
  try {
    const res = await axios.post(`/api/sync/rollback/${row.id}`)
    if (res.data.error) {
      ElMessage.error(`回滚失败: ${res.data.error}`)
    } else {
      ElMessage.success(`回滚成功，恢复 ${res.data.recordCountAfter} 条记录`)
      loadSheets()
      loadLogs()
    }
  } catch (e) {
    ElMessage.error('回滚失败: ' + (e.response?.data?.error || e.message))
  } finally {
    rollingBack.value = null
  }
}

const formatTime = (t) => {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN')
}

const statusType = (s) => {
  if (s === 'SUCCESS') return 'success'
  if (s === 'FAILED') return 'danger'
  if (s === 'ROLLED_BACK') return 'warning'
  return 'info'
}

const statusText = (s) => {
  if (s === 'SUCCESS') return '成功'
  if (s === 'FAILED') return '失败'
  if (s === 'ROLLED_BACK') return '已回滚'
  return s
}
</script>

<style scoped>
.sync-page { padding: 0; }
.sync-card { margin-bottom: 20px; }
.log-card { margin-bottom: 20px; }
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.never-sync { color: #c0c4cc; font-style: italic; }
.rolled-back { color: #e6a23c; font-size: 12px; }
</style>
