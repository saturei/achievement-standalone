from typing import Optional
from datetime import datetime
from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.orm import Session
from sqlalchemy import or_, func
from app.db.database import get_db
from app.models.user import User
from app.models.achievement import Achievement
from app.schemas.achievement import (
    AchievementResponse,
    AchievementListResponse,
    AchievementCreate,
    AchievementUpdate,
    AchievementPreRegister,
    AchievementRegister,
    AchievementRecord,
    AchievementCheckout,
    AchievementChangeVersion,
    AchievementDelayVersion,
    AchievementCompleteVersion,
    AchievementHistoryResponse,
    AchievementVersionResponse,
    AchievementStatistics,
    AchievementOfflineRequest,
    AchievementOnlineRequest,
    AchievementStatusRecordResponse,
    AchievementStatusRecordListResponse,
)
from app.core.security import get_current_active_user

router = APIRouter()


@router.get("", response_model=AchievementListResponse)
async def get_achievements(
    page: int = Query(1, ge=1, description="页码"),
    page_size: int = Query(20, ge=1, le=100, description="每页数量"),
    status: Optional[str] = Query(None, description="状态筛选"),
    type: Optional[str] = Query(None, description="类型筛选"),
    product_id: Optional[str] = Query(None, description="产品ID筛选"),
    keyword: Optional[str] = Query(None, description="搜索关键词"),
    include_deleted: bool = Query(False, description="是否包含已删除记录"),
    db: Session = Depends(get_db)
):
    query = db.query(Achievement)
    
    if status:
        query = query.filter(Achievement.status == status)
    elif not include_deleted:
        query = query.filter(Achievement.status != "deleted")
    
    if type:
        query = query.filter(Achievement.type == type)
    if product_id:
        query = query.filter(Achievement.product_id == product_id)
    if keyword:
        query = query.filter(
            or_(
                Achievement.name.contains(keyword),
                Achievement.description.contains(keyword),
                Achievement.id.contains(keyword)
            )
        )
    
    total = query.count()
    achievements = query.offset((page - 1) * page_size).limit(page_size).all()
    
    # 转换为响应模型
    items = []
    for achievement in achievements:
        achievement_dict = achievement.__dict__.copy()
        items.append(AchievementResponse(**achievement_dict))
    
    return AchievementListResponse(
        total=total,
        page=page,
        page_size=page_size,
        items=items
    )


@router.get("/statistics", response_model=AchievementStatistics)
async def get_achievement_statistics(
    db: Session = Depends(get_db)
):
    total_count = db.query(func.count(Achievement.id)).scalar()
    
    pre_register_count = db.query(func.count(Achievement.id)).filter(
        Achievement.status == "pre_register"
    ).scalar()
    
    register_count = db.query(func.count(Achievement.id)).filter(
        Achievement.status == "register"
    ).scalar()
    
    record_count = db.query(func.count(Achievement.id)).filter(
        Achievement.status == "record"
    ).scalar()
    
    offline_count = db.query(func.count(Achievement.id)).filter(
        Achievement.status == "offline"
    ).scalar()
    
    deleted_count = db.query(func.count(Achievement.id)).filter(
        Achievement.status == "deleted"
    ).scalar()
    
    by_status = {
        "pre_register": pre_register_count,
        "register": register_count,
        "record": record_count,
        "offline": offline_count,
        "deleted": deleted_count
    }
    
    by_type_result = db.query(
        Achievement.type,
        func.count(Achievement.id).label('count')
    ).group_by(Achievement.type).all()
    
    by_type = {item[0]: item[1] for item in by_type_result}
    
    by_product_result = db.query(
        Achievement.product_id,
        func.count(Achievement.id).label('count')
    ).group_by(Achievement.product_id).all()
    
    by_product = {item[0]: item[1] for item in by_product_result}
    
    return AchievementStatistics(
        total_count=total_count,
        pre_register_count=pre_register_count,
        register_count=register_count,
        record_count=record_count,
        offline_count=offline_count,
        by_status=by_status,
        by_type=by_type,
        by_product=by_product,
        checkout_count=0
    )


@router.get("/{achievement_id}", response_model=AchievementResponse)
async def get_achievement(
    achievement_id: str,
    db: Session = Depends(get_db)
):
    achievement = db.query(Achievement).filter(Achievement.id == achievement_id).first()
    if not achievement:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Achievement not found"
        )
    return achievement


@router.post("/pre-register", response_model=AchievementResponse)
async def pre_register_achievement(
    achievement_data: AchievementPreRegister,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """成果预登记 - 创建新成果版本"""
    from app.models.product import Product
    from app.utils.version_generator import VersionGenerator
    
    product = db.query(Product).filter(Product.id == achievement_data.product_id).first()
    product_external_version = achievement_data.product_external_version or (product.version if product else "V1.0.0")
    
    if achievement_data.version:
        version = achievement_data.version
    else:
        version = VersionGenerator.generateManagementVersion(
            db=db,
            achievement_name=achievement_data.name,
            product_external_version=product_external_version
        )
    
    achievement_id = f"{achievement_data.name}_{version}"
    
    achievement = Achievement(
        id=achievement_id,
        name=achievement_data.name,
        version=version,
        product_external_version=product_external_version,
        product_id=achievement_data.product_id,
        product_name=product.name if product else None,
        organization_id=achievement_data.organization_id,
        organization_name=achievement_data.organization_name,
        department_id=achievement_data.department_id,
        department_name=achievement_data.department_name,
        has_baseline=achievement_data.has_baseline or "无基线",
        requirement_proposer=achievement_data.requirement_proposer,
        achievement_form=achievement_data.achievement_form,
        sale_type=achievement_data.sale_type,
        application_scenario=achievement_data.application_scenario,
        type=achievement_data.type,
        description=achievement_data.description,
        status="pre_register",
        owner=achievement_data.owner or current_user.username,
        achievement_target=achievement_data.achievement_target,
        planned_acceptance_date=achievement_data.planned_acceptance_date,
        acceptance_method=achievement_data.acceptance_method,
        acceptor=achievement_data.acceptor,
        related_project_id=achievement_data.related_project_id,
        related_project_name=achievement_data.related_project_name,
        related_order_id=achievement_data.related_order_id,
        related_order_name=achievement_data.related_order_name,
        acceptance_requirements=achievement_data.acceptance_requirements,
        risk_tags=achievement_data.risk_tags,
        created_by=current_user.username,
    )
    
    db.add(achievement)
    db.commit()
    db.refresh(achievement)
    
    return achievement


@router.post("/{achievement_id}/register", response_model=AchievementResponse)
async def register_achievement(
    achievement_id: str,
    register_data: AchievementRegister,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    achievement = db.query(Achievement).filter(Achievement.id == achievement_id).first()
    if not achievement:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Achievement not found"
        )
    
    if achievement.status != "pre_register":
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Achievement is not in pre_register status"
        )
    
    achievement.status = "register"
    achievement.register_time = register_data.register_time or datetime.utcnow()
    
    db.commit()
    db.refresh(achievement)
    
    return achievement


@router.post("/{achievement_id}/record", response_model=AchievementResponse)
async def record_achievement(
    achievement_id: str,
    record_data: AchievementRecord,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    achievement = db.query(Achievement).filter(Achievement.id == achievement_id).first()
    if not achievement:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Achievement not found"
        )
    
    if achievement.status != "register":
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Achievement is not in register status"
        )
    
    achievement.status = "record"
    achievement.record_time = record_data.record_time or datetime.utcnow()
    achievement.related_order_id = record_data.related_order_id
    achievement.related_project_id = record_data.related_project_id
    
    db.commit()
    db.refresh(achievement)
    
    return achievement


@router.post("/{achievement_id}/change", response_model=AchievementResponse)
async def change_achievement(
    achievement_id: str,
    change_data: AchievementChangeVersion,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """成果变更 - 支持多种变更类型"""
    from app.models.achievement_version_record import AchievementVersionRecord
    from app.utils.version_generator import VersionGenerator
    import uuid
    
    achievement = db.query(Achievement).filter(Achievement.id == achievement_id).first()
    if not achievement:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Achievement not found"
        )
    
    change_time = datetime.utcnow()
    from_version = achievement.version
    
    changed_fields = {}
    
    if change_data.description is not None and change_data.description != achievement.description:
        changed_fields["description"] = {
            "old": achievement.description,
            "new": change_data.description
        }
        achievement.description = change_data.description
    
    if change_data.owner is not None and change_data.owner != achievement.owner:
        changed_fields["owner"] = {
            "old": achievement.owner,
            "new": change_data.owner
        }
        achievement.owner = change_data.owner
    
    if change_data.related_project_id is not None and change_data.related_project_id != achievement.related_project_id:
        changed_fields["related_project_id"] = {
            "old": achievement.related_project_id,
            "new": change_data.related_project_id
        }
        achievement.related_project_id = change_data.related_project_id
    
    if change_data.related_order_id is not None and change_data.related_order_id != achievement.related_order_id:
        changed_fields["related_order_id"] = {
            "old": achievement.related_order_id,
            "new": change_data.related_order_id
        }
        achievement.related_order_id = change_data.related_order_id
    
    if change_data.acceptance_requirements is not None and change_data.acceptance_requirements != achievement.acceptance_requirements:
        changed_fields["acceptance_requirements"] = {
            "old": achievement.acceptance_requirements,
            "new": change_data.acceptance_requirements
        }
        achievement.acceptance_requirements = change_data.acceptance_requirements
    
    if change_data.register_time is not None and change_data.register_time != achievement.register_time:
        changed_fields["register_time"] = {
            "old": achievement.register_time.isoformat() if achievement.register_time else None,
            "new": change_data.register_time.isoformat()
        }
        achievement.register_time = change_data.register_time
    
    if change_data.product_external_version is not None and change_data.product_external_version != achievement.product_external_version:
        changed_fields["product_external_version"] = {
            "old": achievement.product_external_version,
            "new": change_data.product_external_version
        }
        achievement.product_external_version = change_data.product_external_version
    
    if change_data.module_id is not None and change_data.module_id != achievement.module_id:
        changed_fields["module_id"] = {
            "old": achievement.module_id,
            "new": change_data.module_id
        }
        achievement.module_id = change_data.module_id
    
    if change_data.module_name is not None and change_data.module_name != achievement.module_name:
        changed_fields["module_name"] = {
            "old": achievement.module_name,
            "new": change_data.module_name
        }
        achievement.module_name = change_data.module_name
    
    if not changed_fields:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="No fields to change"
        )
    
    auto_risk_tags = []
    
    if "register_time" in changed_fields:
        old_time = achievement.register_time
        new_time = change_data.register_time
        if old_time and new_time:
            if new_time > old_time:
                auto_risk_tags.append("延期")
            elif new_time < old_time:
                auto_risk_tags.append("提前完成")
    
    current_risk_tags = achievement.risk_tags or []
    all_risk_tags = list(set(current_risk_tags + auto_risk_tags + (change_data.risk_tags or [])))
    achievement.risk_tags = all_risk_tags
    
    to_version = VersionGenerator.generateManagementVersion(
        db=db,
        achievement_name=achievement.name,
        product_external_version=achievement.product_external_version
    )
    
    achievement.version = to_version
    achievement.id = f"{achievement.name}_{to_version}"
    
    version_record = AchievementVersionRecord(
        id=str(uuid.uuid4()),
        achievement_name=achievement.name,
        achievement_id=achievement_id,
        product_external_version=achievement.product_external_version,
        from_version=from_version,
        to_version=to_version,
        changed_fields=changed_fields,
        change_description=change_data.change_description,
        risk_tags=all_risk_tags,
        operator=current_user.username,
        change_time=change_time,
        created_at=change_time,
    )
    
    db.add(version_record)
    db.commit()
    db.refresh(achievement)
    
    return achievement


@router.post("/{achievement_id}/delay", response_model=AchievementResponse)
async def delay_achievement(
    achievement_id: str,
    delay_data: AchievementDelayVersion,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """成果延期 - 更新版本状态"""
    achievement = db.query(Achievement).filter(Achievement.id == achievement_id).first()
    if not achievement:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Achievement not found"
        )
    
    # 更新成果
    achievement.description = delay_data.delay_reason
    
    db.commit()
    db.refresh(achievement)
    
    return achievement


@router.post("/{achievement_id}/complete", response_model=AchievementResponse)
async def complete_achievement(
    achievement_id: str,
    complete_data: AchievementCompleteVersion,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """成果完成 - 标记为完成状态"""
    achievement = db.query(Achievement).filter(Achievement.id == achievement_id).first()
    if not achievement:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Achievement not found"
        )
    
    # 更新成果
    achievement.description = complete_data.acceptance_result
    
    db.commit()
    db.refresh(achievement)
    
    return achievement


@router.get("/{achievement_id}/history", response_model=AchievementHistoryResponse)
async def get_achievement_history(
    achievement_id: str,
    page: int = Query(1, ge=1, description="页码"),
    page_size: int = Query(20, ge=1, le=100, description="每页数量"),
    db: Session = Depends(get_db)
):
    """获取成果版本历史"""
    # 先查询当前成果
    current_achievement = db.query(Achievement).filter(
        Achievement.id == achievement_id
    ).first()
    
    if not current_achievement:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Achievement not found"
        )
    
    # 查询所有同名成果
    query = db.query(Achievement).filter(
        Achievement.name == current_achievement.name
    ).order_by(Achievement.created_at.desc())
    
    total = query.count()
    achievements = query.offset((page - 1) * page_size).limit(page_size).all()
    
    return AchievementHistoryResponse(
        total=total,
        page=page,
        page_size=page_size,
        items=achievements
    )


@router.post("/{achievement_id}/checkout")
async def checkout_achievement(
    achievement_id: str,
    checkout_data: AchievementCheckout,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    achievement = db.query(Achievement).filter(Achievement.id == achievement_id).first()
    if not achievement:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Achievement not found"
        )
    
    if achievement.status == "offline":
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="成果已下架，不允许出库"
        )
    
    return {
        "message": "Achievement checked out successfully",
        "achievement_id": achievement_id,
        "purpose": checkout_data.purpose,
        "user_id": checkout_data.user_id,
        "checkout_time": datetime.utcnow()
    }


@router.put("/{achievement_id}/offline", response_model=AchievementResponse)
async def offline_achievement(
    achievement_id: str,
    offline_data: AchievementOfflineRequest,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """成果下架 - 不再允许出库"""
    from app.models.achievement_status_record import AchievementStatusRecord
    import uuid
    
    achievement = db.query(Achievement).filter(Achievement.id == achievement_id).first()
    if not achievement:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Achievement not found"
        )
    
    if achievement.status == "offline":
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="成果已处于下架状态"
        )
    
    from_status = achievement.status
    achievement.status = "offline"
    
    status_record = AchievementStatusRecord(
        id=str(uuid.uuid4()),
        achievement_id=achievement.id,
        achievement_name=achievement.name,
        from_status=from_status,
        to_status="offline",
        change_type="下架",
        change_reason=offline_data.reason,
        operator=current_user.username,
    )
    
    db.add(status_record)
    db.commit()
    db.refresh(achievement)
    
    return achievement


@router.put("/{achievement_id}/online", response_model=AchievementResponse)
async def online_achievement(
    achievement_id: str,
    online_data: AchievementOnlineRequest,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """成果上架 - 允许再次出库"""
    from app.models.achievement_status_record import AchievementStatusRecord
    import uuid
    
    achievement = db.query(Achievement).filter(Achievement.id == achievement_id).first()
    if not achievement:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Achievement not found"
        )
    
    if achievement.status != "offline":
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="只有下架状态的成果才能上架"
        )
    
    achievement.status = "record"
    
    status_record = AchievementStatusRecord(
        id=str(uuid.uuid4()),
        achievement_id=achievement.id,
        achievement_name=achievement.name,
        from_status="offline",
        to_status="record",
        change_type="上架",
        change_reason=online_data.reason,
        operator=current_user.username,
    )
    
    db.add(status_record)
    db.commit()
    db.refresh(achievement)
    
    return achievement


@router.put("/{achievement_id}/delete", response_model=AchievementResponse)
async def delete_achievement(
    achievement_id: str,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    """成果删除 - 逻辑删除预注册成果"""
    from app.models.achievement_status_record import AchievementStatusRecord
    import uuid
    
    achievement = db.query(Achievement).filter(Achievement.id == achievement_id).first()
    if not achievement:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Achievement not found"
        )
    
    if achievement.status != "pre_register":
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="只有预注册状态的成果才能删除"
        )
    
    from_status = achievement.status
    achievement.status = "deleted"
    
    status_record = AchievementStatusRecord(
        id=str(uuid.uuid4()),
        achievement_id=achievement.id,
        achievement_name=achievement.name,
        from_status=from_status,
        to_status="deleted",
        change_type="删除",
        change_reason="预注册成果删除",
        operator=current_user.username,
    )
    
    db.add(status_record)
    db.commit()
    db.refresh(achievement)
    
    return achievement


@router.get("/{achievement_id}/status-records", response_model=AchievementStatusRecordListResponse)
async def get_achievement_status_records(
    achievement_id: str,
    page: int = Query(1, ge=1, description="页码"),
    page_size: int = Query(20, ge=1, le=100, description="每页数量"),
    db: Session = Depends(get_db)
):
    """获取成果状态变更记录"""
    from app.models.achievement_status_record import AchievementStatusRecord
    
    achievement = db.query(Achievement).filter(Achievement.id == achievement_id).first()
    if not achievement:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Achievement not found"
        )
    
    query = db.query(AchievementStatusRecord).filter(
        AchievementStatusRecord.achievement_id == achievement_id
    ).order_by(AchievementStatusRecord.change_time.desc())
    
    total = query.count()
    records = query.offset((page - 1) * page_size).limit(page_size).all()
    
    return AchievementStatusRecordListResponse(
        total=total,
        page=page,
        page_size=page_size,
        items=records
    )


@router.put("/{achievement_id}", response_model=AchievementResponse)
async def update_achievement(
    achievement_id: str,
    achievement_data: AchievementUpdate,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    achievement = db.query(Achievement).filter(Achievement.id == achievement_id).first()
    if not achievement:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Achievement not found"
        )
    
    update_data = achievement_data.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(achievement, field, value)
    
    db.commit()
    db.refresh(achievement)
    
    return achievement


@router.delete("/{achievement_id}")
async def delete_achievement(
    achievement_id: str,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    achievement = db.query(Achievement).filter(Achievement.id == achievement_id).first()
    if not achievement:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Achievement not found"
        )
    
    db.delete(achievement)
    db.commit()
    
    return {"message": "Achievement deleted successfully"}
