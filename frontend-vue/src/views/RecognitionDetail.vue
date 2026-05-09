<template>
  <div class="detail-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>确权/收入明细管理 — 数据同步至目标统计「确权实际值」和「预算实际值」</span>
          <div>
            <el-button size="small" @click="handlePaste">粘贴Excel</el-button>
            <el-button size="small" type="primary" @click="handleAdd">新增行</el-button>
            <el-button size="small" type="success" @click="handleSaveAll" :loading="savingAll" :disabled="editingRows.size === 0">
              全部保存 ({{ editingRows.size }})
            </el-button>
          </div>
        </div>
      </template>

      <el-form :inline="true" class="filter-form">
        <el-form-item label="确权归属月">
          <el-date-picker v-model="filterMonth" clearable @change="loadData" type="month" format="YYYY-MM" value-format="YYYY-MM" placeholder="选择月份" style="width: 150px" />
        </el-form-item>
        <el-form-item label="机构">
          <el-select v-model="filterOrg" clearable @change="loadData" filterable style="width: 200px">
            <el-option v-for="o in orgOptions" :key="o" :label="o" :value="o" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="danger" size="small" @click="handleBatchDelete" :disabled="selectedRows.length === 0">
            批量删除 ({{ selectedRows.length }})
          </el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" stripe border style="width: 100%; font-size: 13px" v-loading="loading"
        @selection-change="handleSelectionChange" max-height="600" row-key="id">
        <el-table-column type="selection" width="40" />
        <el-table-column prop="orderId" label="订单ID" width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <el-input v-if="editingRows.has(row.id)" v-model="row.orderId" size="small" />
            <span v-else>{{ row.orderId || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="contractId" label="合同ID" width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <el-input v-if="editingRows.has(row.id)" v-model="row.contractId" size="small" />
            <span v-else>{{ row.contractId || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="contractName" label="合同名称" width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <el-input v-if="editingRows.has(row.id)" v-model="row.contractName" size="small" />
            <span v-else>{{ row.contractName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="确权金额(万元)" width="130" align="right">
          <template #default="{ row }">
            <el-input v-if="editingRows.has(row.id)" v-model="row._recWan" size="small" type="number" placeholder="万元" />
            <span v-else>{{ formatWan(row.recognitionAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="收入金额(万元,含税)" width="150" align="right">
          <template #default="{ row }">
            <el-input v-if="editingRows.has(row.id)" v-model="row._revWan" size="small" type="number" placeholder="万元" />
            <span v-else>{{ formatWan(row.revenueAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="确权归属年月" width="130">
          <template #default="{ row }">
            <el-date-picker v-if="editingRows.has(row.id)" v-model="row.recognitionMonth" size="small" type="month" format="YYYY-MM" value-format="YYYY-MM" style="width:100%" />
            <span v-else>{{ row.recognitionMonth || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="确权风险等级" width="120">
          <template #default="{ row }">
            <el-select v-if="editingRows.has(row.id)" v-model="row.recognitionRiskLevel" size="small" clearable style="width:100%">
              <el-option v-for="r in riskOptions" :key="r" :label="r" :value="r" />
            </el-select>
            <span v-else>{{ row.recognitionRiskLevel || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="organization" label="所属机构" width="140">
          <template #default="{ row }">
            <el-select v-if="editingRows.has(row.id)" v-model="row.organization" size="small" filterable allow-create style="width:100%">
              <el-option v-for="o in orgOptions" :key="o" :label="o" :value="o" />
            </el-select>
            <span v-else>{{ row.organization || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="customerName" label="关联客户" width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <el-input v-if="editingRows.has(row.id)" v-model="row.customerName" size="small" />
            <span v-else>{{ row.customerName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="经营岗" width="90">
          <template #default="{ row }">
            <el-input v-if="editingRows.has(row.id)" v-model="row.operator" size="small" />
            <span v-else>{{ row.operator || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <template v-if="editingRows.has(row.id)">
              <el-button link type="primary" size="small" @click="saveRow(row)" :loading="row._saving">保存</el-button>
              <el-button link size="small" @click="cancelEdit(row)">取消</el-button>
            </template>
            <template v-else>
              <el-button link type="primary" size="small" @click="startEdit(row)">编辑</el-button>
              <el-button link type="danger" size="small" @click="handleDeleteRow(row)">删除</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 粘贴Excel -->
    <el-dialog v-model="pasteVisible" title="粘贴Excel数据" width="800px">
      <p style="color:#909399;margin-bottom:8px;font-size:13px">
        从Excel复制多列（Tab分隔）粘贴到下方。列顺序：订单ID / 合同ID / 合同名称 / 确权金额(万元) / 收入金额(万元,含税) / 确权归属年月 / 确权风险等级 / 关联客户 / 经营岗 / 备注 / 所属机构
      </p>
      <el-input v-model="pasteText" type="textarea" :rows="8" placeholder="粘贴Excel数据..." @input="parsePaste" />
      <div v-if="pastePreview.length > 0" style="margin-top:12px">
        <p style="font-size:13px;color:#606266">预览解析结果（共 {{ pastePreview.length }} 条）：</p>
        <el-table :data="pastePreview.slice(0, 5)" border size="small" style="font-size:12px">
          <el-table-column prop="orderId" label="订单ID" width="100" />
          <el-table-column prop="contractId" label="合同ID" width="100" />
          <el-table-column prop="contractName" label="合同名称" width="130" show-overflow-tooltip />
          <el-table-column label="确权金额" width="90" align="right">
            <template #default="{ row: r }">{{ (r.recognitionAmount / 10000).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="收入金额" width="90" align="right">
            <template #default="{ row: r }">{{ (r.revenueAmount / 10000).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="organization" label="机构" width="120" />
        </el-table>
        <p v-if="pastePreview.length > 5" style="font-size:12px;color:#909399">... 仅展示前5条</p>
      </div>
      <template #footer>
        <el-button @click="pasteVisible = false">取消</el-button>
        <el-button type="primary" @click="doPaste" :loading="pasting" :disabled="pastePreview.length === 0">
          确认导入 ({{ pastePreview.length }} 条)
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

const loading = ref(false)
const pasting = ref(false)
const savingAll = ref(false)
const pasteVisible = ref(false)
const pasteText = ref('')
const pastePreview = ref([])
const filterMonth = ref(null)
const filterOrg = ref('')
const tableData = ref([])
const selectedRows = ref([])
const orgOptions = ref([])
const riskOptions = ['高', '中', '低']
const editingRows = reactive(new Set())
const originalRows = ref({})

onMounted(async () => {
  await loadOrgs()
  loadData()
})

const loadOrgs = async () => {
  try {
    const res = await axios.get('/api/achievements/organizations')
    orgOptions.value = res.data || []
  } catch (e) {}
}

const loadData = async () => {
  loading.value = true
  try {
    const params = {}
    if (filterMonth.value) params.recognitionMonth = filterMonth.value
    if (filterOrg.value) params.organization = filterOrg.value
    const res = await axios.get('/api/detail/recognitions', { params })
    tableData.value = (res.data || []).map(r => {
      const recWan = r.recognitionAmount ? (Number(r.recognitionAmount) / 10000).toFixed(2) : ''
      const revWan = r.revenueAmount ? (Number(r.revenueAmount) / 10000).toFixed(2) : ''
      return { ...r, _recWan: recWan, _revWan: revWan, _saving: false }
    })
  } catch (e) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const formatWan = (amount) => {
  if (!amount) return '-'
  return (Number(amount) / 10000).toFixed(2)
}

const startEdit = (row) => {
  originalRows.value[row.id] = { ...row }
  editingRows.add(row.id)
}

const cancelEdit = (row) => {
  const orig = originalRows.value[row.id]
  if (orig) {
    Object.keys(orig).forEach(k => { row[k] = orig[k] })
  }
  editingRows.delete(row.id)
  delete originalRows.value[row.id]
}

const handleAdd = async () => {
  try {
    const res = await axios.post('/api/detail/recognitions', {
      organization: filterOrg.value || '',
      recognitionAmount: 0,
      revenueAmount: 0
    })
    const r = { ...res.data, _recWan: '0.00', _revWan: '0.00', _saving: false }
    tableData.value.unshift(r)
    editingRows.add(r.id)
  } catch (e) {
    ElMessage.error('新增失败')
  }
}

const saveRow = async (row) => {
  row._saving = true
  try {
    const payload = { ...row }
    payload.recognitionAmount = row._recWan ? Math.round(parseFloat(row._recWan) * 10000) : (row.recognitionAmount || 0)
    payload.revenueAmount = row._revWan ? Math.round(parseFloat(row._revWan) * 10000) : (row.revenueAmount || 0)
    delete payload._recWan
    delete payload._revWan
    delete payload._saving
    await axios.put(`/api/detail/recognitions/${row.id}`, payload)
    editingRows.delete(row.id)
    delete originalRows.value[row.id]
    ElMessage.success('保存成功')
    loadData()
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    row._saving = false
  }
}

const handleSaveAll = async () => {
  savingAll.value = true
  let saved = 0
  let failed = 0
  for (const row of tableData.value) {
    if (!editingRows.has(row.id)) continue
    try {
      const payload = { ...row }
      payload.recognitionAmount = row._recWan ? Math.round(parseFloat(row._recWan) * 10000) : (row.recognitionAmount || 0)
      payload.revenueAmount = row._revWan ? Math.round(parseFloat(row._revWan) * 10000) : (row.revenueAmount || 0)
      delete payload._recWan
      delete payload._revWan
      delete payload._saving
      await axios.put(`/api/detail/recognitions/${row.id}`, payload)
      editingRows.delete(row.id)
      delete originalRows.value[row.id]
      saved++
    } catch (e) { failed++ }
  }
  if (saved > 0 || failed > 0) {
    ElMessage.success(`保存完成：成功 ${saved} 条${failed > 0 ? `，失败 ${failed} 条` : ''}`)
  }
  savingAll.value = false
  loadData()
}

const handleDeleteRow = (row) => {
  ElMessageBox.confirm('确认删除该条记录吗？', '确认删除', { type: 'warning' })
    .then(async () => {
      await axios.delete(`/api/detail/recognitions/${row.id}`)
      editingRows.delete(row.id)
      ElMessage.success('删除成功')
      loadData()
    }).catch(() => {})
}

const handleSelectionChange = (rows) => { selectedRows.value = rows }

const handleBatchDelete = () => {
  ElMessageBox.confirm(`确认删除选中的 ${selectedRows.value.length} 条？`, '批量删除', { type: 'warning' })
    .then(async () => {
      for (const row of selectedRows.value) {
        await axios.delete(`/api/detail/recognitions/${row.id}`)
        editingRows.delete(row.id)
      }
      ElMessage.success('批量删除成功')
      loadData()
    }).catch(() => {})
}

const handlePaste = () => {
  pasteText.value = ''
  pastePreview.value = []
  pasteVisible.value = true
}

const parsePaste = () => {
  const lines = pasteText.value.split('\n').filter(l => l.trim())
  pastePreview.value = lines.map(line => {
    const cols = line.split('\t')
    const recWan = parseFloat(cols[3]) || 0
    const revWan = parseFloat(cols[4]) || 0
    return {
      orderId: cols[0] || '',
      contractId: cols[1] || '',
      contractName: cols[2] || '',
      recognitionAmount: Math.round(recWan * 10000),
      revenueAmount: Math.round(revWan * 10000),
      recognitionMonth: parseMonth(cols[5]),
      recognitionRiskLevel: cols[6] || '',
      customerName: cols[7] || '',
      operator: cols[8] || '',
      remark: cols[9] || '',
      organization: (cols[10] && cols[10].trim()) || filterOrg.value || ''
    }
  })
}

const parseMonth = (val) => {
  if (!val || !val.trim()) return ''
  const m = val.trim()
  const match = m.match(/(\d{4})\s*年\s*(\d{1,2})\s*月/)
  if (match) return match[1] + '-' + match[2].padStart(2, '0')
  if (/^\d{4}-\d{2}$/.test(m)) return m
  const m2 = m.match(/^(\d{4})-(\d{1,2})$/)
  if (m2) return m2[1] + '-' + m2[2].padStart(2, '0')
  return m
}

const doPaste = async () => {
  if (pastePreview.value.length === 0) return
  pasting.value = true
  try {
    const res = await axios.post('/api/detail/recognitions/batch', pastePreview.value)
    ElMessage.success(`成功导入 ${res.data.count} 条`)
    pasteVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error('导入失败')
  } finally { pasting.value = false }
}
</script>

<style scoped>
.detail-page { padding: 0; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.filter-form { margin-bottom: 16px; }
</style>
