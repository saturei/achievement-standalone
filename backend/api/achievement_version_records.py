from typing import Optional
from datetime import datetime
from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.orm import Session
from sqlalchemy import and_
from app.db.database import get_db
from app.models.user import User
from app.models.achievement_version_record import AchievementVersionRecord
from app.schemas.achievement_version_record import (
    AchievementVersionRecordResponse,
    AchievementVersionRecordListResponse,
)
from app.core.security import get_current_active_user

router = APIRouter()


def extract_achievement_name(achievement_id: str) -> str:
    """从achievement_id中提取achievement_name
    
    achievement_id格式通常为: "成果名称_版本号"
    例如: "企业手机业务规划及架构设计_V1.0.1" -> "企业手机业务规划及架构设计"
    """
    if '_' in achievement_id:
        parts = achievement_id.rsplit('_', 1)
        return parts[0]
    return achievement_id


@router.get("", response_model=AchievementVersionRecordListResponse)
async def get_achievement_version_records(
    page: int = Query(1, ge=1, description="页码"),
    page_size: int = Query(20, ge=1, le=100, description="每页数量"),
    achievement_name: Optional[str] = Query(None, description="成果名称筛选"),
    start_time: Optional[datetime] = Query(None, description="开始时间"),
    end_time: Optional[datetime] = Query(None, description="结束时间"),
    risk_tag: Optional[str] = Query(None, description="风险标签筛选"),
    db: Session = Depends(get_db)
):
    """查询所有变更记录，支持多条件筛选和分页"""
    query = db.query(AchievementVersionRecord)
    
    if achievement_name:
        query = query.filter(AchievementVersionRecord.achievement_name.contains(achievement_name))
    
    if start_time:
        query = query.filter(AchievementVersionRecord.change_time >= start_time)
    
    if end_time:
        query = query.filter(AchievementVersionRecord.change_time <= end_time)
    
    if risk_tag:
        query = query.filter(AchievementVersionRecord.risk_tags.contains([risk_tag]))
    
    query = query.order_by(AchievementVersionRecord.change_time.desc())
    
    total = query.count()
    records = query.offset((page - 1) * page_size).limit(page_size).all()
    
    return AchievementVersionRecordListResponse(
        total=total,
        page=page,
        page_size=page_size,
        items=records
    )


@router.get("/{achievement_id}", response_model=AchievementVersionRecordListResponse)
async def get_achievement_version_records_by_id(
    achievement_id: str,
    page: int = Query(1, ge=1, description="页码"),
    page_size: int = Query(20, ge=1, le=100, description="每页数量"),
    db: Session = Depends(get_db)
):
    """查询指定成果的变更记录
    
    使用achievement_name查询，因为变更记录存储的是变更前的achievement_id，
    而前端传入的是当前的achievement_id（变更后可能版本号已变化）
    """
    achievement_name = extract_achievement_name(achievement_id)
    
    query = db.query(AchievementVersionRecord).filter(
        AchievementVersionRecord.achievement_name == achievement_name
    ).order_by(AchievementVersionRecord.change_time.desc())
    
    total = query.count()
    records = query.offset((page - 1) * page_size).limit(page_size).all()
    
    return AchievementVersionRecordListResponse(
        total=total,
        page=page,
        page_size=page_size,
        items=records
    )
