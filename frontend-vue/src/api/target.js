import request from './request';

export const targetApi = {
  // 获取目标统计数据
  getStatistics: (params) => request.get(`/targets/statistics`, { params }),
  
  // 获取月度分布数据
  getMonthlyDistribution: (year) => request.get(`/targets/distribution`, { params: { year } }),
  
  // 保存实际数据
  saveActualData: (data) => request.post('/targets/actual', data),
  
  // 导入目标数据
  importTargets: (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return request.post('/targets/import', formData, { headers: { 'Content-Type': 'multipart/form-data' } });
  },
  
  // 获取目标详情列表
  getTargetList: (params) => request.get('/targets/list', { params }),
  
  // 更新实际数据
  updateActualData: (id, data) => request.put(`/targets/actual/${id}`, data),
  
  // 获取所有产品列表
  getAllProducts: () => request.get('/targets/products'),
  
  // 获取所有机构列表
  getAllOrganizations: () => request.get('/targets/organizations'),
  
  // 获取所有负责人列表
  getAllOwners: () => request.get('/targets/owners'),
  
  // 创建新目标
  createTarget: (data) => request.post('/targets', data),

  // 更新目标
  updateTarget: (id, data) => request.put(`/targets/${id}`, data),

  // 删除目标
  deleteTarget: (id) => request.delete(`/targets/${id}`),

  // 获取季度汇总数据
  getQuarterlySummary: (params) => request.get('/targets/quarterly-summary', { params })
};

export default targetApi;
