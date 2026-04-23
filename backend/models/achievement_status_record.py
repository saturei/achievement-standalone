from sqlalchemy import Column, String, DateTime, Text, ForeignKey, func
from sqlalchemy.orm import relationship
from app.db.database import Base


class AchievementStatusRecord(Base):
    __tablename__ = "achievement_status_records"

    id = Column(String(50), primary_key=True, index=True)
    achievement_id = Column(String(50), ForeignKey("achievements.id"), nullable=False, index=True)
    achievement_name = Column(String(200), nullable=False)
    from_status = Column(String(50), nullable=False, comment="变更前状态")
    to_status = Column(String(50), nullable=False, comment="变更后状态")
    change_type = Column(String(50), nullable=False, comment="变更类型：上架/下架")
    change_reason = Column(Text, comment="变更原因")
    operator = Column(String(100), comment="操作人")
    change_time = Column(DateTime(timezone=True), server_default=func.now())
    created_at = Column(DateTime(timezone=True), server_default=func.now())

    def __repr__(self):
        return f"<AchievementStatusRecord(id={self.id}, achievement_id={self.achievement_id}, change_type={self.change_type})>"

    def to_dict(self):
        return {
            "id": self.id,
            "achievement_id": self.achievement_id,
            "achievement_name": self.achievement_name,
            "from_status": self.from_status,
            "to_status": self.to_status,
            "change_type": self.change_type,
            "change_reason": self.change_reason,
            "operator": self.operator,
            "change_time": self.change_time.isoformat() if self.change_time else None,
            "created_at": self.created_at.isoformat() if self.created_at else None,
        }
