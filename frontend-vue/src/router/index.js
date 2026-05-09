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
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
