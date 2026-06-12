<template>
  <div id="app">
    <!-- 钉钉环境：未登录时显示登录页面 -->
    <div v-if="isDingTalk && !loggedIn" class="login-container">
      <div class="login-card">
        <h2>产品目标管理平台</h2>
        <p>正在通过钉钉登录...</p>
        <el-button type="primary" :loading="loginLoading" @click="doDingTalkLogin">
          钉钉授权登录
        </el-button>
        <p v-if="loginError" class="login-error">{{ loginError }}</p>
      </div>
    </div>

    <el-container v-else>
      <el-header>
        <div class="header-content">
          <h1>产品目标管理平台</h1>
          <el-menu
            mode="horizontal"
            :default-active="activeMenu"
            router
            class="header-menu"
          >
            <el-menu-item index="/target-statistics">目标统计</el-menu-item>
            <el-sub-menu index="/dashboard-group">
              <template #title>生产总览</template>
              <el-menu-item index="/dashboard">总览看板</el-menu-item>
              <el-menu-item index="/signing-tracker">签约跟踪</el-menu-item>
              <el-menu-item index="/revenue-tracker">确权收入跟踪</el-menu-item>
              <el-menu-item index="/cost-tracker">成本跟踪</el-menu-item>
              <el-menu-item index="/achievement-tracker">成果跟踪</el-menu-item>
            </el-sub-menu>
            <el-menu-item index="/">成果列表</el-menu-item>
            <el-menu-item index="/pre-register">预注册</el-menu-item>
          </el-menu>
          <div class="header-right">
            <!-- 已登录：显示用户名 + 角色 -->
            <template v-if="loggedIn">
              <span class="user-info">
                <el-avatar v-if="avatar" :size="28" :src="avatar" />
                {{ displayName }}
                <el-tag size="small" type="info" style="margin-left:6px">{{ roleTag }}</el-tag>
              </span>
              <el-button size="small" text @click="handleLogout">退出</el-button>
            </template>
            <!-- 未登录（开发环境）：用户选择 -->
            <template v-else>
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
            </template>
            <el-dropdown trigger="click">
              <el-button size="small" type="info" plain>
                管理
                <el-icon><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="router.push('/contract-signings')">
                    签约明细管理
                  </el-dropdown-item>
                  <el-dropdown-item @click="router.push('/revenue-recognitions')">
                    确权/收入明细管理
                  </el-dropdown-item>
                  <el-dropdown-item @click="router.push('/user-management')">
                    用户管理
                  </el-dropdown-item>
                  <el-dropdown-item divided @click="router.push('/dingtalk-sync')">
                    钉钉数据同步
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
import { ArrowDown } from '@element-plus/icons-vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const activeMenu = computed(() => route.path)

// ===== 钉钉登录状态 =====
const loggedIn = ref(false)
const displayName = ref('')
const avatar = ref('')
const roleTag = ref('')
const loginLoading = ref(false)
const loginError = ref('')
const isDingTalk = /DingTalk/i.test(navigator.userAgent)

// ===== 开发环境用户选择（降级） =====
const currentUser = ref('admin')
const userList = ref([{ username: 'admin', displayName: '管理员', role: 'ADMIN' }])

onMounted(async () => {
  // 1. 先检查是否已有 JWT Token
  const token = localStorage.getItem('dingtalk_token')
  if (token) {
    await checkLoginStatus()
    return
  }

  // 2. 钉钉环境：自动发起登录
  if (isDingTalk) {
    doDingTalkLogin()
    return
  }

  // 3. 开发环境：加载旧用户列表
  await loadUserList()
})

// ---------- 钉钉登录 ----------
const doDingTalkLogin = async () => {
  loginLoading.value = true
  loginError.value = ''

  try {
    // 调用钉钉 JSAPI 获取 authCode
    let authCode = null
    if (window.dd && typeof window.dd.getAuthCode === 'function') {
      authCode = await new Promise((resolve, reject) => {
        window.dd.getAuthCode({
          corpId: 'ding4651594f561e9defacaaa37764f94726',
          success: (res) => resolve(res.authCode || res.code),
          fail: (err) => reject(new Error(err.errorMessage || '获取授权码失败'))
        })
      })
    }

    if (!authCode) {
      loginError.value = '请点击按钮授权登录'
      loginLoading.value = false
      return
    }

    // 后端交换 token
    const res = await axios.post('/api/auth/dingtalk/login', { authCode })
    const data = res.data

    // 保存 token 和用户信息
    localStorage.setItem('dingtalk_token', data.token)
    const userInfo = {
      username: data.user.username,
      displayName: data.user.displayName,
      role: data.user.role,
      avatar: data.user.avatar || ''
    }
    localStorage.setItem('currentUser', JSON.stringify(userInfo))

    loggedIn.value = true
    displayName.value = data.user.displayName
    avatar.value = data.user.avatar || ''
    roleTag.value = roleLabel(data.user.role)

    ElMessage.success('钉钉登录成功')
  } catch (e) {
    const msg = e.response?.data?.error || e.message || '登录失败'
    loginError.value = msg
    console.error('钉钉登录失败:', e)
  } finally {
    loginLoading.value = false
  }
}

// ---------- 检查登录状态 ----------
const checkLoginStatus = async () => {
  try {
    const res = await axios.get('/api/auth/me')
    const user = res.data
    if (user) {
      loggedIn.value = true
      displayName.value = user.displayName || user.username
      avatar.value = user.avatar || ''
      roleTag.value = roleLabel(user.role)
    }
  } catch (e) {
    // token 失效，清除
    localStorage.removeItem('dingtalk_token')
    localStorage.removeItem('currentUser')
    if (isDingTalk) {
      doDingTalkLogin()
    } else {
      await loadUserList()
    }
  }
}

const handleLogout = () => {
  localStorage.removeItem('dingtalk_token')
  localStorage.removeItem('currentUser')
  window.location.reload()
}

// ---------- 开发环境用户列表 ----------
const loadUserList = async () => {
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
}

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

// ---------- 钉钉数据同步 ----------
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

.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  background: #fff;
  padding: 40px 48px;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  text-align: center;
  max-width: 400px;
  width: 100%;
}

.login-card h2 {
  margin: 0 0 12px 0;
  font-size: 22px;
  color: #303133;
}

.login-card p {
  color: #909399;
  font-size: 14px;
  margin-bottom: 24px;
}

.login-error {
  color: #f56c6c;
  margin-top: 12px;
  font-size: 13px;
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

.header-menu .el-sub-menu__title {
  color: #fff !important;
  border-bottom: none !important;
}
.header-menu .el-sub-menu__title:hover {
  background-color: rgba(255, 255, 255, 0.1) !important;
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

.user-info {
  display: flex;
  align-items: center;
  color: #fff;
  font-size: 14px;
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
