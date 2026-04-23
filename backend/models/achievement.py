from sqlalchemy import Column, String, DateTime, Text, ForeignKey, func, JSON, UniqueConstraint, Date
from sqlalchemy.orm import relationship
from app.db.database import Base


class Achievement(Base):
    __tablename__ = "achievements"

    id = Column(String(50), primary_key=True, index=True)
    
    # ==================== 基础数据 ====================
    product_id = Column(String(50), nullable=False, index=True, comment="所属产品ID")
    product_name = Column(String(200), comment="所属产品名称")
    organization_id = Column(String(50), index=True, comment="所属机构ID")
    organization_name = Column(String(200), comment="所属机构名称")
    department_id = Column(String(50), index=True, comment="所属部门ID")
    department_name = Column(String(200), comment="所属部门名称")
    name = Column(String(200), nullable=False, comment="成果名称")
    version = Column(String(50), nullable=True, comment="成果版本，格式：V+版本号")
    product_external_version = Column(String(50), nullable=True, index=True, comment="产品对外版本号")
    has_baseline = Column(String(20), default="无基线", comment="是否有基线：有基线/无基线")
    requirement_proposer = Column(String(100), comment="成果需求提出人")
    achievement_form = Column(String(100), comment="成果形态")
    sale_type = Column(String(100), comment="成果可售类型")
    function_list_file = Column(String(500), comment="成果对应功能清单文件路径")
    package_ids = Column(JSON, comment="所属套餐ID（JSON数组格式）")
    application_scenario = Column(Text, comment="应用场景描述，多个用；隔开")
    module_id = Column(String(50), index=True, comment="所属模块ID")
    module_name = Column(String(200), comment="所属模块名称")
    type = Column(String(50), index=True, comment="成果类型")
    description = Column(Text, comment="成果描述")
    owner = Column(String(100), index=True, comment="负责人")
    
    # ==================== 计划数据 ====================
    achievement_target = Column(Text, comment="成果目标描述")
    planned_acceptance_date = Column(Date, comment="计划验收日期")
    acceptance_method = Column(Text, comment="验收方式描述")
    acceptor = Column(String(200), comment="成果验收人，可多个")
    related_project_id = Column(String(50), comment="关联项目ID")
    related_project_name = Column(String(200), comment="关联项目名称")
    related_order_id = Column(String(50), comment="关联订单编号")
    related_order_name = Column(String(200), comment="关联订单名称")
    acceptance_requirements = Column(Text, comment="验收要求")
    acceptance_organization = Column(String(200), comment="验收机构")
    
    # ==================== 变更数据 ====================
    change_reason = Column(Text, comment="变更原因（当期未验收的措施）")
    
    # ==================== 实际数据 ====================
    deliverables = Column(Text, comment="成果提交物")
    code_repository_url = Column(String(500), comment="代码仓库/在线文档地址")
    demo_url = Column(String(500), comment="DEMO地址")
    actual_acceptance_date = Column(Date, comment="实际验收日期")
    
    # ==================== 系统审计数据 ====================
    status = Column(String(50), default="pre_register", index=True, comment="成果状态")
    estimated_acceptance_month = Column(String(20), comment="预估验收年月，用于筛选统计")
    pre_register_time = Column(DateTime(timezone=True), comment="预注册时间")
    register_time = Column(DateTime(timezone=True), comment="注册时间")
    record_time = Column(DateTime(timezone=True), comment="登记时间")
    created_by = Column(String(100), comment="创建人")
    updated_by = Column(String(100), comment="更新人")
    created_at = Column(DateTime(timezone=True), server_default=func.now(), comment="创建时间")
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), comment="最后更新时间")
    
    # 其他字段
    risk_tags = Column(JSON, comment="关联的风险标签ID（JSON数组格式）")
    
    # 兼容旧字段（保留以支持现有数据）
    project_target = Column(Text, comment="成果对应项目目标（已弃用，使用achievement_target）")

    def __repr__(self):
        return f"<Achievement(id={self.id}, name={self.name}, version={self.version})>"

    def to_dict(self):
        return {
            "id": self.id,
            # 基础数据
            "product_id": self.product_id,
            "product_name": self.product_name,
            "organization_id": self.organization_id,
            "organization_name": self.organization_name,
            "department_id": self.department_id,
            "department_name": self.department_name,
            "name": self.name,
            "version": self.version,
            "product_external_version": self.product_external_version,
            "has_baseline": self.has_baseline,
            "requirement_proposer": self.requirement_proposer,
            "achievement_form": self.achievement_form,
            "sale_type": self.sale_type,
            "function_list_file": self.function_list_file,
            "package_ids": self.package_ids,
            "application_scenario": self.application_scenario,
            "module_id": self.module_id,
            "module_name": self.module_name,
            "type": self.type,
            "description": self.description,
            "owner": self.owner,
            # 计划数据
            "achievement_target": self.achievement_target or self.project_target,
            "planned_acceptance_date": self.planned_acceptance_date.isoformat() if self.planned_acceptance_date else None,
            "acceptance_method": self.acceptance_method,
            "acceptor": self.acceptor,
            "related_project_id": self.related_project_id,
            "related_project_name": self.related_project_name,
            "related_order_id": self.related_order_id,
            "related_order_name": self.related_order_name,
            "acceptance_requirements": self.acceptance_requirements,
            "acceptance_organization": self.acceptance_organization,
            # 变更数据
            "change_reason": self.change_reason,
            # 实际数据
            "deliverables": self.deliverables,
            "code_repository_url": self.code_repository_url,
            "demo_url": self.demo_url,
            "actual_acceptance_date": self.actual_acceptance_date.isoformat() if self.actual_acceptance_date else None,
            # 系统审计数据
            "status": self.status,
            "estimated_acceptance_month": self.estimated_acceptance_month,
            "pre_register_time": self.pre_register_time.isoformat() if self.pre_register_time else None,
            "register_time": self.register_time.isoformat() if self.register_time else None,
            "record_time": self.record_time.isoformat() if self.record_time else None,
            "created_by": self.created_by,
            "updated_by": self.updated_by,
            "created_at": self.created_at.isoformat() if self.created_at else None,
            "updated_at": self.updated_at.isoformat() if self.updated_at else None,
            # 其他
            "risk_tags": self.risk_tags,
        }
