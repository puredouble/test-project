# 외부 API 연동 아키텍처 가이드 (External API Integration)

본 문서는 우리 프로젝트에서 다수의 외부 API(20개 이상)를 안정적이고 확장성 있게 연동하기 위한 아키텍처 패턴과 개발 가이드를 정의합니다.

## 1. 설계 목표 및 원칙
* **SOLID & OCP (개방-폐쇄 원칙):** 새로운 외부 API가 추가되더라도 기존 코드를 수정하지 않고 확장이 가능해야 합니다.
* **KISS & DRY:** HTTP 통신 설정, 에러 핸들링, 로깅 등 공통 로직은 부모 클래스에서 통합 관리하여 중복 코드를 방지합니다.
* **비동기 논블로킹 (Non-blocking):** `FastAPI`의 장점을 극대화하기 위해 API 호출 및 DB 저장은 100% 비동기(`asyncio`, `httpx.AsyncClient`, `AsyncSession`)로 처리하여 이벤트 루프 블로킹을 방지합니다.
* **격리성 (Resilience):** 특정 외부 API의 장애나 지연이 우리 시스템 전체의 장애로 전파되지 않도록 타임아웃과 예외 처리를 캡슐화합니다.

---

## 2. 아키텍처 구조 (Strategy & Template Method Pattern)



* **`BaseApiProvider`**: 모든 API 구현체가 상속받아야 하는 추상 기판입니다. 공통 예외 처리와 응답 시간 측정 로직(Template Method)을 포함합니다.
* **`Concrete Providers`**: 각 외부 API(HCP 등)의 엔드포인트, 헤더, 파라미터 등 고유한 요청/응답 스펙을 정의하는 클래스입니다.
* **`ApiIntegrationManager`**: 등록된 Provider들을 관리하고, `asyncio.gather`를 통해 여러 API를 병렬로 호출하는 역할을 합니다.
* **`AsyncSession`**: ~~DB 저장 시 이벤트 루프 블로킹을 막기 위해 SQLAlchemy의 비동기 세션을 사용합니다.~~ (현재 동기 세션으로 적용.)

---

## 3. 디렉토리 구조 예상
```text
app/
 ├── api_integrations/
 │    ├── __init__.py
 │    ├── schemas.py       # API 연동 관련 Pydantic DTO 및 Enum
 │    ├── providers.py     # BaseApiProvider 및 개별 API 구현체
 │    └── manager.py       # ApiIntegrationManager (병렬 처리 및 DB 로깅)
 └── api/
      └── endpoints.py     # FastAPI 라우터 (manager 의존성 주입)
```

---

## 4. 핵심 컴포넌트 코드
### 4.1. **`schemas.py`** (데이터 모델)
```python
from pydantic import BaseModel
from typing import Any, Dict, Optional
from enum import Enum

class ProviderName(str, Enum):
    SERVICE_A = "service_a"
    SERVICE_B = "service_b"
    # [!] 신규 API 추가 시 여기에 이름 등록

class ExternalResponse(BaseModel):
    provider: ProviderName
    success: bool
    data: Optional[Dict[str, Any]] = None
    error_message: Optional[str] = None
    response_time_ms: float
```

### 4.2. **`providers.py`** (인터페이스 및 구현체)
```python
import time
import httpx
from abc import ABC, abstractmethod
from typing import Any, Dict
from integrations.schemas import ProviderName, ExternalResponse

class BaseApiProvider(ABC):
    @property
    @abstractmethod
    def name(self) -> ProviderName:
        """API 서비스 식별자 반환"""
        pass

    @abstractmethod
    async def _fetch(self, client: httpx.AsyncClient, params: Dict[str, Any]) -> Dict[str, Any]:
        """개별 API 호출 로직 (하위 클래스에서 순수 API 통신만 구현)"""
        pass

    async def execute(self, client: httpx.AsyncClient, params: Dict[str, Any]) -> ExternalResponse:
        """[Template Method] 공통 로직 (시간 측정, 예외 처리) 처리"""
        start_time = time.perf_counter()
        try:
            data = await self._fetch(client, params)
            elapsed_ms = (time.perf_counter() - start_time) * 1000
            return ExternalResponse(provider=self.name, success=True, data=data, response_time_ms=elapsed_ms)
        except httpx.TimeoutException:
            elapsed_ms = (time.perf_counter() - start_time) * 1000
            return ExternalResponse(provider=self.name, success=False, error_message="Timeout", response_time_ms=elapsed_ms)
        except Exception as e:
            elapsed_ms = (time.perf_counter() - start_time) * 1000
            return ExternalResponse(provider=self.name, success=False, error_message=str(e), response_time_ms=elapsed_ms)

# --- 신규 API 구현 예시 ---
class ServiceAProvider(BaseApiProvider):
    @property
    def name(self) -> ProviderName:
        return ProviderName.SERVICE_A

    async def _fetch(self, client: httpx.AsyncClient, params: Dict[str, Any]) -> Dict[str, Any]:
        response = await client.get("[https://api.service-a.com/data](https://api.service-a.com/data)", params=params)
        response.raise_for_status()
        return response.json()
```

### 4.3. **`manager.py`** (병렬 처리 및 DB 비동기 로깅)
```python
import asyncio
import httpx
from sqlalchemy.ext.asyncio import AsyncSession
from typing import List, Dict, Any
from integrations.schemas import ExternalResponse
from integrations.providers import BaseApiProvider

class ApiIntegrationManager:
    def __init__(self, providers: List[BaseApiProvider]):
        self._providers = {p.name: p for p in providers}

    async def fetch_all(self, client: httpx.AsyncClient, db_session: AsyncSession, params: Dict[str, Any]) -> List[ExternalResponse]:
        # 1. 병렬 API 호출 (Non-blocking)
        tasks = [provider.execute(client, params) for provider in self._providers.values()]
        results: List[ExternalResponse] = await asyncio.gather(*tasks)
        
        # 2. 결과 비동기 DB 로깅
        await self._log_results_to_db(db_session, results)
        return results

    async def _log_results_to_db(self, db_session: AsyncSession, results: List[ExternalResponse]):
        # [주의] 반드시 비동기 세션(AsyncSession)을 사용하여 이벤트 루프 블로킹 방지
        for result in results:
            # log_entry = ExternalApiLog(**result.model_dump())
            # db_session.add(log_entry)
            pass
        await db_session.commit()
```

## 5. 신규 API 추가 방법 (개발자 가이드)
새로운 API를 연동해야 할 경우 아래 3단계만 진행하세요. 기존 코드는 수정하지 않습니다.

1. Enum 추가: `schemas.py`의 `ProviderName`에 신규 서비스 이름 추가

2. 클래스 생성: `providers.py`에 `BaseApiProvider`를 상속받는 신규 클래스 생성 (_fetch 메서드 구현)

3. Manager 등록: FastAPI의 Dependency Injection(의존성 주입) 설정에서 `ApiIntegrationManager` 초기화 시 신규 클래스 인스턴스 주입

## 6. 주의 사항 (반드시 필독)
- ~~절대 동기 DB 세션 사용 금지: Manager에서 API 결과를 저장할 때 동기 세션(sqlalchemy.orm.Session)을 쓰면 워커의 이벤트 루프가 멈춰 심각한 성능 저하가 발생합니다. 반드시 AsyncSession을 사용하세요.~~ 
  - (일단 동기 DB 세션 사용 하는 대신 thread pool 활용하여 병렬 진행)

- 커넥션 재사용: httpx.AsyncClient는 매 요청마다 생성하지 않습니다. FastAPI의 lifespan을 활용하여 앱 시작 시 1개만 생성하고 의존성 주입으로 받아 사용합니다.