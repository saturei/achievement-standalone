import { createRouter, createWebHistory } from 'vue-router'
import AchievementList from '../views/AchievementList.vue'
import AchievementDetail from '../views/AchievementDetail.vue'
import PreRegister from '../views/PreRegister.vue'
import ChangeAchievement from '../views/ChangeAchievement.vue'
import RecordAchievement from '../views/RecordAchievement.vue'
import RegisterAchievement from '../views/RegisterAchievement.vue'

const routes = [
  {
    path: '/target-statistics',
    name: 'TargetStatistics',
    component: () => import('../views/TargetStatistics.vue')
  },
  {
    path: '/',
    name: 'AchievementList',
    component: AchievementList
  },
  {
    path: '/achievement/:id',
    name: 'AchievementDetail',
    component: AchievementDetail
  },
  {
    path: '/pre-register',
    name: 'PreRegister',
    component: PreRegister
  },
  {
    path: '/achievement/:id/change',
    name: 'ChangeAchievement',
    component: ChangeAchievement
  },
  {
    path: '/achievement/:id/record',
    name: 'RecordAchievement',
    component: RecordAchievement
  },
  {
    path: '/achievement/:id/register',
    name: 'RegisterAchievement',
    component: RegisterAchievement
  },
  {
    path: '/target-edit',
    name: 'TargetEdit',
    component: () => import('../views/TargetEdit.vue')
  },
  {
    path: '/user-management',
    name: 'UserManagement',
    component: () => import('../views/UserManagement.vue')
  },
  {
    path: '/contract-signings',
    name: 'ContractSignings',
    component: () => import('../views/SigningDetail.vue')
  },
  {
    path: '/revenue-recognitions',
    name: 'RevenueRecognitions',
    component: () => import('../views/RecognitionDetail.vue')
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('../views/Dashboard.vue')
  },
  {
    path: '/signing-tracker',
    name: 'SigningTracker',
    component: () => import('../views/SigningTracker.vue')
  },
  {
    path: '/revenue-tracker',
    name: 'RevenueTracker',
    component: () => import('../views/RevenueTracker.vue')
  },
  {
    path: '/cost-tracker',
    name: 'CostTracker',
    component: () => import('../views/CostTracker.vue')
  },
  {
    path: '/achievement-tracker',
    name: 'AchievementTracker',
    component: () => import('../views/AchievementTracker.vue')
  },
  {
    path: '/dingtalk-sync',
    name: 'DingTalkSync',
    component: () => import('../views/DingTalkSync.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：检查登录状态
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('dingtalk_token')
  // 有 token 直接放行
  if (token) {
    next()
    return
  }
  // 开发环境：有 currentUser 也放行（兼容旧逻辑）
  const stored = localStorage.getItem('currentUser')
  if (stored) {
    try {
      const user = JSON.parse(stored)
      if (user.username) {
        next()
        return
      }
    } catch (e) {}
  }
  // 未登录，检查是否在钉钉环境中
  const isDingTalk = /DingTalk/i.test(navigator.userAgent)
  if (!isDingTalk) {
    // 非钉钉环境：允许访问（后续 App.vue 会处理登录）
    next()
    return
  }
  // 钉钉内未登录：允许访问，App.vue 会自动触发登录
  next()
})

export default router
