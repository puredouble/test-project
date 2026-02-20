# --- api/router.py ---
from fastapi import APIRouter, BackgroundTasks, status
from integrations.schemas import ApiTaskPayload, ProviderName
from integrations.worker import task_queue

router = APIRouter(prefix="/api/v1/jobs", tags=["Jobs"])

@router.post("/kakao-profile", status_code=status.HTTP_202_ACCEPTED)
async def enqueue_kakao_profile(user_id: str, kakao_token: str):
    """
    요청을 즉시 처리하지 않고, Queue에 적재한 뒤 클라이언트에게 바로 응답합니다.
    누락 없이 백그라운드 워커가 순차적으로 처리합니다.
    """
    payload = ApiTaskPayload(
        provider_name=ProviderName.KAKAO_PROFILE,
        params={"user_id": user_id, "kakao_token": kakao_token}
    )
    
    # 작업 큐에 푸시 (Non-blocking)
    await task_queue.put(payload)
    
    return {"message": "Task has been enqueued.", "queue_size": task_queue.qsize()}


# --- main.py ---
import asyncio
from contextlib import asynccontextmanager
from fastapi import FastAPI
import httpx

from api.router import router as jobs_router
from integrations.worker import api_consumer_worker

@asynccontextmanager
async def lifespan(app: FastAPI):
    # 1. 전역 HTTP 클라이언트 생성
    http_client = httpx.AsyncClient(timeout=10.0, limits=httpx.Limits(max_connections=100))
    app.state.http_client = http_client
    
    # 2. 백그라운드 워커 실행 (예: 3개의 워커를 띄워 동시 처리량 확보)
    workers = [
        asyncio.create_task(api_consumer_worker(http_client)) 
        for _ in range(3)
    ]
    
    yield
    
    # 3. 앱 종료 시 자원 정리 (워커 취소 및 클라이언트 닫기)
    for w in workers:
        w.cancel()
    await http_client.aclose()

app = FastAPI(lifespan=lifespan)
app.include_router(jobs_router)


from fastapi import APIRouter, status
from integrations.schemas import ApiTaskPayload, ProviderName
from integrations.worker import task_queue

router = APIRouter(prefix="/api/v1/jobs", tags=["Jobs"])

@router.post("/kakao-message", status_code=status.HTTP_202_ACCEPTED)
async def send_kakao_message_async(
    user_id: str, 
    message: str,
    with_retry: bool = False  # 클라이언트가 Query Parameter 등으로 재시도 여부 결정
):
    # 큐에 작업 적재 시 should_retry 옵션 부여
    payload = ApiTaskPayload(
        provider_name=ProviderName.KAKAO_SEND_MSG,
        params={"user_id": user_id, "message": message},
        should_retry=with_retry  # ✨ 이 파라미터 하나로 워커가 알아서 3회 재시도 처리
    )
    
    await task_queue.put(payload)
    
    return {"message": "Task queued.", "retry_enabled": with_retry}