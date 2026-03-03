package com.project.integration.api.service.provider;

import com.project.integration.api.dto.ApiResponseDto;
import com.project.integration.api.enums.ProviderName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractApiProvider implements ApiProvider {

    // Timeout이 설정된 RestTemplate을 주입받음 (Config에서 빈으로 등록 필요)
    protected final RestTemplate restTemplate;

    @Override
    public ApiResponseDto execute(Map<String, Object> params) {
        long start = System.currentTimeMillis();
        try {
            // 하위 클래스에서 구현한 실제 HTTP 통신 로직 호출
            Map<String, Object> result = fetch(params);
            
            return buildResponse(true, result, null, start);
            
        } catch (Exception e) {
            log.error("[{}] API 연동 실패: {}", getProviderName().name(), e.getMessage());
            return buildResponse(false, null, e.getMessage(), start);
        }
    }

    /**
     * 개별 API 공급자 클래스에서 구현할 실제 통신 로직
     * @param params 요청 파라미터
     * @return API 응답 데이터 (Map 형태)
     */
    protected abstract Map<String, Object> fetch(Map<String, Object> params) throws Exception;

    // 공통 응답 포맷 생성
    private ApiResponseDto buildResponse(boolean success, Map<String, Object> data, String error, long start) {
        return ApiResponseDto.builder()
                .providerName(getProviderName().name())
                .success(success)
                .data(data)
                .errorMessage(error)
                .responseTimeMs(System.currentTimeMillis() - start)
                .build();
    }
}