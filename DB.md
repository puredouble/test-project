# 🗄️ 외부 API 로깅 데이터베이스 구조 및 저장 로직

본 문서는 외부 API 호출 이력을 저장하는 SQLAlchemy 2.0 비동기 모델(`ExternalApiLog`)과 매니저 내부의 DB 저장 로직을 정의합니다.

## 1. `models.py` (SQLAlchemy 2.0 모델 정의)
최신 SQLAlchemy 2.0의 타입 힌트 기반 선언(`Mapped`, `mapped_column`) 방식을 사용하여 명확하고 안전한 데이터베이스 모델을 구성합니다.

```python
from datetime import datetime
from typing import Any, Dict, Optional
from sqlalchemy import Integer, String, Boolean, Float, JSON, Text, DateTime
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column
from sqlalchemy.sql import func

class Base(DeclarativeBase):
    pass

class ExternalApiLog(Base):
    __tablename__ = "external_api_logs"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    
    # API 제공자 식별자 (인덱스를 걸어 나중에 통계 추출 시 성능 향상)
    provider: Mapped[str] = mapped_column(String(50), index=True, nullable=False)
    
    # 성공 여부 및 유효성 검사 실패 여부
    success: Mapped[bool] = mapped_column(Boolean, nullable=False)
    is_validation_error: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    
    # 응답 속도 (병목 구간 탐지용)
    response_time_ms: Mapped[float] = mapped_column(Float, nullable=False)
    
    # JSON 형태의 응답 데이터 및 에러 메시지
    data: Mapped[Optional[Dict[str, Any]]] = mapped_column(JSON, nullable=True)
    error_message: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    
    # 생성 일시
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())

    def __repr__(self):
        return f"<ExternalApiLog(provider={self.provider}, success={self.success}, time={self.response_time_ms}ms)>"
```

## 2. `manager.py` (Manager의 비동기 저장 로직 업데이트)
`ApiIntegrationManager`에 누락되었던 DB 저장 메서드를 완성합니다.

```python
import asyncio
import httpx
import logging
from typing import List, Dict, Any
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.exc import SQLAlchemyError

from integrations.schemas import ExternalResponse
from integrations.providers import BaseApiProvider
from core.models import ExternalApiLog  # 방금 생성한 모델 임포트

logger = logging.getLogger(__name__)

class ApiIntegrationManager:
    def __init__(self, providers: List[BaseApiProvider]):
        self._providers = {p.name: p for p in providers}

    async def fetch_all(
        self, 
        client: httpx.AsyncClient, 
        db_session: AsyncSession, 
        params: Dict[str, Any]
    ) -> List[ExternalResponse]:
        # 1. 병렬 API 호출 (Non-blocking)
        tasks = [provider.execute(client, params) for provider in self._providers.values()]
        results: List[ExternalResponse] = await asyncio.gather(*tasks)
        
        # 2. 결과 비동기 DB 로깅 (Background처럼 처리)
        await self._log_results_to_db(db_session, results)
        
        return results

    async def _log_results_to_db(self, db_session: AsyncSession, results: List[ExternalResponse]) -> None:
        """
        API 호출 결과를 DB에 일괄 저장합니다.
        DB 저장 중 에러가 발생해도 메인 비즈니스 로직(클라이언트 응답)이 실패하지 않도록 격리합니다.
        """
        try:
            for result in results:
                # Pydantic DTO -> SQLAlchemy Model 변환
                log_entry = ExternalApiLog(
                    provider=result.provider,
                    success=result.success,
                    is_validation_error=result.is_validation_error,
                    response_time_ms=result.response_time_ms,
                    data=result.data,
                    error_message=result.error_message
                )
                db_session.add(log_entry)
            
            # 이벤트 루프를 양보하며 DB에 비동기 커밋
            await db_session.commit()
            
        except SQLAlchemyError as db_err:
            # DB 저장 실패 시 롤백 및 에러 로깅 처리 (클라이언트에게 에러를 전파하지 않음)
            await db_session.rollback()
            logger.error(f"Failed to save API logs to database: {str(db_err)}")
        except Exception as e:
            await db_session.rollback()
            logger.error(f"Unexpected error during API logging: {str(e)}")
```