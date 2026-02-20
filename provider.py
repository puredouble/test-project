import time
import httpx
from abc import ABC, abstractmethod
from typing import Any, Dict, Type, TypeVar, Generic
from pydantic import BaseModel, ValidationError
from schema import ProviderName, ExternalResponse

# 제네릭 타입 변수 선언 (요청/응답 스키마용)
ReqT = TypeVar("ReqT", bound=BaseModel)
ResT = TypeVar("ResT", bound=BaseModel)

class BaseApiProvider(ABC, Generic[ReqT, ResT]):
    @property
    @abstractmethod
    def name(self) -> ProviderName:
        pass

    @property
    @abstractmethod
    def request_schema(self) -> Type[ReqT]:
        """요청 파라미터 검증용 Pydantic 모델 반환"""
        pass

    @property
    @abstractmethod
    def response_schema(self) -> Type[ResT]:
        """API 응답 데이터 검증용 Pydantic 모델 반환"""
        pass

    @abstractmethod
    async def _fetch(self, client: httpx.AsyncClient, validated_req: ReqT) -> dict:
        """
        검증이 완료된 Pydantic 객체(validated_req)를 받아 실제 API 통신을 수행합니다.
        """
        pass

    async def execute(self, client: httpx.AsyncClient, raw_params: Dict[str, Any]) -> ExternalResponse:
        """[Template Method] Request 검증 -> API 호출 -> Response 검증"""
        start_time = time.perf_counter()
        elapsed_ms = 0.0
        
        try:
            # 1. Request Validation (필요한 데이터만 추출 및 타입 검사)
            validated_req = self.request_schema(**raw_params)
            
            # 2. API Call
            raw_response_data = await self._fetch(client, validated_req)
            
            # 3. Response Validation (외부 API 응답 스펙 검사)
            validated_res = self.response_schema(**raw_response_data)
            
            elapsed_ms = (time.perf_counter() - start_time) * 1000
            return ExternalResponse(
                provider=self.name, 
                success=True, 
                data=validated_res.model_dump(), 
                response_time_ms=elapsed_ms
            )
            
        except ValidationError as e:
            elapsed_ms = (time.perf_counter() - start_time) * 1000
            # Request 또는 Response 스키마 검증 실패 시
            return ExternalResponse(
                provider=self.name, 
                success=False, 
                error_message=f"Validation Error: {e.errors()}", 
                response_time_ms=elapsed_ms,
                is_validation_error=True
            )
        except httpx.HTTPError as e:
            elapsed_ms = (time.perf_counter() - start_time) * 1000
            return ExternalResponse(
                provider=self.name, success=False, error_message=f"HTTP Error: {str(e)}", response_time_ms=elapsed_ms
            )
        except Exception as e:
            elapsed_ms = (time.perf_counter() - start_time) * 1000
            return ExternalResponse(
                provider=self.name, success=False, error_message=f"Internal Error: {str(e)}", response_time_ms=elapsed_ms
            )