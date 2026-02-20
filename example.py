from pydantic import Field, ConfigDict

# --- 1. DTO 정의 (Request / Response) ---
class KakaoProfileRequest(BaseModel):
    # Manager에서 넘겨주는 무수한 파라미터 중 이 API에 필요한 것만 추출하고 나머지는 무시
    model_config = ConfigDict(extra='ignore') 
    
    user_id: str = Field(..., description="내부 시스템 유저 ID")
    kakao_token: str = Field(..., description="카카오 API 호출용 엑세스 토큰")

class KakaoProfileResponse(BaseModel):
    # 외부 API가 응답하는 필드 중 우리 시스템에서 사용할 데이터만 정의 및 타입 검증
    model_config = ConfigDict(extra='ignore')
    
    id: int
    connected_at: str
    properties: Dict[str, str] = Field(default_factory=dict)

# --- 2. Provider 구현 ---
class KakaoProfileApi(BaseApiProvider[KakaoProfileRequest, KakaoProfileResponse]):
    @property
    def name(self) -> ProviderName:
        return ProviderName.KAKAO_PROFILE

    @property
    def request_schema(self) -> Type[KakaoProfileRequest]:
        return KakaoProfileRequest

    @property
    def response_schema(self) -> Type[KakaoProfileResponse]:
        return KakaoProfileResponse

    async def _fetch(self, client: httpx.AsyncClient, validated_req: KakaoProfileRequest) -> dict:
        # Request 스키마 검증을 통과한 안전한 객체를 사용
        headers = {"Authorization": f"Bearer {validated_req.kakao_token}"}
        
        # 실제 API 호출
        response = await client.get("[https://kapi.kakao.com/v2/user/me](https://kapi.kakao.com/v2/user/me)", headers=headers)
        response.raise_for_status()
        
        # 반환된 Dict는 부모 클래스(execute)에서 response_schema로 2차 검증됨
        return response.json()