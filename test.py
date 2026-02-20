import pytest
import httpx
import respx
from integrations.schemas import ProviderName
from integrations.providers import KakaoProfileApi  # 작성하신 경로에 맞게 수정

# --- Pytest Fixture 설정 ---
@pytest.fixture
def kakao_provider():
    return KakaoProfileApi()

@pytest.fixture
async def async_client():
    # 테스트용 비동기 HTTP 클라이언트 생명주기 관리
    async with httpx.AsyncClient() as client:
        yield client

# --- 테스트 케이스 ---

@pytest.mark.asyncio
async def test_kakao_profile_success(kakao_provider, async_client):
    """
    ✅ 성공 시나리오: Request 정상, 외부 API Response 정상
    """
    valid_params = {
        "user_id": "user_123",
        "kakao_token": "valid_token",
        "extra_unused_param": "ignore_me" # 이 값은 Pydantic에 의해 무시되어야 함
    }
    
    mock_response_data = {
        "id": 99999,
        "connected_at": "2026-02-20T10:00:00Z",
        "properties": {"nickname": "홍길동"},
        "unnecessary_field": "이것도 무시됨"
    }

    # respx를 사용하여 외부 API 통신 모킹 (가로채기)
    with respx.mock:
        respx.get("[https://kapi.kakao.com/v2/user/me](https://kapi.kakao.com/v2/user/me)").respond(
            status_code=200, 
            json=mock_response_data
        )

        # Provider 실행
        result = await kakao_provider.execute(async_client, valid_params)

        assert result.success is True
        assert result.provider == ProviderName.KAKAO_PROFILE
        assert result.is_validation_error is False
        assert result.data["id"] == 99999  # 검증된 데이터 확인


@pytest.mark.asyncio
async def test_request_validation_failure_missing_token(kakao_provider, async_client):
    """
    🚨 실패 시나리오 1: 필수 Request 파라미터 누락 (Fast-Fail)
    """
    # kakao_token이 누락된 잘못된 파라미터
    invalid_params = {
        "user_id": "user_123"
    }

    with respx.mock:
        # Request 단계에서 실패하므로 API 호출 자체가 발생하지 않아야 함
        mock_route = respx.get("[https://kapi.kakao.com/v2/user/me](https://kapi.kakao.com/v2/user/me)").respond(status_code=200)

        result = await kakao_provider.execute(async_client, invalid_params)

        assert result.success is False
        assert result.is_validation_error is True
        assert "kakao_token" in result.error_message # 에러 메시지에 누락된 필드명 포함 확인
        assert mock_route.called is False # 실제 API 호출이 차단되었는지 완벽 검증


@pytest.mark.asyncio
async def test_response_validation_failure_schema_changed(kakao_provider, async_client):
    """
    🚨 실패 시나리오 2: 외부 API의 응답 스펙이 예고 없이 변경됨
    """
    valid_params = {
        "user_id": "user_123",
        "kakao_token": "valid_token"
    }
    
    # 카카오 측에서 갑자기 'id' 필드를 내려주지 않거나 타입이 바뀐 상황 가정
    changed_mock_response = {
        "connected_at": "2026-02-20T10:00:00Z",
        # "id": 99999  <-- 필수 필드 누락!
    }

    with respx.mock:
        respx.get("[https://kapi.kakao.com/v2/user/me](https://kapi.kakao.com/v2/user/me)").respond(
            status_code=200, 
            json=changed_mock_response
        )

        result = await kakao_provider.execute(async_client, valid_params)

        assert result.success is False
        assert result.is_validation_error is True
        assert "id" in result.error_message # Response 모델 검증 실패 원인 확인
        assert "Field required" in result.error_message