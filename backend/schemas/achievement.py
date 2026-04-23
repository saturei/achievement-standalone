from pydantic import BaseModel, Field
from typing import Optional, List
from datetime import datetime, date


class AchievementBase(BaseModel):
    """成果基础模型 - 按五大分类组织字段"""
    
    # ==================== 基础数据 ====================
    product_id: str = Field(..., description="所属产品ID")
    product_name: Optional[str] = Field(None, description="所属产品名称")
    organization_id: Optional[str] = Field(None, description="所属机构ID")
    organization_name: Optional[str] = Field(None, description="所属机构名称")
    department_id: Optional[str] = Field(None, description="所属部门ID")
    department_name: Optional[str] = Field(None, description="所属部门名称")
    name: str = Field(..., description="成果名称")
    version: Optional[str] = Field(None, description="成果版本，格式：V+版本号")
    product_external_version: Optional[str] = Field(None, description="产品对外版本")
    has_baseline: Optional[str] = Field("无基线", description="是否有基线：有基线/无基线")
    requirement_proposer: Optional[str] = Field(None, description="成果需求提出人")
    achievement_form: Optional[str] = Field(None, description="成果形态")
    sale_type: Optional[str] = Field(None, description="成果可售类型")
    function_list_file: Optional[str] = Field(None, description="成果对应功能清单文件路径")
    package_ids: Optional[List[str]] = Field(None, description="所属套餐ID列表")
    application_scenario: Optional[str] = Field(None, description="应用场景描述，多个用；隔开")
    module_id: Optional[str] = Field(None, description="所属模块ID")
    module_name: Optional[str] = Field(None, description="所属模块名称")
    type: Optional[str] = Field(None, description="成果类型")
    description: Optional[str] = Field(None, description="成果描述")
    owner: Optional[str] = Field(None, description="负责人")
    
    # ==================== 计划数据 ====================
    achievement_target: Optional[str] = Field(None, description="成果目标描述")
    planned_acceptance_date: Optional[date] = Field(None, description="计划验收日期")
    acceptance_method: Optional[str] = Field(None, description="验收方式描述")
    acceptor: Optional[str] = Field(None, description="成果验收人，可多个")
    related_project_id: Optional[str] = Field(None, description="关联项目ID")
    related_project_name: Optional[str] = Field(None, description="关联项目名称")
    related_order_id: Optional[str] = Field(None, description="关联订单编号")
    related_order_name: Optional[str] = Field(None, description="关联订单名称")
    acceptance_requirements: Optional[str] = Field(None, description="验收要求")
    acceptance_organization: Optional[str] = Field(None, description="验收机构")
    
    # ==================== 变更数据 ====================
    change_reason: Optional[str] = Field(None, description="变更原因（当期未验收的措施）")
    
    # ==================== 实际数据 ====================
    deliverables: Optional[str] = Field(None, description="成果提交物")
    code_repository_url: Optional[str] = Field(None, description="代码仓库/在线文档地址")
    demo_url: Optional[str] = Field(None, description="DEMO地址")
    actual_acceptance_date: Optional[date] = Field(None, description="实际验收日期")
    
    # ==================== 系统审计数据 ====================
    status: Optional[str] = Field("pre_register", description="成果状态")
    estimated_acceptance_month: Optional[str] = Field(None, description="预估验收年月")
    pre_register_time: Optional[datetime] = Field(None, description="预注册时间")
    register_time: Optional[datetime] = Field(None, description="注册时间")
    record_time: Optional[datetime] = Field(None, description="登记时间")
    created_by: Optional[str] = Field(None, description="创建人")
    updated_by: Optional[str] = Field(None, description="更新人")
    
    # 其他字段
    risk_tags: Optional[List[str]] = Field(None, description="关联的风险标签ID")


class AchievementCreate(AchievementBase):
    """创建成果"""
    pass


class AchievementUpdate(BaseModel):
    """更新成果"""
    # 基础数据
    product_id: Optional[str] = None
    product_name: Optional[str] = None
    organization_id: Optional[str] = None
    organization_name: Optional[str] = None
    department_id: Optional[str] = None
    department_name: Optional[str] = None
    name: Optional[str] = None
    version: Optional[str] = None
    product_external_version: Optional[str] = None
    has_baseline: Optional[str] = None
    requirement_proposer: Optional[str] = None
    achievement_form: Optional[str] = None
    sale_type: Optional[str] = None
    function_list_file: Optional[str] = None
    package_ids: Optional[List[str]] = None
    application_scenario: Optional[str] = None
    module_id: Optional[str] = None
    module_name: Optional[str] = None
    type: Optional[str] = None
    description: Optional[str] = None
    owner: Optional[str] = None
    # 计划数据
    achievement_target: Optional[str] = None
    planned_acceptance_date: Optional[date] = None
    acceptance_method: Optional[str] = None
    acceptor: Optional[str] = None
    related_project_id: Optional[str] = None
    related_project_name: Optional[str] = None
    related_order_id: Optional[str] = None
    related_order_name: Optional[str] = None
    acceptance_requirements: Optional[str] = None
    acceptance_organization: Optional[str] = None
    # 变更数据
    change_reason: Optional[str] = None
    # 实际数据
    deliverables: Optional[str] = None
    code_repository_url: Optional[str] = None
    demo_url: Optional[str] = None
    actual_acceptance_date: Optional[date] = None
    # 系统审计数据
    status: Optional[str] = None
    estimated_acceptance_month: Optional[str] = None
    pre_register_time: Optional[datetime] = None
    register_time: Optional[datetime] = None
    record_time: Optional[datetime] = None
    updated_by: Optional[str] = None
    # 其他
    risk_tags: Optional[List[str]] = None


class AchievementResponse(AchievementBase):
    """成果响应模型"""
    id: str
    created_at: datetime
    updated_at: datetime
    risk_tags: Optional[List[str]] = None
    package_ids: Optional[List[str]] = None
    
    class Config:
        from_attributes = True


class AchievementVersionResponse(AchievementResponse):
    """成果版本响应模型"""
    pass


class AchievementHistoryResponse(BaseModel):
    """成果版本历史响应模型"""
    total: int = Field(..., description="历史版本总数")
    page: int = Field(..., description="当前页码")
    page_size: int = Field(..., description="每页数量")
    items: List[AchievementVersionResponse] = Field(..., description="历史版本列表")


class AchievementChangeVersion(BaseModel):
    """成果变更版本模型 - 新版本"""
    change_description: str = Field(..., description="变更说明")
    description: Optional[str] = Field(None, description="成果描述")
    owner: Optional[str] = Field(None, description="负责人")
    related_project_id: Optional[str] = Field(None, description="关联项目ID")
    related_order_id: Optional[str] = Field(None, description="关联订单ID")
    acceptance_requirements: Optional[str] = Field(None, description="验收要求")
    register_time: Optional[datetime] = Field(None, description="计划完工时间")
    risk_tags: Optional[List[str]] = Field(None, description="手动选择或添加的其他风险标签")
    product_external_version: Optional[str] = Field(None, description="成果对外版本")
    module_id: Optional[str] = Field(None, description="所属模块ID")
    module_name: Optional[str] = Field(None, description="所属模块名称")


class AchievementDelayVersion(BaseModel):
    """成果延期版本模型"""
    version_type: str = Field("延期", description="版本类型：延期")
    delay_reason: str = Field(..., description="延期原因")


class AchievementCompleteVersion(BaseModel):
    """成果完成版本模型"""
    version_type: str = Field("完成", description="版本类型：完成")
    acceptance_result: str = Field(..., description="验收结果")


class AchievementListResponse(BaseModel):
    total: int
    page: int
    page_size: int
    items: List[AchievementResponse]


class AchievementPreRegister(BaseModel):
    """成果预注册模型"""
    name: str
    version: Optional[str] = None
    product_id: str
    product_external_version: Optional[str] = None
    organization_id: Optional[str] = None
    organization_name: Optional[str] = None
    department_id: Optional[str] = None
    department_name: Optional[str] = None
    has_baseline: Optional[str] = "无基线"
    requirement_proposer: Optional[str] = None
    achievement_form: Optional[str] = None
    sale_type: Optional[str] = None
    application_scenario: Optional[str] = None
    type: Optional[str] = None
    description: Optional[str] = None
    achievement_target: Optional[str] = None
    planned_acceptance_date: Optional[date] = None
    acceptance_method: Optional[str] = None
    acceptor: Optional[str] = None
    related_project_id: Optional[str] = None
    related_project_name: Optional[str] = None
    related_order_id: Optional[str] = None
    related_order_name: Optional[str] = None
    acceptance_requirements: Optional[str] = None
    owner: Optional[str] = None
    risk_tags: Optional[List[str]] = None


class AchievementRegister(BaseModel):
    """成果注册模型"""
    register_time: Optional[datetime] = None
    register_notes: Optional[str] = None


class AchievementRecord(BaseModel):
    """成果登记模型"""
    record_time: Optional[datetime] = None
    record_notes: Optional[str] = None
    related_order_id: Optional[str] = None
    related_project_id: Optional[str] = None


class AchievementCheckout(BaseModel):
    """成果出库模型"""
    purpose: str
    user_id: str


class AchievementStatistics(BaseModel):
    """成果统计模型"""
    total_count: int
    pre_register_count: int
    register_count: int
    record_count: int
    offline_count: int = Field(0, description="下架成果数量")
    deleted_count: int = Field(0, description="已删除成果数量")
    by_status: dict
    by_type: dict
    by_product: dict
    checkout_count: int


class AchievementStatusRecordBase(BaseModel):
    """成果状态变更记录基础模型"""
    achievement_id: str = Field(..., description="成果ID")
    achievement_name: str = Field(..., description="成果名称")
    from_status: str = Field(..., description="变更前状态")
    to_status: str = Field(..., description="变更后状态")
    change_type: str = Field(..., description="变更类型：上架/下架/删除")
    change_reason: Optional[str] = Field(None, description="变更原因")
    operator: Optional[str] = Field(None, description="操作人")


class AchievementStatusRecordCreate(AchievementStatusRecordBase):
    pass


class AchievementStatusRecordResponse(AchievementStatusRecordBase):
    id: str
    change_time: datetime
    created_at: datetime
    
    class Config:
        from_attributes = True


class AchievementStatusRecordListResponse(BaseModel):
    total: int
    page: int
    page_size: int
    items: List[AchievementStatusRecordResponse]


class AchievementOfflineRequest(BaseModel):
    """成果下架请求"""
    reason: Optional[str] = Field(None, description="下架原因")


class AchievementOnlineRequest(BaseModel):
    """成果上架请求"""
    reason: Optional[str] = Field(None, description="上架原因")
