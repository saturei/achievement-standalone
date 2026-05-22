from dataclasses import dataclass, field
from typing import List


@dataclass
class DingTalkConfig:
    app_key: str
    app_secret: str
    operator_id: str


@dataclass
class TableConfig:
    id: str
    name: str
    description: str = ""


@dataclass
class AppConfig:
    dingtalk: DingTalkConfig
    base_id: str
    tables: List[TableConfig] = field(default_factory=list)
