from pydantic import BaseModel
from typing import Any, Dict, Optional
from enum import Enum

class ProviderName(str, Enum):
    SERVICE_A = "service_a"
    SERVICE_B = "service_b"
    SERVICE_C = "service_c"
    # 추가되는 API 식별자 기입

class ExternalResponse(BaseModel):
    provider: ProviderName
    success: bool
    data: Optional[Dict[str, Any]] = None
    error_message: Optional[str] = None
    response_time_ms: float

# Queue에 담을 작업(Task)의 명세서
class ApiTaskPayload(BaseModel):
    provider_name: ProviderName
    params: Dict[str, Any]
    should_retry: bool = False  # ✨ 재시도 여부 파라미터 (기본값: 하지 않음)