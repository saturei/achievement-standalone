// 成果管理相关类型定义

export interface Achievement {
  id: string
  name: string
  version: string
  productVersion: string
  productExternalVersion: string
  versionStatus: string
  versionType: string
  changeDescription: string
  delayReason: string
  acceptanceResult: string
  productId: string
  productName: string
  moduleId: string
  moduleName: string
  type: AchievementType
  description: string
  status: AchievementStatus
  owner: string
  preRegisterTime: string
  registerTime: string | null
  recordTime: string | null
  relatedOrderId: string | null
  relatedProjectId: string | null
  acceptanceRequirements: string
  attachments: AchievementAttachment[]
  checkoutHistory: AchievementCheckout[]
  versionHistory: AchievementVersion[]
  createdAt: string
  updatedAt: string
}

export type AchievementType = "模块" | "功能" | "资产" | "文档" | "其他"
export type AchievementStatus = "预注册" | "注册" | "登记" | "下架" | "已删除"

export interface AchievementAttachment {
  id: string
  name: string
  type: string
  size: number
  uploadTime: string
  uploader: string
  url: string
}

export interface AchievementCheckout {
  id: string
  achievementId: string
  achievementVersion: string
  userId: string
  userName: string
  checkoutTime: string
  purpose: string
  downloadCount: number
  returnTime: string | null
}

export interface AchievementVersion {
  id: string
  version: string
  fromVersion: string
  toVersion: string
  changeDescription: string
  changedFields: Record<string, { from: any; to: any }>
  riskTags: string[]
  operator: string
  changeTime: string
}

export interface AchievementStatistics {
  total_count: number
  pre_register_count: number
  register_count: number
  record_count: number
  offline_count: number
  by_status: Record<string, number>
  by_type: Record<string, number>
  by_product: Record<string, number>
  checkout_count: number
}

export interface Product {
  id: string
  name: string
}

export interface ProductModule {
  id: string
  productId: string
  name: string
}

export interface AchievementProcess {
  id: string
  achievementId: string
  step: AchievementProcessStep
  status: "completed" | "active" | "pending"
  date: string
  owner: string
  notes: string
}

export type AchievementProcessStep = "已登记" | "排产" | "验收评审" | "登记"

export interface AchievementMeeting {
  id: string
  achievementId: string
  processStep: AchievementProcessStep
  name: string
  date: string
  attendees: string[]
  status: "scheduled" | "completed" | "cancelled"
  notes: string
}

export interface AchievementFunction {
  id: string
  achievementId: string
  processStep: AchievementProcessStep
  name: string
  description: string
  owner: string
  status: "pending" | "in-progress" | "completed"
  startDate: string
  endDate: string
}

export interface AchievementApproval {
  id: string
  achievementId: string
  processStep: AchievementProcessStep
  name: string
  approver: string
  status: "pending" | "approved" | "rejected"
  date: string
  comments: string
}

// API请求/响应类型
export interface AchievementListResponse {
  total: number
  page: number
  page_size: number
  items: Achievement[]
}

export interface AchievementPreRegisterRequest {
  name: string
  version?: string
  product_id: string
  product_external_version?: string
  organization_id?: string
  organization_name?: string
  department_id?: string
  department_name?: string
  has_baseline?: string
  requirement_proposer?: string
  achievement_form?: string
  sale_type?: string
  application_scenario?: string
  type?: string
  description?: string
  achievement_target?: string
  planned_acceptance_date?: string
  acceptance_method?: string
  acceptor?: string
  related_project_id?: string
  related_project_name?: string
  related_order_id?: string
  related_order_name?: string
  acceptance_requirements?: string
  owner?: string
  risk_tags?: string[]
}

export interface AchievementChangeRequest {
  change_description: string
  description?: string
  owner?: string
  related_project_id?: string
  related_order_id?: string
  acceptance_requirements?: string
  register_time?: string
  risk_tags?: string[]
  product_external_version?: string
  module_id?: string
  module_name?: string
}

export interface AchievementOfflineRequest {
  reason?: string
}

export interface AchievementOnlineRequest {
  reason?: string
}

export interface AchievementCheckoutRequest {
  purpose: string
  user_id: string
}

// 状态变更记录类型
export interface AchievementStatusRecord {
  id: string
  achievement_id: string
  achievement_name: string
  from_status: string
  to_status: string
  change_type: string
  change_reason?: string
  operator?: string
  change_time: string
  created_at: string
}

export interface AchievementStatusRecordListResponse {
  total: number
  page: number
  page_size: number
  items: AchievementStatusRecord[]
}

// 版本记录类型
export interface AchievementVersionRecord {
  id: string
  achievement_name: string
  achievement_id: string
  product_external_version: string
  from_version: string
  to_version: string
  changed_fields: Record<string, { old: any; new: any }>
  change_description?: string
  risk_tags?: string[]
  operator?: string
  change_time: string
  created_at: string
}

export interface AchievementVersionRecordListResponse {
  total: number
  page: number
  page_size: number
  items: AchievementVersionRecord[]
}
