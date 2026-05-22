import httpx
import logging
from typing import Any, Dict, List
from .config import AppConfig
from .auth import AuthManager

logger = logging.getLogger(__name__)

API_BASE = "https://api.dingtalk.com/v1.0/notable"


class DingTalkNotableClient:
    def __init__(self, config: AppConfig, auth: AuthManager):
        self._config = config
        self._auth = auth

    def _url(self, path: str) -> str:
        return f"{API_BASE}{path}"

    async def _request(self, method: str, path: str, **kwargs) -> dict:
        token = await self._auth.get_token()
        headers = kwargs.pop("headers", {})
        headers["x-acs-dingtalk-access-token"] = token
        async with httpx.AsyncClient(timeout=30) as client:
            resp = await client.request(method, self._url(path), headers=headers, **kwargs)
            resp.raise_for_status()
            return resp.json()

    async def list_sheets(self) -> List[dict]:
        data = await self._request(
            "GET",
            f"/bases/{self._config.base_id}/sheets?operatorId={self._config.dingtalk.operator_id}",
        )
        return data.get("value", [])

    async def get_sheet_fields(self, sheet_id: str) -> List[dict]:
        data = await self._request(
            "GET",
            f"/bases/{self._config.base_id}/sheets/{sheet_id}/fields?operatorId={self._config.dingtalk.operator_id}",
        )
        return data.get("value", [])

    async def list_records(
        self, sheet_id: str, filter_conditions: dict = None, max_results: int = 100
    ) -> List[Dict[str, Any]]:
        operator = self._config.dingtalk.operator_id
        all_records: List[Dict[str, Any]] = []
        next_token = None

        while True:
            path = (
                f"/bases/{self._config.base_id}/sheets/{sheet_id}/records/list?"
                f"operatorId={operator}"
            )
            body: dict = {"maxResults": max_results}
            if next_token:
                body["nextToken"] = next_token
            if filter_conditions:
                body["filter"] = filter_conditions

            data = await self._request(
                "POST", path, json=body,
                headers={"Content-Type": "application/json"},
            )
            records = data.get("records", [])
            all_records.extend(records)
            logger.info(
                "sheet=%s 读取 %d 条，累计 %d 条", sheet_id, len(records), len(all_records)
            )

            has_more = data.get("hasMore", False)
            next_token = data.get("nextToken", "")
            if not has_more or not next_token:
                break

        logger.info("sheet=%s 全部读取完成，共 %d 条", sheet_id, len(all_records))
        return all_records

    async def list_records_with_fields(
        self, sheet_id: str, filter_conditions: dict = None, max_results: int = 100
    ) -> List[Dict[str, Any]]:
        records = await self.list_records(sheet_id, filter_conditions, max_results)
        fields = await self.get_sheet_fields(sheet_id)
        field_map: Dict[str, str] = {}
        for f in fields:
            fid = f.get("id", "")
            fname = f.get("name", "")
            if fid and fname:
                field_map[fid] = fname

        result: List[Dict[str, Any]] = []
        for rec in records:
            row: Dict[str, Any] = {}
            raw_fields = rec.get("fields", {})
            for fid, fval in raw_fields.items():
                name = field_map.get(fid, fid)
                # 钉钉返回的字段值可能是列表（如日期、人员），取第一个
                if isinstance(fval, list) and len(fval) > 0:
                    row[name] = fval[0]
                else:
                    row[name] = fval
            # 保留记录 ID
            rec_id = rec.get("id")
            if rec_id:
                row["_recordId"] = rec_id
            result.append(row)
        return result
