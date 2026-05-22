import yaml
from pathlib import Path
from .config import AppConfig, DingTalkConfig, TableConfig


def load_config(path: str = None) -> AppConfig:
    if path is None:
        path = Path(__file__).parent.parent / "config.yaml"
    with open(path, "r", encoding="utf-8") as f:
        raw = yaml.safe_load(f)

    dt = raw.get("dingtalk", {})
    dingtalk = DingTalkConfig(
        app_key=dt.get("app_key", ""),
        app_secret=dt.get("app_secret", ""),
        operator_id=dt.get("operator_id", ""),
    )

    tables = []
    for t in raw.get("tables", []):
        tables.append(TableConfig(
            id=t.get("id", ""),
            name=t.get("name", ""),
            description=t.get("description", ""),
        ))

    return AppConfig(
        dingtalk=dingtalk,
        base_id=raw.get("base_id", ""),
        tables=tables,
    )
