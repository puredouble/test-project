import logging
import httpx
from sqlalchemy.ext.asyncio import AsyncSession
from integrations.providers import BaseApiProvider
from integrations.schemas import ExternalResponse
from core.models import ExternalApiLog

logger = logging.getLogger(__name__)

class DbLoggingProviderProxy:
    """
    기존 Provider를 감싸서 API 호출 후 자동으로 DB에 로깅하는 프록시 클래스
    """
    def __init__(self, target_provider: BaseApiProvider, db_session: AsyncSession):
        self._target = target_provider
        self._db_session = db_session

    @property
    def name(self):
        # 기존 Provider의 이름을 그대로 모방 (Duck Typing)
        return self._target.name

    async def execute(self, client: httpx.AsyncClient, params: dict) -> ExternalResponse:
        # 1. 실제 Provider에게 API 호출 위임
        result = await self._target.execute(client, params)
        
        # 2. 결과 가로채서 DB 로깅 (에러 격리)
        try:
            log_entry = ExternalApiLog(
                provider=result.provider,
                success=result.success,
                is_validation_error=result.is_validation_error,
                response_time_ms=result.response_time_ms,
                data=result.data,
                error_message=result.error_message
            )
            self._db_session.add(log_entry)
            await self._db_session.commit()
        except Exception as e:
            await self._db_session.rollback()
            logger.error(f"Provider({self.name}) DB 로깅 실패: {str(e)}")
            
        # 3. 결과 반환
        return result