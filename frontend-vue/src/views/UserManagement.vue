<template>
  <div class="user-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户权限配置</span>
          <el-button type="primary" size="small" @click="handleAdd">新增用户</el-button>
        </div>
      </template>
      <el-table :data="users" stripe border style="width: 100%" v-loading="loading" max-height="65vh">
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="displayName" label="显示名称" width="150" />
        <el-table-column prop="role" label="角色" width="130">
          <template #default="{ row }">
            <el-tag>{{ roleLabel(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="department" label="所属部门" width="150" />
        <el-table-column prop="organization" label="所属机构" width="180" />
        <el-table-column prop="enabled" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.enabled === 1 ? 'success' : 'danger'">{{ row.enabled === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="用户名" required>
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="显示名称">
          <el-input v-model="form.displayName" placeholder="请输入显示名称" />
        </el-form-item>
        <el-form-item label="角色" required>
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="部门负责人" value="DEPT_LEADER" />
            <el-option label="机构负责人" value="ORG_LEADER" />
            <el-option label="普通用户" value="USER" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属部门" v-if="form.role === 'DEPT_LEADER' || form.role === 'ORG_LEADER'">
          <el-select v-model="form.department" style="width: 100%" placeholder="选择部门" clearable>
            <el-option v-for="d in deptOptions" :key="d" :label="d" :value="d" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属机构" v-if="form.role === 'ORG_LEADER'">
          <el-input v-model="form.organization" placeholder="请输入机构名称" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const users = ref([])
const deptOptions = ref(['AI方案中心', 'FM平台中心', '业务方案中心', '数据中心', '能力中心'])
const form = ref({ username: '', displayName: '', role: 'USER', department: '', organization: '', enabled: 1 })

onMounted(() => loadUsers())

const loadUsers = async () => {
  loading.value = true
  try {
    const res = await axios.get('/api/users/config')
    users.value = res.data || []
  } catch (e) {
    ElMessage.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  isEdit.value = false
  form.value = { username: '', displayName: '', role: 'USER', department: '', organization: '', enabled: 1 }
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  form.value = { ...row }
  dialogVisible.value = true
}

const handleSave = async () => {
  saving.value = true
  try {
    if (isEdit.value) {
      await axios.put(`/api/users/${form.value.id}`, form.value)
    } else {
      await axios.post('/api/users', form.value)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadUsers()
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确认删除用户「${row.displayName || row.username}」吗？`, '确认删除', { type: 'warning' })
    .then(async () => {
      await axios.delete(`/api/users/${row.id}`)
      ElMessage.success('删除成功')
      loadUsers()
    })
    .catch(() => {})
}

const roleLabel = (role) => {
  const m = { ADMIN: '管理员', DEPT_LEADER: '部门负责人', ORG_LEADER: '机构负责人', USER: '普通用户' }
  return m[role] || role
}
</script>

<style scoped>
.user-management { padding: 0; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
