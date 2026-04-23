import { createRouter, createWebHistory } from 'vue-router'
import AchievementList from '../views/AchievementList.vue'
import AchievementDetail from '../views/AchievementDetail.vue'
import PreRegister from '../views/PreRegister.vue'
import ChangeAchievement from '../views/ChangeAchievement.vue'
import RecordAchievement from '../views/RecordAchievement.vue'
import RegisterAchievement from '../views/RegisterAchievement.vue'

const routes = [
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
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
