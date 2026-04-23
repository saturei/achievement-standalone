from sqlalchemy import Column, String, DateTime, Text, Float, JSON
from sqlalchemy.orm import relationship
from app.db.database import Base


class AchievementVersionRecord(Base):
    """成果变更记录表 - 专门记录所有成果的版本变更历史"""
    __tablename__ = "achievement_version_records"

    id = Column(String(50), primary_key=True, index=True, comment="变更记录ID")
    achievement_name = Column(String(100), nullable=False, index=True, comment="成果名称")
    achievement_id = Column(String(50), nullable=False, index=True, comment="成果ID（基于achievement_name + product_external_version）")
    product_external_version = Column(String(20), nullable=False, index=True, comment="产品对外版本号")
    from_version = Column(String(20), nullable=False, comment="变更前版本号")
    to_version = Column(String(20), nullable=False, comment="变更后版本号")
    changed_fields = Column(JSON, nullable=False, comment="变更字段（JSON格式，记录变更前后的字段值差异）")
    change_description = Column(Text, comment="变更说明")
    risk_tags = Column(JSON, comment="关联的风险标签ID（JSON数组格式）")
    operator = Column(String(50), comment="操作人")
    change_time = Column(DateTime(timezone=True), nullable=False, comment="变更时间")
    created_at = Column(DateTime(timezone=True), nullable=False, comment="创建时间")

    def __repr__(self):
        return f"<AchievementVersionRecord(id={self.id}, achievement_name={self.achievement_name}, from_version={self.from_version}, to_version={self.to_version})>"
