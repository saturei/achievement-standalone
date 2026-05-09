import api from './request'

export default {
  getAchievements(params) {
    return api.get('/achievements', { params })
  },

  getAchievement(id) {
    return api.get(`/achievements/${id}`)
  },

  getStatistics() {
    return api.get('/achievements/statistics')
  },

  preRegister(data) {
    return api.post('/achievements/pre-register', data)
  },

  register(id, data) {
    return api.post(`/achievements/${id}/register`, data)
  },

  record(id, data) {
    return api.post(`/achievements/${id}/record`, data)
  },

  change(id, data) {
    return api.post(`/achievements/${id}/change`, data)
  },

  offline(id, data) {
    return api.put(`/achievements/${id}/offline`, data)
  },

  online(id, data) {
    return api.put(`/achievements/${id}/online`, data)
  },

  delete(id) {
    return api.put(`/achievements/${id}/delete`)
  },

  getHistory(id) {
    return api.get(`/achievements/${id}/history`)
  },

  getOrganizations() {
    return api.get('/achievements/organizations')
  },

  getDepartments() {
    return api.get('/achievements/departments')
  },

  getFilterOptions(params) {
    return api.get('/achievements/filter-options', { params })
  }
}
