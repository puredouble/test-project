# 🔌 FastAPI 라우터 및 의존성 주입 (Endpoint & DI)

본 문서는 설계된 외부 API 연동 아키텍처(`Provider`, `Manager`)를 FastAPI 앱에 연결하여 실제 HTTP 요청을 처리하는 엔드포인트 구현 가이드입니다.

## 1. `dependencies.py` (의존성 주입 설정)
FastAPI의 `Depends`에 사용될 의존성 함수들을 모아둡니다.

```python
from typing import AsyncGenerator
from fastapi import Request
import httpx
from sqlalchemy.ext.asyncio import AsyncSession

# 프로젝트의 DB 설정 및 Manager 경로에 맞게 임포트하세요.
from core.database import AsyncSessionLocal 
from integrations.manager import ApiIntegrationManager
from integrations.providers import KakaoProfileApi  # 필요한 API Provider들 임포트

async def get_db_session() -> AsyncGenerator[AsyncSession, None]:
    """비동기 DB 세션을 생성하고 요청 종료 시 안전하게 닫습니다."""
    async with AsyncSessionLocal() as session:
        yield session

def get_http_client(request: Request) -> httpx.AsyncClient:
    """
    FastAPI app state에 저장된 전역 HTTP 클라이언트를 반환합니다.
    (매번 클라이언트를 생성하지 않고 Connection Pool을 재사용하여 성능을 극대화합니다)
    """
    return request.app.state.http_client

def get_api_manager() -> ApiIntegrationManager:
    """
    Manager 인스턴스를 생성하여 반환합니다.
    새로운 API가 추가되면 여기에 Provider 인스턴스만 리스트에 추가하면 됩니다.
    """
    providers = [
        KakaoProfileApi(),
        # NaverProfileApi(), 
        # GoogleDataApi() ... 등 신규 Provider 추가 영역
    ]
    return ApiIntegrationManager(providers)
```

## 2. `router.py` (API 엔드포인트)
클라이언트의 요청을 받아 검증하고 Manager에게 작업을 위임합니다.

```python
from fastapi import APIRouter, Depends
from pydantic import BaseModel, Field
from typing import List
import httpx
from sqlalchemy.ext.asyncio import AsyncSession

from api.dependencies import get_api_manager, get_db_session, get_http_client
from integrations.manager import ApiIntegrationManager
from integrations.schemas import ExternalResponse

router = APIRouter(prefix="/api/v1/integrations", tags=["External APIs"])

# 클라이언트로부터 받을 통합 요청 Body 스키마
class SyncExternalDataRequest(BaseModel):
    user_id: str = Field(..., description="내부 서비스 유저 ID")
    kakao_token: str = Field(..., description="카카오 연동을 위한 엑세스 토큰")
    # 향후 다른 API에 필요한 토큰이나 데이터가 있다면 여기에 추가

@router.post("/sync", response_model=List[ExternalResponse])
async def sync_external_data(
    request_data: SyncExternalDataRequest,
    manager: ApiIntegrationManager = Depends(get_api_manager),
    client: httpx.AsyncClient = Depends(get_http_client),
    db_session: AsyncSession = Depends(get_db_session)
):
    """
    클라이언트의 요청을 받아 등록된 20여 개의 외부 API를 병렬로 호출하고, 
    결과를 DB에 로깅한 후 성공/실패 여부를 통합하여 반환합니다.
    """
    # Pydantic 모델을 딕셔너리로 변환하여 Manager에게 전달
    params = request_data.model_dump()
    
    # Manager 내부에서 각 Provider가 필요한 데이터만 추출하여 API 호출 (병렬 처리)
    results = await manager.fetch_all(client, db_session, params)
    
    return results
```

## 3. `main.py` (App 진입점 및 Lifespan 설정)
HTTP 클라이언트의 생성과 소멸을 애플리케이션의 시작/종료 주기(`lifespan`)와 동기화합니다.

```python
from contextlib import asynccontextmanager
from fastapi import FastAPI
import httpx
from api.router import router as integration_router

@asynccontextmanager
async def lifespan(app: FastAPI):
    # [Start-up] 애플리케이션 시작 시: HTTP Connection Pool 생성
    # timeout 설정, max_connections 등 최적화 옵션을 여기서 부여합니다.
    app.state.http_client = httpx.AsyncClient(
        timeout=httpx.Timeout(10.0), 
        limits=httpx.Limits(max_keepalive_connections=50, max_connections=100)
    )
    yield
    
    # [Shut-down] 애플리케이션 종료 시: 남아있는 커넥션을 안전하게 닫음
    await app.state.http_client.aclose()

# FastAPI 인스턴스 생성
app = FastAPI(
    title="External API Integration Service",
    lifespan=lifespan
)

# 라우터 등록
app.include_router(integration_router)
```