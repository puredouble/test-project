import asyncio
import logging
import httpx
from core.database import AsyncSessionLocal
from integrations.schemas import ApiTaskPayload
from integrations.providers import KakaoProfileApi
from integrations.proxies import DbLoggingProviderProxy

logger = logging.getLogger(__name__)

task_queue: asyncio.Queue[ApiTaskPayload] = asyncio.Queue()

PROVIDER_REGISTRY = {
    KakaoProfileApi().name: KakaoProfileApi(),
}

async def api_consumer_worker(client: httpx.AsyncClient):
    """
    백그라운드에서 끊임없이 큐를 확인하고 작업을 처리하는 워커입니다.
    """
    logger.info("Background API Worker started.")
    
    while True:
        payload: ApiTaskPayload = await task_queue.get()
        
        try:
            pure_provider = PROVIDER_REGISTRY.get(payload.provider_name)
            if not pure_provider:
                logger.error(f"Provider not found: {payload.provider_name}")
                continue

            # ✨ 재시도 여부에 따른 최대 실행 횟수 설정 (최초 1회 + 재시도 3회 = 총 4회)
            max_attempts = 4 if payload.should_retry else 1
            
            for attempt in range(1, max_attempts + 1):
                # 매 시도마다 독립적인 DB 세션을 열어 로깅 (성공/실패 이력이 모두 남음)
                async with AsyncSessionLocal() as db_session:
                    proxy_provider = DbLoggingProviderProxy(pure_provider, db_session)
                    
                    # API 실행
                    result = await proxy_provider.execute(client, payload.params)
                    
                    # 1) 성공했거나,
                    # 2) 유효성 검사 실패(파라미터 오류 등은 재시도해도 실패하므로)인 경우 루프 즉시 탈출
                    if result.success or result.is_validation_error:
                        break 
                    
                    # 실패했고, 아직 재시도 기회가 남은 경우
                    if attempt < max_attempts:
                        # 2초, 4초, 8초 간격으로 대기 (Exponential Backoff)
                        sleep_time = 2 ** attempt 
                        logger.warning(
                            f"API 호출 실패 ({result.error_message}). "
                            f"{sleep_time}초 후 재시도 합니다... (시도 횟수: {attempt}/3)"
                        )
                        await asyncio.sleep(sleep_time)
                    else:
                        logger.error(f"API 호출 3회 재시도 최종 실패: {payload.provider_name}")
                        
        except asyncio.CancelledError:
            break
        except Exception as e:
            logger.error(f"Worker encountered a critical error: {e}")
        finally:
            task_queue.task_done()