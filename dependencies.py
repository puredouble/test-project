from fastapi import Depends
from sqlalchemy.ext.asyncio import AsyncSession
from api.dependencies import get_db_session
from integrations.providers import KakaoProfileApi
from integrations.proxies import DbLoggingProviderProxy

def get_kakao_profile_provider(
    db_session: AsyncSession = Depends(get_db_session)
) -> DbLoggingProviderProxy:
    """
    라우터에게 순수 Provider 대신, DB 로깅 기능이 장착된 Proxy를 주입합니다.
    """
    pure_provider = KakaoProfileApi()
    return DbLoggingProviderProxy(target_provider=pure_provider, db_session=db_session)