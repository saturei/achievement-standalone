import logging
from fastapi import FastAPI, HTTPException, Query
from pydantic import BaseModel, Field
from typing import Any, Dict, List, Optional

from .config_loader import load_config
from .auth import AuthManager
from .client import DingTalkNotableClient

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(name)s: %(message)s")
logger = logging.getLogger(__name__)

config = load_config()
auth = AuthManager(config)
client = DingTalkNotableClient(config, auth)

app = FastAPI(
    title="DingTalk AI Table API",
    description="连接钉钉多维表格（notable API），提供 REST 接口读取表格数据和字段结构",
    version="1.0.0",
)


class TableSummary(BaseModel):
    id: str
    name: str
    description: str


class FilterCondition(BaseModel):
    field: str
    operator: str = Field(default="equal", description="equal / notEqual / contain / notContain / greater / less / empty / notEmpty")
    value: Any = None


class QueryRequest(BaseModel):
    conditions: Optional[List[FilterCondition]] = None
    max_results: int = Field(default=100, ge=1, le=100)


@app.on_event("startup")
async def startup():
    logger.info("服务启动，预取 access_token ...")
    await auth.get_token()
    logger.info("钉钉连接就绪")


@app.get("/api/table/list", response_model=List[TableSummary])
async def list_tables():
    """列出配置文件中所有表格"""
    return [
        TableSummary(id=t.id, name=t.name, description=t.description)
        for t in config.tables
    ]


@app.get("/api/table/{table_id}/data")
async def get_table_data(
    table_id: str,
    max_results: int = Query(default=100, ge=1, le=100),
):
    """读取指定表格的全部数据，字段名使用中文"""
    table = _find_table(table_id)
    try:
        rows = await client.list_records_with_fields(table_id, max_results=max_results)
        return {
            "table": {"id": table.id, "name": table.name, "description": table.description},
            "total": len(rows),
            "data": rows,
        }
    except Exception as e:
        logger.exception("读取表格数据失败: %s", e)
        raise HTTPException(status_code=502, detail=f"钉钉API调用失败: {e}")


@app.get("/api/table/{table_id}/schema")
async def get_table_schema(table_id: str):
    """读取指定表格的字段结构"""
    table = _find_table(table_id)
    try:
        fields = await client.get_sheet_fields(table_id)
        return {
            "table": {"id": table.id, "name": table.name, "description": table.description},
            "fields": [
                {
                    "id": f.get("id"),
                    "name": f.get("name"),
                    "type": f.get("type"),
                }
                for f in fields
            ],
        }
    except Exception as e:
        logger.exception("读取字段结构失败: %s", e)
        raise HTTPException(status_code=502, detail=f"钉钉API调用失败: {e}")


@app.post("/api/table/{table_id}/query")
async def query_table(table_id: str, req: QueryRequest):
    """带筛选条件查询表格数据"""
    table = _find_table(table_id)
    try:
        filter_body = None
        if req.conditions and len(req.conditions) > 0:
            conds = []
            for c in req.conditions:
                cond = {"field": c.field, "operator": c.operator}
                # 钉钉API要求embed/notEmpty除外, 其余operator的value必须是数组
                if c.value is not None:
                    cond["value"] = [c.value] if not isinstance(c.value, list) else c.value
                else:
                    cond["value"] = []
                conds.append(cond)
            filter_body = {"conditions": conds}
            if len(conds) > 1:
                filter_body["combination"] = "and"

        rows = await client.list_records_with_fields(table_id, filter_conditions=filter_body, max_results=req.max_results)
        return {
            "table": {"id": table.id, "name": table.name, "description": table.description},
            "filter": filter_body,
            "total": len(rows),
            "data": rows,
        }
    except Exception as e:
        logger.exception("查询表格数据失败: %s", e)
        raise HTTPException(status_code=502, detail=f"钉钉API调用失败: {e}")


def _find_table(table_id: str):
    for t in config.tables:
        if t.id == table_id:
            return t
    raise HTTPException(status_code=404, detail=f"表格未配置: {table_id}")
