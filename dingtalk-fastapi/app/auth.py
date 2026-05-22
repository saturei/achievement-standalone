import time
import logging
import httpx
from .config import AppConfig

logger = logging.getLogger(__name__)

TOKEN_URL = "https://api.dingtalk.com/v1.0/oauth2/accessToken"


class AuthManager:
    def __init__(self, config: AppConfig):
        self._app_key = config.dingtalk.app_key
        self._app_secret = config.dingtalk.app_secret
        self._token: str | None = None
        self._expire_at: float = 0

    async def get_token(self) -> str:
        if self._token is None or time.time() + 300 > self._expire_at:
            await self._refresh()
        return self._token

    async def _refresh(self):
        async with httpx.AsyncClient() as client:
            resp = await client.post(
                TOKEN_URL,
                json={"appKey": self._app_key, "appSecret": self._app_secret},
                headers={"Content-Type": "application/json"},
                timeout=10,
            )
            resp.raise_for_status()
            data = resp.json()
            self._token = data["accessToken"]
            expire_in = int(data.get("expireIn", 7200))
            self._expire_at = time.time() + expire_in
            logger.info("钉钉 access_token 获取成功，有效期 %d 秒", expire_in)
