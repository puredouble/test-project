import asyncio
import httpx
from sqlalchemy.ext.asyncio import AsyncSession
from typing import List, Dict, Any
from schema import ExternalResponse
from provider import BaseApiProvider

class ApiIntegrationManager:
    def __init__(self, providers: List[BaseApiProvider]):
        self._providers = {p.name: p for p in providers}

    ## DB 비동기 방식
    # async def fetch_all(
    #     self, 
    #     client: httpx.AsyncClient, 
    #     db_session: AsyncSession, 
    #     params: Dict[str, Any]
    # ) -> List[ExternalResponse]:
    #     """
    #     등록된 모든 Provider를 병렬로 호출하고, 결과를 반환합니다.
    #     """
    #     tasks = [
    #         provider.execute(client, params) 
    #         for provider in self._providers.values()
    #     ]
        
    #     # asyncio.gather를 이용한 병렬 Non-blocking 호출
    #     results: List[ExternalResponse] = await asyncio.gather(*tasks)
        
    #     # TODO: 결과를 순회하며 DB Logging 로직 수행 (db_session 활용)
    #     # await self._log_results_to_db(db_session, results)
        
    #     return results
    
    
    # DB 동기 방식
    def _sync_db_save(self, db_session: Session, results: List[ExternalResponse]):
        """동기 방식의 DB 저장 로직 (일반 def)"""
        for result in results:
            log_entry = ExternalApiLog(**result.dict())
            db_session.add(log_entry)
        db_session.commit()

    async def fetch_all(self, client: httpx.AsyncClient, db_session: Session, params: Dict[str, Any]):
        # 1. 외부 API 비동기 호출
        tasks = [provider.execute(client, params) for provider in self._providers.values()]
        results = await asyncio.gather(*tasks)
        
        # 2. 동기 DB 작업을 Thread Pool로 넘겨서 실행 (이벤트 루프 블로킹 방지)
        await run_in_threadpool(self._sync_db_save, db_session, results)
        
        return results