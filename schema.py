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