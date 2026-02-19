import time
import httpx
from abc import ABC, abstractmethod
from typing import Any, Dict
from base_api_schema import ProviderName, ExternalResponse

class BaseApiProvider(ABC):
    @property
    @abstractmethod
    def name(self) -> ProviderName:
        """API 서비스 식별자 반환"""
        pass

    @abstractmethod
    async def _fetch(self, client: httpx.AsyncClient, params: Dict[str, Any]) -> Dict[str, Any]:
        """개별 API 호출 로직 (하위 클래스에서 구현)"""
        pass

    async def execute(self, client: httpx.AsyncClient, params: Dict[str, Any]) -> ExternalResponse:
        """
        [Template Method]
        공통 예외 처리, 시간 측정 로직을 담당합니다.
        """
        start_time = time.perf_counter()
        try:
            # 개별 API 로직 호출
            data = await self._fetch(client, params)
            elapsed_ms = (time.perf_counter() - start_time) * 1000
            
            return ExternalResponse(
                provider=self.name, 
                success=True, 
                data=data, 
                response_time_ms=elapsed_ms
            )
            
        except httpx.TimeoutException:
            elapsed_ms = (time.perf_counter() - start_time) * 1000
            return ExternalResponse(
                provider=self.name, success=False, error_message="Request Timeout", response_time_ms=elapsed_ms
            )
        except httpx.HTTPStatusError as e:
            elapsed_ms = (time.perf_counter() - start_time) * 1000
            return ExternalResponse(
                provider=self.name, success=False, error_message=f"HTTP Error: {e.response.status_code}", response_time_ms=elapsed_ms
            )
        except Exception as e:
            elapsed_ms = (time.perf_counter() - start_time) * 1000
            return ExternalResponse(
                provider=self.name, success=False, error_message=f"Internal Error: {str(e)}", response_time_ms=elapsed_ms
            )

# --- 개별 API 구현체 예시 ---

class ServiceCProvider(BaseApiProvider):
    @property
    def name(self) -> ProviderName:
        return ProviderName.SERVICE_C

    async def _fetch(self, client: httpx.AsyncClient, params: Dict[str, Any]) -> Dict[str, Any]:
        headers = {"Authorization": f"Bearer {params.get('token')}"}
        response = await client.get("https://api.service-c.com/v2/users", headers=headers)
        response.raise_for_status()
        return response.json()