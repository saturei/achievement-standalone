from pydantic import BaseModel, Field
from typing import Optional, List, Dict, Any
from datetime import datetime


class AchievementVersionRecordBase(BaseModel):
    achievement_name: str = Field(..., description="成果名称")
    achievement_id: str = Field(..., description="成果ID")
    product_external_version: str = Field(..., description="产品对外版本号")
    from_version: str = Field(..., description="变更前版本号")
    to_version: str = Field(..., description="变更后版本号")
    changed_fields: Dict[str, Any] = Field(..., description="变更字段（JSON格式，记录变更前后的字段值差异）")
    change_description: Optional[str] = Field(None, description="变更说明")
    risk_tags: Optional[List[str]] = Field(None, description="关联的风险标签ID")
    operator: Optional[str] = Field(None, description="操作人")


class AchievementChangeRequest(BaseModel):
    """成果变更请求Schema"""
    achievement_name: str = Field(..., description="成果名称")
    achievement_id: str = Field(..., description="成果ID")
    product_external_version: str = Field(..., description="产品对外版本号")
    from_version: str = Field(..., description="变更前版本号")
    to_version: str = Field(..., description="变更后版本号")
    changed_fields: Dict[str, Any] = Field(..., description="变更字段（JSON格式，记录变更前后的字段值差异）")
    change_description: Optional[str] = Field(None, description="变更说明")
    risk_tags: Optional[List[str]] = Field(None, description="关联的风险标签ID")
    operator: Optional[str] = Field(None, description="操作人")


class AchievementVersionRecordResponse(AchievementVersionRecordBase):
    id: str
    change_time: datetime
    created_at: datetime
    
    class Config:
        from_attributes = True


class AchievementVersionRecordListResponse(BaseModel):
    total: int
    page: int
    page_size: int
    items: List[AchievementVersionRecordResponse]
