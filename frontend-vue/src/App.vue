<template>
  <div id="app">
    <el-container>
      <el-header>
        <div class="header-content">
          <h1>成果管理系统</h1>
          <el-menu
            mode="horizontal"
            :default-active="activeMenu"
            router
            class="header-menu"
          >
            <el-menu-item index="/target-statistics">目标统计</el-menu-item>
            <el-menu-item index="/dashboard">生产总览</el-menu-item>
            <el-menu-item index="/">成果列表</el-menu-item>
            <el-menu-item index="/pre-register">预注册</el-menu-item>
          </el-menu>
          <div class="header-right">
            <el-select
              v-model="currentUser"
              placeholder="选择当前用户"
              size="small"
              style="width: 160px; margin-right: 12px"
              @change="handleUserChange"
            >
              <el-option
                v-for="u in userList"
                :key="u.username"
                :label="u.displayName + ' (' + roleLabel(u.role) + ')'"
                :value="u.username"
              />
            </el-select>
            <el-dropdown trigger="click">
              <el-button size="small" type="info" plain>
                管理
                <el-icon><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="router.push('/signing-tracker')">
                    签约跟踪
                  </el-dropdown-item>
                  <el-dropdown-item @click="router.push('/revenue-tracker')">
                    确权收入跟踪
                  </el-dropdown-item>
                  <el-dropdown-item @click="router.push('/cost-tracker')">
                    成本跟踪
                  </el-dropdown-item>
                  <el-dropdown-item @click="router.push('/achievement-tracker')">
                    成果跟踪
                  </el-dropdown-item>
                  <el-dropdown-item divided @click="router.push('/contract-signings')">
                    签约明细管理
                  </el-dropdown-item>
                  <el-dropdown-item @click="router.push('/revenue-recognitions')">
                    确权/收入明细管理
                  </el-dropdown-item>
                  <el-dropdown-item @click="router.push('/user-management')">
                    用户管理
                  </el-dropdown-item>
                  <el-dropdown-item divided @click="handleSyncData">
                    同步钉钉数据
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown, User, Document, Money } from '@element-plus/icons-vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const activeMenu = computed(() => route.path)
const currentUser = ref('admin')
const userList = ref([{ username: 'admin', displayName: '管理员', role: 'ADMIN' }])

onMounted(async () => {
  try {
    const res = await axios.get('/api/users')
    if (res.data) {
      userList.value = res.data
    }
  } catch (e) {
    console.error('加载用户列表失败:', e)
  }
  const stored = localStorage.getItem('currentUser')
  if (stored) {
    try {
      const u = JSON.parse(stored)
      currentUser.value = u.username
    } catch (e) {}
  }
})

const handleUserChange = (val) => {
  const user = userList.value.find(u => u.username === val)
  if (user) {
    localStorage.setItem('currentUser', JSON.stringify(user))
    window.location.reload()
  }
}

const roleLabel = (role) => {
  const m = { ADMIN: '管理员', DEPT_LEADER: '部门负责人', ORG_LEADER: '机构负责人', USER: '普通用户' }
  return m[role] || role
}

const handleSyncData = async () => {
  try {
    const res = await axios.post('/api/data/sync-all')
    const total = res.data.total_count || 0
    ElMessage.success(`钉钉数据同步完成，共 ${total} 条`)
  } catch (e) {
    ElMessage.error('同步失败: ' + (e.response?.data?.error || e.message))
  }
}
</script>

<style>
#app {
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB',
    'Microsoft YaHei', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
}

.el-header {
  background-color: #545c64;
  color: #fff;
  padding: 0;
}

.header-content {
  display: flex;
  align-items: center;
  height: 100%;
  padding: 0 20px;
}

.header-content h1 {
  margin: 0;
  font-size: 20px;
  margin-right: 30px;
  white-space: nowrap;
}

.header-menu {
  flex: 1;
  background-color: transparent;
  border-bottom: none;
}

.header-menu .el-menu-item {
  color: #fff;
  border-bottom: none;
}

.header-menu .el-menu-item:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.header-menu .el-menu-item.is-active {
  background-color: rgba(255, 255, 255, 0.2);
  border-bottom: 2px solid #409eff;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.el-main {
  padding: 20px;
  background-color: #f5f5f5;
  min-height: calc(100vh - 60px);
}

/* ---- 全局表格样式 ---- */
.el-table th .cell,
.el-table td .cell {
  text-align: center;
}
/* 数值列右对齐覆盖 */
.el-table .el-table-column--align-right .cell {
  text-align: right;
}
</style>
