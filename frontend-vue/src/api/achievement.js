import api from './request'

export default {
  // 获取成果列表
  getAchievements(params) {
    return api.get('/achievements', { params })
  },

  // 获取成果详情
  getAchievement(id) {
    return api.get(`/achievements/${id}`)
  },

  // 获取成果统计
  getStatistics() {
    return api.get('/achievements/statistics')
  },

  // 成果预注册
  preRegister(data) {
    return api.post('/achievements/pre-register', data)
  },

  // 成果注册
  register(id, data) {
    return api.post(`/achievements/${id}/register`, data)
  },

  // 成果登记
  record(id, data) {
    return api.post(`/achievements/${id}/record`, data)
  },

  // 成果变更
  change(id, data) {
    return api.post(`/achievements/${id}/change`, data)
  },

  // 成果下架
  offline(id, data) {
    return api.put(`/achievements/${id}/offline`, data)
  },

  // 成果上架
  online(id, data) {
    return api.put(`/achievements/${id}/online`, data)
  },

  // 成果删除
  delete(id) {
    return api.put(`/achievements/${id}/delete`)
  },

  // 获取变更历史
  getHistory(id) {
    return api.get(`/achievements/${id}/history`)
  },

  // 获取机构列表
  getOrganizations() {
    return api.get('/achievements/organizations')
  }
}
