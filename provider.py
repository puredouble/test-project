# --- integrations/providers.py ---
import time
import httpx
from abc import ABC, abstractmethod
from typing import Any, Dict, Type, TypeVar, Generic
from pydantic import BaseModel, ValidationError, ConfigDict, Field
from integrations.schemas import ProviderName, ExternalResponse

ReqT = TypeVar("ReqT", bound=BaseModel)
ResT = TypeVar("ResT", bound=BaseModel)

class BaseApiProvider(ABC, Generic[ReqT, ResT]):
    @property
    @abstractmethod
    def name(self) -> ProviderName: pass

    @property
    @abstractmethod
    def request_schema(self) -> Type[ReqT]: pass

    @property
    @abstractmethod
    def response_schema(self) -> Type[ResT]: pass

    @abstractmethod
    async def _fetch(self, client: httpx.AsyncClient, validated_req: ReqT) -> dict: pass

    async def execute(self, client: httpx.AsyncClient, raw_params: Dict[str, Any]) -> ExternalResponse:
        start_time = time.perf_counter()
        try:
            validated_req = self.request_schema(**raw_params)
            raw_res = await self._fetch(client, validated_req)
            validated_res = self.response_schema(**raw_res)
            
            elapsed_ms = (time.perf_counter() - start_time) * 1000
            return ExternalResponse(
                provider=self.name, success=True, 
                data=validated_res.model_dump(), response_time_ms=elapsed_ms
            )
        except ValidationError as e:
            return ExternalResponse(
                provider=self.name, success=False, error_message=str(e.errors()), 
                response_time_ms=(time.perf_counter() - start_time) * 1000, is_validation_error=True
            )
        except Exception as e:
            return ExternalResponse(
                provider=self.name, success=False, error_message=str(e), 
                response_time_ms=(time.perf_counter() - start_time) * 1000
            )

# --- 개별 API 구현체 ---
class KakaoProfileRequest(BaseModel):
    model_config = ConfigDict(extra='ignore')
    user_id: str
    kakao_token: str

class KakaoProfileResponse(BaseModel):
    model_config = ConfigDict(extra='ignore')
    id: int

class KakaoProfileApi(BaseApiProvider[KakaoProfileRequest, KakaoProfileResponse]):
    @property
    def name(self) -> ProviderName:
        return ProviderName.KAKAO_PROFILE
    @property
    def request_schema(self): return KakaoProfileRequest
    @property
    def response_schema(self): return KakaoProfileResponse

    async def _fetch(self, client: httpx.AsyncClient, validated_req: KakaoProfileRequest) -> dict:
        headers = {"Authorization": f"Bearer {validated_req.kakao_token}"}
        response = await client.get("https://kapi.kakao.com/v2/user/me", headers=headers)
        response.raise_for_status()
        return response.json()